(function () {
    "use strict";

    const state = {
        page: 0,
        size: 20,
        search: "",
        course: "",
        tenantId: "",
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
            section: "",
            batch: "",
            phone: "",
            email: ""
        },
        faceUploadStatus: {},
        pendingFaceStudentId: null,
        faceCropDraft: null
    };

    const refs = {
        gridBody: document.getElementById("gridBody"),
        search: document.getElementById("studentSearch"),
        courseFilter: document.getElementById("courseFilter"),
        tenantSelector: document.getElementById("tenantSelector"),
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
        facePreviewModal: document.getElementById("facePreviewModal"),
        faceCropCanvas: document.getElementById("faceCropCanvas"),
        faceZoomRange: document.getElementById("faceZoomRange"),
        confirmFacePreviewBtn: document.getElementById("confirmFacePreviewBtn"),
        cancelFacePreviewBtn: document.getElementById("cancelFacePreviewBtn"),
        faceUploadInput: document.getElementById("faceUploadInput"),
        sidebar: document.getElementById("sidebar"),
        sidebarToggle: document.getElementById("sidebarToggle"),
        topSearch: document.getElementById("globalSearch"),
        chart: document.getElementById("marksChart"),
        topPerformers: document.getElementById("topPerformers"),
        audits: document.getElementById("auditItems")
    };

    const debouncedFetch = debounce(fetchStudents, 300);

    const commands = [
        { label: "Go to Dashboard", run: () => (window.location.href = "/dashboard") },
        { label: "Go to Manage Students", run: () => (window.location.href = "/admin/students") },
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
        refs.search?.addEventListener("input", (event) => {
            state.search = event.target.value.trim();
            state.page = 0;
            debouncedFetch();
        });

        refs.courseFilter?.addEventListener("change", (event) => {
            state.course = event.target.value;
            state.page = 0;
            fetchStudents();
        });

        refs.tenantSelector?.addEventListener("change", (event) => {
            state.tenantId = event.target.value || "";
        });

        refs.prevPageBtn?.addEventListener("click", () => {
            if (state.page > 0) {
                state.page -= 1;
                fetchStudents();
            }
        });

        refs.nextPageBtn?.addEventListener("click", () => {
            if (state.page + 1 < state.totalPages) {
                state.page += 1;
                fetchStudents();
            }
        });

        refs.selectAll?.addEventListener("change", (event) => {
            state.selected.clear();
            if (event.target.checked) {
                state.items.forEach((item) => state.selected.add(item.id));
            }
            renderGrid();
            syncBulkActions();
        });

        refs.bulkDeleteBtn?.addEventListener("click", onBulkDelete);
        refs.exportBtn?.addEventListener("click", onExport);

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

        refs.form?.addEventListener("input", (event) => {
            const field = event.target.name;
            if (!field) {
                return;
            }
            state.formData[field] = event.target.value;
            validateField(event.target);
        });

        refs.stepNextBtn?.addEventListener("click", () => {
            if (state.step === 1 && !validateStep1()) {
                toast("Please complete required personal fields", "error");
                return;
            }
            if (state.step < 3) {
                state.step += 1;
                renderSteps();
            }
        });

        refs.stepPrevBtn?.addEventListener("click", () => {
            if (state.step > 1) {
                state.step -= 1;
                renderSteps();
            }
        });

        refs.form?.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!validateStep1()) {
                toast("Student ID and Name are required", "error");
                state.step = 1;
                renderSteps();
                return;
            }
            await createStudent();
        });

        refs.cancelDeleteBtn?.addEventListener("click", closeDeleteModal);
        refs.confirmDeleteBtn?.addEventListener("click", confirmDelete);

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

        refs.commandInput?.addEventListener("input", () => {
            const token = refs.commandInput.value.toLowerCase();
            renderCommands(commands.filter((command) => command.label.toLowerCase().includes(token)));
        });

        refs.sidebarToggle?.addEventListener("click", toggleSidebar);
        refs.topSearch?.addEventListener("focus", openCommandPalette);

        refs.cancelFacePreviewBtn?.addEventListener("click", closeFacePreviewModal);
        refs.confirmFacePreviewBtn?.addEventListener("click", confirmFaceUpload);
        refs.faceZoomRange?.addEventListener("input", () => {
            if (!state.faceCropDraft) {
                return;
            }
            state.faceCropDraft.zoom = Number(refs.faceZoomRange.value || 1);
            drawFaceCropPreview();
        });

        bindFaceCanvasDragEvents();

        refs.faceUploadInput?.addEventListener("change", async (event) => {
            const selectedFile = event.target.files && event.target.files[0] ? event.target.files[0] : null;
            if (!selectedFile || !state.pendingFaceStudentId) {
                return;
            }

            const studentId = state.pendingFaceStudentId;
            event.target.value = "";
            await prepareFacePreview(studentId, selectedFile);
        });
    }

    async function fetchStudents() {
        showSkeleton();
        try {
            const payload = await window.smsApi.admin.students.list({
                page: state.page,
                size: state.size,
                search: state.search,
                course: state.course,
                sortBy: state.sortBy,
                sortDir: state.sortDir
            });
            state.items = payload.items || [];
            state.totalPages = payload.totalPages || 0;
            state.totalElements = payload.totalElements || 0;
            state.selected.clear();
            renderGrid();
            renderPagination();
            renderInsights();
            syncBulkActions();
        } catch (error) {
            refs.gridBody.innerHTML = `<tr><td colspan="11" class="empty-state">Unable to load students. ${escapeHtml(error.message)}</td></tr>`;
            toast("Unable to fetch students", "error");
        }
    }

    function showSkeleton() {
        const skeletonRows = Array.from({ length: 7 }).map(() =>
            `<tr><td colspan="11"><div class="skeleton-row"></div></td></tr>`
        ).join("");
        refs.gridBody.innerHTML = skeletonRows;
    }

    function renderGrid() {
        if (!state.items.length) {
            refs.gridBody.innerHTML = `<tr><td colspan="11" class="empty-state">No students match the current filters.</td></tr>`;
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
                    <td>${escapeHtml(item.semester || "-")}</td>
                    <td>${escapeHtml(item.classGroup || item.section || "-")}</td>
                    <td>${escapeHtml(item.batchGroup || item.batch || "-")}</td>
                    <td>
                        <div style="display:flex; align-items:center; gap:10px;">
                            <div class="progress ${progressClass}"><span style="width:${marksPct}%;"></span></div>
                            <span>${marks.toFixed(1)}</span>
                        </div>
                    </td>
                    <td>
                        <div class="row-actions">
                            <span class="tooltip-wrap" data-tip="Edit profile">
                                <button class="row-icon-btn" aria-label="Edit ${escapeHtml(item.name)}" data-edit="${escapeHtml(item.id)}">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"></path></svg>
                                </button>
                            </span>
                            <span class="tooltip-wrap" data-tip="Upload and register student face">
                                <button class="row-icon-btn" aria-label="Upload face for ${escapeHtml(item.name)}" data-upload-face="${escapeHtml(item.id)}">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="3.5"></circle><path d="M4 20c1.8-3.5 5-5 8-5s6.2 1.5 8 5"></path></svg>
                                </button>
                            </span>
                            <span class="tooltip-wrap" data-tip="Delete student">
                                <button class="row-icon-btn danger" aria-label="Delete ${escapeHtml(item.name)}" data-delete="${escapeHtml(item.id)}">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"></path><path d="M8 6V4h8v2"></path><path d="M19 6l-1 14H6L5 6"></path></svg>
                                </button>
                            </span>
                        </div>
                        <div class="face-upload-state ${statusClass(state.faceUploadStatus[item.id])}">${escapeHtml(statusLabel(state.faceUploadStatus[item.id]))}</div>
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

        Array.from(document.querySelectorAll("[data-upload-face]"))
            .forEach((button) => {
                button.addEventListener("click", () => {
                    const studentId = button.getAttribute("data-upload-face");
                    if (!studentId) {
                        return;
                    }

                    state.pendingFaceStudentId = studentId;
                    refs.faceUploadInput?.click();
                });
            });
    }

    function renderPagination() {
        if (refs.pageLabel) {
            refs.pageLabel.textContent = state.totalPages ? `Page ${state.page + 1} / ${state.totalPages}` : "Page 0 / 0";
        }
        if (refs.totalLabel) {
            refs.totalLabel.textContent = `${state.totalElements} records`;
        }
        if (refs.prevPageBtn) {
            refs.prevPageBtn.disabled = state.page <= 0;
        }
        if (refs.nextPageBtn) {
            refs.nextPageBtn.disabled = state.page + 1 >= state.totalPages;
        }
        if (refs.selectAll) {
            refs.selectAll.checked = false;
        }
    }

    function syncBulkActions() {
        const count = state.selected.size;
        if (refs.bulkDeleteBtn) {
            refs.bulkDeleteBtn.disabled = count === 0;
            refs.bulkDeleteBtn.textContent = count ? `Bulk Delete (${count})` : "Bulk Delete";
        }
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
            name: state.formData.name.trim(),
            course: (state.formData.course || "").trim(),
            semester: (state.formData.semester || "").trim(),
            section: (state.formData.section || "").trim(),
            batch: (state.formData.batch || "").trim(),
            phone: (state.formData.phone || "").trim()
        };

        try {
            await window.smsApi.admin.students.create(payload);
        } catch (error) {
            const message = error && error.message ? error.message : 'Unable to create student';
            toast(message, "error");
            return;
        }

        refs.form.reset();
        state.formData = { id: "", name: "", course: "", semester: "", section: "", batch: "", phone: "", email: "" };
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

        try {
            await window.smsApi.admin.students.remove(state.pendingDeleteId);
        } catch (_error) {
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
        let payload;
        try {
            payload = await window.smsApi.admin.students.bulkDelete(ids);
        } catch (_error) {
            toast("Bulk delete failed", "error");
            return;
        }
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
            const rows = await window.smsApi.admin.students.activity(8);
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
        if (window.SMSTheme && typeof window.SMSTheme.get === "function") {
            window.SMSTheme.set(window.SMSTheme.get());
        }
    }

    function openCommandPalette() {
        refs.commandPalette?.classList.add("open");
        if (refs.commandInput) {
            refs.commandInput.value = "";
        }
        renderCommands(commands);
        refs.commandInput?.focus();
    }

    function closeCommandPalette() {
        refs.commandPalette?.classList.remove("open");
    }

    function renderCommands(list) {
        if (!refs.commandList) {
            return;
        }
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

    async function uploadFaceForStudent(studentId, file) {
        state.faceUploadStatus[studentId] = { type: "pending", message: "Uploading..." };
        renderGrid();

        try {
            const tenantId = state.tenantId ? Number(state.tenantId) : null;
            const response = await window.smsApi.admin.students.uploadFace(studentId, file, {
                tenantId,
                livenessPrompt: "blink-and-turn",
                livenessVerified: true
            });
            const message = response && response.message ? response.message : "Face registered";
            state.faceUploadStatus[studentId] = { type: "ok", message };
            toast(`Face registered for ${studentId}`, "success");
        } catch (error) {
            const message = error && error.message ? error.message : "Face upload failed";
            state.faceUploadStatus[studentId] = { type: "error", message };
            toast(message, "error");
        }

        renderGrid();
    }

    function statusLabel(status) {
        if (!status || !status.message) {
            return "No face uploaded";
        }
        return status.message;
    }

    function statusClass(status) {
        if (!status || !status.type) {
            return "muted";
        }
        if (status.type === "ok") {
            return "ok";
        }
        if (status.type === "error") {
            return "error";
        }
        return "pending";
    }

    async function prepareFacePreview(studentId, file) {
        try {
            const image = await fileToImage(file);
            state.faceCropDraft = {
                studentId,
                originalFileName: file.name,
                image,
                zoom: 1,
                offsetX: 0,
                offsetY: 0,
                dragging: false,
                dragStartX: 0,
                dragStartY: 0
            };
            refs.faceZoomRange.value = "1";
            openFacePreviewModal();
            drawFaceCropPreview();
        } catch (_error) {
            state.pendingFaceStudentId = null;
            toast("Unable to open selected image for preview", "error");
        }
    }

    function openFacePreviewModal() {
        refs.facePreviewModal?.classList.add("open");
    }

    function closeFacePreviewModal() {
        refs.facePreviewModal?.classList.remove("open");
        state.faceCropDraft = null;
        state.pendingFaceStudentId = null;
    }

    function drawFaceCropPreview() {
        if (!state.faceCropDraft || !refs.faceCropCanvas) {
            return;
        }

        const canvas = refs.faceCropCanvas;
        const ctx = canvas.getContext("2d");
        const image = state.faceCropDraft.image;
        if (!ctx || !image) {
            return;
        }

        const canvasWidth = canvas.width;
        const canvasHeight = canvas.height;
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);

        const baseScale = Math.max(canvasWidth / image.width, canvasHeight / image.height);
        const zoom = state.faceCropDraft.zoom || 1;
        const drawWidth = image.width * baseScale * zoom;
        const drawHeight = image.height * baseScale * zoom;

        const x = (canvasWidth - drawWidth) / 2 + state.faceCropDraft.offsetX;
        const y = (canvasHeight - drawHeight) / 2 + state.faceCropDraft.offsetY;

        ctx.drawImage(image, x, y, drawWidth, drawHeight);

        ctx.strokeStyle = "rgba(255,255,255,0.9)";
        ctx.lineWidth = 2;
        ctx.strokeRect(1, 1, canvasWidth - 2, canvasHeight - 2);
    }

    function bindFaceCanvasDragEvents() {
        const canvas = refs.faceCropCanvas;
        if (!canvas) {
            return;
        }

        canvas.addEventListener("pointerdown", (event) => {
            if (!state.faceCropDraft) {
                return;
            }
            state.faceCropDraft.dragging = true;
            state.faceCropDraft.dragStartX = event.clientX;
            state.faceCropDraft.dragStartY = event.clientY;
            canvas.classList.add("dragging");
            canvas.setPointerCapture(event.pointerId);
        });

        canvas.addEventListener("pointermove", (event) => {
            if (!state.faceCropDraft || !state.faceCropDraft.dragging) {
                return;
            }

            const deltaX = event.clientX - state.faceCropDraft.dragStartX;
            const deltaY = event.clientY - state.faceCropDraft.dragStartY;
            state.faceCropDraft.dragStartX = event.clientX;
            state.faceCropDraft.dragStartY = event.clientY;
            state.faceCropDraft.offsetX += deltaX;
            state.faceCropDraft.offsetY += deltaY;
            drawFaceCropPreview();
        });

        const stopDrag = (event) => {
            if (!state.faceCropDraft) {
                return;
            }
            state.faceCropDraft.dragging = false;
            canvas.classList.remove("dragging");
            try {
                canvas.releasePointerCapture(event.pointerId);
            } catch (_error) {
                // ignore pointer release errors
            }
        };

        canvas.addEventListener("pointerup", stopDrag);
        canvas.addEventListener("pointercancel", stopDrag);
    }

    async function confirmFaceUpload() {
        if (!state.faceCropDraft || !refs.faceCropCanvas) {
            return;
        }

        const studentId = state.faceCropDraft.studentId;
        const blob = await canvasToBlob(refs.faceCropCanvas, "image/jpeg", 0.92);
        if (!blob) {
            toast("Unable to generate cropped image", "error");
            return;
        }

        const croppedFile = new File([blob], state.faceCropDraft.originalFileName || "face-crop.jpg", {
            type: "image/jpeg"
        });

        closeFacePreviewModal();
        await uploadFaceForStudent(studentId, croppedFile);
    }

    function fileToImage(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const image = new Image();
                image.onload = () => resolve(image);
                image.onerror = () => reject(new Error("Invalid image"));
                image.src = String(reader.result || "");
            };
            reader.onerror = () => reject(new Error("File read failed"));
            reader.readAsDataURL(file);
        });
    }

    function canvasToBlob(canvas, mimeType, quality) {
        return new Promise((resolve) => {
            canvas.toBlob((blob) => resolve(blob), mimeType, quality);
        });
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
