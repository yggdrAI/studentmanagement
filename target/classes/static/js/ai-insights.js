(function () {
    "use strict";

    /* ── Resilient bootstrap ─────────────────────────────────────────────
     * The api-client.js may not have executed yet (e.g. loaded via sidebar
     * fragment with defer).  Instead of bailing out permanently we wait
     * for DOMContentLoaded and re-check, then fall back to direct fetch
     * if smsApi is still missing.
     */
    function ensureAnalyticsApi() {
        if (window.smsApi && window.smsApi.analytics) {
            return window.smsApi.analytics;
        }
        // Build a minimal shim so the rest of the code still works
        return {
            summary: function (qs) {
                return fetch('/api/analytics/summary' + (qs ? '?' + qs : ''), {
                    headers: { 'Accept': 'application/json' }
                }).then(function (r) {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                });
            },
            live: function () {
                return fetch('/api/analytics/live', {
                    headers: { 'Accept': 'application/json' }
                }).then(function (r) {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                });
            },
            sendDigest: function () {
                return fetch('/api/analytics/reports/digest', {
                    method: 'POST',
                    headers: { 'Accept': 'application/json' }
                }).then(function (r) {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                });
            }
        };
    }

    var analyticsApi = ensureAnalyticsApi();

    const app = document.getElementById("aiApp");
    const role = app?.getAttribute("data-role") || "ADMIN";

    const refs = {
        filterCourse: document.getElementById("filterCourse"),
        filterSemester: document.getElementById("filterSemester"),
        filterSection: document.getElementById("filterSection"),
        filterRange: document.getElementById("filterRange"),
        filterFrom: document.getElementById("filterFrom"),
        filterTo: document.getElementById("filterTo"),
        customFromWrap: document.getElementById("customFromWrap"),
        customToWrap: document.getElementById("customToWrap"),

        metricTotal: document.getElementById("metricTotal"),
        metricActive: document.getElementById("metricActive"),
        metricRisk: document.getElementById("metricRisk"),
        metricHigh: document.getElementById("metricHigh"),

        smartCards: document.getElementById("smartCards"),
        heatmapGrid: document.getElementById("heatmapGrid"),
        studentList: document.getElementById("studentList"),
        listSentinel: document.getElementById("listSentinel"),
        activityFeed: document.getElementById("activityFeed"),
        recommendationList: document.getElementById("recommendationList"),
        feedStatus: document.getElementById("feedStatus"),

        insightModal: document.getElementById("insightModal"),
        modalTitle: document.getElementById("modalTitle"),
        modalSubtitle: document.getElementById("modalSubtitle"),
        modalDetails: document.getElementById("modalDetails"),
        modalClose: document.getElementById("modalClose"),

        commandPalette: document.getElementById("commandPalette"),
        paletteBtn: document.getElementById("paletteBtn"),
        paletteInput: document.getElementById("paletteInput"),
        paletteList: document.getElementById("paletteList"),

        notificationPanel: document.getElementById("notificationPanel"),
        notificationBtn: document.getElementById("notificationBtn"),
        notificationClose: document.getElementById("notificationClose"),
        notificationList: document.getElementById("notificationList"),

        digestBtn: document.getElementById("digestBtn"),
        liveStatusBtn: document.getElementById("liveStatusBtn"),
        fabAdd: document.getElementById("fabAdd"),
        toastStack: document.getElementById("toastStack")
    };

    const state = {
        summary: null,
        displayedRows: 0,
        rowBatchSize: 20,
        notifications: [],
        stompClient: null,
        charts: {}
    };

    function cssVar(name, fallback) {
        const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
        return value || fallback;
    }

    function chartPalette() {
        return {
            axis: cssVar("--chart-axis", "#475569"),
            grid: cssVar("--chart-grid", "rgba(15, 23, 42, 0.12)"),
            tooltipBg: cssVar("--chart-tooltip-bg", "#ffffff"),
            tooltipText: cssVar("--chart-tooltip-text", "#0f172a"),
            c1: cssVar("--chart-accent-1", "#3b82f6"),
            c2: cssVar("--chart-accent-2", "#60a5fa"),
            c3: cssVar("--chart-accent-3", "#22c55e"),
            c4: cssVar("--chart-accent-4", "#f59e0b"),
            c5: cssVar("--chart-accent-5", "#ef4444"),
            accentRgb: cssVar("--accent-rgb", "59, 130, 246")
        };
    }

    const commands = [
        { label: "Go to Dashboard", action: () => (window.location.href = "/dashboard") },
        { label: "Go to Students", action: () => (window.location.href = "/admin/students") },
        { label: "Go to Analytics", action: () => (window.location.href = "/ai-insights") },
        { label: "Open Profile", action: () => (window.location.href = "/student/profile") },
        { label: "Voice: Show top students", action: () => startVoiceAssistant() },
        { label: "Refresh AI Insights", action: () => refreshSummary() }
    ];

    if (role === "ADMIN") {
        commands.push(
            { label: "Export CSV Report", action: () => window.location.href = "/api/analytics/export/csv" },
            { label: "Export PDF Report", action: () => window.location.href = "/api/analytics/export/pdf" },
            { label: "Send Leadership Digest", action: () => sendLeadershipDigest() }
        );
    }

    function init() {
        bindEvents();
        renderPalette(commands);
        refreshSummary();
        setupVirtualLoad();
        setupPullToRefresh();
        try {
            setupRealtime();
        } catch (err) {
            console.warn("Real-time setup failed, falling back to polling:", err);
            if (refs.feedStatus) refs.feedStatus.textContent = "Live stream unavailable; using polling";
            setInterval(pollLiveSnapshot, 7000);
        }
    }

    function bindEvents() {
        const debouncedRefresh = debounce(refreshSummary, 350);
        [refs.filterCourse, refs.filterSemester, refs.filterSection].forEach((el) => {
            if (!el) return;
            el.addEventListener("input", debouncedRefresh);
            el.addEventListener("change", debouncedRefresh);
        });

        refs.filterRange?.addEventListener("change", () => {
            const isCustom = refs.filterRange.value === "custom";
            refs.customFromWrap.classList.toggle("hidden", !isCustom);
            refs.customToWrap.classList.toggle("hidden", !isCustom);
            refreshSummary();
        });

        refs.filterFrom?.addEventListener("change", refreshSummary);
        refs.filterTo?.addEventListener("change", refreshSummary);

        refs.modalClose?.addEventListener("click", closeInsightModal);
        refs.insightModal?.addEventListener("click", (event) => {
            if (event.target === refs.insightModal) {
                closeInsightModal();
            }
        });

        refs.paletteBtn?.addEventListener("click", openPalette);
        refs.paletteInput?.addEventListener("input", onPaletteInput);
        refs.commandPalette?.addEventListener("click", (event) => {
            if (event.target === refs.commandPalette) {
                closePalette();
            }
        });

        refs.notificationBtn?.addEventListener("click", () => refs.notificationPanel.classList.add("open"));
        refs.notificationClose?.addEventListener("click", () => refs.notificationPanel.classList.remove("open"));
        refs.notificationPanel?.addEventListener("click", (event) => {
            if (event.target === refs.notificationPanel) {
                refs.notificationPanel.classList.remove("open");
            }
        });

        refs.digestBtn?.addEventListener("click", sendLeadershipDigest);

        refs.fabAdd?.addEventListener("click", () => {
            window.location.href = "/admin/students";
        });

        document.addEventListener("keydown", (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
                event.preventDefault();
                openPalette();
            }
            if (event.key === "Escape") {
                closePalette();
                closeInsightModal();
                refs.notificationPanel?.classList.remove("open");
            }
        });

        window.addEventListener("theme:changed", () => {
            if (!state.summary) {
                return;
            }
            renderCharts(state.summary.charts || {});
            renderHeatmap((state.summary.charts || {}).weeklyHeatmap || []);
        });
    }

    async function refreshSummary() {
        const query = buildSummaryQuery();
        try {
            state.summary = await analyticsApi.summary(query);
            state.displayedRows = 0;
            renderSummary();
            toast("AI insights synchronized", "success");
        } catch (error) {
            console.warn("Analytics summary fetch failed:", error);
            toast(error.message || "Failed to fetch analytics", "error");
            // Render empty state so the page is still usable
            if (!state.summary) {
                state.summary = { metrics: {}, smartCards: [], studentTags: [], charts: {}, activityFeed: [], recommendations: [] };
                renderSummary();
            }
        }
    }

    async function sendLeadershipDigest() {
        try {
            await analyticsApi.sendDigest();
            toast("Digest queued for delivery", "success");
        } catch (error) {
            toast(error.message || "Unable to send digest", "error");
        }
    }

    function buildSummaryQuery() {
        const params = new URLSearchParams();
        if (refs.filterCourse?.value) params.set("course", refs.filterCourse.value);
        if (refs.filterSemester?.value) params.set("semester", refs.filterSemester.value);
        if (refs.filterSection?.value) params.set("section", refs.filterSection.value);

        const range = refs.filterRange?.value || "30d";
        const today = new Date();
        const to = today.toISOString().slice(0, 10);

        if (range === "custom") {
            if (refs.filterFrom?.value) params.set("from", refs.filterFrom.value);
            if (refs.filterTo?.value) params.set("to", refs.filterTo.value);
        } else if (range === "7d") {
            const from = new Date(today);
            from.setDate(today.getDate() - 6);
            params.set("from", from.toISOString().slice(0, 10));
            params.set("to", to);
        } else {
            const from = new Date(today);
            from.setDate(today.getDate() - 29);
            params.set("from", from.toISOString().slice(0, 10));
            params.set("to", to);
        }

        return params.toString();
    }

    function renderSummary() {
        if (!state.summary) {
            return;
        }

        const metrics = state.summary.metrics || {};
        animateNumber(refs.metricTotal, Number(metrics.totalStudents || 0));
        animateNumber(refs.metricActive, Number(metrics.activeStudents || 0));
        animateNumber(refs.metricRisk, Number(metrics.atRiskStudents || 0));
        animateNumber(refs.metricHigh, Number(metrics.highPerformers || 0));

        renderSmartCards(state.summary.smartCards || []);
        renderCharts(state.summary.charts || {});
        renderHeatmap((state.summary.charts || {}).weeklyHeatmap || []);
        renderRecommendations(state.summary.recommendations || []);
        renderFeed(state.summary.activityFeed || []);
        renderStudentRowsIncremental(true);
    }

    function renderSmartCards(cards) {
        refs.smartCards.innerHTML = cards.map((card) => {
            const icon = toneEmoji(card.icon, card.tone);
            return `
                <article class="smart-card ${escapeHtml(card.tone || "info")}" data-card-id="${escapeHtml(card.id || "")}">
                    <div class="tone-icon">${icon}</div>
                    <h3>${escapeHtml(card.title || "Insight")}</h3>
                    <p>${escapeHtml(card.subtitle || "")}</p>
                </article>
            `;
        }).join("");

        Array.from(refs.smartCards.querySelectorAll(".smart-card")).forEach((cardEl) => {
            cardEl.addEventListener("click", () => {
                const cardId = cardEl.getAttribute("data-card-id");
                const card = cards.find((item) => item.id === cardId);
                if (card) {
                    openInsightModal(card);
                }
            });
        });
    }

    function renderCharts(charts) {
        renderLineChart("attendanceLine", charts.attendanceTrend || []);
        renderBarChart("marksBar", charts.marksDistribution || []);
        renderPieChart("deptPie", charts.departmentPerformance || []);
    }

    function renderLineChart(id, points) {
        const palette = chartPalette();
        const labels = points.map((p) => p.date?.slice(5) || "");
        const values = points.map((p) => Number(p.value || 0));
        const ctx = document.getElementById(id);
        if (!ctx) return;

        if (!points.length) {
            destroyChart(id);
            toggleChartEmpty(id, true);
            return;
        }

        toggleChartEmpty(id, false);

        destroyChart(id);
        state.charts[id] = new Chart(ctx, {
            type: "line",
            data: {
                labels,
                datasets: [{
                    label: "Attendance %",
                    data: values,
                    borderColor: palette.c1,
                    backgroundColor: `rgba(${palette.accentRgb}, 0.22)`,
                    tension: 0.35,
                    fill: true,
                    pointRadius: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { labels: { color: palette.axis } },
                    tooltip: {
                        backgroundColor: palette.tooltipBg,
                        titleColor: palette.tooltipText,
                        bodyColor: palette.tooltipText,
                        borderColor: palette.grid,
                        borderWidth: 1
                    }
                },
                scales: {
                    x: {
                        ticks: { color: palette.axis },
                        grid: { color: palette.grid }
                    },
                    y: {
                        suggestedMin: 0,
                        suggestedMax: 100,
                        ticks: { color: palette.axis },
                        grid: { color: palette.grid }
                    }
                }
            }
        });
    }

    function renderBarChart(id, buckets) {
        const palette = chartPalette();
        const labels = buckets.map((b) => b.label || "");
        const values = buckets.map((b) => Number(b.value || 0));
        const ctx = document.getElementById(id);
        if (!ctx) return;

        if (!buckets.length) {
            destroyChart(id);
            toggleChartEmpty(id, true);
            return;
        }

        toggleChartEmpty(id, false);

        destroyChart(id);
        state.charts[id] = new Chart(ctx, {
            type: "bar",
            data: {
                labels,
                datasets: [{
                    label: "Students",
                    data: values,
                    backgroundColor: [palette.c2, palette.c1, palette.c3, palette.c4, palette.c5]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { labels: { color: palette.axis } },
                    tooltip: {
                        backgroundColor: palette.tooltipBg,
                        titleColor: palette.tooltipText,
                        bodyColor: palette.tooltipText,
                        borderColor: palette.grid,
                        borderWidth: 1
                    }
                },
                scales: {
                    x: {
                        ticks: { color: palette.axis },
                        grid: { color: palette.grid }
                    },
                    y: {
                        ticks: { color: palette.axis },
                        grid: { color: palette.grid }
                    }
                }
            }
        });
    }

    function renderPieChart(id, entries) {
        const palette = chartPalette();
        const labels = entries.map((e) => e.label || "N/A");
        const values = entries.map((e) => Number(e.value || 0));
        const ctx = document.getElementById(id);
        if (!ctx) return;

        if (!entries.length) {
            destroyChart(id);
            toggleChartEmpty(id, true);
            return;
        }

        toggleChartEmpty(id, false);

        destroyChart(id);
        state.charts[id] = new Chart(ctx, {
            type: "pie",
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: [palette.c1, palette.c2, palette.c3, palette.c4, palette.c5, "#8b5cf6"]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { labels: { color: palette.axis } },
                    tooltip: {
                        backgroundColor: palette.tooltipBg,
                        titleColor: palette.tooltipText,
                        bodyColor: palette.tooltipText,
                        borderColor: palette.grid,
                        borderWidth: 1
                    }
                }
            }
        });
    }

    function destroyChart(id) {
        if (state.charts[id]) {
            state.charts[id].destroy();
            delete state.charts[id];
        }
    }

    function renderHeatmap(cells) {
        const palette = chartPalette();
        const heatmapEmpty = document.getElementById("heatmapEmpty");
        if (!cells.length) {
            refs.heatmapGrid.innerHTML = "";
            heatmapEmpty?.classList.add("show");
            return;
        }

        heatmapEmpty?.classList.remove("show");
        refs.heatmapGrid.innerHTML = cells.map((cell) => {
            const count = Number(cell.count || 0);
            const alpha = Math.min(0.95, 0.15 + count * 0.12);
            return `<div class="heat-cell" title="Day ${cell.day}, ${cell.hour}:00 -> ${count}" style="background: rgba(${palette.accentRgb},${alpha});"></div>`;
        }).join("");
    }

    function toggleChartEmpty(id, show) {
        const empty = document.getElementById(`${id}Empty`);
        const canvas = document.getElementById(id);
        if (!empty || !canvas) {
            return;
        }
        empty.classList.toggle("show", show);
        canvas.style.visibility = show ? "hidden" : "visible";
    }

    function renderStudentRowsIncremental(reset) {
        const rows = state.summary?.studentTags || [];
        if (reset) {
            refs.studentList.innerHTML = "";
            state.displayedRows = 0;
        }
        if (state.displayedRows >= rows.length) {
            return;
        }

        const slice = rows.slice(state.displayedRows, state.displayedRows + state.rowBatchSize);
        const html = slice.map((row) => `
            <article class="student-row ${escapeHtml(row.glow || "stable")}" data-student-id="${escapeHtml(row.studentId || "")}">
                <div class="student-main">
                    <div>
                        <strong>${escapeHtml(row.name || row.studentId || "Student")}</strong>
                        <div class="student-meta">${escapeHtml(row.course || "Course N/A")} • ${escapeHtml(row.semester || "Semester N/A")}</div>
                    </div>
                    <span class="student-tag">${tagEmoji(row.tag)} ${escapeHtml(row.tag || "Stable")}</span>
                </div>
                <div class="student-meta">Attendance: ${escapeHtml(String(row.attendance ?? 0))}% | Marks: ${escapeHtml(String(row.avgMarks ?? 0))} | Trend: ${escapeHtml(String(row.trendDelta ?? 0))}%</div>
                <div class="student-anomalies">${escapeHtml((row.anomalies || []).join(" • ") || "No critical anomalies")}</div>
                <div class="swipe-actions" aria-hidden="true">
                    <button data-action="edit">Edit</button>
                    <button data-action="delete">Delete</button>
                </div>
            </article>
        `).join("");

        refs.studentList.insertAdjacentHTML("beforeend", html);
        state.displayedRows += slice.length;
        attachRowInteractions();
    }

    function setupVirtualLoad() {
        if (!refs.listSentinel) return;

        const observer = new IntersectionObserver((entries) => {
            if (entries.some((entry) => entry.isIntersecting)) {
                renderStudentRowsIncremental(false);
            }
        }, { rootMargin: "200px" });

        observer.observe(refs.listSentinel);
    }

    function renderFeed(items) {
        refs.activityFeed.innerHTML = items.slice(0, 40).map((item) => `
            <article class="feed-item">
                <div>${escapeHtml(item.message || "Live event")}</div>
                <span class="feed-time">${escapeHtml(item.time || "")}</span>
            </article>
        `).join("");

        refs.notificationList.innerHTML = refs.activityFeed.innerHTML;
    }

    function appendFeedItem(message, time) {
        const node = document.createElement("article");
        node.className = "feed-item";
        node.innerHTML = `<div>${escapeHtml(message)}</div><span class="feed-time">${escapeHtml(time || new Date().toISOString())}</span>`;
        refs.activityFeed.prepend(node);

        while (refs.activityFeed.children.length > 60) {
            refs.activityFeed.removeChild(refs.activityFeed.lastChild);
        }

        refs.notificationList.prepend(node.cloneNode(true));
        while (refs.notificationList.children.length > 80) {
            refs.notificationList.removeChild(refs.notificationList.lastChild);
        }
    }

    function renderRecommendations(items) {
        refs.recommendationList.innerHTML = items.map((item) => `<li>${escapeHtml(item)}</li>`).join("");
    }

    function openInsightModal(card) {
        refs.modalTitle.textContent = card.title || "Insight";
        refs.modalSubtitle.textContent = card.subtitle || "";
        refs.modalDetails.innerHTML = (card.details || []).map((line) => `<li>${escapeHtml(line)}</li>`).join("");
        refs.insightModal.classList.add("open");
        refs.insightModal.setAttribute("aria-hidden", "false");
    }

    function closeInsightModal() {
        refs.insightModal.classList.remove("open");
        refs.insightModal.setAttribute("aria-hidden", "true");
    }

    function openPalette() {
        refs.commandPalette.classList.add("open");
        refs.paletteInput.value = "";
        renderPalette(commands);
        refs.paletteInput.focus();
    }

    function closePalette() {
        refs.commandPalette.classList.remove("open");
    }

    function onPaletteInput() {
        const term = refs.paletteInput.value.trim().toLowerCase();
        renderPalette(commands.filter((cmd) => cmd.label.toLowerCase().includes(term)));
    }

    function renderPalette(list) {
        refs.paletteList.innerHTML = list.map((cmd, idx) => `<button type="button" class="palette-item" data-cmd="${idx}">${escapeHtml(cmd.label)}</button>`).join("");

        Array.from(refs.paletteList.querySelectorAll("[data-cmd]")).forEach((btn) => {
            btn.addEventListener("click", () => {
                const idx = Number(btn.getAttribute("data-cmd"));
                const cmd = list[idx];
                if (cmd) {
                    closePalette();
                    cmd.action();
                }
            });
        });
    }

    function setupRealtime() {
        if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
            console.warn("SockJS or Stomp not loaded, falling back to API polling");
            if (refs.feedStatus) refs.feedStatus.textContent = "Live stream libraries unavailable; using polling";
            setInterval(pollLiveSnapshot, 7000);
            return;
        }

        try {
            const socket = new SockJS("/ws");
            state.stompClient = Stomp.over(socket);
            state.stompClient.debug = function () {};

            state.stompClient.connect({}, () => {
                if (refs.feedStatus) refs.feedStatus.textContent = "Live stream connected";
                if (refs.liveStatusBtn) refs.liveStatusBtn.classList.add("connected");

                state.stompClient.subscribe("/topic/analytics/live", (msg) => {
                    try {
                        const payload = JSON.parse(msg.body);
                        animateNumber(refs.metricActive, Number(payload.activeStudents || 0));
                        appendFeedItem(`Live update: ${Number(payload.activeStudents || 0)} active students`, payload.timestamp);
                    } catch (_error) {
                        toast("Live message parse error", "error");
                    }
                });

                state.stompClient.subscribe("/topic/analytics/feed", (msg) => {
                    try {
                        const payload = JSON.parse(msg.body);
                        appendFeedItem(payload.message || "New activity", payload.timestamp);
                        toast(payload.message || "New activity", "info");
                    } catch (_error) {
                        toast("Feed update parse error", "error");
                    }
                });
            }, () => {
                if (refs.feedStatus) refs.feedStatus.textContent = "Live stream unavailable; retrying via API";
                setInterval(pollLiveSnapshot, 7000);
            });
        } catch (err) {
            console.warn("WebSocket setup error:", err);
            if (refs.feedStatus) refs.feedStatus.textContent = "Live stream unavailable; using polling";
            setInterval(pollLiveSnapshot, 7000);
        }
    }

    async function pollLiveSnapshot() {
        try {
            const payload = await analyticsApi.live();
            animateNumber(refs.metricActive, Number(payload.activeStudents || 0));
        } catch (_error) {
            // no-op fallback
        }
    }

    function attachRowInteractions() {
        Array.from(refs.studentList.querySelectorAll(".student-row")).forEach((row) => {
            if (row.dataset.bound === "true") return;
            row.dataset.bound = "true";

            let startX = 0;
            row.addEventListener("touchstart", (event) => {
                startX = event.touches[0].clientX;
            }, { passive: true });

            row.addEventListener("touchmove", (event) => {
                const delta = event.touches[0].clientX - startX;
                if (delta < -30) {
                    row.style.transform = "translateX(-58px)";
                } else if (delta > 12) {
                    row.style.transform = "translateX(0)";
                }
            }, { passive: true });

            Array.from(row.querySelectorAll(".swipe-actions button")).forEach((btn) => {
                btn.addEventListener("click", () => {
                    const action = btn.getAttribute("data-action");
                    const studentId = row.getAttribute("data-student-id");
                    if (action === "edit") {
                        window.location.href = `/admin/students/${encodeURIComponent(studentId)}/profile`;
                    } else {
                        toast(`Use bulk controls to delete ${studentId}`, "warning");
                        row.style.transform = "translateX(0)";
                    }
                });
            });
        });
    }

    function setupPullToRefresh() {
        let touchStartY = 0;
        let touchedAtTop = false;

        document.addEventListener("touchstart", (event) => {
            touchedAtTop = window.scrollY <= 0;
            touchStartY = event.touches[0].clientY;
        }, { passive: true });

        document.addEventListener("touchend", (event) => {
            if (!touchedAtTop) return;
            const endY = event.changedTouches[0].clientY;
            if (endY - touchStartY > 90) {
                toast("Refreshing insights...", "info");
                refreshSummary();
            }
        }, { passive: true });
    }

    function startVoiceAssistant() {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            toast("Voice assistant is not supported in this browser", "warning");
            return;
        }

        const recog = new SpeechRecognition();
        recog.lang = "en-US";
        recog.interimResults = false;
        recog.maxAlternatives = 1;
        toast("Listening... try saying: show top students", "info");
        recog.onresult = (event) => {
            const text = String(event.results?.[0]?.[0]?.transcript || "").toLowerCase();
            if (text.includes("top") && text.includes("student")) {
                document.querySelector('.student-row')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                toast("Showing top student signals", "success");
            } else if (text.includes("analytics")) {
                refreshSummary();
            } else {
                toast(`Heard: ${text}`, "info");
            }
        };
        recog.onerror = () => toast("Voice assistant failed to capture command", "error");
        recog.start();
    }

    function animateNumber(node, target) {
        if (!node) return;
        const start = Number((node.textContent || "0").replace(/[^0-9.-]/g, "")) || 0;
        const duration = 380;
        const startTime = performance.now();

        function frame(now) {
            const t = Math.min(1, (now - startTime) / duration);
            const value = start + ((target - start) * easeOutCubic(t));
            node.textContent = Math.round(value).toString();
            if (t < 1) requestAnimationFrame(frame);
        }

        requestAnimationFrame(frame);
    }

    function toast(message, tone) {
        const item = document.createElement("div");
        item.className = "toast" + (tone ? ` ${tone}` : "");
        item.textContent = message;
        refs.toastStack.appendChild(item);
        setTimeout(() => item.remove(), 3000);
    }

    function toneEmoji(icon, tone) {
        if (icon === "trend-down") return "📉";
        if (icon === "spark") return "✨";
        if (icon === "alert") return "⚠";
        if (icon === "calendar") return "🗓";
        if (tone === "success") return "✅";
        if (tone === "danger") return "🚨";
        return "🧠";
    }

    function tagEmoji(tag) {
        if (tag === "At Risk") return "⚠";
        if (tag === "High Performer") return "🔥";
        if (tag === "Declining") return "📉";
        return "•";
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) return "";
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/\"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function debounce(fn, delay) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    function easeOutCubic(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
