(function () {
    "use strict";

    const state = {
        page: 0,
        size: 20,
        search: "",
        course: "",
        sortBy: "id",
        sortDir: "asc",
        totalPages: 0,
        totalElements: 0,
        items: [],
        selected: new Set(),
        pendingDeleteId: null,
        step: 1,
        formData: {
            id: "",
            name: "",
            course: "",
            semester: "",
            phone: "",
            email: ""
        }
    };

    const refs = {
        gridBody: document.getElementById("gridBody"),
        search: document.getElementById("studentSearch"),
        courseFilter: document.getElementById("courseFilter"),
        pageLabel: document.getElementById("pageLabel"),
        totalLabel: document.getElementById("totalLabel"),
        prevPageBtn: document.getElementById("prevPageBtn"),
        nextPageBtn: document.getElementById("nextPageBtn"),
        selectAll: document.getElementById("selectAllRows"),
        bulkDeleteBtn: document.getElementById("bulkDeleteBtn"),
        exportBtn: document.getElementById("exportBtn"),
        form: document.getElementById("createStudentForm"),
        stepLabel: document.getElementById("stepLabel"),
        steps: Array.from(document.querySelectorAll(".step")),
        stepNextBtn: document.getElementById("stepNextBtn"),
        stepPrevBtn: document.getElementById("stepPrevBtn"),
        submitBtn: document.getElementById("createSubmitBtn"),
        modal: document.getElementById("confirmModal"),
        confirmDeleteBtn: document.getElementById("confirmDeleteBtn"),
        cancelDeleteBtn: document.getElementById("cancelDeleteBtn"),
        toastStack: document.getElementById("toastStack"),
        commandPalette: document.getElementById("commandPalette"),
        commandInput: document.getElementById("commandInput"),
        commandList: document.getElementById("commandList"),
        sidebar: document.getElementById("sidebar"),
        sidebarToggle: document.getElementById("sidebarToggle"),
        themeToggle: document.getElementById("themeToggle"),
        topSearch: document.getElementById("globalSearch"),
        chart: document.getElementById("marksChart"),
        topPerformers: document.getElementById("topPerformers"),
        audits: document.getElementById("auditItems")
    };

    const debouncedFetch = debounce(fetchStudents, 300);

    const commands = [
        { label: "Go to Dashboard", run: () => (window.location.href = "/dashboard") },
        { label: "Go to Manage Students", run: () => (window.location.href = "/admin/students") },
        { label: "Toggle Theme", run: () => toggleTheme() },
        { label: "New Student", run: () => document.getElementById("studentIdInput").focus() }
    ];

    function init() {
        bindEvents();
        setupTheme();
        renderSteps();
        renderCommands(commands);
        fetchStudents();
        fetchActivity();
    }

    function bindEvents() {
        refs.search.addEventListener("input", (event) => {
            state.search = event.target.value.trim();
            state.page = 0;
            debouncedFetch();
        });

        refs.courseFilter.addEventListener("change", (event) => {
            state.course = event.target.value;
            state.page = 0;
            fetchStudents();
        });

        refs.prevPageBtn.addEventListener("click", () => {
            if (state.page > 0) {
                state.page -= 1;
                fetchStudents();
            }
        });

        refs.nextPageBtn.addEventListener("click", () => {
            if (state.page + 1 < state.totalPages) {
                state.page += 1;
                fetchStudents();
            }
        });

        refs.selectAll.addEventListener("change", (event) => {
            state.selected.clear();
            if (event.target.checked) {
                state.items.forEach((item) => state.selected.add(item.id));
            }
            renderGrid();
            syncBulkActions();
        });

        refs.bulkDeleteBtn.addEventListener("click", onBulkDelete);
        refs.exportBtn.addEventListener("click", onExport);

        Array.from(document.querySelectorAll("[data-sort]"))
            .forEach((button) => {
                button.addEventListener("click", () => {
                    const sortBy = button.getAttribute("data-sort");
                    if (state.sortBy === sortBy) {
                        state.sortDir = state.sortDir === "asc" ? "desc" : "asc";
                    } else {
                        state.sortBy = sortBy;
                        state.sortDir = "asc";
                    }
                    fetchStudents();
                });
            });

        refs.form.addEventListener("input", (event) => {
            const field = event.target.name;
            if (!field) {
                return;
            }
            state.formData[field] = event.target.value;
            validateField(event.target);
        });

        refs.stepNextBtn.addEventListener("click", () => {
            if (state.step === 1 && !validateStep1()) {
                toast("Please complete required personal fields", "error");
                return;
            }
            if (state.step < 3) {
                state.step += 1;
                renderSteps();
            }
        });

        refs.stepPrevBtn.addEventListener("click", () => {
            if (state.step > 1) {
                state.step -= 1;
                renderSteps();
            }
        });

        refs.form.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!validateStep1()) {
                toast("Student ID and Name are required", "error");
                state.step = 1;
                renderSteps();
                return;
            }
            await createStudent();
        });

        refs.cancelDeleteBtn.addEventListener("click", closeDeleteModal);
        refs.confirmDeleteBtn.addEventListener("click", confirmDelete);

        document.addEventListener("keydown", (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
                event.preventDefault();
                openCommandPalette();
            }
            if (event.key === "Escape") {
                closeCommandPalette();
                closeDeleteModal();
            }
        });

        refs.commandInput.addEventListener("input", () => {
            const token = refs.commandInput.value.toLowerCase();
            renderCommands(commands.filter((command) => command.label.toLowerCase().includes(token)));
        });

        refs.sidebarToggle.addEventListener("click", toggleSidebar);
        refs.themeToggle.addEventListener("click", toggleTheme);
        refs.topSearch.addEventListener("focus", openCommandPalette);
    }

    async function fetchStudents() {
        showSkeleton();
        const params = new URLSearchParams({
            page: String(state.page),
            size: String(state.size),
            search: state.search,
            course: state.course,
            sortBy: state.sortBy,
            sortDir: state.sortDir
        });

        try {
            const response = await fetch(`/api/admin/students?${params.toString()}`);
            if (!response.ok) {
                throw new Error("Failed to load students");
            }
            const payload = await response.json();
            state.items = payload.items || [];
            state.totalPages = payload.totalPages || 0;
            state.totalElements = payload.totalElements || 0;
            state.selected.clear();
            renderGrid();
            renderPagination();
            renderInsights();
            syncBulkActions();
        } catch (error) {
            refs.gridBody.innerHTML = `<tr><td colspan="8" class="empty-state">Unable to load students. ${escapeHtml(error.message)}</td></tr>`;
            toast("Unable to fetch students", "error");
        }
    }

    function showSkeleton() {
        const skeletonRows = Array.from({ length: 7 }).map(() =>
            `<tr><td colspan="8"><div class="skeleton-row"></div></td></tr>`
        ).join("");
        refs.gridBody.innerHTML = skeletonRows;
    }

    function renderGrid() {
        if (!state.items.length) {
            refs.gridBody.innerHTML = `<tr><td colspan="8" class="empty-state">No students match the current filters.</td></tr>`;
            return;
        }

        refs.gridBody.innerHTML = state.items.map((item) => {
            const checked = state.selected.has(item.id) ? "checked" : "";
            const marks = Number(item.averageMarks || 0);
            const marksPct = Math.max(0, Math.min(100, marks));
            const progressClass = marks >= 75 ? "" : marks >= 50 ? "warn" : "error";
            return `
                <tr>
                    <td><input type="checkbox" aria-label="Select ${escapeHtml(item.name)}" data-select-row="${escapeHtml(item.id)}" ${checked}></td>
                    <td>${escapeHtml(item.id)}</td>
                    <td><span class="avatar">${escapeHtml(item.avatar || "ST")}</span>${escapeHtml(item.name)}</td>
                    <td>${escapeHtml(item.enrollment || item.id)}</td>
                    <td>${escapeHtml(item.email || "")}</td>
                    <td><span class="badge">${escapeHtml(item.course || "N/A")}</span></td>
                    <td>
                        <div style="display:flex; align-items:center; gap:10px;">
                            <div class="progress ${progressClass}"><span style="width:${marksPct}%;"></span></div>
                            <span>${marks.toFixed(1)}</span>
                        </div>
                    </td>
                    <td>
                        <div class="row-actions">
                            <span class="tooltip-wrap" data-tip="Edit profile">
                                <button class="icon-btn" aria-label="Edit ${escapeHtml(item.name)}" data-edit="${escapeHtml(item.id)}">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"></path></svg>
                                </button>
                            </span>
                            <span class="tooltip-wrap" data-tip="Delete student">
                                <button class="icon-btn" aria-label="Delete ${escapeHtml(item.name)}" data-delete="${escapeHtml(item.id)}">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"></path><path d="M8 6V4h8v2"></path><path d="M19 6l-1 14H6L5 6"></path></svg>
                                </button>
                            </span>
                        </div>
                    </td>
                </tr>
            `;
        }).join("");

        Array.from(document.querySelectorAll("[data-select-row]"))
            .forEach((checkbox) => {
                checkbox.addEventListener("change", (event) => {
                    const id = event.target.getAttribute("data-select-row");
                    if (event.target.checked) {
                        state.selected.add(id);
                    } else {
                        state.selected.delete(id);
                    }
                    syncBulkActions();
                });
            });

        Array.from(document.querySelectorAll("[data-delete]"))
            .forEach((button) => {
                button.addEventListener("click", () => openDeleteModal(button.getAttribute("data-delete")));
            });

        Array.from(document.querySelectorAll("[data-edit]"))
            .forEach((button) => {
                button.addEventListener("click", () => {
                    const id = button.getAttribute("data-edit");
                    window.location.href = `/admin/students/${id}/profile`;
                });
            });
    }

    function renderPagination() {
        refs.pageLabel.textContent = state.totalPages ? `Page ${state.page + 1} / ${state.totalPages}` : "Page 0 / 0";
        refs.totalLabel.textContent = `${state.totalElements} records`;
        refs.prevPageBtn.disabled = state.page <= 0;
        refs.nextPageBtn.disabled = state.page + 1 >= state.totalPages;
        refs.selectAll.checked = false;
    }

    function syncBulkActions() {
        const count = state.selected.size;
        refs.bulkDeleteBtn.disabled = count === 0;
        refs.bulkDeleteBtn.textContent = count ? `Bulk Delete (${count})` : "Bulk Delete";
    }

    function validateField(input) {
        if (!input) {
            return true;
        }
        const required = input.hasAttribute("required");
        if (required && !input.value.trim()) {
            input.setAttribute("aria-invalid", "true");
            return false;
        }
        input.removeAttribute("aria-invalid");
        return true;
    }

    function validateStep1() {
        const idOk = validateField(document.getElementById("studentIdInput"));
        const nameOk = validateField(document.getElementById("studentNameInput"));
        return idOk && nameOk;
    }

    function renderSteps() {
        refs.stepLabel.textContent = `Step ${state.step} of 3`;
        refs.steps.forEach((step, index) => {
            step.classList.toggle("active", index < state.step);
        });

        document.querySelectorAll("[data-step-panel]")
            .forEach((panel) => {
                panel.hidden = Number(panel.getAttribute("data-step-panel")) !== state.step;
            });

        refs.stepPrevBtn.disabled = state.step === 1;
        refs.stepNextBtn.hidden = state.step === 3;
        refs.submitBtn.hidden = state.step !== 3;
    }

    async function createStudent() {
        const payload = {
            id: state.formData.id.trim(),
            name: state.formData.name.trim()
        };

        const response = await fetch("/api/admin/students", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const message = await extractError(response, "Unable to create student");
            toast(message, "error");
            return;
        }

        refs.form.reset();
        state.formData = { id: "", name: "", course: "", semester: "", phone: "", email: "" };
        state.step = 1;
        renderSteps();
        toast("Student created and persisted successfully", "success");
        fetchStudents();
    }

    function openDeleteModal(id) {
        state.pendingDeleteId = id;
        refs.modal.classList.add("open");
        refs.confirmDeleteBtn.focus();
    }

    function closeDeleteModal() {
        state.pendingDeleteId = null;
        refs.modal.classList.remove("open");
    }

    async function confirmDelete() {
        if (!state.pendingDeleteId) {
            return;
        }

        const response = await fetch(`/api/admin/students/${encodeURIComponent(state.pendingDeleteId)}`, {
            method: "DELETE"
        });

        if (!response.ok) {
            toast("Delete failed", "error");
            return;
        }

        toast("Student deleted", "success");
        closeDeleteModal();
        fetchStudents();
    }

    async function onBulkDelete() {
        if (!state.selected.size) {
            return;
        }
        const ids = Array.from(state.selected);
        const response = await fetch("/api/admin/students/bulk-delete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ids })
        });

        if (!response.ok) {
            toast("Bulk delete failed", "error");
            return;
        }

        const payload = await response.json();
        toast(`Deleted ${payload.deleted || 0} students`, "success");
        state.selected.clear();
        fetchStudents();
    }

    function onExport() {
        window.location.href = "/api/admin/students/export";
    }

    function renderInsights() {
        if (!state.items.length) {
            refs.chart.innerHTML = "";
            refs.topPerformers.innerHTML = "<li class=\"audit-item\">No student data loaded.</li>";
            return;
        }

        const bins = [0, 0, 0, 0, 0];
        state.items.forEach((item) => {
            const marks = Number(item.averageMarks || 0);
            if (marks < 20) bins[0] += 1;
            else if (marks < 40) bins[1] += 1;
            else if (marks < 60) bins[2] += 1;
            else if (marks < 80) bins[3] += 1;
            else bins[4] += 1;
        });

        const max = Math.max(...bins, 1);
        refs.chart.innerHTML = bins.map((value) => {
            const pct = Math.max(8, Math.round((value / max) * 100));
            return `<div class=\"chart-bar\" style=\"height:${pct}%;\" title=\"${value} students\"></div>`;
        }).join("");

        const top = [...state.items]
            .sort((a, b) => Number(b.averageMarks || 0) - Number(a.averageMarks || 0))
            .slice(0, 5);

        refs.topPerformers.innerHTML = top.map((item, index) =>
            `<li class=\"audit-item\">#${index + 1} ${escapeHtml(item.name)} <strong>${Number(item.averageMarks || 0).toFixed(1)}</strong></li>`
        ).join("");
    }

    async function fetchActivity() {
        try {
            const response = await fetch("/api/admin/students/activity?limit=8");
            if (!response.ok) {
                throw new Error("Failed");
            }
            const rows = await response.json();
            if (!rows.length) {
                refs.audits.innerHTML = "<li class='audit-item'>No recent audit events.</li>";
                return;
            }
            refs.audits.innerHTML = rows.map((row) =>
                `<li class=\"audit-item\"><strong>${escapeHtml(row.type || "EVENT")}</strong> • ${escapeHtml(row.studentId || "N/A")}<br><span>${escapeHtml(row.description || "")}</span><br><small>${escapeHtml(row.time || "")}</small></li>`
            ).join("");
        } catch (_error) {
            refs.audits.innerHTML = "<li class='audit-item'>Unable to load activity feed.</li>";
        }
    }

    function toggleSidebar() {
        const isMobile = window.innerWidth <= 1100;
        if (isMobile) {
            refs.sidebar.dataset.open = refs.sidebar.dataset.open === "true" ? "false" : "true";
            return;
        }
        refs.sidebar.dataset.collapsed = refs.sidebar.dataset.collapsed === "true" ? "false" : "true";
    }

    function setupTheme() {
        const saved = localStorage.getItem("sms_theme");
        if (saved === "light") {
            document.body.classList.add("theme-light");
        }
    }

    function toggleTheme() {
        document.body.classList.toggle("theme-light");
        const light = document.body.classList.contains("theme-light");
        localStorage.setItem("sms_theme", light ? "light" : "dark");
    }

    function openCommandPalette() {
        refs.commandPalette.classList.add("open");
        refs.commandInput.value = "";
        renderCommands(commands);
        refs.commandInput.focus();
    }

    function closeCommandPalette() {
        refs.commandPalette.classList.remove("open");
    }

    function renderCommands(list) {
        refs.commandList.innerHTML = list.map((command, index) =>
            `<button type=\"button\" class=\"btn btn-outline\" data-command=\"${index}\" style=\"width:100%; text-align:left;\">${escapeHtml(command.label)}</button>`
        ).join("");

        Array.from(refs.commandList.querySelectorAll("[data-command]"))
            .forEach((button) => {
                button.addEventListener("click", () => {
                    const idx = Number(button.getAttribute("data-command"));
                    const command = list[idx];
                    if (command) {
                        closeCommandPalette();
                        command.run();
                    }
                });
            });
    }

    function toast(message, type) {
        const node = document.createElement("div");
        node.className = `toast ${type || ""}`;
        node.textContent = message;
        refs.toastStack.appendChild(node);
        window.setTimeout(() => {
            node.remove();
        }, 2800);
    }

    async function extractError(response, fallback) {
        try {
            const text = await response.text();
            return text || fallback;
        } catch (_error) {
            return fallback;
        }
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return "";
        }
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/\"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function debounce(fn, delay) {
        let timer;
        return function debounced(...args) {
            window.clearTimeout(timer);
            timer = window.setTimeout(() => fn.apply(this, args), delay);
        };
    }

    init();
})();
