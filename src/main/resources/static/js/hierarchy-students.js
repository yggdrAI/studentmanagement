/**
 * Fullscreen hierarchy dashboard — Program → Class → Batch → Student
 * With enrollment-driven grouping, program cards, drag-drop, search, filters, and live updates.
 */

(function () {
    "use strict";

    const state = {
        hierarchy: { summary: { totalClasses: 0, totalBatches: 0, totalStudents: 0 }, classes: [] },
        programs: [],
        selectedProgramId: null,
        filters: {
            course: "",
            semester: "",
            performance: "",
            searchQuery: "",
            groupingMode: "number"
        },
        expandedClasses: new Set(),
        expandedBatches: new Set(),
        loading: false,
        regenerating: false,
        allExpanded: false,
        toastTimer: null,
        draggingStudent: null,
        pendingFaceStudentId: null,
        searchResults: [],
        searchActiveIndex: -1,
        searchAbortController: null
    };

    const refs = {
        classesContainer: document.getElementById("classesContainer"),
        loadingSpinner: document.getElementById("loadingSpinner"),
        noDataState: document.getElementById("noDataState"),
        courseFilter: document.getElementById("courseFilter"),
        semesterFilter: document.getElementById("semesterFilter"),
        performanceFilter: document.getElementById("performanceFilter"),
        programFilter: document.getElementById("programFilter"),
        groupingMode: document.getElementById("groupingMode"),
        globalSearch: document.getElementById("globalSearch"),
        refreshBtn: document.getElementById("refreshBtn"),
        expandAllBtn: document.getElementById("expandAllBtn"),
        collapseAllBtn: document.getElementById("collapseAllBtn"),
        aiGroupBtn: document.getElementById("aiGroupBtn"),
        regenerateBtn: document.getElementById("regenerateBtn"),
        totalPrograms: document.getElementById("totalPrograms"),
        totalClasses: document.getElementById("totalClasses"),
        totalBatches: document.getElementById("totalBatches"),
        totalStudents: document.getElementById("totalStudents"),
        avgAttendance: document.getElementById("avgAttendance"),
        sidebarToggle: document.getElementById("sidebarToggle"),
        programsGrid: document.getElementById("programsGrid"),
        faceUploadInput: createHiddenFaceUploadInput(),
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
        totalLabel: document.getElementById("totalLabel"),
        avgMarksLabel: document.getElementById("avgMarksLabel"),
        topPerformers: document.getElementById("topPerformers")
    };

    const createFormState = {
        step: 1,
        formData: { id: "", name: "", course: "", semester: "", phone: "" }
    };

    /* ─── Color palette for program cards ─── */
    const PROGRAM_COLORS = [
        { bg: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)", text: "#fff" },
        { bg: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)", text: "#fff" },
        { bg: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)", text: "#fff" },
        { bg: "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)", text: "#1a1a2e" },
        { bg: "linear-gradient(135deg, #fa709a 0%, #fee140 100%)", text: "#1a1a2e" },
        { bg: "linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)", text: "#1a1a2e" },
        { bg: "linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)", text: "#fff" },
        { bg: "linear-gradient(135deg, #30cfd0 0%, #330867 100%)", text: "#fff" },
    ];

    /* ─── Program code → human-readable name resolver ─── */
    const PROGRAM_CODE_NAMES = {
        "CSE": "Computer Science & Engineering",
        "CSEU": "Computer Science & Engineering",
        "ECEU": "Electronics & Communication Engineering",
        "ECE": "Electronics & Communication Engineering",
        "MEU": "Mechanical Engineering",
        "ME": "Mechanical Engineering",
        "CEU": "Civil Engineering",
        "CE": "Civil Engineering",
        "EEU": "Electrical Engineering",
        "EE": "Electrical Engineering",
        "BIO": "Biotechnology",
        "BIOU": "Biotechnology",
        "BT": "Biotechnology",
        "LAW": "Law",
        "BLAU": "B.A. LL.B (Hons.)",
        "BLBU": "B.B.A. LL.B (Hons.)",
        "BALU": "B.A. LL.B",
        "MBA": "Master of Business Administration",
        "MCA": "Master of Computer Applications",
        "BBA": "Bachelor of Business Administration",
        "BCA": "Bachelor of Computer Applications",
        "BBAU": "Bachelor of Business Administration",
        "BCAU": "Bachelor of Computer Applications",
        "BAMU": "Bachelor of Arts (Multimedia)",
        "BMBU": "Bachelor of Mass Media",
        "ARIU": "B.Arch / Interior Design",
        "DESU": "B.Des (Design)",
        "DESGP": "M.Des (Design)",
        "CSEGP": "M.Tech (Computer Science)",
        "ECEGP": "M.Tech (Electronics)",
        "BBAGP": "MBA",
        "BLAGP": "LL.M",
        "PHD": "Ph.D.",
    };
    function resolveProgramName(code, fallbackName) {
        if (!code) return fallbackName || "Unknown Program";
        const upper = code.replace(/[0-9]+$/, "").toUpperCase();
        // Try full code, then without trailing digits, then first 3 chars
        return PROGRAM_CODE_NAMES[code.toUpperCase()]
            || PROGRAM_CODE_NAMES[upper]
            || PROGRAM_CODE_NAMES[upper.substring(0, 3)]
            || fallbackName
            || code;
    }

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
        loadPrograms();
        loadHierarchy();
        loadTopPerformers();
    }

    function bindEvents() {
        refs.courseFilter?.addEventListener("change", onServerFilterChange);
        refs.semesterFilter?.addEventListener("change", onServerFilterChange);
        refs.performanceFilter?.addEventListener("change", onServerFilterChange);
        refs.programFilter?.addEventListener("change", onProgramFilterChange);
        refs.groupingMode?.addEventListener("change", onGroupingModeChange);
        refs.refreshBtn?.addEventListener("click", () => { loadPrograms(); loadHierarchy(); });
        refs.expandAllBtn?.addEventListener("click", expandAll);
        refs.collapseAllBtn?.addEventListener("click", collapseAll);
        refs.aiGroupBtn?.addEventListener("click", runAiGrouping);
        refs.regenerateBtn?.addEventListener("click", regenerateStructure);
        refs.sidebarToggle?.addEventListener("click", toggleSidebar);

        refs.createStudentForm?.addEventListener("input", (event) => {
            const field = event.target.name;
            if (field) createFormState.formData[field] = event.target.value;
        });
        refs.stepPrevBtn?.addEventListener("click", prevStep);
        refs.stepNextBtn?.addEventListener("click", nextStep);
        refs.createSubmitBtn?.addEventListener("click", submitCreateStudent);
        refs.createStudentForm?.addEventListener("submit", (e) => { e.preventDefault(); submitCreateStudent(); });

        // ─── Advanced Search ────────────────────────────────────────────
        initAdvancedSearch();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROGRAMS
    // ═══════════════════════════════════════════════════════════════════════════

    function loadPrograms() {
        fetch("/api/admin/grouping/programs/summaries")
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(programs => {
                state.programs = Array.isArray(programs) ? programs : [];
                renderProgramCards();
                populateProgramFilter();
                updateProgramCount();
            })
            .catch(error => {
                console.warn("Failed to load programs:", error);
                state.programs = [];
                renderProgramCards();
            });
    }

    function renderProgramCards() {
        if (!refs.programsGrid) return;

        if (!state.programs.length) {
            refs.programsGrid.innerHTML = `
                <div class="program-empty-state">
                    <div class="program-empty-icon">📦</div>
                    <p>No programs found. Click <strong>Regenerate Structure</strong> to auto-detect programs from enrollment numbers.</p>
                </div>`;
            return;
        }

        refs.programsGrid.innerHTML = state.programs.map((program, index) => {
            const color = PROGRAM_COLORS[index % PROGRAM_COLORS.length];
            const isActive = state.selectedProgramId === program.id;
            const totalStudents = program.totalStudents || 0;
            const totalClasses = program.totalClasses || 0;
            const totalBatches = program.totalBatches || 0;
            const displayName = resolveProgramName(program.code, program.name);
            const typeLabel = (program.programType || '').toUpperCase() === 'UG' ? 'Undergraduate' : (program.programType || '').toUpperCase() === 'PG' ? 'Postgraduate' : program.programType || '';

            return `
                <article class="program-card ${isActive ? 'program-card-active' : ''}" 
                         data-program-id="${program.id}" 
                         style="background:${color.bg}; color:${color.text}"
                         tabindex="0" role="button"
                         aria-label="${escapeHtml(displayName)} — ${totalStudents} students">
                    <div class="program-card-header">
                        <span class="program-card-code">${escapeHtml(program.code || '')}</span>
                        <span class="program-card-type">${escapeHtml(typeLabel)}</span>
                    </div>
                    <h3 class="program-card-name">${escapeHtml(displayName)}</h3>
                    <div class="program-card-stats">
                        <div class="program-stat">
                            <span class="program-stat-value">${totalStudents}</span>
                            <span class="program-stat-label">Students</span>
                        </div>
                        <div class="program-stat">
                            <span class="program-stat-value">${totalClasses}</span>
                            <span class="program-stat-label">Classes</span>
                        </div>
                        <div class="program-stat">
                            <span class="program-stat-value">${totalBatches}</span>
                            <span class="program-stat-label">Batches</span>
                        </div>
                    </div>
                    ${program.admissionYear ? `<div class="program-card-year">Year: ${escapeHtml(program.admissionYear)}</div>` : ''}
                </article>`;
        }).join("");

        // Bind click events
        document.querySelectorAll(".program-card").forEach(card => {
            card.addEventListener("click", () => {
                const programId = Number(card.dataset.programId);
                if (state.selectedProgramId === programId) {
                    state.selectedProgramId = null; // deselect
                } else {
                    state.selectedProgramId = programId;
                }
                renderProgramCards();
                loadProgramHierarchy();
            });
            card.addEventListener("keydown", (e) => {
                if (e.key === "Enter" || e.key === " ") { e.preventDefault(); card.click(); }
            });
        });
    }

    function populateProgramFilter() {
        if (!refs.programFilter) return;
        const currentValue = refs.programFilter.value;
        refs.programFilter.innerHTML = '<option value="">All Programs</option>';
        state.programs.forEach(program => {
            const opt = document.createElement("option");
            opt.value = program.id;
            opt.textContent = `${program.code} — ${program.name}`;
            refs.programFilter.appendChild(opt);
        });
        refs.programFilter.value = currentValue;
    }

    function updateProgramCount() {
        if (refs.totalPrograms) refs.totalPrograms.textContent = state.programs.length;
    }

    function onProgramFilterChange() {
        const val = refs.programFilter?.value;
        if (val) {
            state.selectedProgramId = Number(val);
        } else {
            state.selectedProgramId = null;
        }
        renderProgramCards();
        loadProgramHierarchy();
    }

    function loadProgramHierarchy() {
        if (state.selectedProgramId) {
            // Load the specific program tree
            fetch(`/api/admin/grouping/programs`)
                .then(response => {
                    if (!response.ok) throw new Error(`HTTP ${response.status}`);
                    return response.json();
                })
                .then(programTree => {
                    const programs = Array.isArray(programTree) ? programTree : [];
                    const selected = programs.find(p => p.id === state.selectedProgramId);
                    if (selected && selected.classes) {
                        // Convert program tree to hierarchy format
                        const classes = selected.classes.map(clazz => ({
                            id: clazz.id,
                            number: clazz.localClassNumber,
                            classNumber: clazz.localClassNumber,
                            label: clazz.label || `Class ${clazz.localClassNumber}`,
                            totalStudents: clazz.totalStudents || 0,
                            analytics: { avgMarks: 0, attendance: 0, riskStudents: 0 },
                            batches: (clazz.batches || []).map(batch => ({
                                id: batch.id,
                                number: batch.localBatchNumber,
                                batchNumber: batch.localBatchNumber,
                                label: batch.label || `Batch ${batch.localBatchNumber}`,
                                studentsCount: batch.totalStudents || 0,
                                totalStudents: batch.totalStudents || 0,
                                analytics: { avgMarks: 0, attendance: 0, riskStudents: 0 },
                                students: (batch.students || []).map(s => ({
                                    id: s.id,
                                    name: s.name,
                                    enrollment: s.enrollmentNumber || s.id,
                                    enrollmentNumber: s.enrollmentNumber || s.id,
                                    email: s.email || "",
                                    marks: 0,
                                    attendance: 0,
                                    performanceBand: "average"
                                }))
                            }))
                        }));

                        state.hierarchy = {
                            summary: {
                                totalClasses: classes.length,
                                totalBatches: classes.reduce((sum, c) => sum + (c.batches?.length || 0), 0),
                                totalStudents: selected.totalStudents || 0
                            },
                            classes
                        };
                        state.expandedClasses.clear();
                        state.expandedBatches.clear();
                        updateStatistics();
                        renderHierarchy();
                    }
                })
                .catch(error => {
                    showToast(`Failed to load program hierarchy: ${error.message}`);
                });
        } else {
            loadHierarchy();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGENERATE
    // ═══════════════════════════════════════════════════════════════════════════

    function regenerateStructure() {
        if (state.regenerating) return;

        if (!window.confirm("This will regenerate the entire Program → Class → Batch structure from enrollment numbers.\n\nAll existing groupings will be recalculated. Continue?")) {
            return;
        }

        state.regenerating = true;
        const btn = refs.regenerateBtn;
        if (btn) {
            btn.classList.add("regenerating");
            btn.querySelector(".regen-text").textContent = "Regenerating...";
        }

        fetch("/api/admin/grouping/regenerate", {
            method: "POST",
            headers: { "Content-Type": "application/json" }
        })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(result => {
                state.regenerating = false;
                if (btn) {
                    btn.classList.remove("regenerating");
                    btn.querySelector(".regen-text").textContent = "Regenerate Structure";
                }

                const assigned = result.totalAssigned || 0;
                const skipped = result.totalSkipped || 0;
                const courses = result.totalCourses || 0;
                showToast(`✅ Regeneration complete: ${assigned} students assigned across ${courses} programs (${skipped} skipped)`);

                // Reload everything
                state.selectedProgramId = null;
                loadPrograms();
                loadHierarchy();
            })
            .catch(error => {
                state.regenerating = false;
                if (btn) {
                    btn.classList.remove("regenerating");
                    btn.querySelector(".regen-text").textContent = "Regenerate Structure";
                }
                showToast(`❌ Regeneration failed: ${error.message}`);
            });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HIERARCHY
    // ═══════════════════════════════════════════════════════════════════════════

    function loadHierarchy(options = {}) {
        if (state.loading) return;

        // If we already have hierarchy data and are just refreshing, render immediately from cache
        // to avoid visible loading delay, then silently refresh in background
        const hasExistingData = state.hierarchy?.classes?.length > 0;
        if (options.preserveState && hasExistingData) {
            // Don't show loading spinner — just silently refresh
            state.loading = true;
            const params = new URLSearchParams();
            if (state.filters.course) params.append("course", state.filters.course);
            if (state.filters.semester) params.append("semester", state.filters.semester);
            if (state.filters.performance) params.append("performance", state.filters.performance);

            fetch(`/api/admin/students-hierarchy?${params.toString()}`)
                .then(response => { if (!response.ok) throw new Error(`HTTP ${response.status}`); return response.json(); })
                .then(payload => {
                    state.hierarchy = normalizeHierarchy(payload);
                    state.loading = false;
                    updateStatistics();
                    renderHierarchy();
                })
                .catch(() => { state.loading = false; });
            return;
        }

        state.loading = true;
        showLoadingState();

        const params = new URLSearchParams();
        if (state.filters.course) params.append("course", state.filters.course);
        if (state.filters.semester) params.append("semester", state.filters.semester);
        if (state.filters.performance) params.append("performance", state.filters.performance);

        fetch(`/api/admin/students-hierarchy?${params.toString()}`)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(payload => {
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
            .catch(error => {
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

        if (refs.totalPrograms) refs.totalPrograms.textContent = summary.totalPrograms ?? state.programs?.length ?? 0;
        if (refs.totalClasses) refs.totalClasses.textContent = summary.totalClasses ?? classes.length ?? 0;
        if (refs.totalBatches) refs.totalBatches.textContent = summary.totalBatches ?? countBatches(classes);
        if (refs.totalStudents) refs.totalStudents.textContent = summary.totalStudents ?? countStudents(classes);
        if (refs.avgAttendance) refs.avgAttendance.textContent = `${averageAttendance(classes).toFixed(1)}%`;
        if (refs.totalLabel) refs.totalLabel.textContent = `${summary.totalStudents ?? countStudents(classes)} records`;
    }

    function renderHierarchy() {
        const classes = Array.isArray(state.hierarchy?.classes) ? state.hierarchy.classes : [];
        const filtered = applyFilters(classes);

        // Fallback: If filters remove all data but classes exist, reset filters and re-render
        if (!filtered.length && classes.length > 0) {
            console.warn("Filters removed all data → resetting filters");
            state.filters.searchQuery = "";
            state.filters.course = "";
            state.filters.semester = "";
            state.filters.performance = "";
            // Optionally, reset UI filter controls if needed
            if (refs.courseFilter) refs.courseFilter.value = "";
            if (refs.semesterFilter) refs.semesterFilter.value = "";
            if (refs.performanceFilter) refs.performanceFilter.value = "";
            return renderHierarchy();
        }

        // If still no data, show a user-friendly message and reset button
        if (!filtered.length) {
            if (refs.noDataState) {
                refs.noDataState.innerHTML = `
                    <p>No students match current filters.</p>
                    <button onclick="resetFilters()">Reset Filters</button>
                `;
                refs.noDataState.hidden = false;
            }
            if (refs.classesContainer) refs.classesContainer.innerHTML = "";
            return;
        }

        refs.noDataState.hidden = true;
        if (refs.loadingSpinner) refs.loadingSpinner.hidden = true;
        refs.classesContainer.hidden = false;
        refs.classesContainer.style.display = "grid";
        refs.classesContainer.innerHTML = filtered.map((classItem, index) => renderClassCard(classItem, index)).join("");
        bindRenderedInteractions();
    }

    // Add a global function for the reset button
    window.resetFilters = function() {
        state.filters.searchQuery = "";
        state.filters.course = "";
        state.filters.semester = "";
        state.filters.performance = "";
        if (refs.courseFilter) refs.courseFilter.value = "";
        if (refs.semesterFilter) refs.semesterFilter.value = "";
        if (refs.performanceFilter) refs.performanceFilter.value = "";
        renderHierarchy();
    }

    function applyFilters(classes) {
        const query = state.filters.searchQuery.trim().toLowerCase();
        const mode = state.filters.groupingMode;

        return sortClasses(classes, mode)
            .map(classItem => {
                const batches = sortBatches(classItem.batches || [], mode)
                    .map(batch => {
                        const students = (batch.students || []).filter(student => studentMatches(student, query));
                        const batchMatch = !query || batchMatchesQuery(batch, query) || students.length > 0;
                        return batchMatch ? { ...batch, students: query ? students : batch.students || [] } : null;
                    })
                    .filter(Boolean);

                const classMatch = !query || classMatchesQuery(classItem, query) || batches.length > 0;
                return classMatch ? { ...classItem, batches } : null;
            })
            .filter(Boolean);
    }

    function sortClasses(classes, mode) {
        const pinnedClasses = JSON.parse(localStorage.getItem("pinnedClasses") || "[]");
        return [...classes].sort((left, right) => {
            const la = left.analytics || left.classAnalytics || {};
            const ra = right.analytics || right.classAnalytics || {};
            const lid = String(left.id ?? left.classId ?? `class-${left.number}`);
            const rid = String(right.id ?? right.classId ?? `class-${right.number}`);
            const lp = pinnedClasses.includes(lid);
            const rp = pinnedClasses.includes(rid);
            if (lp && !rp) return -1;
            if (!lp && rp) return 1;
            if (mode === "performance") return (ra.avgMarks || ra.averageMarks || 0) - (la.avgMarks || la.averageMarks || 0) || (left.number || 0) - (right.number || 0);
            if (mode === "attendance") return (ra.attendance || 0) - (la.attendance || 0) || (left.number || 0) - (right.number || 0);
            if (mode === "ai") return (ra.riskStudents || 0) - (la.riskStudents || 0) || (ra.avgMarks || 0) - (la.avgMarks || 0);
            // Default ("number" or any other value): sort ascending by class number.
            return (left.number || left.classNumber || 0) - (right.number || right.classNumber || 0);
        });
    }

    function sortBatches(batches, mode) {
        return [...batches].sort((left, right) => {
            const la = left.analytics || {};
            const ra = right.analytics || {};
            if (mode === "performance") return (ra.avgMarks || ra.averageMarks || 0) - (la.avgMarks || la.averageMarks || 0) || (left.number || 0) - (right.number || 0);
            if (mode === "attendance") return (ra.attendance || 0) - (la.attendance || 0) || (left.number || 0) - (right.number || 0);
            if (mode === "ai") return (ra.riskStudents || 0) - (la.riskStudents || 0) || (ra.avgMarks || 0) - (la.avgMarks || 0);
            // Default ("number" or any other value): sort ascending by batch number.
            return (left.number || left.batchNumber || 0) - (right.number || right.batchNumber || 0);
        });
    }

    function renderClassCard(classItem, index) {
        const classId = classItem.id ?? classItem.classId ?? `class-${classItem.number}`;
        const classNumber = classItem.number ?? classItem.classNumber ?? index + 1;
        const classLabel = classItem.label || `Class ${classNumber}`;
        const analytics = classItem.analytics || classItem.classAnalytics || {};
        const batches = Array.isArray(classItem.batches) ? classItem.batches : [];
        const query = state.filters.searchQuery.trim().toLowerCase();
        const matchesSearch = query ? (classMatchesQuery(classItem, query) || batches.some(b => batchMatchesQuery(b, query) || (b.students || []).some(s => studentMatches(s, query)))) : false;
        const expanded = state.allExpanded || state.expandedClasses.has(String(classId)) || matchesSearch;

        const totalStudents = countStudentsInClass(classItem);
        const avgMarks = analytics.avgMarks ?? analytics.averageMarks ?? 0;
        const attendance = analytics.attendance ?? analytics.averageAttendance ?? 0;
        const riskStudents = analytics.riskStudents ?? 0;
        const riskFactor = totalStudents > 0 ? (riskStudents * 100) / totalStudents : 0;
        const healthScore = analytics.healthScore ?? Math.max(0, Math.min(100, avgMarks + attendance - riskFactor));

        let status = "healthy";
        let statusText = "Healthy";
        if (riskStudents > totalStudents * 0.3) { status = "critical"; statusText = "Critical"; }
        else if (riskStudents > totalStudents * 0.15) { status = "moderate"; statusText = "Moderate"; }

        const heatmapColor = healthScore < 40 ? "heatmap-critical" : healthScore < 60 ? "heatmap-poor" : healthScore < 75 ? "heatmap-average" : healthScore < 85 ? "heatmap-good" : "heatmap-excellent";
        const radius = 25;
        const circumference = 2 * Math.PI * radius;
        const offset = circumference - (healthScore / 100) * circumference;

        const batchPills = batches.map(b => {
            const bid = b.id ?? b.number;
            const blabel = b.label || `Batch ${b.number}`;
            const bStudents = Array.isArray(b.students) ? b.students.length : (b.totalStudents || b.studentsCount || 0);
            return `<button class="batch-pill-btn" data-batch-pill-class="${escapeHtml(String(classId))}" data-batch-pill-id="${escapeHtml(String(bid))}">${escapeHtml(blabel)} <span class="pill-count">${bStudents}</span></button>`;
        }).join("");

        return `
            <article class="class-card card-container glass-panel ${heatmapColor}" data-class-id="${escapeHtml(String(classId))}" data-class-number="${escapeHtml(String(classNumber))}">
                <header class="header" data-open-class="${escapeHtml(String(classId))}">
                    <div class="header-left">
                        <h3 class="class-title">${escapeHtml(classLabel)}</h3>
                        <span class="status-badge ${status}">${status === "healthy" ? "🟢" : status === "moderate" ? "🟡" : "🔴"} ${statusText}</span>
                    </div>
                    <div class="progress-container" title="Health score ${formatNumber(healthScore)}%">
                        <svg class="progress-ring" width="60" height="60" viewBox="0 0 60 60" aria-hidden="true">
                            <circle cx="30" cy="30" r="${radius}" stroke="rgba(148, 163, 184, 0.18)" stroke-width="6" fill="none"></circle>
                            <circle class="progress-ring-circle" cx="30" cy="30" r="${radius}" stroke="rgba(96, 165, 250, 0.9)" stroke-width="6" fill="none" style="stroke-dasharray:${circumference};stroke-dashoffset:${offset}"></circle>
                        </svg>
                        <div class="progress-value">${formatNumber(healthScore)}%</div>
                    </div>
                </header>

                <section class="new-inner-panel">
                    <div class="metrics-grid">
                        <div class="metric">
                            <div class="metric-label">Students</div>
                            <div class="metric-value">${totalStudents}</div>
                        </div>
                        <div class="metric">
                            <div class="metric-label">Avg Marks</div>
                            <div class="metric-value">${formatNumber(avgMarks)}</div>
                        </div>
                        <div class="metric">
                            <div class="metric-label">Attendance</div>
                            <div class="metric-value">${formatNumber(attendance)}%</div>
                        </div>
                    </div>
                    ${batches.length ? `<div class="batch-pills-row">${batchPills}</div>` : ""}
                </section>
            </article>
        `;
    }

    function renderBatchCard(batch, batchIndex, classNumber) {
        const batchId = batch.id ?? batch.batchId ?? `batch-${classNumber}-${batch.number}`;
        const batchNumber = batch.number ?? batch.batchNumber ?? batchIndex + 1;
        const analytics = batch.analytics || {};
        const students = Array.isArray(batch.students) ? batch.students : [];
        const query = state.filters.searchQuery.trim().toLowerCase();
        const matchesSearch = query ? (batchMatchesQuery(batch, query) || students.some(s => studentMatches(s, query))) : false;
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
                    ${students.map(student => renderStudentRow(student, classNumber, batchNumber)).join("")}
                </div>
            </section>
        `;
    }

    function renderStudentRow(student, classNumber, batchNumber) {
        const studentId = student.id ?? student.studentId ?? student.enrollment;
        const initials = getInitials(student.name || "Student");
        const photoUrl = student.profileImage || student.profilePhotoUrl || student.photoUrl || "";
        const performanceBand = student.performanceBand || student.performance?.status || performanceBandFromMarks(student.performance?.averageMarks ?? student.marks ?? 0);
        const marks = student.performance?.averageMarks ?? student.marks ?? 0;
        const attendance = student.attendance ?? student.performance?.attendance ?? 0;
        const searchMatch = studentMatches(student, state.filters.searchQuery.trim().toLowerCase());

        return `
            <article class="student-row ${searchMatch ? "" : "match-hidden"}" draggable="true" data-student-id="${escapeHtml(String(studentId))}" data-class-number="${escapeHtml(String(classNumber))}" data-batch-number="${escapeHtml(String(batchNumber))}">
                <div class="student-info">
                    ${photoUrl
                ? `<img src="${escapeHtml(photoUrl)}" class="student-avatar" alt="${escapeHtml(initials)}">`
                : `<div class="student-avatar">${escapeHtml(initials)}</div>`
            }
                    <div class="student-details">
                        <div class="student-name">${escapeHtml(student.name || "Unnamed Student")}</div>
                        <div class="student-meta">${escapeHtml(student.enrollment || student.enrollmentNumber || student.rollNumber || "")}${student.email ? ` • ${escapeHtml(student.email)}` : ""}</div>
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

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    function bindRenderedInteractions() {
        // Batch pill clicks → open batch modal directly
        document.querySelectorAll(".batch-pill-btn").forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.stopPropagation();
                const classId = btn.dataset.batchPillClass;
                const batchId = btn.dataset.batchPillId;
                openBatchModal(classId, batchId);
            });
        });

        // Class card clicks → open class modal
        document.querySelectorAll(".class-card").forEach(card => {
            card.addEventListener("click", (e) => {
                if (e.target.closest(".batch-pill-btn")) return;
                const classId = card.dataset.classId;
                if (classId) openClassModal(classId);
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL-SCREEN CLASS MODAL
    // ═══════════════════════════════════════════════════════════════════════════

    function openClassModal(classId) {
        const classes = state.hierarchy?.classes || [];
        const classItem = classes.find(c => String(c.id ?? c.classId ?? `class-${c.number}`) === String(classId));
        if (!classItem) return;

        closeAllModals();

        const classLabel = classItem.label || `Class ${classItem.number}`;
        const analytics = classItem.analytics || {};
        const batches = Array.isArray(classItem.batches) ? classItem.batches : [];
        const totalStudents = countStudentsInClass(classItem);
        const avgMarks = analytics.avgMarks ?? analytics.averageMarks ?? 0;
        const attendance = analytics.attendance ?? 0;
        const riskStudents = analytics.riskStudents ?? 0;
        const healthScore = analytics.healthScore ?? Math.max(0, Math.min(100, avgMarks + attendance - (totalStudents > 0 ? (riskStudents * 100) / totalStudents : 0)));

        // Performance distribution
        let excellent = 0, good = 0, average = 0, poor = 0;
        batches.forEach(b => {
            (b.students || []).forEach(s => {
                const m = s.marks ?? 0;
                if (m >= 75) excellent++; else if (m >= 60) good++; else if (m >= 50) average++; else poor++;
            });
        });

        const batchCards = batches.map(batch => {
            const ba = batch.analytics || {};
            const students = Array.isArray(batch.students) ? batch.students : [];
            const bLabel = batch.label || `Batch ${batch.number}`;
            const bAvg = ba.avgMarks ?? 0;
            const bAtt = ba.attendance ?? 0;
            const bRisk = ba.riskStudents ?? 0;
            const bid = batch.id ?? batch.number;
            const perf = bAvg >= 75 ? "excellent" : bAvg >= 60 ? "good" : bAvg >= 50 ? "average" : "poor";
            return `
                <div class="fm-batch-card" data-fm-class="${escapeHtml(String(classId))}" data-fm-batch="${escapeHtml(String(bid))}">
                    <div class="fm-batch-top">
                        <span class="fm-batch-name">${escapeHtml(bLabel)}</span>
                        <span class="fm-perf-dot ${perf}"></span>
                    </div>
                    <div class="fm-batch-stats">
                        <div class="fm-bs"><div class="fm-bs-val">${students.length}</div><div class="fm-bs-lbl">Students</div></div>
                        <div class="fm-bs"><div class="fm-bs-val">${formatNumber(bAvg)}</div><div class="fm-bs-lbl">Avg Marks</div></div>
                        <div class="fm-bs"><div class="fm-bs-val">${formatNumber(bAtt)}%</div><div class="fm-bs-lbl">Attendance</div></div>
                        <div class="fm-bs"><div class="fm-bs-val fm-risk">${bRisk}</div><div class="fm-bs-lbl">At Risk</div></div>
                    </div>
                </div>`;
        }).join("");

        const overlay = document.createElement("div");
        overlay.className = "fm-overlay";
        overlay.id = "fmClassModal";
        overlay.innerHTML = `
            <div class="fm-modal">
                <div class="fm-header">
                    <div>
                        <h2 class="fm-title">${escapeHtml(classLabel)}</h2>
                        <p class="fm-subtitle">${batches.length} Batches · ${totalStudents} Students</p>
                    </div>
                    <button class="fm-close" aria-label="Close">✕</button>
                </div>
                <div class="fm-body">
                    <div class="fm-stats-row">
                        <div class="fm-stat"><div class="fm-stat-val">${totalStudents}</div><div class="fm-stat-lbl">Total Students</div></div>
                        <div class="fm-stat"><div class="fm-stat-val">${formatNumber(avgMarks)}</div><div class="fm-stat-lbl">Avg Marks</div></div>
                        <div class="fm-stat"><div class="fm-stat-val">${formatNumber(attendance)}%</div><div class="fm-stat-lbl">Attendance</div></div>
                        <div class="fm-stat"><div class="fm-stat-val">${batches.length}</div><div class="fm-stat-lbl">Batches</div></div>
                        <div class="fm-stat"><div class="fm-stat-val fm-risk">${riskStudents}</div><div class="fm-stat-lbl">At Risk</div></div>
                        <div class="fm-stat"><div class="fm-stat-val">${formatNumber(healthScore)}%</div><div class="fm-stat-lbl">Health Score</div></div>
                    </div>

                    <div class="fm-section-row">
                        <div class="fm-section fm-health-section">
                            <h3 class="fm-section-title">Health Score</h3>
                            <div class="fm-health-bar-wrap">
                                <div class="fm-health-bar" style="width:${healthScore}%;background:${healthScore >= 75 ? '#22c55e' : healthScore >= 50 ? '#f59e0b' : '#ef4444'}"></div>
                            </div>
                            <span class="fm-health-val">${formatNumber(healthScore)}%</span>
                        </div>
                        <div class="fm-section fm-dist-section">
                            <h3 class="fm-section-title">Performance Distribution</h3>
                            <div class="fm-dist-grid">
                                <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#22c55e"></span>Excellent <strong>${excellent}</strong></div>
                                <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#60a5fa"></span>Good <strong>${good}</strong></div>
                                <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#f59e0b"></span>Average <strong>${average}</strong></div>
                                <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#ef4444"></span>Poor <strong>${poor}</strong></div>
                            </div>
                        </div>
                    </div>

                    <h3 class="fm-section-title" style="margin-top:20px;">Batches</h3>
                    <div class="fm-batches-grid">${batchCards || '<p style="color:#94a3b8">No batches</p>'}</div>
                </div>
            </div>`;

        document.body.appendChild(overlay);
        requestAnimationFrame(() => overlay.classList.add("fm-visible"));

        overlay.querySelector(".fm-close").onclick = () => closeAllModals();
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeAllModals(); });

        overlay.querySelectorAll(".fm-batch-card").forEach(card => {
            card.addEventListener("click", () => {
                openBatchModal(card.dataset.fmClass, card.dataset.fmBatch);
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL-SCREEN BATCH MODAL
    // ═══════════════════════════════════════════════════════════════════════════

    function openBatchModal(classId, batchId) {
        const classes = state.hierarchy?.classes || [];
        const classItem = classes.find(c => String(c.id ?? c.classId ?? `class-${c.number}`) === String(classId));
        if (!classItem) return;
        const batches = Array.isArray(classItem.batches) ? classItem.batches : [];
        const batch = batches.find(b => String(b.id ?? b.number) === String(batchId));
        if (!batch) return;

        closeAllModals();

        const ba = batch.analytics || {};
        const students = Array.isArray(batch.students) ? batch.students : [];
        const batchLabel = batch.label || `Batch ${batch.number}`;
        const classLabel = classItem.label || `Class ${classItem.number}`;
        const bAvg = ba.avgMarks ?? 0;
        const bAtt = ba.attendance ?? 0;
        const bRisk = ba.riskStudents ?? 0;

        let excellent = 0, good = 0, average = 0, poor = 0;
        students.forEach(s => { const m = s.marks ?? 0; if (m >= 75) excellent++; else if (m >= 60) good++; else if (m >= 50) average++; else poor++; });

        // Build class/batch options for transfer dropdown
        const allClasses = state.hierarchy?.classes || [];
        const transferOptions = allClasses.flatMap(c => {
            const cLabel = c.label || `Class ${c.number}`;
            return (c.batches || []).map(b => {
                const bLabel = b.label || `Batch ${b.number}`;
                const cNum = c.number ?? c.classNumber;
                const bNum = b.number ?? b.batchNumber;
                return `<option value="${cNum}:${bNum}">${escapeHtml(cLabel)} → ${escapeHtml(bLabel)}</option>`;
            });
        }).join("");

        // Sort students by enrollment number
        students.sort((a, b) => {
            const eA = (a.enrollment || a.enrollmentNumber || a.id || '').toString();
            const eB = (b.enrollment || b.enrollmentNumber || b.id || '').toString();
            return eA.localeCompare(eB, undefined, { numeric: true });
        });

        const studentRows = students.length === 0
            ? '<div style="text-align:center;padding:40px 20px;color:#94a3b8;grid-column:1/-1"><div style="font-size:48px;margin-bottom:12px">📭</div><p>No students in this batch.</p></div>'
            : students.map(s => {
                const sid = s.id ?? s.studentId ?? s.enrollment;
                const initials = getInitials(s.name || "Student");
                const marks = s.marks ?? 0;
                const att = s.attendance ?? 0;
                const band = s.performanceBand || performanceBandFromMarks(marks);
                const photoUrl = s.profilePhotoUrl || s.photoUrl || "";
                const avatarHtml = photoUrl
                    ? `<img src="${escapeHtml(photoUrl)}" class="fm-student-avatar fm-avatar-img" alt="${escapeHtml(initials)}">`
                    : `<div class="fm-student-avatar">${escapeHtml(initials)}</div>`;

                return `
                    <div class="fm-student-card-wrap" data-student-id="${escapeHtml(String(sid))}">
                        <div class="fm-student-card">
                            <div class="fm-avatar-wrap">
                                ${avatarHtml}
                                <button class="fm-photo-btn" title="Upload photo">📷</button>
                                <input type="file" class="fm-photo-input" accept="image/*" hidden>
                            </div>
                            <div class="fm-student-info">
                                <div class="fm-student-name">${escapeHtml(s.name || "Unnamed")}</div>
                                <div class="fm-student-meta">${escapeHtml(s.enrollment || s.enrollmentNumber || sid || "")}${s.email ? ` · ${escapeHtml(s.email)}` : ""}</div>
                            </div>
                            <div class="fm-student-badges">
                                <span class="fm-perf-badge ${band}">${formatNumber(marks)} marks</span>
                                <span class="fm-att-badge">${formatNumber(att)}%</span>
                            </div>
                            <div class="fm-student-actions">
                                <button class="fm-action-btn fm-edit-btn" title="Edit details">✏️</button>
                                <button class="fm-action-btn fm-transfer-btn" title="Transfer">🔀</button>
                                <button class="fm-action-btn fm-profile-btn" title="View profile">👤</button>
                            </div>
                        </div>
                        <div class="fm-transfer-panel" hidden>
                            <select class="fm-transfer-select"><option value="">Select target...</option>${transferOptions}</select>
                            <button class="fm-transfer-confirm">Transfer</button>
                            <button class="fm-transfer-cancel">Cancel</button>
                        </div>
                        <div class="fm-edit-panel" hidden>
                            <div class="fm-edit-grid">
                                <div class="fm-edit-field"><label>Name</label><input class="fm-edit-input" data-field="fullName" value="${escapeHtml(s.name || "")}"></div>
                                <div class="fm-edit-field"><label>Email</label><input class="fm-edit-input" data-field="email" value="${escapeHtml(s.email || "")}"></div>
                                <div class="fm-edit-field"><label>Phone</label><input class="fm-edit-input" data-field="phone" value="${escapeHtml(s.phone || "")}"></div>
                                <div class="fm-edit-field"><label>Course</label><input class="fm-edit-input" data-field="course" value="${escapeHtml(s.course || "")}"></div>
                                <div class="fm-edit-field"><label>Semester</label><input class="fm-edit-input" data-field="semester" value="${escapeHtml(s.semester || "")}"></div>
                                <div class="fm-edit-field"><label>Department</label><input class="fm-edit-input" data-field="department" value="${escapeHtml(s.department || "")}"></div>
                            </div>
                            <div class="fm-edit-actions">
                                <button class="fm-edit-save">💾 Save</button>
                                <button class="fm-edit-cancel">Cancel</button>
                            </div>
                        </div>
                    </div>`;
            }).join("");

        const overlay = document.createElement("div");
        overlay.className = "fm-overlay";
        overlay.id = "fmBatchModal";
        overlay.innerHTML = `
            <div class="fm-modal fm-modal-wide">
                <div class="fm-header">
                    <div>
                        <button class="fm-back" aria-label="Back to class">← Back</button>
                        <h2 class="fm-title">${escapeHtml(batchLabel)}</h2>
                        <p class="fm-subtitle">${escapeHtml(classLabel)} · ${students.length} Students</p>
                    </div>
                    <button class="fm-close" aria-label="Close">✕</button>
                </div>
                <div class="fm-body">
                    <div class="fm-stats-row">
                        <div class="fm-stat"><div class="fm-stat-val">${students.length}</div><div class="fm-stat-lbl">Students</div></div>
                        <div class="fm-stat"><div class="fm-stat-val">${formatNumber(bAvg)}</div><div class="fm-stat-lbl">Avg Marks</div></div>
                        <div class="fm-stat"><div class="fm-stat-val">${formatNumber(bAtt)}%</div><div class="fm-stat-lbl">Attendance</div></div>
                        <div class="fm-stat"><div class="fm-stat-val fm-risk">${bRisk}</div><div class="fm-stat-lbl">At Risk</div></div>
                    </div>
                    <div class="fm-section fm-dist-section" style="margin-bottom:16px">
                        <h3 class="fm-section-title">Performance Distribution</h3>
                        <div class="fm-dist-grid">
                            <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#22c55e"></span>Excellent <strong>${excellent}</strong></div>
                            <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#60a5fa"></span>Good <strong>${good}</strong></div>
                            <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#f59e0b"></span>Average <strong>${average}</strong></div>
                            <div class="fm-dist-item"><span class="fm-dist-dot" style="background:#ef4444"></span>Poor <strong>${poor}</strong></div>
                        </div>
                    </div>
                    <h3 class="fm-section-title">Students</h3>
                    <div class="fm-students-grid">${studentRows}</div>
                </div>
            </div>`;

        document.body.appendChild(overlay);
        requestAnimationFrame(() => overlay.classList.add("fm-visible"));

        overlay.querySelector(".fm-close").onclick = () => closeAllModals();
        overlay.querySelector(".fm-back").onclick = () => { closeAllModals(); openClassModal(classId); };
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeAllModals(); });

        // Bind student actions
        overlay.querySelectorAll(".fm-student-card-wrap").forEach(wrap => {
            const sid = wrap.dataset.studentId;

            // Profile button
            wrap.querySelector(".fm-profile-btn")?.addEventListener("click", (e) => { e.stopPropagation(); openProfile(sid); });

            // Photo upload
            const photoBtn = wrap.querySelector(".fm-photo-btn");
            const photoInput = wrap.querySelector(".fm-photo-input");
            photoBtn?.addEventListener("click", (e) => { e.stopPropagation(); photoInput?.click(); });
            photoInput?.addEventListener("change", (e) => {
                e.stopPropagation();
                const file = photoInput.files?.[0];
                if (!file) return;
                // Validate file type client-side
                if (!file.type.startsWith("image/")) {
                    showToast("❌ Please select a valid image file");
                    photoInput.value = "";
                    return;
                }
                // Validate file size (max 10MB raw)
                if (file.size > 10 * 1024 * 1024) {
                    showToast("❌ Image is too large. Please select an image under 10MB.");
                    photoInput.value = "";
                    return;
                }
                photoBtn.textContent = "⏳";
                // Compress aggressively to avoid server body-size limits
                compressImageSafe(file).then(base64 => {
                    return fetch(`/api/admin/student/${encodeURIComponent(sid)}/profile`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ profileImage: base64 })
                    });
                })
                    .then(r => {
                        if (!r.ok) return r.text().then(t => { throw new Error(t || `HTTP ${r.status}`); });
                        return r.json();
                    })
                    .then(res => {
                        photoBtn.textContent = "✅";
                        const avatarWrap = wrap.querySelector(".fm-avatar-wrap");
                        const existingAvatar = avatarWrap.querySelector(".fm-student-avatar");
                        if (existingAvatar) {
                            const img = document.createElement("img");
                            img.src = res.profileImage || "";
                            img.className = "fm-student-avatar fm-avatar-img";
                            existingAvatar.replaceWith(img);
                        }
                        showToast("✅ Photo uploaded successfully");
                        setTimeout(() => { photoBtn.textContent = "📷"; }, 2000);
                    })
                    .catch(err => {
                        photoBtn.textContent = "📷";
                        const msg = err.message || "Unknown error";
                        if (msg.includes("413") || msg.toLowerCase().includes("too large") || msg.toLowerCase().includes("size")) {
                            showToast("❌ Image too large even after compression. Try a smaller photo.");
                        } else {
                            showToast(`❌ Photo upload failed: ${msg.substring(0, 120)}`);
                        }
                        photoInput.value = "";
                    });
            });

            // Transfer
            const transferBtn = wrap.querySelector(".fm-transfer-btn");
            const transferPanel = wrap.querySelector(".fm-transfer-panel");
            transferBtn?.addEventListener("click", (e) => { e.stopPropagation(); transferPanel.hidden = !transferPanel.hidden; wrap.querySelector(".fm-edit-panel").hidden = true; });
            wrap.querySelector(".fm-transfer-cancel")?.addEventListener("click", (e) => { e.stopPropagation(); transferPanel.hidden = true; });
            wrap.querySelector(".fm-transfer-confirm")?.addEventListener("click", (e) => {
                e.stopPropagation();
                const select = wrap.querySelector(".fm-transfer-select");
                const val = select?.value;
                if (!val) { showToast("⚠️ Select a target class/batch"); return; }
                const [classNum, batchNum] = val.split(":").map(Number);
                const confirmBtn = wrap.querySelector(".fm-transfer-confirm");
                confirmBtn.textContent = "Transferring...";
                confirmBtn.disabled = true;
                fetch("/api/admin/students-hierarchy/reassign", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ studentId: sid, classNumber: classNum, batchNumber: batchNum })
                })
                    .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
                    .then(() => {
                        showToast(`✅ Student transferred successfully`);
                        transferPanel.hidden = true;
                        loadHierarchy({ preserveState: true });
                        setTimeout(() => openBatchModal(classId, batchId), 800);
                    })
                    .catch(err => { showToast(`❌ Transfer failed: ${err.message}`); confirmBtn.textContent = "Transfer"; confirmBtn.disabled = false; });
            });

            // Edit
            const editBtn = wrap.querySelector(".fm-edit-btn");
            const editPanel = wrap.querySelector(".fm-edit-panel");
            editBtn?.addEventListener("click", (e) => { e.stopPropagation(); editPanel.hidden = !editPanel.hidden; wrap.querySelector(".fm-transfer-panel").hidden = true; });
            wrap.querySelector(".fm-edit-cancel")?.addEventListener("click", (e) => { e.stopPropagation(); editPanel.hidden = true; });
            wrap.querySelector(".fm-edit-save")?.addEventListener("click", (e) => {
                e.stopPropagation();
                const payload = {};
                editPanel.querySelectorAll(".fm-edit-input").forEach(input => {
                    const field = input.dataset.field;
                    const val = input.value.trim();
                    if (field && val) payload[field] = val;
                });
                const saveBtn = wrap.querySelector(".fm-edit-save");
                saveBtn.textContent = "Saving...";
                saveBtn.disabled = true;
                fetch(`/api/admin/student/${encodeURIComponent(sid)}/profile`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                })
                    .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
                    .then(() => {
                        showToast("✅ Student details updated and saved to database");
                        editPanel.hidden = true;
                        saveBtn.textContent = "💾 Save";
                        saveBtn.disabled = false;
                        // Update name in the card
                        if (payload.fullName) {
                            const nameEl = wrap.querySelector(".fm-student-name");
                            if (nameEl) nameEl.textContent = payload.fullName;
                        }
                        // Reload hierarchy to reflect changes in the main view
                        loadHierarchy({ preserveState: true });
                    })
                    .catch(err => { showToast(`❌ Update failed: ${err.message}`); saveBtn.textContent = "💾 Save"; saveBtn.disabled = false; });
            });
        });
    }

    function closeAllModals() {
        document.querySelectorAll(".fm-overlay").forEach(el => el.remove());
    }

    function handleClassToggle(event) {
        const classCard = event.currentTarget.closest(".class-card");
        const classId = classCard?.dataset.classId;
        const classBody = classCard?.querySelector(".class-body");
        if (!classId || !classBody) return;
        const expanded = classBody.classList.contains("collapsed");
        if (expanded) {
            classBody.classList.remove("collapsed");
            state.expandedClasses.add(String(classId));
        } else {
            classBody.classList.add("collapsed");
            state.expandedClasses.delete(String(classId));
        }
    }

    function handleClassKeydown(event) {
        if (event.key === "Enter" || event.key === " ") { event.preventDefault(); handleClassToggle(event); }
    }

    function toggleClassById(classId) {
        const classCard = document.querySelector(`.class-card[data-class-id="${cssEscape(classId)}"]`);
        const classBody = classCard?.querySelector(".class-body");
        if (!classBody) return;
        const isCollapsed = classBody.classList.contains("collapsed");
        if (isCollapsed) {
            classBody.classList.remove("collapsed");
            state.expandedClasses.add(String(classId));
        } else {
            classBody.classList.add("collapsed");
            state.expandedClasses.delete(String(classId));
        }
    }

    function togglePinnedClass(classId) {
        const pinnedClasses = JSON.parse(localStorage.getItem("pinnedClasses") || "[]");
        const id = String(classId);
        const idx = pinnedClasses.indexOf(id);
        if (idx >= 0) { pinnedClasses.splice(idx, 1); } else { pinnedClasses.push(id); }
        localStorage.setItem("pinnedClasses", JSON.stringify(pinnedClasses));
        renderHierarchy();
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
        if (event.key === "Enter" || event.key === " ") { event.preventDefault(); handleBatchToggle(event); }
    }

    // ─── Drag and Drop ─────────────────────────────────────────────────────

    function handleStudentDragStart(event) {
        const row = event.currentTarget;
        const payload = { studentId: row.dataset.studentId, classNumber: row.dataset.classNumber, batchNumber: row.dataset.batchNumber };
        state.draggingStudent = payload;
        row.classList.add("dragging");
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", JSON.stringify(payload));
    }

    function handleStudentDragEnd(event) {
        event.currentTarget.classList.remove("dragging");
        state.draggingStudent = null;
        document.querySelectorAll(".batch-drop-target").forEach(card => card.classList.remove("batch-drop-target"));
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
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                showToast(`Moved student to Class ${classNumber}, Batch ${batchNumber}`);
            })
            .catch(error => {
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
                const index = (batch.students || []).findIndex(s => String(s.id ?? s.studentId ?? s.enrollment) === String(studentId));
                if (index >= 0) { movingStudent = batch.students.splice(index, 1)[0]; break; }
            }
            if (movingStudent) break;
        }
        if (!movingStudent) return;
        movingStudent.classNumber = targetClassNumber;
        movingStudent.batchNumber = targetBatchNumber;
        const targetClass = classes.find(c => Number(c.number ?? c.classNumber) === Number(targetClassNumber));
        if (!targetClass) {
            classes.push({
                id: `class-${targetClassNumber}`, number: targetClassNumber, label: `Class ${targetClassNumber}`,
                analytics: { avgMarks: 0, attendance: 0, riskStudents: 0, presentToday: 0 },
                batches: [{ id: `batch-${targetClassNumber}-${targetBatchNumber}`, number: targetBatchNumber, label: `Batch ${targetBatchNumber}`, analytics: {}, students: [movingStudent] }]
            });
            return;
        }
        let targetBatch = (targetClass.batches || []).find(b => Number(b.number ?? b.batchNumber) === Number(targetBatchNumber));
        if (!targetBatch) {
            targetBatch = { id: `batch-${targetClassNumber}-${targetBatchNumber}`, number: targetBatchNumber, label: `Batch ${targetBatchNumber}`, analytics: {}, students: [] };
            targetClass.batches = [...(targetClass.batches || []), targetBatch];
        }
        targetBatch.students = [...(targetBatch.students || []), movingStudent];
    }

    // ─── Filters ────────────────────────────────────────────────────────────

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

    function onSearchChange(query) {
        state.filters.searchQuery = (query || "").toLowerCase();
        renderHierarchy();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADVANCED SEARCH SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════

    function initAdvancedSearch() {
        if (!refs.globalSearch) return;

        // Create search results dropdown container - append to body for fixed positioning
        const dropdown = document.createElement("div");
        dropdown.className = "search-dropdown";
        dropdown.id = "searchDropdown";
        dropdown.hidden = true;
        document.body.appendChild(dropdown);

        // Update placeholder
        refs.globalSearch.placeholder = "Search by name, enrollment, ID, phone, email…  Ctrl+K";

        // Debounced search handler
        const debouncedSearch = debounce((query) => {
            onSearchChange(query);
            if (query.length >= 2) {
                performServerSearch(query);
            } else {
                hideSearchDropdown();
            }
        }, 250);

        refs.globalSearch.addEventListener("input", (e) => {
            const query = e.target.value.trim();
            debouncedSearch(query);
        });

        // Keyboard navigation
        refs.globalSearch.addEventListener("keydown", (e) => {
            const dropdown = document.getElementById("searchDropdown");
            if (!dropdown || dropdown.hidden) {
                if (e.key === "Escape") { refs.globalSearch.blur(); return; }
                return;
            }

            const items = dropdown.querySelectorAll(".search-result-item");
            if (e.key === "ArrowDown") {
                e.preventDefault();
                state.searchActiveIndex = Math.min(state.searchActiveIndex + 1, items.length - 1);
                updateSearchHighlight(items);
            } else if (e.key === "ArrowUp") {
                e.preventDefault();
                state.searchActiveIndex = Math.max(state.searchActiveIndex - 1, -1);
                updateSearchHighlight(items);
            } else if (e.key === "Enter") {
                e.preventDefault();
                if (state.searchActiveIndex >= 0 && items[state.searchActiveIndex]) {
                    items[state.searchActiveIndex].click();
                }
            } else if (e.key === "Escape") {
                hideSearchDropdown();
            }
        });

        // Focus/blur management
        refs.globalSearch.addEventListener("focus", () => {
            if (state.searchResults.length > 0 && refs.globalSearch.value.trim().length >= 2) {
                renderSearchDropdown();
            }
        });

        document.addEventListener("click", (e) => {
            const dropdown = document.getElementById("searchDropdown");
            if (dropdown && !dropdown.contains(e.target) && e.target !== refs.globalSearch) {
                hideSearchDropdown();
            }
        });

        // Ctrl+K shortcut
        document.addEventListener("keydown", (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === "k") {
                e.preventDefault();
                refs.globalSearch.focus();
                refs.globalSearch.select();
            }
        });

        // Reposition dropdown on scroll/resize
        window.addEventListener("scroll", () => {
            const dd = document.getElementById("searchDropdown");
            if (dd && !dd.hidden) positionSearchDropdown();
        }, { passive: true });
        window.addEventListener("resize", () => {
            const dd = document.getElementById("searchDropdown");
            if (dd && !dd.hidden) positionSearchDropdown();
        }, { passive: true });
    }

    function performServerSearch(query) {
        // Cancel previous request
        if (state.searchAbortController) {
            state.searchAbortController.abort();
        }
        state.searchAbortController = new AbortController();

        const params = new URLSearchParams({ search: query, size: "12", sortBy: "name", sortDir: "asc" });
        fetch(`/api/admin/students?${params.toString()}`, { signal: state.searchAbortController.signal })
            .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
            .then(data => {
                state.searchResults = Array.isArray(data.items) ? data.items : [];
                state.searchActiveIndex = -1;
                const totalElements = data.totalElements || state.searchResults.length;
                renderSearchDropdown(totalElements);
            })
            .catch(err => {
                if (err.name === "AbortError") return;
                console.warn("Search failed:", err);
            });
    }

    function renderSearchDropdown(totalCount) {
        const dropdown = document.getElementById("searchDropdown");
        if (!dropdown) return;

        if (!state.searchResults.length) {
            dropdown.innerHTML = `
                <div class="search-empty">
                    <span class="search-empty-icon">🔍</span>
                    <span>No students found for this query</span>
                </div>`;
            dropdown.hidden = false;
            return;
        }

        const countLabel = totalCount > state.searchResults.length
            ? `Showing ${state.searchResults.length} of ${totalCount} results`
            : `${state.searchResults.length} results`;

        dropdown.innerHTML = `
            <div class="search-header">
                <span>${escapeHtml(countLabel)}</span>
                <span class="search-hint">↑↓ navigate · Enter select · Esc close</span>
            </div>
            <div class="search-results-list">
                ${state.searchResults.map((s, i) => {
            const initials = getInitials(s.name || "ST");
            const photoUrl = s.profileImage || s.profilePhotoUrl || s.photoUrl || "";
            const enrollment = s.enrollment || s.id || "";
            const course = s.course || s.degree || "";
            const email = s.email || "";
            const gender = s.gender || "";
            const classGroup = s.classGroup || "";
            const batchGroup = s.batchGroup || "";
            const avgMarks = typeof s.averageMarks === "number" ? s.averageMarks.toFixed(1) : "--";
            const phone = s.phone || "";
            const band = performanceBandFromMarks(s.averageMarks || 0);

            return `
                        <div class="search-result-item ${i === state.searchActiveIndex ? 'active' : ''}" data-student-id="${escapeHtml(s.id)}" data-index="${i}">
                            ${photoUrl
                    ? `<img src="${escapeHtml(photoUrl)}" class="search-result-avatar" alt="${escapeHtml(initials)}">`
                    : `<div class="search-result-avatar">${escapeHtml(initials)}</div>`
                }
                            <div class="search-result-info">
                                <div class="search-result-name">${escapeHtml(s.name || "Unnamed")}</div>
                                <div class="search-result-meta">
                                    <span class="search-meta-tag">🆔 ${escapeHtml(enrollment)}</span>
                                    ${course ? `<span class="search-meta-tag">📚 ${escapeHtml(course)}</span>` : ""}
                                    ${email ? `<span class="search-meta-tag">✉️ ${escapeHtml(truncate(email, 24))}</span>` : ""}
                                    ${phone ? `<span class="search-meta-tag">📞 ${escapeHtml(phone)}</span>` : ""}
                                    ${classGroup ? `<span class="search-meta-tag">C${escapeHtml(classGroup)}</span>` : ""}
                                    ${batchGroup ? `<span class="search-meta-tag">B${escapeHtml(batchGroup)}</span>` : ""}
                                </div>
                            </div>
                            <div class="search-result-score">
                                <span class="performance-badge ${band}">${avgMarks}</span>
                            </div>
                        </div>`;
        }).join("")}
            </div>`;

        dropdown.hidden = false;
        positionSearchDropdown();

        // Bind clicks
        dropdown.querySelectorAll(".search-result-item").forEach(item => {
            item.addEventListener("click", () => {
                const sid = item.dataset.studentId;
                if (sid) {
                    hideSearchDropdown();
                    openProfile(sid);
                }
            });
            item.addEventListener("mouseenter", () => {
                state.searchActiveIndex = parseInt(item.dataset.index);
                updateSearchHighlight(dropdown.querySelectorAll(".search-result-item"));
            });
        });
    }

    function updateSearchHighlight(items) {
        items.forEach((item, i) => {
            item.classList.toggle("active", i === state.searchActiveIndex);
        });
        // Scroll into view
        if (state.searchActiveIndex >= 0 && items[state.searchActiveIndex]) {
            items[state.searchActiveIndex].scrollIntoView({ block: "nearest" });
        }
    }

    function hideSearchDropdown() {
        const dropdown = document.getElementById("searchDropdown");
        if (dropdown) dropdown.hidden = true;
        state.searchActiveIndex = -1;
    }

    function positionSearchDropdown() {
        const dropdown = document.getElementById("searchDropdown");
        if (!dropdown || !refs.globalSearch) return;
        const rect = refs.globalSearch.getBoundingClientRect();
        const vpW = window.innerWidth;
        const vpH = window.innerHeight;
        const dropW = Math.max(rect.width, 420);
        const top = rect.bottom + 6;
        let left = rect.left;
        // Prevent right-edge overflow
        if (left + dropW > vpW - 12) {
            left = Math.max(12, vpW - dropW - 12);
        }
        // Clamp max-height so it never clips off the bottom of the viewport
        const availableHeight = vpH - top - 16;
        const maxH = Math.max(200, Math.min(480, availableHeight));
        dropdown.style.top = top + "px";
        dropdown.style.left = left + "px";
        dropdown.style.width = dropW + "px";
        dropdown.style.maxHeight = maxH + "px";
    }

    function expandAll() {
        state.allExpanded = true;
        state.expandedClasses.clear();
        state.expandedBatches.clear();
        (state.hierarchy.classes || []).forEach(classItem => {
            const classId = classItem.id ?? classItem.classId ?? `class-${classItem.number}`;
            state.expandedClasses.add(String(classId));
            (classItem.batches || []).forEach(batch => {
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
            body: JSON.stringify({ classNumber: null, course: state.filters.course || null, semester: state.filters.semester || null, clusters: 4 })
        })
            .then(response => { if (!response.ok) throw new Error(`HTTP ${response.status}`); return response.json(); })
            .then(payload => {
                const suggestions = Array.isArray(payload?.suggestions) ? payload.suggestions : [];
                const changedCount = suggestions.filter(item => item.changed).length;
                showToast(`AI grouping ready: ${changedCount} students suggested for reassignment`);
                renderHierarchy();
            })
            .catch(error => showToast(`AI grouping failed: ${error.message}`));
    }

    function toggleSidebar() {
        const sidebar = document.querySelector(".sidebar");
        if (!sidebar) return;
        sidebar.dataset.open = String(sidebar.dataset.open !== "true");
    }

    // ─── Actions ────────────────────────────────────────────────────────────

    function openProfile(studentId) { window.location.href = `/admin/students/${encodeURIComponent(studentId)}/profile`; }

    function openFaceUpload(studentId) { state.pendingFaceStudentId = studentId; refs.faceUploadInput?.click(); }

    function handleFaceUploadChange(event) {
        const file = event.target.files?.[0];
        const studentId = state.pendingFaceStudentId;
        if (!file || !studentId) return;
        const formData = new FormData();
        formData.append("studentId", studentId);
        formData.append("file", file);
        fetch("/api/admin/upload-face", { method: "POST", body: formData })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(text || `HTTP ${response.status}`);
                    });
                }
                return response.json();
            })
            .then(payload => showToast(payload?.message || `Face uploaded for ${studentId}`))
            .catch(error => showToast(`Face upload failed: ${error.message}`))
            .finally(() => { state.pendingFaceStudentId = null; event.target.value = ""; });
    }

    function deleteStudent(studentId) {
        if (!window.confirm(`Delete student ${studentId}? This cannot be undone.`)) return;
        fetch(`/api/admin/students/${encodeURIComponent(studentId)}`, { method: "DELETE" })
            .then(response => { if (!response.ok) throw new Error(`HTTP ${response.status}`); showToast(`Student ${studentId} deleted`); loadHierarchy({ preserveState: true }); })
            .catch(error => showToast(`Delete failed: ${error.message}`));
    }

    // ─── UI States ──────────────────────────────────────────────────────────

    function showLoadingState() {
        if (refs.loadingSpinner) refs.loadingSpinner.hidden = false;
        if (refs.noDataState) refs.noDataState.hidden = true;
        if (refs.classesContainer) { refs.classesContainer.hidden = true; refs.classesContainer.innerHTML = skeletonMarkup(); }
    }

    function showEmptyState() {
        if (refs.loadingSpinner) refs.loadingSpinner.hidden = true;
        if (refs.noDataState) refs.noDataState.hidden = false;
        if (refs.classesContainer) { refs.classesContainer.hidden = true; refs.classesContainer.innerHTML = ""; }
    }

    function skeletonMarkup() {
        return new Array(3).fill(0).map((_, i) => `
            <section class="class-card glass-panel">
                <div class="class-header"><div class="class-info"><div class="class-title">Loading class ${i + 1}...</div></div><div class="class-toggle">⌄</div></div>
                <div class="class-body"><div class="batch-card batch-1"><div class="loading-spinner"><div class="spinner"></div><p>Loading batches...</p></div></div></div>
            </section>
        `).join("");
    }

    // ─── Utilities ──────────────────────────────────────────────────────────

    function countBatches(classes) { return classes.reduce((sum, c) => sum + (c.batches?.length || 0), 0); }
    function countStudents(classes) { return classes.reduce((sum, c) => sum + countStudentsInClass(c), 0); }
    function countStudentsInClass(c) { return (c.batches || []).reduce((sum, b) => sum + (b.students?.length || 0), 0); }

    function averageAttendance(classes) {
        const values = [];
        classes.forEach(c => (c.batches || []).forEach(b => {
            const v = (b.analytics || {}).attendance ?? (b.analytics || {}).averageAttendance;
            if (typeof v === "number") values.push(v);
        }));
        return values.length ? values.reduce((s, v) => s + v, 0) / values.length : 0;
    }

    function studentMatches(student, query) {
        if (!query) return true;
        return [student.name, student.enrollment, student.enrollmentNumber, student.email, student.phone, student.classNumber, student.batchNumber].filter(Boolean).join(" ").toLowerCase().includes(query);
    }
    function batchMatchesQuery(batch, query) {
        if (!query) return true;
        return [batch.label, batch.batchLabel, batch.number, batch.id, batch.batchId].filter(Boolean).join(" ").toLowerCase().includes(query);
    }
    function classMatchesQuery(classItem, query) {
        if (!query) return true;
        return [classItem.label, classItem.classLabel, classItem.number, classItem.id, classItem.classId].filter(Boolean).join(" ").toLowerCase().includes(query);
    }

    function performanceBandFromMarks(marks) {
        const v = Number(marks) || 0;
        if (v >= 75) return "excellent";
        if (v >= 60) return "good";
        if (v >= 50) return "average";
        return "poor";
    }

    /** Compress an image file to a max dimension and quality, returns a base64 data URI promise. */
    function compressImage(file, maxDim, quality) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onerror = () => reject(new Error("Failed to read file"));
            reader.onload = () => {
                const img = new Image();
                img.onerror = () => reject(new Error("Failed to decode image"));
                img.onload = () => {
                    try {
                        let w = img.width, h = img.height;
                        if (w > maxDim || h > maxDim) {
                            if (w > h) { h = Math.round(h * maxDim / w); w = maxDim; }
                            else { w = Math.round(w * maxDim / h); h = maxDim; }
                        }
                        // Ensure minimum dimensions
                        w = Math.max(1, w);
                        h = Math.max(1, h);
                        const canvas = document.createElement("canvas");
                        canvas.width = w;
                        canvas.height = h;
                        const ctx = canvas.getContext("2d");
                        ctx.drawImage(img, 0, 0, w, h);
                        resolve(canvas.toDataURL("image/jpeg", quality));
                    } catch (err) {
                        reject(err);
                    }
                };
                img.src = reader.result;
            };
            reader.readAsDataURL(file);
        });
    }

    /**
     * Safe image compression with automatic quality reduction.
     * Tries progressively lower quality + dimensions until the result
     * is under the 2MB target (safe for JSON body in most Spring configs).
     */
    function compressImageSafe(file) {
        const MAX_BASE64_SIZE = 10 * 1024 * 1024; // keep original quality up to backend limit
        const fileToDataUri = (inputFile) => new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result || ""));
            reader.onerror = () => reject(new Error("Failed to read file"));
            reader.readAsDataURL(inputFile);
        });
        const attempts = [
            { maxDim: 1400, quality: 0.92 },
            { maxDim: 1200, quality: 0.88 },
            { maxDim: 1000, quality: 0.84 },
            { maxDim: 900, quality: 0.8 },
            { maxDim: 768, quality: 0.76 },
        ];

        return (async () => {
            const original = await fileToDataUri(file);
            if (original.length <= MAX_BASE64_SIZE) {
                return original;
            }
            for (const { maxDim, quality } of attempts) {
                const result = await compressImage(file, maxDim, quality);
                // Check byte size of the base64 payload
                const payloadSize = result.length;
                if (payloadSize <= MAX_BASE64_SIZE) {
                    return result;
                }
            }
            // Last resort: smallest possible
            return compressImage(file, 640, 0.72);
        })();
    }

    function formatNumber(value) { const n = Number(value) || 0; return n.toFixed(1).replace(/\.0$/, ""); }
    function truncate(text, max) { const v = String(text || ""); return v.length > max ? `${v.slice(0, max)}…` : v; }
    function getInitials(name) { const parts = String(name || "").trim().split(/\s+/).filter(Boolean); return parts.length ? parts.slice(0, 2).map(p => p.charAt(0).toUpperCase()).join("") : "ST"; }
    function escapeHtml(value) { const div = document.createElement("div"); div.textContent = String(value ?? ""); return div.innerHTML; }
    function cssEscape(value) { return window.CSS?.escape ? window.CSS.escape(String(value)) : String(value).replace(/"/g, '\\"'); }
    function safeParse(value) { try { return JSON.parse(value); } catch { return null; } }
    function cloneHierarchy(value) { return JSON.parse(JSON.stringify(value || { summary: {}, classes: [] })); }
    function debounce(callback, delay) { let timer = null; return function (...args) { clearTimeout(timer); timer = setTimeout(() => callback.apply(this, args), delay); }; }

    function showToast(message) {
        if (!message) return;
        let container = document.querySelector(".toast-stack");
        if (!container) { container = document.createElement("div"); container.className = "toast-stack"; document.body.appendChild(container); }
        const toast = document.createElement("div");
        toast.className = "toast";
        toast.textContent = message;
        container.appendChild(toast);
        clearTimeout(state.toastTimer);
        state.toastTimer = setTimeout(() => { toast.remove(); if (!container.children.length) container.remove(); }, 2600);
    }

    // ─── Create Student Form ────────────────────────────────────────────────

    function prevStep() { if (createFormState.step > 1) { createFormState.step -= 1; renderSteps(); } }
    function nextStep() {
        if (createFormState.step === 1 && (!refs.studentIdInput?.value.trim() || !refs.studentNameInput?.value.trim())) { showToast("Student ID and Name are required"); return; }
        if (createFormState.step < 3) { createFormState.step += 1; renderSteps(); }
    }
    function renderSteps() {
        refs.steps?.forEach((step, i) => step.classList.toggle("active", i < createFormState.step));
        if (refs.stepLabel) refs.stepLabel.textContent = `Step ${createFormState.step} of 3`;
        document.querySelectorAll("[data-step-panel]").forEach(panel => panel.hidden = parseInt(panel.dataset.stepPanel) !== createFormState.step);
        if (refs.stepNextBtn) refs.stepNextBtn.hidden = createFormState.step === 3;
        if (refs.createSubmitBtn) refs.createSubmitBtn.hidden = createFormState.step !== 3;
    }

    function submitCreateStudent() {
        if (!refs.studentIdInput?.value.trim() || !refs.studentNameInput?.value.trim()) { showToast("⚠️ Student ID and Name are required"); return; }
        const payload = { id: refs.studentIdInput.value.trim(), name: refs.studentNameInput.value.trim(), course: refs.studentCourseInput?.value || "", semester: refs.studentSemesterInput?.value || "", phone: refs.studentPhoneInput?.value || "" };
        const submitBtn = refs.createSubmitBtn;
        if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Creating..."; }
        fetch("/api/admin/students", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
            .then(response => {
                if (response.status === 409) return Promise.reject("Student ID already exists");
                if (!response.ok) return response.text().then(t => Promise.reject(t || `HTTP ${response.status}`));
                return response.json();
            })
            .then((data) => {
                showToast(`✅ Student "${data.name}" created successfully`);
                refs.createStudentForm?.reset();
                createFormState.step = 1;
                renderSteps();
                loadHierarchy({ preserveState: true });
                loadPrograms();
                loadTopPerformers();
            })
            .catch(error => showToast(`❌ Create failed: ${error}`))
            .finally(() => { if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "Create"; } });
    }

    function loadTopPerformers() {
        fetch("/api/admin/students?size=10&sortBy=name&sortDir=asc")
            .then(response => response.ok ? response.json() : Promise.reject())
            .then(data => {
                const items = Array.isArray(data.items) ? data.items : [];
                // Sort by averageMarks descending to get top performers
                const sorted = [...items].sort((a, b) => (b.averageMarks || 0) - (a.averageMarks || 0)).slice(0, 5);
                if (refs.topPerformers) {
                    refs.topPerformers.innerHTML = sorted.map((student, i) => {
                        const marks = student.averageMarks || 0;
                        const band = performanceBandFromMarks(marks);
                        return `<li class="audit-item" style="cursor:pointer" data-student-id="${escapeHtml(student.id)}">
                            <span class="performance-badge ${band}" style="font-size:10px;padding:2px 6px;margin-right:6px">${marks.toFixed(1)}</span>
                            #${i + 1} ${escapeHtml(student.name || "Unknown")}
                        </li>`;
                    }).join("") || '<li class="audit-item">No data available</li>';
                    // Bind click to view profile
                    refs.topPerformers.querySelectorAll("[data-student-id]").forEach(li => {
                        li.addEventListener("click", () => openProfile(li.dataset.studentId));
                    });
                }
                if (refs.avgMarksLabel && sorted.length > 0) {
                    const avgMarks = sorted.reduce((sum, s) => sum + (s.averageMarks || 0), 0) / sorted.length;
                    refs.avgMarksLabel.textContent = avgMarks.toFixed(1);
                }
            })
            .catch(() => { if (refs.topPerformers) refs.topPerformers.innerHTML = '<li class="audit-item">Unable to load data</li>'; });
    }

    window.viewStudentProfile = openProfile;
    window.uploadFace = openFaceUpload;
    window.showStudentMenu = function (event, studentId) {
        event?.stopPropagation?.();
        const confirmed = window.confirm("Student actions:\n\nOK: delete student\nCancel: close menu");
        if (confirmed) deleteStudent(studentId);
    };

    if (document.readyState === "loading") { document.addEventListener("DOMContentLoaded", init); }
    else { init(); }
})();
