(function () {
    "use strict";

    const refs = {
        courseIdFilter: document.getElementById("courseIdFilter"),
        semesterFilter: document.getElementById("semesterFilter"),
        sectionFilter: document.getElementById("sectionFilter"),
        academicYearFilter: document.getElementById("academicYearFilter"),
        courseNameInput: document.getElementById("courseNameInput"),
        effectiveFromInput: document.getElementById("effectiveFromInput"),
        effectiveToInput: document.getElementById("effectiveToInput"),
        createdByInput: document.getElementById("createdByInput"),
        createTimetableBtn: document.getElementById("createTimetableBtn"),
        loadBtn: document.getElementById("loadTimetablesBtn"),
        timetableSelect: document.getElementById("timetableSelect"),
        addRowBtn: document.getElementById("addRowBtn"),
        generateWeekBtn: document.getElementById("generateWeekBtn"),
        saveWeeklyBtn: document.getElementById("saveWeeklyBtn"),
        exportJsonBtn: document.getElementById("exportJsonBtn"),
        weeklyJsonOutput: document.getElementById("weeklyJsonOutput"),
        copyJsonBtn: document.getElementById("copyJsonBtn"),
        weeklyRows: document.getElementById("weeklyRows"),
        weeklyStatus: document.getElementById("weeklyStatus")
    };

    const classTypeOptions = ["LECTURE", "TUTORIAL", "PRACTICAL", "SEMINAR", "PROJECT"];
    const dayOrder = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
    const dayKeyMap = {
        MONDAY: "monday",
        TUESDAY: "tuesday",
        WEDNESDAY: "wednesday",
        THURSDAY: "thursday",
        FRIDAY: "friday",
        SATURDAY: "saturday"
    };

    const subjectCatalog = {
        OOP: {
            subject: "Object Oriented Programming using Java",
            code: "2025CSET152",
            faculty: "Dr. Neha Sharma",
            roomLecture: "P-LH-101",
            roomPractical: "P-CC-018"
        },
        DMS: {
            subject: "Discrete Mathematical Structures",
            code: "2025MATH141",
            faculty: "Prof. Ankit Verma",
            roomLecture: "P-LH-102",
            roomPractical: "P-LH-102"
        },
        LAODE: {
            subject: "Linear Algebra and Ordinary Differential Equations",
            code: "2025MATH153",
            faculty: "Dr. Priyanka Singh",
            roomLecture: "P-LH-103",
            roomPractical: "P-LH-103"
        },
        DD: {
            subject: "Digital Design",
            code: "2025ECET161",
            faculty: "Dr. Raghavendra Iyer",
            roomLecture: "P-LH-201",
            roomPractical: "P-LA-302"
        },
        EVS: {
            subject: "Environment and Sustainability",
            code: "2025ENVS101",
            faculty: "Dr. Meera Nair",
            roomLecture: "P-LH-104",
            roomPractical: "P-LH-104"
        },
        SOFT: {
            subject: "Soft Skills and Personality Development",
            code: "2025HUMA115",
            faculty: "Ms. Kavya Menon",
            roomLecture: "P-LH-105",
            roomPractical: "P-LH-105"
        },
        EEE: {
            subject: "Introduction to Electrical and Electronics Engineering",
            code: "2025EEET121",
            faculty: "Prof. Sandeep Kulkarni",
            roomLecture: "P-LH-106",
            roomPractical: "P-LA-305"
        }
    };

    const weeklyTemplate = {
        MONDAY: [
            { key: "OOP", type: "LECTURE", startTime: "08:20", endTime: "09:20" },
            { key: "DMS", type: "LECTURE", startTime: "09:30", endTime: "10:30" },
            { key: "LAODE", type: "LECTURE", startTime: "10:40", endTime: "11:40" },
            { key: "SOFT", type: "TUTORIAL", startTime: "11:45", endTime: "12:45" },
            { key: "EEE", type: "LECTURE", startTime: "13:25", endTime: "14:25" }
        ],
        TUESDAY: [
            { key: "DD", type: "LECTURE", startTime: "08:20", endTime: "09:20" },
            { key: "OOP", type: "TUTORIAL", startTime: "09:30", endTime: "10:30" },
            { key: "EVS", type: "LECTURE", startTime: "10:40", endTime: "11:40" },
            { key: "DD", type: "PRACTICAL", startTime: "13:25", endTime: "14:25" },
            { key: "DD", type: "PRACTICAL", startTime: "14:35", endTime: "15:35" }
        ],
        WEDNESDAY: [
            { key: "LAODE", type: "LECTURE", startTime: "08:20", endTime: "09:20" },
            { key: "DMS", type: "TUTORIAL", startTime: "09:30", endTime: "10:30" },
            { key: "OOP", type: "LECTURE", startTime: "10:40", endTime: "11:40" },
            { key: "EEE", type: "LECTURE", startTime: "11:45", endTime: "12:45" },
            { key: "SOFT", type: "LECTURE", startTime: "13:25", endTime: "14:25" }
        ],
        THURSDAY: [
            { key: "EEE", type: "PRACTICAL", startTime: "08:20", endTime: "09:20" },
            { key: "EEE", type: "PRACTICAL", startTime: "09:30", endTime: "10:30" },
            { key: "DD", type: "LECTURE", startTime: "10:40", endTime: "11:40" },
            { key: "OOP", type: "LECTURE", startTime: "11:45", endTime: "12:45" },
            { key: "EVS", type: "LECTURE", startTime: "13:25", endTime: "14:25" }
        ],
        FRIDAY: [
            { key: "DMS", type: "LECTURE", startTime: "08:20", endTime: "09:20" },
            { key: "LAODE", type: "LECTURE", startTime: "09:30", endTime: "10:30" },
            { key: "OOP", type: "PRACTICAL", startTime: "10:40", endTime: "11:40" },
            { key: "OOP", type: "PRACTICAL", startTime: "11:45", endTime: "12:45" },
            { key: "DD", type: "TUTORIAL", startTime: "13:25", endTime: "14:25" }
        ],
        SATURDAY: [
            { key: "EEE", type: "TUTORIAL", startTime: "08:20", endTime: "09:20" },
            { key: "OOP", type: "LECTURE", startTime: "09:30", endTime: "10:30" },
            { key: "LAODE", type: "TUTORIAL", startTime: "10:40", endTime: "11:40" },
            { key: "EVS", type: "TUTORIAL", startTime: "11:45", endTime: "12:45" }
        ]
    };

    let currentTimetableId = null;

    function init() {
        if (refs.effectiveFromInput) {
            refs.effectiveFromInput.value = new Date().toISOString().slice(0, 10);
        }
        if (refs.createdByInput) {
            refs.createdByInput.value = "admin";
        }

        refs.loadBtn?.addEventListener("click", loadTimetables);
        refs.createTimetableBtn?.addEventListener("click", createTimetable);
        refs.timetableSelect?.addEventListener("change", onTimetableChange);
        refs.addRowBtn?.addEventListener("click", () => addRow());
        refs.generateWeekBtn?.addEventListener("click", generateRecommendedWeek);
        refs.saveWeeklyBtn?.addEventListener("click", saveWeekly);
        refs.exportJsonBtn?.addEventListener("click", exportWeeklyJson);
        refs.copyJsonBtn?.addEventListener("click", copyJson);
    }

    async function createTimetable() {
        const courseId = (refs.courseIdFilter?.value || "").trim();
        const semester = Number(refs.semesterFilter?.value || 0);
        const section = (refs.sectionFilter?.value || "").trim();
        const academicYear = (refs.academicYearFilter?.value || "").trim();
        const courseName = (refs.courseNameInput?.value || "").trim() || courseId;
        const effectiveFrom = (refs.effectiveFromInput?.value || "").trim();
        const effectiveTo = (refs.effectiveToInput?.value || "").trim();
        const createdBy = (refs.createdByInput?.value || "").trim() || "admin";

        if (!courseId || !semester || !academicYear || !effectiveFrom) {
            setStatus("Course ID, Semester, Academic Year and Effective From are required.");
            return;
        }

        setStatus("Creating timetable for class...");
        try {
            const payload = {
                courseId,
                courseName,
                semester,
                section,
                academicYear,
                effectiveFrom,
                effectiveTo: effectiveTo || null,
                createdBy
            };

            const created = await window.smsApi.request("/api/admin/timetables", {
                method: "POST",
                body: JSON.stringify(payload)
            });

            await loadTimetables();
            if (created && created.id) {
                refs.timetableSelect.value = String(created.id);
                await onTimetableChange();
            }
            setStatus("Timetable created. You can now generate and save the weekly plan.");
        } catch (error) {
            setStatus("Failed to create timetable: " + (error.message || "Unknown error"));
        }
    }

    async function loadTimetables() {
        setStatus("Loading timetables...");
        const query = {
            courseId: refs.courseIdFilter?.value?.trim() || "",
            semester: refs.semesterFilter?.value?.trim() || "",
            section: refs.sectionFilter?.value?.trim() || "",
            academicYear: refs.academicYearFilter?.value?.trim() || ""
        };

        try {
            const list = await window.smsApi.request("/api/admin/timetables?" + new URLSearchParams(query).toString());
            renderTimetableOptions(list || []);
            setStatus((list || []).length + " timetable(s) loaded");
        } catch (error) {
            setStatus("Failed to load timetables: " + (error.message || "Unknown error"));
        }
    }

    function renderTimetableOptions(list) {
        refs.timetableSelect.innerHTML = "<option value=\"\">Select timetable</option>";
        list.forEach((tt) => {
            const label = [tt.timetableCode, tt.courseName, "Sem " + (tt.semester || "-"), tt.section || "-", tt.academicYear || "-"].join(" | ");
            const opt = document.createElement("option");
            opt.value = String(tt.id);
            opt.textContent = label;
            refs.timetableSelect.appendChild(opt);
        });
    }

    async function onTimetableChange() {
        const id = refs.timetableSelect.value;
        currentTimetableId = id ? Number(id) : null;
        refs.weeklyRows.innerHTML = "";

        if (!currentTimetableId) {
            setStatus("Select a timetable to edit weekly schedule.");
            return;
        }

        setStatus("Loading schedule entries...");
        try {
            const tt = await window.smsApi.request("/api/admin/timetables/" + encodeURIComponent(currentTimetableId));
            const entries = Array.isArray(tt.scheduleEntries) ? tt.scheduleEntries : [];
            const weeklyEntries = entries.filter((entry) => entry.dayOfWeek);
            if (!weeklyEntries.length) {
                addRow();
                setStatus("No weekly rows found. Add rows and save.");
                return;
            }
            weeklyEntries
                .sort((a, b) => (a.dayOfWeek || "").localeCompare(b.dayOfWeek || "") || (a.startTime || "").localeCompare(b.startTime || ""))
                .forEach((entry) => addRow(entry));
            setStatus("Loaded " + weeklyEntries.length + " weekly rows.");
            exportWeeklyJson();
        } catch (error) {
            setStatus("Failed to load timetable details: " + (error.message || "Unknown error"));
        }
    }

    function generateRecommendedWeek() {
        refs.weeklyRows.innerHTML = "";
        dayOrder.forEach((day) => {
            const sessions = weeklyTemplate[day] || [];
            sessions.forEach((session) => {
                const subject = subjectCatalog[session.key];
                if (!subject) {
                    return;
                }

                addRow({
                    dayOfWeek: day,
                    startTime: session.startTime,
                    endTime: session.endTime,
                    subjectId: subject.code,
                    subjectName: subject.subject,
                    subjectCode: subject.code,
                    facultyId: toFacultyId(subject.faculty),
                    facultyName: subject.faculty,
                    roomId: session.type === "PRACTICAL" ? subject.roomPractical : subject.roomLecture,
                    roomNumber: session.type === "PRACTICAL" ? subject.roomPractical : subject.roomLecture,
                    classType: session.type
                });
            });
        });

        setStatus("Recommended weekly timetable generated. Review and click Save Entire Week.");
        exportWeeklyJson();
    }

    function addRow(entry) {
        const tr = document.createElement("tr");
        tr.innerHTML = [
            "<td>" + daySelectHtml(entry && entry.dayOfWeek) + "</td>",
            "<td><input data-field=\"startTime\" type=\"time\" value=\"" + esc(entry && entry.startTime) + "\"></td>",
            "<td><input data-field=\"endTime\" type=\"time\" value=\"" + esc(entry && entry.endTime) + "\"></td>",
            "<td><input data-field=\"subjectId\" value=\"" + esc(entry && entry.subjectId) + "\"></td>",
            "<td><input data-field=\"subjectName\" value=\"" + esc(entry && entry.subjectName) + "\"></td>",
            "<td><input data-field=\"subjectCode\" value=\"" + esc(entry && entry.subjectCode) + "\"></td>",
            "<td><input data-field=\"facultyId\" value=\"" + esc(entry && entry.facultyId) + "\"></td>",
            "<td><input data-field=\"facultyName\" value=\"" + esc(entry && entry.facultyName) + "\"></td>",
            "<td><input data-field=\"roomId\" value=\"" + esc(entry && entry.roomId) + "\"></td>",
            "<td><input data-field=\"roomNumber\" value=\"" + esc(entry && entry.roomNumber) + "\"></td>",
            "<td>" + classTypeSelectHtml(entry && entry.classType) + "</td>",
            "<td><button type=\"button\" class=\"btn btn-danger btn-sm\" data-remove-row>Remove</button></td>"
        ].join("");

        tr.querySelector("[data-remove-row]").addEventListener("click", () => tr.remove());
        refs.weeklyRows.appendChild(tr);
    }

    function daySelectHtml(value) {
        const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
        const opts = days.map((d) => "<option value=\"" + d + "\"" + (d === value ? " selected" : "") + ">" + d + "</option>").join("");
        return "<select data-field=\"dayOfWeek\">" + opts + "</select>";
    }

    function classTypeSelectHtml(value) {
        const opts = classTypeOptions.map((type) => "<option value=\"" + type + "\"" + (type === value ? " selected" : "") + ">" + type + "</option>").join("");
        return "<select data-field=\"classType\">" + opts + "</select>";
    }

    async function saveWeekly() {
        if (!currentTimetableId) {
            setStatus("Select a timetable first.");
            return;
        }

        const rows = Array.from(refs.weeklyRows.querySelectorAll("tr"));
        if (!rows.length) {
            setStatus("Add at least one row before saving.");
            return;
        }

        const entries = rows.map((row) => {
            const get = (name) => {
                const el = row.querySelector("[data-field='" + name + "']");
                return el ? String(el.value || "").trim() : "";
            };

            return {
                dayOfWeek: get("dayOfWeek"),
                startTime: get("startTime"),
                endTime: get("endTime"),
                subjectId: get("subjectId"),
                subjectName: get("subjectName"),
                subjectCode: get("subjectCode"),
                facultyId: get("facultyId"),
                facultyName: get("facultyName"),
                roomId: get("roomId"),
                roomNumber: get("roomNumber"),
                classType: get("classType") || "LECTURE"
            };
        });

        for (const entry of entries) {
            if (!entry.dayOfWeek || !entry.startTime || !entry.endTime || !entry.subjectId || !entry.subjectName) {
                setStatus("Please fill required fields: Day, Time, Subject ID, Subject Name.");
                return;
            }
        }

        const validationError = validateWeeklyConstraints(entries);
        if (validationError) {
            setStatus(validationError);
            return;
        }

        setStatus("Saving weekly schedule...");
        try {
            await window.smsApi.request("/api/admin/timetables/" + encodeURIComponent(currentTimetableId) + "/weekly-schedule", {
                method: "PUT",
                body: JSON.stringify({ entries: entries, replaceExisting: true })
            });
            setStatus("Weekly schedule updated successfully.");
            exportWeeklyJson();
        } catch (error) {
            setStatus("Failed to update weekly schedule: " + (error.message || "Unknown error"));
        }
    }

    function exportWeeklyJson() {
        const rows = Array.from(refs.weeklyRows.querySelectorAll("tr"));
        const result = {
            monday: [],
            tuesday: [],
            wednesday: [],
            thursday: [],
            friday: [],
            saturday: []
        };

        rows.forEach((row) => {
            const get = (name) => {
                const el = row.querySelector("[data-field='" + name + "']");
                return el ? String(el.value || "").trim() : "";
            };

            const day = get("dayOfWeek");
            const dayKey = dayKeyMap[day];
            if (!dayKey) {
                return;
            }

            result[dayKey].push({
                subject: get("subjectName"),
                code: get("subjectCode"),
                faculty: get("facultyName"),
                room: get("roomNumber"),
                startTime: toAmPm(get("startTime")),
                endTime: toAmPm(get("endTime")),
                type: normalizeType(get("classType")),
                attendanceStatus: "PENDING"
            });
        });

        Object.keys(result).forEach((key) => {
            result[key].sort((a, b) => to24(a.startTime).localeCompare(to24(b.startTime)));
        });

        if (refs.weeklyJsonOutput) {
            refs.weeklyJsonOutput.value = JSON.stringify(result, null, 2);
        }
    }

    function validateWeeklyConstraints(entries) {
        const byDay = new Map();
        entries.forEach((entry) => {
            const day = entry.dayOfWeek || "";
            if (!byDay.has(day)) {
                byDay.set(day, []);
            }
            byDay.get(day).push(entry);
        });

        for (const day of dayOrder) {
            const sessions = byDay.get(day) || [];
            if (sessions.length < 4 || sessions.length > 6) {
                return day + " must contain 4 to 6 classes.";
            }

            const sorted = sessions.slice().sort((a, b) => a.startTime.localeCompare(b.startTime));
            if (sorted[0].startTime !== "08:20") {
                return day + " must start at 08:20.";
            }

            for (let i = 0; i < sorted.length; i++) {
                const current = sorted[i];
                if (minutesBetween(current.startTime, current.endTime) !== 60) {
                    return day + " has a class that is not 60 minutes long.";
                }

                if (i > 0) {
                    const prev = sorted[i - 1];
                    const gap = minutesBetween(prev.endTime, current.startTime);
                    if (gap < 0) {
                        return day + " has overlapping classes.";
                    }
                    if (gap > 0 && gap < 5) {
                        return day + " has a gap shorter than 5 minutes.";
                    }
                    if (gap > 10 && gap < 30) {
                        return day + " has an invalid short gap; keep gaps 5-10 mins or a longer lunch break.";
                    }
                }
            }
        }

        return "";
    }

    function minutesBetween(start, end) {
        const s = (start || "").split(":");
        const e = (end || "").split(":");
        if (s.length < 2 || e.length < 2) {
            return 0;
        }
        const startM = Number(s[0]) * 60 + Number(s[1]);
        const endM = Number(e[0]) * 60 + Number(e[1]);
        return endM - startM;
    }

    async function copyJson() {
        const value = refs.weeklyJsonOutput?.value || "";
        if (!value.trim()) {
            setStatus("No JSON to copy. Generate or load timetable first.");
            return;
        }
        try {
            await navigator.clipboard.writeText(value);
            setStatus("Weekly JSON copied.");
        } catch (_error) {
            setStatus("Unable to copy automatically. Please copy from the textbox.");
        }
    }

    function setStatus(text) {
        if (refs.weeklyStatus) {
            refs.weeklyStatus.textContent = text;
        }
    }

    function esc(value) {
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

    function toFacultyId(name) {
        if (!name) {
            return "FAC-UNKNOWN";
        }
        return "FAC-" + name.toUpperCase().replace(/[^A-Z]+/g, "-").replace(/^-+|-+$/g, "");
    }

    function normalizeType(value) {
        const t = (value || "LECTURE").toUpperCase();
        if (t === "PRACTICAL") {
            return "Practical";
        }
        if (t === "TUTORIAL") {
            return "Tutorial";
        }
        return "Lecture";
    }

    function toAmPm(value) {
        if (!value || !value.includes(":")) {
            return value;
        }
        const parts = value.split(":");
        const hour = Number(parts[0]);
        const minute = parts[1];
        if (Number.isNaN(hour)) {
            return value;
        }
        const suffix = hour >= 12 ? "PM" : "AM";
        const h12 = hour % 12 === 0 ? 12 : hour % 12;
        return String(h12).padStart(2, "0") + ":" + minute + " " + suffix;
    }

    function to24(value) {
        if (!value || !value.includes(" ")) {
            return value;
        }
        const parts = value.split(" ");
        const hm = parts[0];
        const suffix = parts[1];
        const hmParts = hm.split(":");
        let h = Number(hmParts[0]);
        const m = hmParts[1] || "00";
        if (suffix === "PM" && h < 12) {
            h += 12;
        }
        if (suffix === "AM" && h === 12) {
            h = 0;
        }
        return String(h).padStart(2, "0") + ":" + m;
    }

    init();
})();
