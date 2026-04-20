package com.sms.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sms.dto.profile.StudentDemographicConsentRequest;
import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.AcademicRecordRepository;
import com.sms.repository.StudentDocumentRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentDemographicConsentServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentDocumentRepository studentDocumentRepository;

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private StudentProfileService studentProfileService;

    private Student student;
    private StudentProfile profile;
    private String username;

    @BeforeEach
    void setUp() {
        username = "consent.user";
        student = new Student();
        student.setId("CONSENT_STUDENT_ID_1");
        student.setName("Aarav Sharma");

        profile = new StudentProfile();
        profile.setStudentId(student.getId());
        profile.setGenderSource("SYSTEM_DEFAULT");
        profile.setReligionSource("SYSTEM_DEFAULT");
        profile.setCasteSource("SYSTEM_DEFAULT");
        profile.setDemographicConsentGiven(false);
    }

    @Test
    void shouldPersistConsentBasedDemographics() {
        StudentDemographicConsentRequest request = new StudentDemographicConsentRequest();
        request.setConsentGiven(true);
        request.setGender("Male");
        request.setReligion("Hinduism");
        request.setCategory("General");
        request.setSpecificCaste("Sharma");

        when(studentRepository.findByUserUsername(username)).thenReturn(Optional.of(student));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentProfileRepository.findByStudentId(student.getId())).thenReturn(Optional.of(profile));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentDocumentRepository.findByStudentIdOrderByUploadedAtDesc(student.getId())).thenReturn(List.of());
        when(academicRecordRepository.findByStudentIdOrderBySubjectAsc(student.getId())).thenReturn(List.of());

        StudentProfileResponseDTO response = studentProfileService.submitDemographicConsent(username, request);

        assertThat(response.isDemographicConsentGiven()).isTrue();
        assertThat(response.getGender()).isEqualTo("Male");
        assertThat(response.getReligion()).isEqualTo("Hinduism");
        assertThat(response.getCasteCategory()).isEqualTo("General");
        assertThat(response.getCasteSource()).isEqualTo("SELF_DECLARED");
        assertThat(response.getDemographicConsentAt()).isNotNull();
        assertThat(response.getDemographicConsentVersion()).isEqualTo("1.0");
    }

    @Test
    void shouldRejectMissingConsent() {
        StudentDemographicConsentRequest request = new StudentDemographicConsentRequest();
        request.setConsentGiven(false);
        request.setGender("Male");
        request.setReligion("Hinduism");
        request.setCategory("General");

        when(studentRepository.findByUserUsername(username)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentProfileService.submitDemographicConsent(username, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consent is required");
    }
}
