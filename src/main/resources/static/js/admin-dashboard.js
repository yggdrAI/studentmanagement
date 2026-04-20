(function () {
    function boot() {
        var api = window.smsApi;
        if (!api || !api.admin || !api.admin.dashboard) {
            window.setTimeout(boot, 50);
            return;
        }

        var state = {
            charts: {},
            analyticsData: null,
            analyticsLoaded: false,
            analyticsLoading: false,
            analyticsObserver: null
        };

        if (window.Chart) {
            window.Chart.defaults.font.family = 'Outfit, Inter, system-ui, sans-serif';
            window.Chart.defaults.color = chartPalette().axis;
        }

        wireRestoreControls();
        loadSummaryAndHealth();
        wireAnalyticsLoader();
        window.setTimeout(loadAlerts, 0);
        window.setTimeout(loadRecentActivity, 0);

        window.addEventListener('theme:changed', function () {
            if (state.analyticsLoaded && state.analyticsData) {
                renderAnalytics(state.analyticsData);
            }
        });

        function wireAnalyticsLoader() {
            var analyticsCard = document.querySelector('.analytics-card');
            var startLoading = function () {
                if (state.analyticsLoaded || state.analyticsLoading) {
                    return;
                }
                loadAnalytics();
            };

            if (analyticsCard && 'IntersectionObserver' in window) {
                state.analyticsObserver = new IntersectionObserver(function (entries, observer) {
                    entries.forEach(function (entry) {
                        if (entry.isIntersecting) {
                            observer.disconnect();
                            startLoading();
                        }
                    });
                }, {
                    rootMargin: '180px 0px',
                    threshold: 0.15
                });

                state.analyticsObserver.observe(analyticsCard);
                return;
            }

            window.setTimeout(startLoading, 0);
        }

        function loadSummaryAndHealth() {
            Promise.all([
                api.admin.dashboard.summary(),
                api.admin.dashboard.databaseHealth()
            ]).then(function (results) {
                renderKpis(results[0].kpis || []);
                renderDatabaseHealth(results[1] || {});
            }).catch(function () {
                renderKpis([]);
                renderDatabaseHealth({});
            });
        }

        function loadAnalytics() {
            if (state.analyticsLoaded || state.analyticsLoading) {
                return;
            }

            state.analyticsLoading = true;
            api.admin.dashboard.analytics().then(function (analytics) {
                state.analyticsData = analytics || {};
                state.analyticsLoaded = true;
                renderAnalytics(state.analyticsData);
            }).catch(function () {
                clearCharts();
                renderNoDataChart('studentsGrowthChart', 'Analytics unavailable');
                renderNoDataChart('attendanceTrendChart', 'Analytics unavailable');
                renderNoDataChart('classesPerDayChart', 'Analytics unavailable');
            }).finally(function () {
                state.analyticsLoading = false;
            });
        }

        function renderAnalytics(analytics) {
            var payload = analytics || {};
            renderGrowthChart(payload.studentsGrowth || []);
            renderAttendanceChart(payload.attendanceTrend || []);
            renderClassesChart(payload.classesPerDay || []);
            updateChartInsights(payload);
        }

        function loadAlerts() {
            api.admin.dashboard.alerts().then(function (alerts) {
                renderSignalList(
                    document.getElementById('lowAttendanceList'),
                    alerts.lowAttendance,
                    function (item) {
                        return {
                            title: item.studentId + ' - ' + item.attendanceRate + '% attendance',
                            meta: item.totalClasses + ' classes recorded'
                        };
                    },
                    'No low attendance risks in current window.'
                );

                renderSignalList(
                    document.getElementById('systemIssueList'),
                    alerts.systemIssues,
                    function (item) {
                        return {
                            title: item.title || 'Issue',
                            meta: (item.detail || '-') + ' | ' + (item.time || '-')
                        };
                    },
                    'No critical system issues reported.'
                );

                renderSignalList(
                    document.getElementById('suspiciousList'),
                    alerts.suspiciousActivity,
                    function (item) {
                        return {
                            title: (item.studentId || '-') + ' - ' + (item.decision || '-'),
                            meta: 'Score ' + item.score + ' | ' + (item.time || '-')
                        };
                    },
                    'No suspicious activity found.'
                );
            }).catch(function () {
                renderSignalList(document.getElementById('lowAttendanceList'), [], null, 'Unable to load alerts.');
                renderSignalList(document.getElementById('systemIssueList'), [], null, 'Unable to load system issues.');
                renderSignalList(document.getElementById('suspiciousList'), [], null, 'Unable to load suspicious activity.');
            });
        }

        function loadRecentActivity() {
            var target = document.getElementById('recentActivityList');
            if (!target) {
                return;
            }

            api.admin.dashboard.recentActivity(12).then(function (entries) {
                target.innerHTML = '';
                if (!entries || entries.length === 0) {
                    target.innerHTML = '<li class="activity-item"><p class="title">No recent activity</p></li>';
                    return;
                }

                entries.forEach(function (entry) {
                    var li = document.createElement('li');
                    li.className = 'activity-item';
                    li.innerHTML = '<p class="title">' + esc(entry.actor) + ' - ' + esc(entry.action) + '</p>' +
                        '<p class="meta">' + esc(entry.endpoint) + ' | HTTP ' + esc(entry.status) + ' | ' + esc(entry.time) + '</p>';
                    target.appendChild(li);
                });
            }).catch(function () {
                target.innerHTML = '<li class="activity-item"><p class="title">Unable to load recent activity</p></li>';
            });
        }

        function wireRestoreControls() {
            var restoreBtn = document.getElementById('restoreDbBtn');
            var restoreInput = document.getElementById('restoreDbInput');

            if (!restoreBtn || !restoreInput) {
                return;
            }

            restoreBtn.addEventListener('click', function () {
                restoreInput.click();
            });

            restoreInput.addEventListener('change', function () {
                var file = restoreInput.files && restoreInput.files[0];
                if (!file) {
                    return;
                }

                var body = new FormData();
                body.append('file', file);

                restoreBtn.disabled = true;
                restoreBtn.textContent = 'Restoring...';

                fetch('/api/admin/database/restore', {
                    method: 'POST',
                    body: body,
                    headers: { Accept: 'application/json' }
                }).then(function (response) {
                    return response.json().then(function (payload) {
                        if (!response.ok) {
                            throw new Error(payload.message || 'Restore failed');
                        }
                        return payload;
                    });
                }).then(function (payload) {
                    window.alert(payload.message || 'Restore completed');
                    window.location.reload();
                }).catch(function (err) {
                    window.alert(err.message || 'Restore failed');
                }).finally(function () {
                    restoreBtn.disabled = false;
                    restoreBtn.textContent = 'Restore Backup';
                    restoreInput.value = '';
                });
            });
        }

        function renderKpis(items) {
            var grid = document.getElementById('kpiGrid');
            if (!grid) {
                return;
            }

            grid.innerHTML = '';
            if (!items || items.length === 0) {
                grid.innerHTML = '<article class="kpi-card"><p class="kpi-label">Dashboard metrics unavailable</p></article>';
                return;
            }

            items.forEach(function (item) {
                var card = document.createElement('article');
                card.className = 'kpi-card';
                var dirClass = item.trendDirection === 'down' ? 'down' : 'up';
                card.innerHTML = '<p class="kpi-label">' + esc(item.label) + '</p>' +
                    '<p class="kpi-value">' + icon(item.icon) + ' ' + esc(item.value) + '</p>' +
                    '<p class="kpi-trend ' + dirClass + '">' + esc(item.trend || '') + '</p>';
                grid.appendChild(card);
            });

            var statusKpi = items.find(function (k) { return k.icon === 'system'; });
            var badge = document.getElementById('systemHealthBadge');
            if (statusKpi && badge) {
                badge.textContent = statusKpi.value;
                badge.classList.remove('healthy', 'warning', 'critical');
                badge.classList.add(statusClass(statusKpi.value));
            }
        }

        function renderDatabaseHealth(health) {
            var metrics = document.getElementById('dbHealthMetrics');
            var statusBadge = document.getElementById('dbStatusBadge');
            var fill = document.getElementById('storageBar');
            var storageText = document.getElementById('storageText');
            var advanced = document.getElementById('advancedInfoDump');

            if (!metrics || !statusBadge || !fill || !storageText || !advanced) {
                return;
            }

            metrics.innerHTML = '';
            appendMetric(metrics, 'Total Tables', health.totalTables || 0);
            appendMetric(metrics, 'Total Records', health.totalRecords || 0);
            appendMetric(metrics, 'Last Backup', health.lastBackupTime || '-');
            appendMetric(metrics, 'Migration', health.migrationSuccess ? 'Successful' : 'Pending / Failed');

            var usage = Number(health.storageUsagePercent || 0);
            fill.style.width = usage + '%';
            storageText.textContent = usage + '% utilized';

            statusBadge.textContent = health.status || 'Unknown';
            statusBadge.classList.remove('healthy', 'warning', 'critical');
            statusBadge.classList.add(statusClass(health.status || 'Unknown'));

            var advancedInfo = health.advancedInfo || {};
            var lines = [
                'Mode: ' + safe(advancedInfo.mode),
                'Migration required: ' + safe(advancedInfo.migrationRequired),
                'Already persistent: ' + safe(advancedInfo.alreadyPersistent),
                'Source path: ' + safe(advancedInfo.sourcePath),
                'Persistent path: ' + safe(advancedInfo.persistentPath),
                'Source URL: ' + safe(advancedInfo.sourceUrl),
                'Persistent URL: ' + safe(advancedInfo.persistentUrl),
                'Migration message: ' + safe(health.migrationMessage)
            ];

            advanced.textContent = lines.join('\n');
        }

        function appendMetric(container, label, value) {
            var block = document.createElement('div');
            block.className = 'metric-block';
            block.innerHTML = '<p class="label">' + esc(label) + '</p><p class="value">' + esc(value) + '</p>';
            container.appendChild(block);
        }

        function renderGrowthChart(data) {
            var ctx = getChartCanvas('studentsGrowthChart');
            if (!ctx) {
                return;
            }

            if (!data || data.length === 0) {
                renderNoDataChart('studentsGrowthChart', 'Analytics unavailable');
                return;
            }

            destroyChart(state, 'growth');
            setChartEmptyState('studentsGrowthChart', false);
            state.charts.growth = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: data.map(function (row) { return row.label; }),
                    datasets: [
                        {
                            label: 'Students',
                            data: data.map(function (row) { return row.students; }),
                            borderColor: function (context) {
                                return createHorizontalGradient(context.chart, [
                                    { stop: 0, color: '#3b82f6' },
                                    { stop: 1, color: '#8b5cf6' }
                                ]);
                            },
                            backgroundColor: function (context) {
                                return createVerticalGradient(context.chart, [
                                    { stop: 0, color: 'rgba(59,130,246,0.42)' },
                                    { stop: 1, color: 'rgba(139,92,246,0.02)' }
                                ]);
                            },
                            fill: true,
                            tension: 0.42,
                            cubicInterpolationMode: 'monotone',
                            borderWidth: 3,
                            pointRadius: 3.5,
                            pointHoverRadius: 7,
                            pointBackgroundColor: '#ffffff',
                            pointBorderColor: '#8b5cf6',
                            pointBorderWidth: 2,
                            pointHoverBackgroundColor: '#ffffff',
                            pointHoverBorderColor: '#3b82f6'
                        },
                        {
                            label: 'Teachers',
                            data: data.map(function (row) { return row.teachers; }),
                            borderColor: function (context) {
                                return createHorizontalGradient(context.chart, [
                                    { stop: 0, color: '#22c55e' },
                                    { stop: 1, color: '#14b8a6' }
                                ]);
                            },
                            backgroundColor: 'transparent',
                            fill: false,
                            tension: 0.42,
                            cubicInterpolationMode: 'monotone',
                            borderWidth: 3,
                            pointRadius: 3,
                            pointHoverRadius: 6,
                            pointBackgroundColor: '#ffffff',
                            pointBorderColor: '#14b8a6',
                            pointBorderWidth: 2
                        }
                    ]
                },
                options: chartOptions('line', {
                    plugins: {
                        glow: {
                            color: 'rgba(59,130,246,0.22)',
                            blur: 18
                        },
                        valueLabels: {
                            enabled: true,
                            datasetIndexes: [0],
                            formatter: function (value) {
                                return formatCompactNumber(value);
                            }
                        }
                    }
                })
            });
        }

        function renderAttendanceChart(data) {
            var ctx = getChartCanvas('attendanceTrendChart');
            if (!ctx) {
                return;
            }

            if (!data || data.length === 0) {
                renderNoDataChart('attendanceTrendChart', 'Analytics unavailable');
                return;
            }

            var average = averageValue(data, 'value');
            var averageSeries = data.map(function () {
                return average;
            });

            destroyChart(state, 'attendance');
            setChartEmptyState('attendanceTrendChart', false);
            state.charts.attendance = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: data.map(function (row) { return row.label; }),
                    datasets: [
                        {
                            label: 'Attendance %',
                            data: data.map(function (row) { return row.value; }),
                            borderColor: function (context) {
                                return createHorizontalGradient(context.chart, [
                                    { stop: 0, color: '#10b981' },
                                    { stop: 1, color: '#bef264' }
                                ]);
                            },
                            backgroundColor: function (context) {
                                return createVerticalGradient(context.chart, [
                                    { stop: 0, color: 'rgba(16,185,129,0.4)' },
                                    { stop: 1, color: 'rgba(190,242,100,0.02)' }
                                ]);
                            },
                            fill: true,
                            tension: 0.44,
                            cubicInterpolationMode: 'monotone',
                            borderWidth: 3,
                            pointRadius: 4,
                            pointHoverRadius: 8,
                            pointBackgroundColor: '#ffffff',
                            pointBorderColor: '#10b981',
                            pointBorderWidth: 2,
                            pointHoverBackgroundColor: '#ffffff',
                            pointHoverBorderColor: '#bef264'
                        },
                        {
                            label: 'Average',
                            data: averageSeries,
                            borderColor: 'rgba(148,163,184,0.9)',
                            backgroundColor: 'transparent',
                            borderDash: [8, 8],
                            borderWidth: 2,
                            pointRadius: 0,
                            pointHoverRadius: 0,
                            fill: false,
                            tension: 0
                        }
                    ]
                },
                options: chartOptions('line', {
                    plugins: {
                        glow: {
                            color: 'rgba(16,185,129,0.2)',
                            blur: 16
                        },
                        valueLabels: {
                            enabled: true,
                            datasetIndexes: [0],
                            formatter: function (value) {
                                return formatPercent(value);
                            }
                        }
                    },
                    scales: {
                        y: {
                            suggestedMax: 100,
                            ticks: {
                                callback: function (value) {
                                    return value + '%';
                                }
                            }
                        }
                    }
                })
            });
        }

        function renderClassesChart(data) {
            var ctx = getChartCanvas('classesPerDayChart');
            if (!ctx) {
                return;
            }

            if (!data || data.length === 0) {
                renderNoDataChart('classesPerDayChart', 'Analytics unavailable');
                return;
            }

            destroyChart(state, 'classes');
            setChartEmptyState('classesPerDayChart', false);
            state.charts.classes = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: data.map(function (row) { return row.label; }),
                    datasets: [
                        {
                            data: data.map(function (row) { return row.value; }),
                            borderRadius: 999,
                            borderSkipped: false,
                            borderWidth: 0,
                            maxBarThickness: 44,
                            backgroundColor: function (context) {
                                return createVerticalGradient(context.chart, [
                                    { stop: 0, color: '#14b8a6' },
                                    { stop: 1, color: '#22d3ee' }
                                ]);
                            }
                        }
                    ]
                },
                options: chartOptions('bar', {
                    plugins: {
                        glow: {
                            color: 'rgba(34,211,238,0.24)',
                            blur: 16
                        },
                        valueLabels: {
                            enabled: true,
                            datasetIndexes: [0],
                            formatter: function (value) {
                                return formatCompactNumber(value);
                            }
                        }
                    },
                    scales: {
                        y: {
                            suggestedMin: 0,
                            ticks: {
                                callback: function (value) {
                                    return value;
                                }
                            }
                        }
                    }
                })
            });
        }

        function renderNoDataChart(canvasId, label) {
            var chartKey = canvasId.replace('Chart', '').replace(/^[a-z]/, function (m) { return m.toLowerCase(); });
            destroyChart(state, chartKey);
            setChartEmptyState(canvasId, true, label || 'No analytics data available yet.');
        }

        function renderSignalList(container, items, mapper, emptyMessage) {
            if (!container) {
                return;
            }

            container.innerHTML = '';
            if (!items || items.length === 0) {
                container.innerHTML = '<li class="signal-item"><p class="title">' + esc(emptyMessage || 'No data') + '</p></li>';
                return;
            }

            items.forEach(function (raw) {
                var shaped = mapper ? mapper(raw) : raw;
                var li = document.createElement('li');
                li.className = 'signal-item';
                li.innerHTML = '<p class="title">' + esc(shaped.title || '-') + '</p>' +
                    '<p class="meta">' + esc(shaped.meta || '-') + '</p>';
                container.appendChild(li);
            });
        }

        function chartOptions(kind, overrides) {
            var palette = chartPalette();
            var config = {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    duration: 1400,
                    easing: 'easeOutQuart'
                },
                interaction: {
                    mode: 'nearest',
                    intersect: false
                },
                layout: {
                    padding: {
                        top: 8,
                        right: 8,
                        bottom: 2,
                        left: 2
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false,
                            drawBorder: false
                        },
                        ticks: {
                            color: palette.axis,
                            font: {
                                family: 'Outfit, Inter, system-ui, sans-serif',
                                size: 11,
                                weight: '600'
                            }
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: palette.grid,
                            borderDash: [5, 6],
                            drawBorder: false
                        },
                        ticks: {
                            color: palette.axis,
                            precision: 0,
                            font: {
                                family: 'Outfit, Inter, system-ui, sans-serif',
                                size: 11,
                                weight: '600'
                            }
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: {
                            color: palette.axis,
                            usePointStyle: true,
                            pointStyle: 'circle',
                            boxWidth: 8,
                            boxHeight: 8,
                            padding: 18,
                            font: {
                                family: 'Outfit, Inter, system-ui, sans-serif',
                                size: 12,
                                weight: '600'
                            }
                        }
                    },
                    tooltip: {
                        enabled: true,
                        backgroundColor: palette.tooltipBg,
                        titleColor: palette.tooltipText,
                        bodyColor: palette.tooltipText,
                        borderColor: 'rgba(255, 255, 255, 0.14)',
                        borderWidth: 1,
                        cornerRadius: 14,
                        padding: 12,
                        displayColors: false,
                        titleFont: {
                            family: 'Outfit, Inter, system-ui, sans-serif',
                            weight: '700',
                            size: 13
                        },
                        bodyFont: {
                            family: 'Outfit, Inter, system-ui, sans-serif',
                            weight: '600',
                            size: 12
                        },
                        callbacks: {
                            label: function (context) {
                                var value = context.parsed.y != null ? context.parsed.y : context.parsed;
                                if (kind === 'line' && context.datasetIndex === 0 && context.dataset.label === 'Attendance %') {
                                    return 'Attendance: ' + formatPercent(value);
                                }
                                return context.dataset.label + ': ' + formatCompactNumber(value);
                            }
                        }
                    },
                    valueLabels: {
                        enabled: false,
                        datasetIndexes: [0]
                    },
                    glow: {
                        color: 'rgba(59, 130, 246, 0.18)',
                        blur: 14
                    }
                }
            };

            return mergeChartOptions(config, overrides || {});
        }

        function mergeChartOptions(base, overrides) {
            var merged = {};

            function assign(target, source) {
                Object.keys(source || {}).forEach(function (key) {
                    var value = source[key];
                    if (value && typeof value === 'object' && !Array.isArray(value) && !(value instanceof Function)) {
                        if (!target[key] || typeof target[key] !== 'object') {
                            target[key] = {};
                        }
                        assign(target[key], value);
                    } else {
                        target[key] = value;
                    }
                });
            }

            assign(merged, base || {});
            assign(merged, overrides || {});
            return merged;
        }

        function setChartEmptyState(canvasId, show, message) {
            var canvas = document.getElementById(canvasId);
            var empty = document.getElementById(canvasId + 'Empty');

            if (canvas) {
                canvas.style.display = show ? 'none' : 'block';
            }

            if (empty) {
                if (message) {
                    empty.textContent = message;
                }
                empty.hidden = !show;
            }
        }

        function updateChartInsights(analytics) {
            setInsight('studentsGrowthInsight', summarizeGrowth(analytics.studentsGrowth || []));
            setInsight('attendanceInsight', summarizeAttendance(analytics.attendanceTrend || []));
            setInsight('classesInsight', summarizeClasses(analytics.classesPerDay || []));
        }

        function summarizeGrowth(rows) {
            if (!rows || rows.length === 0) {
                return 'Tracking student and teacher momentum across recent periods.';
            }

            var first = rows[0] || {};
            var last = rows[rows.length - 1] || {};
            var studentChange = percentDelta(first.students, last.students);
            var teacherChange = percentDelta(first.teachers, last.teachers);

            return 'Students ' + studentChange + ' over the window, while teachers are ' + teacherChange + '.';
        }

        function summarizeAttendance(rows) {
            if (!rows || rows.length === 0) {
                return 'Attendance is ready for live trend rendering once new data arrives.';
            }

            var values = rows.map(function (row) { return Number(row.value || 0); });
            var first = values[0] || 0;
            var last = values[values.length - 1] || 0;
            var average = averageValue(rows, 'value');

            return 'Average attendance sits at ' + formatPercent(average) + ' with a ' + percentDelta(first, last) + ' swing since the start.';
        }

        function summarizeClasses(rows) {
            if (!rows || rows.length === 0) {
                return 'Classes per day will appear as rounded throughput bars.';
            }

            var peak = rows.reduce(function (best, row) {
                return Number(row.value || 0) > Number(best.value || 0) ? row : best;
            }, rows[0]);

            return 'Peak throughput hit ' + peak.label + ' with ' + formatCompactNumber(peak.value) + ' classes.';
        }

        function setInsight(id, text) {
            var node = document.getElementById(id);
            if (node) {
                node.textContent = text;
            }
        }

        function averageValue(rows, key) {
            if (!rows || rows.length === 0) {
                return 0;
            }

            var total = rows.reduce(function (sum, row) {
                return sum + Number(row[key] || 0);
            }, 0);

            return total / rows.length;
        }

        function percentDelta(start, end) {
            var initial = Number(start || 0);
            var current = Number(end || 0);
            if (initial === 0) {
                return current === 0 ? 'flat' : 'up ' + formatCompactNumber(current);
            }

            var change = ((current - initial) / initial) * 100;
            var rounded = Math.round(change);
            return (rounded >= 0 ? '+' : '') + rounded + '%';
        }

        function formatPercent(value) {
            return Math.round(Number(value || 0)) + '%';
        }

        function formatCompactNumber(value) {
            var number = Number(value || 0);
            if (!isFinite(number)) {
                return '0';
            }
            if (Math.abs(number) >= 1000) {
                return Math.round(number / 1000) + 'k';
            }
            return String(Math.round(number * 10) / 10);
        }

        function chartPalette() {
            var root = getComputedStyle(document.documentElement);
            return {
                axis: root.getPropertyValue('--chart-axis').trim() || '#475569',
                grid: root.getPropertyValue('--chart-grid').trim() || 'rgba(15, 23, 42, 0.12)',
                tooltipBg: root.getPropertyValue('--chart-tooltip-bg').trim() || 'rgba(15, 23, 42, 0.85)',
                tooltipText: root.getPropertyValue('--chart-tooltip-text').trim() || '#0f172a'
            };
        }

        function createHorizontalGradient(chart, stops) {
            if (!chart || !chart.chartArea) {
                return stops[0].color;
            }

            var ctx = chart.ctx;
            var gradient = ctx.createLinearGradient(chart.chartArea.left, 0, chart.chartArea.right, 0);
            stops.forEach(function (stop) {
                gradient.addColorStop(stop.stop, stop.color);
            });
            return gradient;
        }

        function createVerticalGradient(chart, stops) {
            if (!chart || !chart.chartArea) {
                return stops[0].color;
            }

            var ctx = chart.ctx;
            var gradient = ctx.createLinearGradient(0, chart.chartArea.top, 0, chart.chartArea.bottom);
            stops.forEach(function (stop) {
                gradient.addColorStop(stop.stop, stop.color);
            });
            return gradient;
        }

        var glowPlugin = {
            id: 'glow',
            beforeDatasetsDraw: function (chart, _args, options) {
                var ctx = chart.ctx;
                ctx.save();
                ctx.shadowBlur = options && options.blur ? options.blur : 14;
                ctx.shadowColor = options && options.color ? options.color : 'rgba(59, 130, 246, 0.18)';
            },
            afterDatasetsDraw: function (chart) {
                chart.ctx.restore();
            }
        };

        var valueLabelsPlugin = {
            id: 'valueLabels',
            afterDatasetsDraw: function (chart, _args, options) {
                var pluginOptions = chart.options.plugins && chart.options.plugins.valueLabels;
                if (!pluginOptions || pluginOptions.enabled === false) {
                    return;
                }

                var datasetIndexes = pluginOptions.datasetIndexes || [];
                var ctx = chart.ctx;

                chart.data.datasets.forEach(function (dataset, datasetIndex) {
                    if (datasetIndexes.indexOf(datasetIndex) === -1) {
                        return;
                    }

                    var meta = chart.getDatasetMeta(datasetIndex);
                    if (!meta || meta.hidden) {
                        return;
                    }

                    ctx.save();
                    ctx.font = pluginOptions.font || '600 11px Outfit, Inter, system-ui, sans-serif';
                    ctx.fillStyle = pluginOptions.color || chartPalette().axis;
                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';

                    meta.data.forEach(function (element, index) {
                        var raw = dataset.data[index];
                        if (raw === null || raw === undefined || raw === '') {
                            return;
                        }

                        var label = pluginOptions.formatter ? pluginOptions.formatter(raw, dataset, index) : String(raw);
                        var position = element.tooltipPosition ? element.tooltipPosition() : { x: element.x, y: element.y };
                        var width = ctx.measureText(label).width + 14;
                        var height = 20;
                        var x = position.x - width / 2;
                        var y = meta.type === 'bar' ? element.y - height - 10 : position.y - height - 14;

                        drawRoundedRect(ctx, x, y, width, height, 999, pluginOptions.backgroundColor || 'rgba(15, 23, 42, 0.72)');
                        ctx.fillText(label, position.x, y + height / 2 + 0.5);
                    });

                    ctx.restore();
                });
            }
        };

        if (window.Chart && window.Chart.register) {
            window.Chart.register(glowPlugin, valueLabelsPlugin);
        }

        function drawRoundedRect(ctx, x, y, width, height, radius, fillStyle) {
            var r = Math.min(radius, width / 2, height / 2);
            ctx.beginPath();
            ctx.moveTo(x + r, y);
            ctx.lineTo(x + width - r, y);
            ctx.quadraticCurveTo(x + width, y, x + width, y + r);
            ctx.lineTo(x + width, y + height - r);
            ctx.quadraticCurveTo(x + width, y + height, x + width - r, y + height);
            ctx.lineTo(x + r, y + height);
            ctx.quadraticCurveTo(x, y + height, x, y + height - r);
            ctx.lineTo(x, y + r);
            ctx.quadraticCurveTo(x, y, x + r, y);
            ctx.closePath();
            ctx.fillStyle = fillStyle;
            ctx.fill();
        }

        function clearCharts() {
            destroyChart(state, 'growth');
            destroyChart(state, 'attendance');
            destroyChart(state, 'classes');
            setChartEmptyState('studentsGrowthChart', true, 'Analytics unavailable');
            setChartEmptyState('attendanceTrendChart', true, 'Analytics unavailable');
            setChartEmptyState('classesPerDayChart', true, 'Analytics unavailable');
        }

        function destroyChart(store, name) {
            if (store.charts[name]) {
                store.charts[name].destroy();
                store.charts[name] = null;
            }
        }

        function getChartCanvas(id) {
            var canvas = document.getElementById(id);
            if (!canvas) {
                return null;
            }
            return canvas.getContext('2d');
        }

        function icon(name) {
            var map = {
                students: '👨‍🎓',
                teachers: '👩‍🏫',
                classes: '🧾',
                attendance: '✅',
                system: '🛡',
                alerts: '⚠'
            };
            return map[name] || '•';
        }

        function statusClass(status) {
            var normalized = String(status || '').toLowerCase();
            if (normalized.indexOf('critical') >= 0) {
                return 'critical';
            }
            if (normalized.indexOf('warn') >= 0 || normalized.indexOf('attention') >= 0) {
                return 'warning';
            }
            return 'healthy';
        }

        function esc(value) {
            return String(value == null ? '' : value)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        function safe(value) {
            if (value === null || value === undefined || value === '') {
                return '-';
            }
            return String(value);
        }
    }

    boot();
})();
