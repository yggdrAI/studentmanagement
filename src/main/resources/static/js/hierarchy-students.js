/**
 * Fullscreen hierarchy dashboard
 * Class -> Batch -> Student with drag and drop, search, filters, and live updates.
 */

(function () {
    "use strict";

    const state = {
        hierarchy: { summary: { totalClasses: 0, totalBatches: 0, totalStudents: 0 }, classes: [] },
        filters: {
            course: "",
            semester: "",
            performance: "",
            searchQuery: "",
            groupingMode: "performance"
        },
        expandedClasses: new Set(),
        expandedBatches: new Set(),
        loading: false,
        allExpanded: false,
        toastTimer: null,
        draggingStudent: null,
        pendingFaceStudentId: null
    };

    const refs = {
        classesContainer: document.getElementById("classesContainer"),
        loadingSpinner: document.getElementById("loadingSpinner"),
        noDataState: document.getElementById("noDataState"),
        courseFilter: document.getElementById("courseFilter"),
        semesterFilter: document.getElementById("semesterFilter"),
        performanceFilter: document.getElementById("performanceFilter"),
        groupingMode: document.getElementById("groupingMode"),
        globalSearch: document.getElementById("globalSearch"),
        refreshBtn: document.getElementById("refreshBtn"),
        expandAllBtn: document.getElementById("expandAllBtn"),
        collapseAllBtn: document.getElementById("collapseAllBtn"),
        aiGroupBtn: document.getElementById("aiGroupBtn"),
        totalClasses: document.getElementById("totalClasses"),
        totalBatches: document.getElementById("totalBatches"),
        totalStudents: document.getElementById("totalStudents"),
        avgAttendance: document.getElementById("avgAttendance"),
        sidebarToggle: document.getElementById("sidebarToggle"),
        faceUploadInput: createHiddenFaceUploadInput(),
        // Create student form refs
        createStudentForm: document.getElementById("createStudentForm"),
        stepPrevBtn: document.getElementById("stepPrevBtn"),
        stepNextBtn: document.getElementById("stepNextBtn"),
        createSubmitBtn: document.getElementById("createSubmitBtn"),
        stepLabel: document.getElementById("stepLabel"),
        steps: Array.from(document.querySelectorAll(".step")),
        studentIdInput: document.getElementById("studentIdInput"),
        studentNameInput: document.getElementById("studentNameInput"),
        studentCourseInput: document.getElementById("studentCourseInput"),
        studentSemesterInput: document.getElementById("studentSemesterInput"),
        studentPhoneInput: document.getElementById("studentPhoneInput"),
        // Insights refs
        totalLabel: document.getElementById("totalLabel"),
        avgMarksLabel: document.getElementById("avgMarksLabel"),
        topPerformers: document.getElementById("topPerformers")
    };

    const createFormState = {
        step: 1,
        formData: { id: "", name: "", course: "", semester: "", phone: "" }
    };

    function createHiddenFaceUploadInput() {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = "image/*";
        input.hidden = true;
        input.addEventListener("change", handleFaceUploadChange);
        document.body.appendChild(input);
        return input;
    }

    function init() {
        bindEvents();
        loadHierarchy();
        loadTopPerformers();
    }

    function bindEvents() {
        refs.courseFilter?.addEventListener("change", onServerFilterChange);
        refs.semesterFilter?.addEventListener("change", onServerFilterChange);
        refs.performanceFilter?.addEventListener("change", onServerFilterChange);
        refs.groupingMode?.addEventListener("change", onGroupingModeChange);
        refs.globalSearch?.addEventListener("input", debounce(onSearchChange, 180));
        refs.refreshBtn?.addEventListener("click", () => loadHierarchy());
        refs.expandAllBtn?.addEventListener("click", expandAll);
        refs.collapseAllBtn?.addEventListener("click", collapseAll);
        refs.aiGroupBtn?.addEventListener("click", runAiGrouping);
        refs.sidebarToggle?.addEventListener("click", toggleSidebar);
        
        // Create Student form events
        refs.createStudentForm?.addEventListener("input", (event) => {
            const field = event.target.name;
            if (field) createFormState.formData[field] = event.target.value;
        });
        refs.stepPrevBtn?.addEventListener("click", prevStep);
        refs.stepNextBtn?.addEventListener("click", nextStep);
        refs.createSubmitBtn?.addEventListener("click", submitCreateStudent);
        refs.createStudentForm?.addEventListener("submit", (e) => { e.preventDefault(); submitCreateStudent(); });
    }

    function loadHierarchy(options = {}) {
        if (state.loading) {
            return;
        }

        state.loading = true;
        showLoadingState();

        const params = new URLSearchParams();
        if (state.filters.course) params.append("course", state.filters.course);
        if (state.filters.semester) params.append("semester", state.filters.semester);
        if (state.filters.performance) params.append("performance", state.filters.performance);

        fetch(`/api/admin/students-hierarchy?${params.toString()}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then((payload) => {
                state.hierarchy = normalizeHierarchy(payload);
                state.loading = false;
                if (!options.preserveState) {
                    state.expandedClasses.clear();
                    state.expandedBatches.clear();
                    state.allExpanded = false;
                }
                updateStatistics();
                if (refs.loadingSpinner) refs.loadingSpinner.hidden = true;
                renderHierarchy();
            })
            .catch((error) => {
                state.loading = false;
                showToast(`Failed to load hierarchy: ${error.message}`);
                showEmptyState();
            });
    }

    function normalizeHierarchy(payload) {
        const summary = payload?.summary || payload?.structure || {
            totalClasses: 0,
            totalBatches: 0,
            totalStudents: 0
        };

        const classes = Array.isArray(payload?.classes) ? payload.classes : [];
        return { summary, classes };
    }

    function updateStatistics() {
        const summary = state.hierarchy?.summary || {};
        const classes = Array.isArray(state.hierarchy?.classes) ? state.hierarchy.classes : [];

        if (refs.totalClasses) refs.totalClasses.textContent = summary.totalClasses ?? classes.length ?? 0;
        if (refs.totalBatches) refs.totalBatches.textContent = summary.totalBatches ?? countBatches(classes);
        if (refs.totalStudents) refs.totalStudents.textContent = summary.totalStudents ?? countStudents(classes);
        if (refs.avgAttendance) refs.avgAttendance.textContent = `${averageAttendance(classes).toFixed(1)}%`;
        if (refs.totalLabel) refs.totalLabel.textContent = `${summary.totalStudents ?? countStudents(classes)} records`;
    }

    function renderHierarchy() {
        const classes = Array.isArray(state.hierarchy?.classes) ? state.hierarchy.classes : [];
        const filtered = applyFilters(classes);

        if (!filtered.length) {
            showEmptyState();
            return;
        }

        refs.noDataState.hidden = true;
        if (refs.loadingSpinner) refs.loadingSpinner.hidden = true;
        refs.classesContainer.hidden = false;
        refs.classesContainer.innerHTML = filtered.map((classItem, index) => renderClassCard(classItem, index)).join("");
        bindRenderedInteractions();
    }

    function applyFilters(classes) {
        const query = state.filters.searchQuery.trim().toLowerCase();
        const mode = state.filters.groupingMode;

        return sortClasses(classes, mode)
            .map((classItem) => {
                const batches = sortBatches(classItem.batches || [], mode)
                    .map((batch) => {
                        const students = (batch.students || []).filter((student) => studentMatches(student, query));
                        const batchMatches = !query || batchMatchesQuery(batch, query) || students.length > 0;
                        return batchMatches
                            ? {
                                  ...batch,
                                  students: query ? students : batch.students || []
                              }
                            : null;
                    })
                    .filter(Boolean);

                const classMatches = !query || classMatchesQuery(classItem, query) || batches.length > 0;
                return classMatches ? { ...classItem, batches } : null;
            })
            .filter(Boolean);
    }

    function sortClasses(classes, mode) {
        return [...classes].sort((left, right) => {
            const leftAnalytics = left.analytics || left.classAnalytics || {};
            const rightAnalytics = right.analytics || right.classAnalytics || {};

            if (mode === "attendance") {
                return (rightAnalytics.attendance || 0) - (leftAnalytics.attendance || 0) || (left.number || 0) - (right.number || 0);
            }

            if (mode === "ai") {
                return (rightAnalytics.riskStudents || 0) - (leftAnalytics.riskStudents || 0) || (rightAnalytics.avgMarks || rightAnalytics.averageMarks || 0) - (leftAnalytics.avgMarks || leftAnalytics.averageMarks || 0);
            }

            return (rightAnalytics.avgMarks || rightAnalytics.averageMarks || 0) - (leftAnalytics.avgMarks || leftAnalytics.averageMarks || 0) || (left.number || 0) - (right.number || 0);
        });
    }

    function sortBatches(batches, mode) {
        return [...batches].sort((left, right) => {
            const leftAnalytics = left.analytics || {};
            const rightAnalytics = right.analytics || {};

            if (mode === "attendance") {
                return (rightAnalytics.attendance || rightAnalytics.averageAttendance || 0) - (leftAnalytics.attendance || leftAnalytics.averageAttendance || 0) || (left.number || 0) - (right.number || 0);
            }

            if (mode === "ai") {
                return (rightAnalytics.riskStudents || 0) - (leftAnalytics.riskStudents || 0) || (rightAnalytics.avgMarks || rightAnalytics.averageMarks || 0) - (leftAnalytics.avgMarks || leftAnalytics.averageMarks || 0);
            }

            return (rightAnalytics.avgMarks || rightAnalytics.averageMarks || 0) - (leftAnalytics.avgMarks || leftAnalytics.averageMarks || 0) || (left.number || 0) - (right.number || 0);
        });
    }

    function renderClassCard(classItem, index) {
        const classId = classItem.id ?? classItem.classId ?? `class-${classItem.number}`;
        const classNumber = classItem.number ?? classItem.classNumber ?? index + 1;
        const analytics = classItem.analytics || classItem.classAnalytics || {};
        const batches = Array.isArray(classItem.batches) ? classItem.batches : [];
        const query = state.filters.searchQuery.trim().toLowerCase();
        const matchesSearch = !query || classMatchesQuery(classItem, query) || batches.some((batch) => batchMatchesQuery(batch, query) || (batch.students || []).some((student) => studentMatches(student, query)));
        const expanded = state.allExpanded || state.expandedClasses.has(String(classId)) || matchesSearch;

        return `
            <article class="class-card glass-panel" data-class-id="${escapeHtml(String(classId))}" data-class-number="${escapeHtml(String(classNumber))}">
                <header class="class-header ${expanded ? "expanded" : ""}" data-toggle-class="${escapeHtml(String(classId))}" role="button" tabindex="0" aria-expanded="${expanded}">
                    <div class="class-info">
                        <div class="class-title">${escapeHtml(classItem.label || classItem.classLabel || `Class ${classNumber}`)}</div>
                        <div class="class-stats">
                            <span class="stat-pill">${countStudentsInClass(classItem)} Students</span>
                            <span class="stat-pill">${batches.length} Batches</span>
                            <span class="stat-pill">Avg ${formatNumber(analytics.avgMarks ?? analytics.averageMarks ?? 0)}</span>
                            <span class="stat-pill">Attendance ${formatNumber(analytics.attendance ?? analytics.averageAttendance ?? 0)}%</span>
                            <span class="stat-pill">Risk ${analytics.riskStudents ?? 0}</span>
                        </div>
                    </div>
                    <button class="class-toggle" type="button" aria-label="Toggle class">⌄</button>
                </header>
                <div class="class-body ${expanded ? "" : "collapsed"}">
                    ${batches.map((batch, batchIndex) => renderBatchCard(batch, batchIndex, classNumber)).join("")}
                </div>
            </article>
        `;
    }

    function renderBatchCard(batch, batchIndex, classNumber) {
        const batchId = batch.id ?? batch.batchId ?? `batch-${classNumber}-${batch.number}`;
        const batchNumber = batch.number ?? batch.batchNumber ?? batchIndex + 1;
        const analytics = batch.analytics || {};
        const students = Array.isArray(batch.students) ? batch.students : [];
        const query = state.filters.searchQuery.trim().toLowerCase();
        const matchesSearch = !query || batchMatchesQuery(batch, query) || students.some((student) => studentMatches(student, query));
        const expanded = state.allExpanded || state.expandedBatches.has(String(batchId)) || students.length <= 8 || matchesSearch;
        const topPerformer = analytics.topPerformer?.name || "N/A";

        return `
            <section class="batch-card batch-${batchNumber}" data-batch-id="${escapeHtml(String(batchId))}" data-class-number="${escapeHtml(String(classNumber))}" data-batch-number="${escapeHtml(String(batchNumber))}" aria-label="Batch ${escapeHtml(String(batchNumber))}">
                <header class="batch-header">
                    <div>
                        <div class="batch-title">${escapeHtml(batch.label || batch.batchLabel || `Batch ${batchNumber}`)}</div>
                        <div class="batch-count"><span class="batch-count-badge">${students.length} students</span></div>
                    </div>
                    <button class="batch-toggle" type="button" data-toggle-batch="${escapeHtml(String(batchId))}" aria-label="Toggle students list">${expanded ? "▼" : "▶"}</button>
                </header>

                <div class="batch-analytics">
                    <div class="analytics-item"><span class="analytics-label">Avg Marks</span><span class="analytics-value">${formatNumber(analytics.avgMarks ?? analytics.averageMarks ?? 0)}</span></div>
                    <div class="analytics-item"><span class="analytics-label">Attendance</span><span class="analytics-value">${formatNumber(analytics.attendance ?? analytics.averageAttendance ?? 0)}%</span></div>
                    <div class="analytics-item"><span class="analytics-label">Top Performer</span><span class="analytics-value">${escapeHtml(truncate(topPerformer, 14))}</span></div>
                    <div class="analytics-item"><span class="analytics-label">At Risk</span><span class="analytics-value">${analytics.riskStudents ?? 0}</span></div>
                </div>

                <div class="batch-indicator">
                    <span>Present Today: ${analytics.presentToday ?? 0} / ${students.length}</span>
                    <span>Drop students here to reassign</span>
                </div>

                <div class="students-list ${expanded ? "visible" : ""}">
                    ${students.map((student) => renderStudentRow(student, classNumber, batchNumber)).join("")}
                </div>
            </section>
        `;
    }

    function renderStudentRow(student, classNumber, batchNumber) {
        const studentId = student.id ?? student.studentId ?? student.enrollment;
        const initials = getInitials(student.name || "Student");
        const performanceBand = student.performanceBand || student.performance?.status || performanceBandFromMarks(student.performance?.averageMarks ?? student.marks ?? 0);
        const marks = student.performance?.averageMarks ?? student.marks ?? 0;
        const attendance = student.attendance ?? student.performance?.attendance ?? 0;
        const searchMatch = studentMatches(student, state.filters.searchQuery.trim().toLowerCase());

        return `
            <article class="student-row ${searchMatch ? "" : "match-hidden"}" draggable="true" data-student-id="${escapeHtml(String(studentId))}" data-class-number="${escapeHtml(String(classNumber))}" data-batch-number="${escapeHtml(String(batchNumber))}">
                <div class="student-info">
                    <div class="student-avatar">${escapeHtml(initials)}</div>
                    <div class="student-details">
                        <div class="student-name">${escapeHtml(student.name || "Unnamed Student")}</div>
                        <div class="student-meta">${escapeHtml(student.enrollment || student.rollNumber || "")}${student.email ? ` • ${escapeHtml(student.email)}` : ""}</div>
                    </div>
                </div>
                <div class="student-performance">
                    <div class="performance-badge ${escapeHtml(performanceBand)}">${formatNumber(marks)}</div>
                </div>
                <div class="student-actions">
                    <button class="action-btn" type="button" data-view-profile="${escapeHtml(String(studentId))}">View</button>
                    <button class="action-btn" type="button" data-upload-face="${escapeHtml(String(studentId))}">Face</button>
                    <button class="action-btn danger" type="button" data-delete-student="${escapeHtml(String(studentId))}">Delete</button>
                </div>
            </article>
        `;
    }

    function bindRenderedInteractions() {
        document.querySelectorAll("[data-toggle-class]").forEach((trigger) => {
            trigger.addEventListener("click", handleClassToggle);
            trigger.addEventListener("keydown", handleClassKeydown);
        });

        document.querySelectorAll("[data-toggle-batch]").forEach((trigger) => {
            trigger.addEventListener("click", handleBatchToggle);
            trigger.addEventListener("keydown", handleBatchKeydown);
        });

        document.querySelectorAll(".student-row").forEach((row) => {
            row.addEventListener("dragstart", handleStudentDragStart);
            row.addEventListener("dragend", handleStudentDragEnd);
        });

        document.querySelectorAll(".batch-card").forEach((card) => {
            card.addEventListener("dragover", handleBatchDragOver);
            card.addEventListener("dragleave", handleBatchDragLeave);
            card.addEventListener("drop", handleBatchDrop);
        });

        document.querySelectorAll("[data-view-profile]").forEach((button) => {
            button.addEventListener("click", () => openProfile(button.dataset.viewProfile));
        });

        document.querySelectorAll("[data-upload-face]").forEach((button) => {
            button.addEventListener("click", () => openFaceUpload(button.dataset.uploadFace));
        });

        document.querySelectorAll("[data-delete-student]").forEach((button) => {
            button.addEventListener("click", () => deleteStudent(button.dataset.deleteStudent));
        });
    }

    function handleClassToggle(event) {
        const classCard = event.currentTarget.closest(".class-card");
        const classId = classCard?.dataset.classId;
        const classBody = classCard?.querySelector(".class-body");
        const classHeader = classCard?.querySelector(".class-header");
        if (!classId || !classBody || !classHeader) return;

        const expanded = classBody.classList.contains("collapsed");
        if (expanded) {
            classBody.classList.remove("collapsed");
            state.expandedClasses.add(String(classId));
            classHeader.classList.add("expanded");
        } else {
            classBody.classList.add("collapsed");
            state.expandedClasses.delete(String(classId));
            classHeader.classList.remove("expanded");
        }
    }

    function handleClassKeydown(event) {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            handleClassToggle(event);
        }
    }

    function handleBatchToggle(event) {
        event.stopPropagation();
        const batchId = event.currentTarget.dataset.toggleBatch;
        const batchCard = document.querySelector(`[data-batch-id="${cssEscape(batchId)}"]`);
        const studentsList = batchCard?.querySelector(".students-list");
        if (!batchCard || !studentsList) return;

        const visible = studentsList.classList.contains("visible");
        if (visible) {
            studentsList.classList.remove("visible");
            state.expandedBatches.delete(String(batchId));
            event.currentTarget.textContent = "▶";
        } else {
            studentsList.classList.add("visible");
            state.expandedBatches.add(String(batchId));
            event.currentTarget.textContent = "▼";
        }
    }

    function handleBatchKeydown(event) {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            handleBatchToggle(event);
        }
    }

    function handleStudentDragStart(event) {
        const row = event.currentTarget;
        const payload = {
            studentId: row.dataset.studentId,
            classNumber: row.dataset.classNumber,
            batchNumber: row.dataset.batchNumber
        };
        state.draggingStudent = payload;
        row.classList.add("dragging");
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", JSON.stringify(payload));
    }

    function handleStudentDragEnd(event) {
        event.currentTarget.classList.remove("dragging");
        state.draggingStudent = null;
        document.querySelectorAll(".batch-drop-target").forEach((card) => card.classList.remove("batch-drop-target"));
    }

    function handleBatchDragOver(event) {
        event.preventDefault();
        event.currentTarget.classList.add("batch-drop-target");
        event.dataTransfer.dropEffect = "move";
    }

    function handleBatchDragLeave(event) {
        event.currentTarget.classList.remove("batch-drop-target");
    }

    function handleBatchDrop(event) {
        event.preventDefault();
        const target = event.currentTarget;
        target.classList.remove("batch-drop-target");

        const payload = state.draggingStudent || safeParse(event.dataTransfer.getData("text/plain"));
        if (!payload?.studentId) return;

        const targetClassNumber = Number(target.dataset.classNumber);
        const targetBatchNumber = Number(target.dataset.batchNumber);
        reassignStudent(payload.studentId, targetClassNumber, targetBatchNumber);
    }

    function reassignStudent(studentId, classNumber, batchNumber) {
        const snapshot = cloneHierarchy(state.hierarchy);
        moveStudentLocally(studentId, classNumber, batchNumber);
        renderHierarchy();

        fetch("/api/admin/students-hierarchy/reassign", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ studentId, classNumber, batchNumber })
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                showToast(`Moved student to Class ${classNumber}, Batch ${batchNumber}`);
            })
            .catch((error) => {
                state.hierarchy = snapshot;
                showToast(`Reassign failed: ${error.message}`);
                renderHierarchy();
            });
    }

    function moveStudentLocally(studentId, targetClassNumber, targetBatchNumber) {
        const classes = state.hierarchy.classes || [];
        let movingStudent = null;

        for (const classItem of classes) {
            for (const batch of classItem.batches || []) {
                const index = (batch.students || []).findIndex((student) => String(student.id ?? student.studentId ?? student.enrollment) === String(studentId));
                if (index >= 0) {
                    movingStudent = batch.students.splice(index, 1)[0];
                    break;
                }
            }
            if (movingStudent) break;
        }

        if (!movingStudent) return;

        movingStudent.classNumber = targetClassNumber;
        movingStudent.batchNumber = targetBatchNumber;

        const targetClass = classes.find((classItem) => Number(classItem.number ?? classItem.classNumber) === Number(targetClassNumber));
        if (!targetClass) {
            classes.push({
                id: `class-${targetClassNumber}`,
                number: targetClassNumber,
                label: `Class ${targetClassNumber}`,
                analytics: { avgMarks: 0, attendance: 0, riskStudents: 0, presentToday: 0 },
                batches: [{ id: `batch-${targetClassNumber}-${targetBatchNumber}`, number: targetBatchNumber, label: `Batch ${targetBatchNumber}`, analytics: {}, students: [movingStudent] }]
            });
            return;
        }

        let targetBatch = (targetClass.batches || []).find((batch) => Number(batch.number ?? batch.batchNumber) === Number(targetBatchNumber));
        if (!targetBatch) {
            targetBatch = { id: `batch-${targetClassNumber}-${targetBatchNumber}`, number: targetBatchNumber, label: `Batch ${targetBatchNumber}`, analytics: {}, students: [] };
            targetClass.batches = [...(targetClass.batches || []), targetBatch];
        }

        targetBatch.students = [...(targetBatch.students || []), movingStudent];
    }

    function onServerFilterChange() {
        state.filters.course = refs.courseFilter?.value || "";
        state.filters.semester = refs.semesterFilter?.value || "";
        state.filters.performance = refs.performanceFilter?.value || "";
        state.expandedClasses.clear();
        state.expandedBatches.clear();
        loadHierarchy();
    }

    function onGroupingModeChange() {
        state.filters.groupingMode = refs.groupingMode?.value || "performance";
        renderHierarchy();
    }

    function onSearchChange(event) {
        state.filters.searchQuery = (event.target.value || "").toLowerCase();
        renderHierarchy();
    }

    function expandAll() {
        state.allExpanded = true;
        state.expandedClasses.clear();
        state.expandedBatches.clear();
        (state.hierarchy.classes || []).forEach((classItem) => {
            const classId = classItem.id ?? classItem.classId ?? `class-${classItem.number}`;
            state.expandedClasses.add(String(classId));
            (classItem.batches || []).forEach((batch) => {
                const batchId = batch.id ?? batch.batchId ?? `batch-${classItem.number}-${batch.number}`;
                state.expandedBatches.add(String(batchId));
            });
        });
        renderHierarchy();
    }

    function collapseAll() {
        state.allExpanded = false;
        state.expandedClasses.clear();
        state.expandedBatches.clear();
        renderHierarchy();
    }

    function runAiGrouping() {
        state.filters.groupingMode = "ai";
        if (refs.groupingMode) refs.groupingMode.value = "ai";

        fetch("/api/admin/students-hierarchy/ai-grouping", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                classNumber: state.filters.course ? null : null,
                course: state.filters.course || null,
                semester: state.filters.semester || null,
                clusters: 4
            })
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then((payload) => {
                const suggestions = Array.isArray(payload?.suggestions) ? payload.suggestions : [];
                const changedCount = suggestions.filter((item) => item.changed).length;
                showToast(`AI grouping ready: ${changedCount} students suggested for reassignment`);
                renderHierarchy();
            })
            .catch((error) => showToast(`AI grouping failed: ${error.message}`));
    }

    function toggleSidebar() {
        const sidebar = document.querySelector(".sidebar");
        if (!sidebar) return;
        const isOpen = sidebar.dataset.open === "true";
        sidebar.dataset.open = String(!isOpen);
    }

    function openProfile(studentId) {
        window.location.href = `/admin/students/${encodeURIComponent(studentId)}/profile`;
    }

    function openFaceUpload(studentId) {
        state.pendingFaceStudentId = studentId;
        refs.faceUploadInput?.click();
    }

    function handleFaceUploadChange(event) {
        const file = event.target.files?.[0];
        const studentId = state.pendingFaceStudentId;
        if (!file || !studentId) return;

        const formData = new FormData();
        formData.append("studentId", studentId);
        formData.append("file", file);

        fetch("/api/admin/upload-face", {
            method: "POST",
            body: formData
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then((payload) => showToast(payload?.message || `Face uploaded for ${studentId}`))
            .catch((error) => showToast(`Face upload failed: ${error.message}`))
            .finally(() => {
                state.pendingFaceStudentId = null;
                event.target.value = "";
            });
    }

    function deleteStudent(studentId) {
        if (!window.confirm(`Delete student ${studentId}? This cannot be undone.`)) return;

        fetch(`/api/admin/students/${encodeURIComponent(studentId)}`, { method: "DELETE" })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                showToast(`Student ${studentId} deleted`);
                loadHierarchy({ preserveState: true });
            })
            .catch((error) => showToast(`Delete failed: ${error.message}`));
    }

    function showLoadingState() {
        if (refs.loadingSpinner) refs.loadingSpinner.hidden = false;
        if (refs.noDataState) refs.noDataState.hidden = true;
        if (refs.classesContainer) {
            refs.classesContainer.hidden = true;
            refs.classesContainer.innerHTML = skeletonMarkup();
        }
    }

    function showEmptyState() {
        if (refs.loadingSpinner) refs.loadingSpinner.hidden = true;
        if (refs.noDataState) refs.noDataState.hidden = false;
        if (refs.classesContainer) {
            refs.classesContainer.hidden = true;
            refs.classesContainer.innerHTML = "";
        }
    }

    function skeletonMarkup() {
        return new Array(3).fill(0).map((_, index) => `
            <section class="class-card glass-panel">
                <div class="class-header">
                    <div class="class-info">
                        <div class="class-title">Loading class ${index + 1}...</div>
                        <div class="class-stats"><span class="stat-pill">Fetching analytics</span></div>
                    </div>
                    <div class="class-toggle">⌄</div>
                </div>
                <div class="class-body">
                    <div class="batch-card batch-1"><div class="loading-spinner"><div class="spinner"></div><p>Loading batches...</p></div></div>
                </div>
            </section>
        `).join("");
    }

    function countBatches(classes) {
        return classes.reduce((total, classItem) => total + (classItem.batches?.length || 0), 0);
    }

    function countStudents(classes) {
        return classes.reduce((total, classItem) => total + countStudentsInClass(classItem), 0);
    }

    function countStudentsInClass(classItem) {
        return (classItem.batches || []).reduce((total, batch) => total + (batch.students?.length || 0), 0);
    }

    function averageAttendance(classes) {
        const values = [];
        classes.forEach((classItem) => {
            (classItem.batches || []).forEach((batch) => {
                const analytics = batch.analytics || {};
                const value = analytics.attendance ?? analytics.averageAttendance;
                if (typeof value === "number") values.push(value);
            });
        });
        if (!values.length) return 0;
        return values.reduce((sum, value) => sum + value, 0) / values.length;
    }

    function studentMatches(student, query) {
        if (!query) return true;
        const haystack = [student.name, student.enrollment, student.email, student.phone, student.classNumber, student.batchNumber]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();
        return haystack.includes(query);
    }

    function batchMatchesQuery(batch, query) {
        if (!query) return true;
        const haystack = [batch.label, batch.batchLabel, batch.number, batch.id, batch.batchId].filter(Boolean).join(" ").toLowerCase();
        return haystack.includes(query);
    }

    function classMatchesQuery(classItem, query) {
        if (!query) return true;
        const haystack = [classItem.label, classItem.classLabel, classItem.number, classItem.id, classItem.classId].filter(Boolean).join(" ").toLowerCase();
        return haystack.includes(query);
    }

    function performanceBandFromMarks(marks) {
        const value = Number(marks) || 0;
        if (value >= 75) return "excellent";
        if (value >= 60) return "good";
        if (value >= 50) return "average";
        return "poor";
    }

    function formatNumber(value) {
        const numeric = Number(value) || 0;
        return numeric.toFixed(1).replace(/\.0$/, "");
    }

    function truncate(text, maxLength) {
        const value = String(text || "");
        return value.length > maxLength ? `${value.slice(0, maxLength)}…` : value;
    }

    function getInitials(name) {
        const parts = String(name || "").trim().split(/\s+/).filter(Boolean);
        if (!parts.length) return "ST";
        return parts.slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join("");
    }

    function escapeHtml(value) {
        const div = document.createElement("div");
        div.textContent = String(value ?? "");
        return div.innerHTML;
    }

    function cssEscape(value) {
        if (window.CSS && typeof window.CSS.escape === "function") {
            return window.CSS.escape(String(value));
        }
        return String(value).replace(/"/g, '\\"');
    }

    function safeParse(value) {
        try {
            return JSON.parse(value);
        } catch {
            return null;
        }
    }

    function cloneHierarchy(value) {
        return JSON.parse(JSON.stringify(value || { summary: {}, classes: [] }));
    }

    function debounce(callback, delay) {
        let timer = null;
        return function debounced(...args) {
            clearTimeout(timer);
            timer = setTimeout(() => callback.apply(this, args), delay);
        };
    }

    function showToast(message) {
        if (!message) return;
        let container = document.querySelector(".toast-stack");
        if (!container) {
            container = document.createElement("div");
            container.className = "toast-stack";
            document.body.appendChild(container);
        }

        const toast = document.createElement("div");
        toast.className = "toast";
        toast.textContent = message;
        container.appendChild(toast);

        clearTimeout(state.toastTimer);
        state.toastTimer = setTimeout(() => {
            toast.remove();
            if (!container.children.length) {
                container.remove();
            }
        }, 2600);
    }

    // ─── CREATE STUDENT FORM HANDLERS ─── //
    function prevStep() {
        if (createFormState.step > 1) {
            createFormState.step -= 1;
            renderSteps();
        }
    }

    function nextStep() {
        if (createFormState.step === 1 && (!refs.studentIdInput?.value.trim() || !refs.studentNameInput?.value.trim())) {
            showToast("Student ID and Name are required");
            return;
        }
        if (createFormState.step < 3) {
            createFormState.step += 1;
            renderSteps();
        }
    }

    function renderSteps() {
        const totalSteps = 3;
        refs.steps?.forEach((step, i) => {
            step.classList.toggle("active", i < createFormState.step);
        });
        if (refs.stepLabel) refs.stepLabel.textContent = `Step ${createFormState.step} of ${totalSteps}`;
        document.querySelectorAll("[data-step-panel]").forEach((panel) => {
            const panelStep = parseInt(panel.dataset.stepPanel);
            panel.hidden = panelStep !== createFormState.step;
        });
        if (refs.stepNextBtn) refs.stepNextBtn.hidden = createFormState.step === totalSteps;
        if (refs.createSubmitBtn) refs.createSubmitBtn.hidden = createFormState.step !== totalSteps;
    }

    function submitCreateStudent() {
        if (!refs.studentIdInput?.value.trim() || !refs.studentNameInput?.value.trim()) {
            showToast("Student ID and Name are required");
            return;
        }
        
        const payload = {
            id: refs.studentIdInput.value,
            name: refs.studentNameInput.value,
            course: refs.studentCourseInput?.value || "",
            semester: refs.studentSemesterInput?.value || "",
            phone: refs.studentPhoneInput?.value || ""
        };

        fetch("/api/students", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        })
            .then(response => response.ok ? response.json() : Promise.reject(`HTTP ${response.status}`))
            .then(() => {
                showToast("Student created successfully");
                refs.createStudentForm?.reset();
                createFormState.step = 1;
                renderSteps();
                loadHierarchy({ preserveState: true });
                loadTopPerformers();
            })
            .catch(error => showToast(`Create failed: ${error}`));
    }

    function loadTopPerformers() {
        fetch("/api/students?sort=performance&limit=5")
            .then(response => response.ok ? response.json() : Promise.reject())
            .then(data => {
                const items = Array.isArray(data.content) ? data.content : [];
                if (refs.topPerformers) {
                    refs.topPerformers.innerHTML = items.map((student, i) => `
                        <li class="audit-item">#${i + 1} ${escapeHtml(student.name || "Unknown")} - ${(student.marks || 0).toFixed(1)}</li>
                    `).join("") || "<li class=\"audit-item\">No data available</li>";
                }
                if (refs.avgMarksLabel && items.length > 0) {
                    const avgMarks = items.reduce((sum, s) => sum + (s.marks || 0), 0) / items.length;
                    refs.avgMarksLabel.textContent = avgMarks.toFixed(1);
                }
            })
            .catch(() => {
                if (refs.topPerformers) refs.topPerformers.innerHTML = "<li class=\"audit-item\">Unable to load data</li>";
            });
    }

    window.viewStudentProfile = openProfile;
    window.uploadFace = openFaceUpload;
    window.showStudentMenu = function (event, studentId) {
        event?.stopPropagation?.();
        const confirmed = window.confirm("Student actions:\n\nOK: delete student\nCancel: close menu");
        if (confirmed) deleteStudent(studentId);
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
