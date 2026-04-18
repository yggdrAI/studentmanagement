/**
 * Hierarchical Student Data Management
 * Class → Batch → Student Structure
 */

(function() {
    "use strict";
    
    // ===== APPLICATION STATE =====
    const state = {
        hierarchy: null,
        expandedClasses: new Set(),
        visibleBatches: new Set(),
        filters: {
            course: "",
            semester: "",
            performance: "",
            searchQuery: ""
        },
        allExpanded: false,
        loading: false
    };
    
    // ===== DOM REFERENCES =====
    const refs = {
        classesContainer: document.getElementById("classesContainer"),
        loadingSpinner: document.getElementById("loadingSpinner"),
        noDataState: document.getElementById("noDataState"),
        
        courseFilter: document.getElementById("courseFilter"),
        semesterFilter: document.getElementById("semesterFilter"),
        performanceFilter: document.getElementById("performanceFilter"),
        globalSearch: document.getElementById("globalSearch"),
        
        refreshBtn: document.getElementById("refreshBtn"),
        viewToggleBtn: document.getElementById("viewToggleBtn"),
        
        totalClasses: document.getElementById("totalClasses"),
        totalBatches: document.getElementById("totalBatches"),
        totalStudents: document.getElementById("totalStudents"),
        avgAttendance: document.getElementById("avgAttendance"),
        
        sidebarToggle: document.getElementById("sidebarToggle")
    };
    
    // ===== INITIALIZATION =====
    function init() {
        console.log("🏫 Initializing Student Hierarchy UI...");
        attachEventListeners();
        loadHierarchy();
    }
    
    function attachEventListeners() {
        refs.courseFilter?.addEventListener("change", handleFilterChange);
        refs.semesterFilter?.addEventListener("change", handleFilterChange);
        refs.performanceFilter?.addEventListener("change", handleFilterChange);
        refs.globalSearch?.addEventListener("input", debounce(handleSearch, 300));
        refs.refreshBtn?.addEventListener("click", loadHierarchy);
        refs.viewToggleBtn?.addEventListener("click", toggleExpandAll);
        refs.sidebarToggle?.addEventListener("click", toggleSidebar);
    }
    
    // ===== DATA LOADING =====
    function loadHierarchy() {
        if (state.loading) return;
        state.loading = true;
        refs.loadingSpinner.hidden = false;
        refs.noDataState.hidden = true;
        refs.classesContainer.innerHTML = "";
        
        const params = new URLSearchParams();
        if (state.filters.course) params.append("course", state.filters.course);
        if (state.filters.semester) params.append("semester", state.filters.semester);
        
        const url = `/api/admin/students-hierarchy?${params}`;
        
        fetch(url)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(data => {
                console.log("✅ Hierarchy loaded:", data);
                state.hierarchy = data;
                updateStatistics();
                renderHierarchy();
                refs.loadingSpinner.hidden = true;
                state.loading = false;
            })
            .catch(error => {
                console.error("❌ Failed to load hierarchy:", error);
                refs.loadingSpinner.hidden = true;
                refs.noDataState.hidden = false;
                state.loading = false;
            });
    }
    
    function updateStatistics() {
        if (!state.hierarchy) return;
        
        refs.totalClasses.textContent = state.hierarchy.structure.totalClasses || 0;
        refs.totalBatches.textContent = state.hierarchy.structure.totalBatches || 0;
        refs.totalStudents.textContent = state.hierarchy.structure.totalStudents || 0;
        
        const avgAtt = calculateAverageAttendance();
        refs.avgAttendance.textContent = avgAtt.toFixed(1) + "%";
    }
    
    function calculateAverageAttendance() {
        if (!state.hierarchy?.classes) return 0;
        
        let total = 0, count = 0;
        state.hierarchy.classes.forEach(cls => {
            cls.batches?.forEach(batch => {
                total += batch.analytics?.averageAttendance || 0;
                count++;
            });
        });
        
        return count > 0 ? total / count : 0;
    }
    
    // ===== RENDERING =====
    function renderHierarchy() {
        if (!state.hierarchy || !state.hierarchy.classes || state.hierarchy.classes.length === 0) {
            refs.noDataState.hidden = false;
            refs.classesContainer.innerHTML = "";
            return;
        }
        
        refs.noDataState.hidden = true;
        
        const classesHTML = state.hierarchy.classes
            .map(cls => renderClassCard(cls))
            .join("");
        
        refs.classesContainer.innerHTML = classesHTML;
        attachClassCardListeners();
    }
    
    function renderClassCard(classData) {
        const isExpanded = state.expandedClasses.has(classData.classId);
        const totalStudents = classData.totalStudents || 0;
        const totalBatches = classData.batches?.length || 0;
        const avgMarks = classData.classAnalytics?.averageMarks || 0;
        
        const html = `
            <div class="class-card" data-class-id="${classData.classId}">
                <div class="class-header ${isExpanded ? 'expanded' : ''}" role="button" tabindex="0">
                    <div class="class-info">
                        <div class="class-title">${escapeHtml(classData.classLabel)}</div>
                        <div class="class-stats">
                            <span title="Total Students">📚 ${totalStudents} Students</span>
                            <span title="Total Batches">🎯 ${totalBatches} Batches</span>
                            <span title="Average Marks">📊 Avg: ${avgMarks.toFixed(1)}</span>
                        </div>
                    </div>
                    <div class="class-toggle ${isExpanded ? 'rotated' : ''}">⌄</div>
                </div>
                <div class="class-body ${isExpanded ? '' : 'collapsed'}">
                    ${(classData.batches || [])
                        .map((batch, i) => renderBatchCard(batch, i + 1))
                        .join("")}
                </div>
            </div>
        `;
        
        return html;
    }
    
    function renderBatchCard(batch, batchIndex) {
        const batchClass = `batch-${batchIndex}`;
        const isVisible = state.visibleBatches.has(batch.batchId);
        const analytics = batch.analytics || {};
        const totalStudents = batch.totalStudents || 0;
        
        const topPerformer = analytics.topPerformer;
        const topPerformerName = topPerformer?.name || "N/A";
        
        const html = `
            <div class="batch-card ${batchClass}" data-batch-id="${batch.batchId}">
                <div class="batch-header">
                    <div class="batch-title-group">
                        <div class="batch-title">${escapeHtml(batch.batchLabel)}</div>
                        <div class="batch-count">
                            <span class="batch-count-badge">${totalStudents} students</span>
                        </div>
                    </div>
                    <div class="batch-toggle" data-batch-id="${batch.batchId}" role="button" tabindex="0" title="Toggle students list">
                        ${isVisible ? '▼' : '▶'}
                    </div>
                </div>
                
                <div class="batch-analytics">
                    <div class="analytics-item" title="Average marks for this batch">
                        <span class="analytics-label">📊 Avg Marks</span>
                        <span class="analytics-value">${(analytics.averageMarks || 0).toFixed(1)}</span>
                    </div>
                    <div class="analytics-item" title="Average attendance percentage">
                        <span class="analytics-label">📅 Attendance</span>
                        <span class="analytics-value">${(analytics.averageAttendance || 0).toFixed(1)}%</span>
                    </div>
                    <div class="analytics-item" title="Top performing student">
                        <span class="analytics-label">⭐ Top Performer</span>
                        <span class="analytics-value" title="${topPerformerName}">${truncate(topPerformerName, 12)}</span>
                    </div>
                    <div class="analytics-item" title="Students at risk (marks <50)">
                        <span class="analytics-label">⚠️ At Risk</span>
                        <span class="analytics-value">${analytics.riskStudents || 0}</span>
                    </div>
                </div>
                
                <div class="batch-indicator">
                    <div class="indicator-item">
                        <div class="indicator-dot"></div>
                        <span>Present Today: ${analytics.presentToday || 0} / ${totalStudents}</span>
                    </div>
                </div>
                
                <div class="students-list ${isVisible ? 'visible' : ''}">
                    ${(batch.students || [])
                        .map(student => renderStudentRow(student))
                        .join("")}
                </div>
            </div>
        `;
        
        return html;
    }
    
    function renderStudentRow(student) {
        const initials = getInitials(student.name);
        const performance = student.performance?.status || 'average';
        const marks = (student.performance?.averageMarks || 0).toFixed(1);
        
        const html = `
            <div class="student-row" data-student-id="${student.id}">
                <div class="student-info">
                    <div class="student-avatar" title="${escapeHtml(student.name)}">${initials}</div>
                    <div class="student-details">
                        <div class="student-name" title="${escapeHtml(student.name)}">${escapeHtml(student.name)}</div>
                        <div class="student-meta">${escapeHtml(student.enrollment)} • ${escapeHtml(student.email)}</div>
                    </div>
                </div>
                
                <div class="student-performance">
                    <div class="performance-badge ${performance}" title="Average marks: ${marks}">
                        ${marks}
                    </div>
                </div>
                
                <div class="student-actions">
                    <button class="action-btn" title="View Profile" onclick="window.viewStudentProfile('${student.id}')">👁</button>
                    <button class="action-btn" title="Upload Face" onclick="window.uploadFace('${student.id}')">📷</button>
                    <button class="action-btn" title="More actions" onclick="window.showStudentMenu(event, '${student.id}')">⋯</button>
                </div>
            </div>
        `;
        
        return html;
    }
    
    function attachClassCardListeners() {
        // Attach class header click handlers
        document.querySelectorAll(".class-header").forEach(header => {
            header.addEventListener("click", handleClassToggle);
            header.addEventListener("keydown", e => {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    handleClassToggle.call(header);
                }
            });
        });
        
        // Attach batch toggle click handlers
        document.querySelectorAll(".batch-toggle").forEach(toggle => {
            toggle.addEventListener("click", handleBatchToggle);
            toggle.addEventListener("keydown", e => {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    handleBatchToggle.call(toggle, e);
                }
            });
        });
    }
    
    // ===== EVENT HANDLERS =====
    function handleClassToggle(e) {
        const header = e.currentTarget;
        const classCard = header.closest(".class-card");
        const classId = classCard.dataset.classId;
        const classBody = classCard.querySelector(".class-body");
        const toggle = header.querySelector(".class-toggle");
        
        if (state.expandedClasses.has(classId)) {
            state.expandedClasses.delete(classId);
            classBody.classList.add("collapsed");
            header.classList.remove("expanded");
            toggle.classList.remove("rotated");
        } else {
            state.expandedClasses.add(classId);
            classBody.classList.remove("collapsed");
            header.classList.add("expanded");
            toggle.classList.add("rotated");
        }
    }
    
    function handleBatchToggle(e) {
        e.stopPropagation();
        const toggle = e.currentTarget;
        const batchId = toggle.dataset.batchId;
        const batchCard = document.querySelector(`[data-batch-id="${batchId}"]`);
        const studentsList = batchCard.querySelector(".students-list");
        
        if (state.visibleBatches.has(batchId)) {
            state.visibleBatches.delete(batchId);
            studentsList.classList.remove("visible");
            toggle.textContent = "▶";
        } else {
            state.visibleBatches.add(batchId);
            studentsList.classList.add("visible");
            toggle.textContent = "▼";
        }
    }
    
    function handleFilterChange() {
        state.filters.course = refs.courseFilter.value;
        state.filters.semester = refs.semesterFilter.value;
        state.filters.performance = refs.performanceFilter.value;
        
        // Reset expand state when filtering
        state.expandedClasses.clear();
        state.visibleBatches.clear();
        
        loadHierarchy();
    }
    
    function handleSearch(e) {
        state.filters.searchQuery = e.target.value.toLowerCase();
        filterAndHighlight();
    }
    
    function toggleExpandAll() {
        state.allExpanded = !state.allExpanded;
        
        if (state.allExpanded) {
            state.hierarchy?.classes?.forEach(cls => {
                state.expandedClasses.add(cls.classId);
                cls.batches?.forEach(batch => {
                    state.visibleBatches.add(batch.batchId);
                });
            });
            refs.viewToggleBtn.textContent = "📊 Collapse All";
        } else {
            state.expandedClasses.clear();
            state.visibleBatches.clear();
            refs.viewToggleBtn.textContent = "📊 Expand All";
        }
        
        renderHierarchy();
    }
    
    function toggleSidebar() {
        const sidebar = document.getElementById("sidebar");
        sidebar?.classList.toggle("collapsed");
    }
    
    function filterAndHighlight() {
        const query = state.filters.searchQuery;
        
        document.querySelectorAll(".student-row").forEach(row => {
            const name = row.querySelector(".student-name")?.textContent.toLowerCase() || "";
            const enrollment = row.querySelector(".student-meta")?.textContent.toLowerCase() || "";
            const email = row.querySelector(".student-meta")?.textContent.toLowerCase() || "";
            
            const matches = !query || 
                           name.includes(query) || 
                           enrollment.includes(query) || 
                           email.includes(query);
            
            row.style.display = matches ? "" : "none";
        });
    }
    
    // ===== UTILITY FUNCTIONS =====
    
    function debounce(func, wait) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func(...args), wait);
        };
    }
    
    function escapeHtml(text) {
        const div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
    }
    
    function getInitials(name) {
        return name
            .split(" ")
            .map(n => n[0])
            .join("")
            .toUpperCase()
            .substring(0, 2);
    }
    
    function truncate(text, maxLength) {
        return text.length > maxLength ? text.substring(0, maxLength) + "…" : text;
    }
    
    // ===== GLOBAL ACTION FUNCTIONS =====
    
    window.viewStudentProfile = function(studentId) {
        console.log("👁 Viewing profile for student:", studentId);
        window.location.href = `/admin/students/${studentId}`;
    };
    
    window.uploadFace = function(studentId) {
        console.log("📷 Upload face for student:", studentId);
        // TODO: Implement face upload modal/dialog
        alert("Face upload feature coming soon!");
    };
    
    window.showStudentMenu = function(e, studentId) {
        e.stopPropagation();
        console.log("⋯ Menu for student:", studentId);
        
        // TODO: Implement context menu with options:
        // - View Profile
        // - Upload Face
        // - Edit Details
        // - Attendance
        // - Marks
        // - Delete
    };
    
    // ===== INITIALIZATION =====
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
    
    console.log("🚀 Hierarchy UI script loaded!");
})();
