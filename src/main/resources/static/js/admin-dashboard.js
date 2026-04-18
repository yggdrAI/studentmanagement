(function () {
    function boot() {
        var api = window.smsApi;
        if (!api || !api.admin || !api.admin.dashboard) {
            window.setTimeout(boot, 50);
            return;
        }

        var state = { charts: {} };

        wireRestoreControls();
        loadSummaryAndHealth();
        window.setTimeout(loadAnalytics, 0);
        window.setTimeout(loadAlerts, 0);
        window.setTimeout(loadRecentActivity, 0);

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
            api.admin.dashboard.analytics().then(function (analytics) {
                renderGrowthChart(analytics.studentsGrowth || []);
                renderAttendanceChart(analytics.attendanceTrend || []);
                renderClassesChart(analytics.classesPerDay || []);
            }).catch(function () {
                clearCharts();
                renderNoDataChart('studentsGrowthChart', 'Analytics unavailable');
                renderNoDataChart('attendanceTrendChart', 'Analytics unavailable');
                renderNoDataChart('classesPerDayChart', 'Analytics unavailable');
            });
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

            destroyChart(state, 'growth');
            state.charts.growth = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: data.map(function (row) { return row.label; }),
                    datasets: [
                        {
                            label: 'Students',
                            data: data.map(function (row) { return row.students; }),
                            borderColor: '#1d4ed8',
                            backgroundColor: 'rgba(29,78,216,0.14)',
                            fill: true,
                            tension: 0.3
                        },
                        {
                            label: 'Teachers',
                            data: data.map(function (row) { return row.teachers; }),
                            borderColor: '#0f766e',
                            backgroundColor: 'rgba(15,118,110,0.1)',
                            fill: true,
                            tension: 0.3
                        }
                    ]
                },
                options: chartOptions()
            });
        }

        function renderAttendanceChart(data) {
            var ctx = getChartCanvas('attendanceTrendChart');
            if (!ctx) {
                return;
            }

            destroyChart(state, 'attendance');
            state.charts.attendance = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: data.map(function (row) { return row.label; }),
                    datasets: [
                        {
                            label: 'Attendance %',
                            data: data.map(function (row) { return row.value; }),
                            backgroundColor: 'rgba(22,163,74,0.45)',
                            borderColor: '#15803d',
                            borderWidth: 1
                        }
                    ]
                },
                options: chartOptions()
            });
        }

        function renderClassesChart(data) {
            var ctx = getChartCanvas('classesPerDayChart');
            if (!ctx) {
                return;
            }

            destroyChart(state, 'classes');
            state.charts.classes = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: data.map(function (row) { return row.label; }),
                    datasets: [
                        {
                            data: data.map(function (row) { return row.value; }),
                            backgroundColor: ['#0ea5e9', '#14b8a6', '#84cc16', '#eab308', '#f97316', '#ef4444']
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    aspectRatio: 2.25,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        }

        function renderNoDataChart(canvasId, label) {
            var canvas = document.getElementById(canvasId);
            if (!canvas) {
                return;
            }

            var ctx = canvas.getContext('2d');
            if (!ctx) {
                return;
            }

            var chartKey = canvasId.replace('Chart', '').replace(/^[a-z]/, function (m) { return m.toLowerCase(); });
            destroyChart(state, chartKey);
            state.charts[chartKey] = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: [label],
                    datasets: [{
                        label: 'No data',
                        data: [0],
                        backgroundColor: 'rgba(148,163,184,0.35)',
                        borderColor: '#94a3b8',
                        borderWidth: 1
                    }]
                },
                options: chartOptions()
            });
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

        function chartOptions() {
            return {
                responsive: true,
                maintainAspectRatio: true,
                aspectRatio: 2.25,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { precision: 0 }
                    }
                },
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            };
        }

        function clearCharts() {
            destroyChart(state, 'growth');
            destroyChart(state, 'attendance');
            destroyChart(state, 'classes');
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
