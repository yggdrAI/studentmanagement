package com.sms.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.dto.publication.PublicationCreateRequest;
import com.sms.dto.publication.PublicationResponse;
import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;
import com.sms.model.AcademicPublication;
import com.sms.model.PublicationAudience;
import com.sms.model.PublicationScope;
import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.TeacherAssignment;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;
import com.sms.repository.AcademicPublicationRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherAssignmentRepository;
import com.sms.repository.TeacherRepository;

@Service
public class AcademicPublicationService {

    private final AcademicPublicationRepository academicPublicationRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final AcademicClassRepository academicClassRepository;
    private final AcademicBatchRepository academicBatchRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public AcademicPublicationService(AcademicPublicationRepository academicPublicationRepository,
                                      StudentRepository studentRepository,
                                      TeacherRepository teacherRepository,
                                      TeacherAssignmentRepository teacherAssignmentRepository,
                                      AcademicClassRepository academicClassRepository,
                                      AcademicBatchRepository academicBatchRepository,
                                      DashboardService dashboardService,
                                      ObjectMapper objectMapper) {
        this.academicPublicationRepository = academicPublicationRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.academicClassRepository = academicClassRepository;
        this.academicBatchRepository = academicBatchRepository;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<PublicationResponse> publish(String actorUsername, PublicationCreateRequest request) {
        PublicationCreateRequest safeRequest = request == null ? new PublicationCreateRequest() : request;
        String title = normalizeRequired(safeRequest.getTitle(), "Title is required");
        String actor = normalizeBlank(actorUsername, "ADMIN");

        List<AcademicPublication> publications = buildPublications(actor, title, safeRequest);
        List<AcademicPublication> saved = academicPublicationRepository.saveAll(publications);
        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicationResponse> getAdminPublications() {
        return academicPublicationRepository.findAllByOrderByPublishedAtDescIdDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicationResponse> getStudentPublications(String username) {
        Student student = dashboardService.resolveStudentByUsername(username);
        return academicPublicationRepository.findByPublishedTrueOrderByPublishedAtDescIdDesc().stream()
            .filter(publication -> supportsAudience(publication, PublicationAudience.STUDENT))
            .filter(publication -> matchesStudent(publication, student))
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicationResponse> getTeacherPublications(String username) {
        Teacher teacher = dashboardService.resolveTeacherByUsername(username);
        Set<String> classGroups = new LinkedHashSet<>();
        Set<String> batchKeys = new LinkedHashSet<>();

        for (TeacherAssignment assignment : teacherAssignmentRepository.findByTeacherId(teacher.getId())) {
            AcademicClass academicClass = assignment.getClassId() == null
                ? null
                : academicClassRepository.findById(assignment.getClassId()).orElse(null);
            AcademicBatch academicBatch = assignment.getBatchId() == null
                ? null
                : academicBatchRepository.findById(assignment.getBatchId()).orElse(null);

            String classGroup = academicClass == null || academicClass.getClassNumber() == null
                ? null
                : "Class " + academicClass.getClassNumber();
            String batchGroup = academicBatch == null || academicBatch.getBatchNumber() == null
                ? null
                : "Batch " + academicBatch.getBatchNumber();

            if (classGroup != null) {
                classGroups.add(normalizeLoose(classGroup));
            }
            if (batchGroup != null) {
                batchKeys.add(buildBatchKey(classGroup, batchGroup));
            }
        }

        return academicPublicationRepository.findByPublishedTrueOrderByPublishedAtDescIdDesc().stream()
            .filter(publication -> supportsAudience(publication, PublicationAudience.TEACHER))
            .filter(publication -> matchesTeacher(publication, classGroups, batchKeys))
            .map(this::toResponse)
            .toList();
    }

    private List<AcademicPublication> buildPublications(String actor, String title, PublicationCreateRequest request) {
        PublicationScope scope = request.getScope() == null ? PublicationScope.GLOBAL : request.getScope();
        String summary = trimToNull(request.getSummary());
        String course = trimToNull(request.getCourse());
        String semester = trimToNull(request.getSemester());
        boolean published = request.getPublished() == null || Boolean.TRUE.equals(request.getPublished());
        String payloadJson = toPayloadJson(request.getPayload());

        return switch (scope) {
            case GLOBAL -> List.of(newPublication(actor, request, title, summary, course, semester, published, payloadJson, null, null, null));
            case STUDENT -> expandStudentPublications(actor, request, title, summary, course, semester, published, payloadJson);
            case CLASS -> expandClassPublications(actor, request, title, summary, course, semester, published, payloadJson);
            case BATCH -> expandBatchPublications(actor, request, title, summary, course, semester, published, payloadJson);
        };
    }

    private List<AcademicPublication> expandStudentPublications(String actor,
                                                                PublicationCreateRequest request,
                                                                String title,
                                                                String summary,
                                                                String course,
                                                                String semester,
                                                                boolean published,
                                                                String payloadJson) {
        Set<String> studentIds = new LinkedHashSet<>();
        addIfPresent(studentIds, request.getStudentId());
        normalizeList(request.getStudentIds()).forEach(studentIds::add);
        if (studentIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one studentId is required for STUDENT scope");
        }

        List<AcademicPublication> publications = new ArrayList<>();
        for (String studentId : studentIds) {
            studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
            publications.add(newPublication(actor, request, title, summary, course, semester, published, payloadJson, studentId, null, null));
        }
        return publications;
    }

    private List<AcademicPublication> expandClassPublications(String actor,
                                                              PublicationCreateRequest request,
                                                              String title,
                                                              String summary,
                                                              String course,
                                                              String semester,
                                                              boolean published,
                                                              String payloadJson) {
        Set<String> classGroups = new LinkedHashSet<>();
        addIfPresent(classGroups, request.getClassGroup());
        normalizeList(request.getClassGroups()).forEach(classGroups::add);
        if (classGroups.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one classGroup is required for CLASS scope");
        }

        List<AcademicPublication> publications = new ArrayList<>();
        for (String classGroup : classGroups) {
            publications.add(newPublication(actor, request, title, summary, course, semester, published, payloadJson, null, classGroup, null));
        }
        return publications;
    }

    private List<AcademicPublication> expandBatchPublications(String actor,
                                                              PublicationCreateRequest request,
                                                              String title,
                                                              String summary,
                                                              String course,
                                                              String semester,
                                                              boolean published,
                                                              String payloadJson) {
        Set<String> classGroups = new LinkedHashSet<>();
        Set<String> batchGroups = new LinkedHashSet<>();
        addIfPresent(classGroups, request.getClassGroup());
        normalizeList(request.getClassGroups()).forEach(classGroups::add);
        addIfPresent(batchGroups, request.getBatchGroup());
        normalizeList(request.getBatchGroups()).forEach(batchGroups::add);

        if (batchGroups.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one batchGroup is required for BATCH scope");
        }

        List<AcademicPublication> publications = new ArrayList<>();
        if (classGroups.isEmpty()) {
            for (String batchGroup : batchGroups) {
                publications.add(newPublication(actor, request, title, summary, course, semester, published, payloadJson, null, null, batchGroup));
            }
            return publications;
        }

        for (String classGroup : classGroups) {
            for (String batchGroup : batchGroups) {
                publications.add(newPublication(actor, request, title, summary, course, semester, published, payloadJson, null, classGroup, batchGroup));
            }
        }
        return publications;
    }

    private AcademicPublication newPublication(String actor,
                                               PublicationCreateRequest request,
                                               String title,
                                               String summary,
                                               String course,
                                               String semester,
                                               boolean published,
                                               String payloadJson,
                                               String targetStudentId,
                                               String targetClassGroup,
                                               String targetBatchGroup) {
        AcademicPublication publication = new AcademicPublication();
        publication.setCategory(Objects.requireNonNullElse(request.getCategory(), com.sms.model.PublicationCategory.NOTICE));
        publication.setAudience(Objects.requireNonNullElse(request.getAudience(), PublicationAudience.BOTH));
        publication.setScope(Objects.requireNonNullElse(request.getScope(), PublicationScope.GLOBAL));
        publication.setTitle(title);
        publication.setSummary(summary);
        publication.setPayloadJson(payloadJson);
        publication.setTargetStudentId(targetStudentId);
        publication.setTargetClassGroup(targetClassGroup);
        publication.setTargetBatchGroup(targetBatchGroup);
        publication.setCourse(course);
        publication.setSemester(semester);
        publication.setPublished(published);
        publication.setCreatedBy(actor);
        return publication;
    }

    private boolean matchesStudent(AcademicPublication publication, Student student) {
        if (!matchesStudentMeta(publication, student)) {
            return false;
        }

        String studentClass = trimToNull(student.getClassGroup());
        String studentBatch = trimToNull(student.getBatchGroup());
        return switch (publication.getScope()) {
            case GLOBAL -> true;
            case STUDENT -> Objects.equals(trimToNull(publication.getTargetStudentId()), student.getId());
            case CLASS -> matchesLoose(trimToNull(publication.getTargetClassGroup()), studentClass);
            case BATCH -> matchesLoose(trimToNull(publication.getTargetBatchGroup()), studentBatch)
                && (trimToNull(publication.getTargetClassGroup()) == null
                    || matchesLoose(trimToNull(publication.getTargetClassGroup()), studentClass));
        };
    }

    private boolean matchesTeacher(AcademicPublication publication, Set<String> teacherClassGroups, Set<String> teacherBatchKeys) {
        return switch (publication.getScope()) {
            case GLOBAL -> true;
            case CLASS -> teacherClassGroups.contains(normalizeLoose(publication.getTargetClassGroup()));
            case BATCH -> teacherBatchKeys.contains(buildBatchKey(publication.getTargetClassGroup(), publication.getTargetBatchGroup()));
            case STUDENT -> matchesStudentTargetForTeacher(publication, teacherClassGroups, teacherBatchKeys);
        };
    }

    private boolean matchesStudentTargetForTeacher(AcademicPublication publication,
                                                   Set<String> teacherClassGroups,
                                                   Set<String> teacherBatchKeys) {
        String studentId = trimToNull(publication.getTargetStudentId());
        if (studentId == null) {
            return false;
        }
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null || !matchesStudentMeta(publication, student)) {
            return false;
        }
        String studentClass = trimToNull(student.getClassGroup());
        String studentBatch = trimToNull(student.getBatchGroup());
        return teacherClassGroups.contains(normalizeLoose(studentClass))
            || teacherBatchKeys.contains(buildBatchKey(studentClass, studentBatch));
    }

    private boolean matchesStudentMeta(AcademicPublication publication, Student student) {
        if (!matchesLoose(trimToNull(publication.getCourse()), trimToNull(student.getCourse()))) {
            return false;
        }
        return matchesLoose(trimToNull(publication.getSemester()), trimToNull(student.getSemester()));
    }

    private boolean supportsAudience(AcademicPublication publication, PublicationAudience requestedAudience) {
        return publication.getAudience() == PublicationAudience.BOTH || publication.getAudience() == requestedAudience;
    }

    private PublicationResponse toResponse(AcademicPublication publication) {
        PublicationResponse response = new PublicationResponse();
        response.setId(publication.getId());
        response.setCategory(publication.getCategory());
        response.setAudience(publication.getAudience());
        response.setScope(publication.getScope());
        response.setTitle(publication.getTitle());
        response.setSummary(publication.getSummary());
        response.setStudentId(publication.getTargetStudentId());
        response.setClassGroup(publication.getTargetClassGroup());
        response.setBatchGroup(publication.getTargetBatchGroup());
        response.setCourse(publication.getCourse());
        response.setSemester(publication.getSemester());
        response.setPublished(publication.getPublished());
        response.setPublishedAt(publication.getPublishedAt());
        response.setCreatedAt(publication.getCreatedAt());
        response.setCreatedBy(publication.getCreatedBy());
        response.setPayload(fromPayloadJson(publication.getPayloadJson()));
        return response;
    }

    private String toPayloadJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publication payload is invalid", ex);
        }
    }

    private Map<String, Object> fromPayloadJson(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .map(this::trimToNull)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private void addIfPresent(Set<String> target, String value) {
        String normalized = trimToNull(value);
        if (normalized != null) {
            target.add(normalized);
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean matchesLoose(String left, String right) {
        if (left == null) {
            return true;
        }
        if (right == null) {
            return false;
        }
        return normalizeLoose(left).equals(normalizeLoose(right));
    }

    private String normalizeLoose(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "";
        }
        return normalized.replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
    }

    private String buildBatchKey(String classGroup, String batchGroup) {
        return normalizeLoose(classGroup) + "::" + normalizeLoose(batchGroup);
    }
}
