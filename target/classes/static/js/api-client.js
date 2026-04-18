(function () {
    if (window.smsApi) {
        return;
    }

    function decodeJwtPayload(token) {
        if (!token || token.split('.').length !== 3) {
            return null;
        }

        try {
            var payloadSegment = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
            var padded = payloadSegment + '='.repeat((4 - (payloadSegment.length % 4)) % 4);
            return JSON.parse(atob(padded));
        } catch (_error) {
            return null;
        }
    }

    function clearAuthTokens() {
        ['token', 'authToken', 'jwt', 'accessToken', 'role', 'userRole'].forEach(function (key) {
            localStorage.removeItem(key);
        });
    }

    window.smsClearAuthTokens = clearAuthTokens;

    function isTokenExpired(token) {
        var payload = decodeJwtPayload(token);
        if (!payload || !payload.exp) {
            return false;
        }

        var nowSeconds = Math.floor(Date.now() / 1000);
        return nowSeconds >= Number(payload.exp);
    }

    function readAuthToken() {
        const direct = localStorage.getItem('token') || localStorage.getItem('authToken');
        if (direct) {
            if (isTokenExpired(direct)) {
                clearAuthTokens();
                return '';
            }
            return direct;
        }

        const prefixed = localStorage.getItem('jwt') || localStorage.getItem('accessToken');
        if (prefixed && isTokenExpired(prefixed)) {
            clearAuthTokens();
            return '';
        }
        return prefixed || '';
    }

    async function parseResponse(response) {
        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return response.json();
        }

        const text = await response.text();
        return text;
    }

    function normalizeErrorMessage(payload, fallback) {
        if (!payload) {
            return fallback;
        }

        if (typeof payload === 'string' && payload.trim()) {
            return payload;
        }

        if (typeof payload.message === 'string' && payload.message.trim()) {
            return payload.message;
        }

        if (typeof payload.error === 'string' && payload.error.trim()) {
            return payload.error;
        }

        return fallback;
    }

    async function request(url, options) {
        const requestOptions = options || {};
        const headers = { Accept: 'application/json', ...(requestOptions.headers || {}) };
        const isFormData = typeof FormData !== 'undefined' && requestOptions.body instanceof FormData;
        const token = readAuthToken();

        if (!headers.Authorization && token) {
            headers.Authorization = 'Bearer ' + token;
        }

        if (headers['Content-Type'] === null) {
            delete headers['Content-Type'];
        }

        if (requestOptions.body !== undefined && requestOptions.body !== null && !isFormData && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }

        const response = await fetch(url, { ...requestOptions, headers });
        const payload = await parseResponse(response);

        if (!response.ok) {
            const message = normalizeErrorMessage(payload, 'Request failed (' + response.status + ')');
            const error = new Error(message);
            error.status = response.status;
            error.payload = payload;

            if (response.status === 401) {
                clearAuthTokens();
                window.dispatchEvent(new CustomEvent('sms:auth-expired'));
            }

            throw error;
        }

        return payload;
    }

    function withQuery(path, queryParams) {
        const params = new URLSearchParams();
        Object.entries(queryParams || {}).forEach(([key, value]) => {
            if (value === undefined || value === null || value === '') {
                return;
            }
            params.set(key, String(value));
        });

        const query = params.toString();
        return query ? path + '?' + query : path;
    }

    const api = {
        request,
        get: (url, headers) => request(url, { method: 'GET', headers: headers || {} }),
        post: (url, body, headers) => request(url, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined, headers: headers || {} }),
        put: (url, body, headers) => request(url, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined, headers: headers || {} }),
        delete: (url, headers) => request(url, { method: 'DELETE', headers: headers || {} }),

        student: {
            dashboard: () => request('/api/student/dashboard'),
            completeTask: (taskId) => request('/api/student/task/' + encodeURIComponent(taskId) + '/complete', { method: 'POST' }),
            timetable: () => request('/api/student/timetable'),
            timetableUpdate: (sessionId, payload) => request('/api/student/timetable/session/' + encodeURIComponent(sessionId), { method: 'PATCH', body: JSON.stringify(payload) }),
            dietSuggestion: () => request('/api/student/diet/suggestion'),
            dietLogBatch: (payload) => request('/api/student/diet/log-batch', { method: 'POST', body: JSON.stringify(payload) }),
            dietExportDataset: () => request('/api/student/diet/export-dataset', { method: 'POST' }),
            dietOptimize: () => request('/api/student/diet/optimize'),
            attendanceMark: (payload, tokenOverride) => {
                const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                return request('/api/student/attendance/mark', { method: 'POST', headers, body: JSON.stringify(payload) });
            },
            attendanceCheckToday: (subjectId, tokenOverride) => {
                const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                return request('/api/student/attendance/check-today?subjectId=' + encodeURIComponent(subjectId), { method: 'GET', headers });
            },
            attendanceGeofenceCheck: (latitude, longitude) => request('/api/student/attendance/geofence/check', { method: 'POST', body: JSON.stringify({ latitude, longitude }) }),
            attendanceMetrics: (subjectId) => request('/api/student/attendance/metrics?subjectId=' + encodeURIComponent(subjectId)),
            idCard: () => request('/api/student/id/card'),
            profile: {
                get: (path) => request(path || '/api/student/profile'),
                update: (path, payload) => request(path || '/api/student/profile', { method: 'PUT', body: JSON.stringify(payload) })
            }
        },

        analytics: {
            summary: (queryString) => request('/api/analytics/summary' + (queryString ? '?' + queryString : '')),
            studentSummary: (studentId) => request('/api/analytics/student-summary/' + encodeURIComponent(studentId)),
            live: () => request('/api/analytics/live'),
            sendDigest: () => request('/api/analytics/reports/digest', { method: 'POST' })
        },

        admin: {
            dashboard: {
                summary: () => request('/api/admin/dashboard/summary'),
                databaseHealth: () => request('/api/admin/dashboard/database-health'),
                analytics: () => request('/api/admin/dashboard/analytics'),
                alerts: () => request('/api/admin/dashboard/alerts'),
                recentActivity: (limit) => request('/api/admin/dashboard/recent-activity?limit=' + encodeURIComponent(limit || 12))
            },
            students: {
                list: (queryParams) => request(withQuery('/api/admin/students', queryParams)),
                create: (payload) => request('/api/admin/students', { method: 'POST', body: JSON.stringify(payload) }),
                remove: (id) => request('/api/admin/students/' + encodeURIComponent(id), { method: 'DELETE' }),
                bulkDelete: (ids) => request('/api/admin/students/bulk-delete', { method: 'POST', body: JSON.stringify({ ids }) }),
                activity: (limit) => request('/api/admin/students/activity?limit=' + encodeURIComponent(limit || 8)),
                uploadFace: (studentId, file, options) => {
                    var formData = new FormData();
                    formData.append('studentId', studentId);
                    formData.append('file', file);

                    if (options && options.tenantId) {
                        formData.append('tenantId', String(options.tenantId));
                    }
                    if (options && options.livenessPrompt) {
                        formData.append('livenessPrompt', options.livenessPrompt);
                    }
                    if (options && options.livenessVerified !== undefined) {
                        formData.append('livenessVerified', String(Boolean(options.livenessVerified)));
                    }

                    return request('/api/admin/upload-face', {
                        method: 'POST',
                        body: formData
                    });
                }
            }
        },

        teacher: {
            attendance: {
                sessionStats: (subjectId) => request('/api/teacher/attendance/session-stats?subjectId=' + encodeURIComponent(subjectId)),
                subjectStudents: (subjectId, tokenOverride) => {
                    const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                    return request('/api/teacher/attendance/subject/' + encodeURIComponent(subjectId) + '/students', { headers });
                },
                subjects: (tokenOverride) => {
                    const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                    return request('/api/teacher/attendance/subjects', { headers });
                },
                generateQr: (payload, tokenOverride) => {
                    const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                    return request('/api/teacher/attendance/generate-qr', { method: 'POST', headers, body: JSON.stringify(payload) });
                },
                records: (subjectId, tokenOverride) => {
                    const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                    return request('/api/teacher/attendance/records?subjectId=' + encodeURIComponent(subjectId), { headers });
                },
                manual: (payload, tokenOverride) => {
                    const headers = tokenOverride ? { Authorization: 'Bearer ' + tokenOverride } : {};
                    return request('/api/teacher/attendance/manual', { method: 'POST', headers, body: JSON.stringify(payload) });
                }
            },
            fraud: {
                summary: () => request('/api/teacher/fraud/summary')
            }
        },

        campus: {
            liveMap: (queryParams) => request(withQuery('/api/campus/live-map', queryParams)),
            summary: (queryParams) => request(withQuery('/api/campus/summary', queryParams))
        }
    };

    window.smsApi = api;
})();
