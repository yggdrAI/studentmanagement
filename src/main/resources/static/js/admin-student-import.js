(function () {
    var api = window.smsApi;
    if (!api || !api.request) {
        return;
    }

    var REQUIRED_FIELDS = ['fullName', 'enrollmentNumber'];
    var FIELD_ORDER = [
        'fullName',
        'enrollmentNumber',
        'email',
        'phone',
        'course',
        'semester',
        'department',
        'section',
        'dateOfBirth',
        'gender',
        'address',
        'bloodGroup',
        'guardianName'
    ];

    var state = {
        jobId: null,
        rows: [],
        mergedStudents: [],
        suggestions: [],
        sourceFiles: [],
        logs: [],
        search: '',
        files: [],
        errorReportUrl: null,
        uploading: false,
        availableHeaders: [],
        fieldLabels: {},
        columnMapping: {},
        parseWarnings: [],
        missingRequiredFields: [],
        pendingSaves: {},
        updateTimers: {}
    };

    var refs = {
        dropZone: document.getElementById('dropZone'),
        chooseFileBtn: document.getElementById('chooseFileBtn'),
        fileInput: document.getElementById('importFileInput'),
        uploadBtn: document.getElementById('uploadBtn'),
        uploadProgressBar: document.getElementById('uploadProgressBar'),
        uploadProgressLabel: document.getElementById('uploadProgressLabel'),
        uploadFileName: document.getElementById('uploadFileName'),
        selectedFilesList: document.getElementById('selectedFilesList'),
        duplicateStrategy: document.getElementById('duplicateStrategy'),
        rollbackOnFailure: document.getElementById('rollbackOnFailure'),
        totalRowsCount: document.getElementById('totalRowsCount'),
        validRowsCount: document.getElementById('validRowsCount'),
        invalidRowsCount: document.getElementById('invalidRowsCount'),
        fusedStudentsCount: document.getElementById('fusedStudentsCount'),
        jobStatus: document.getElementById('jobStatus'),
        mergePanel: document.getElementById('mergePanel'),
        mergeBody: document.getElementById('mergeBody'),
        suggestionsPanel: document.getElementById('suggestionsPanel'),
        suggestionsList: document.getElementById('suggestionsList'),
        previewBody: document.getElementById('previewBody'),
        confirmImportBtn: document.getElementById('confirmImportBtn'),
        rollbackLastBtn: document.getElementById('rollbackLastBtn'),
        downloadErrorsBtn: document.getElementById('downloadErrorsBtn'),
        previewSearch: document.getElementById('previewSearch'),
        logsList: document.getElementById('logsList'),
        mappingPanel: document.getElementById('mappingPanel'),
        mappingGrid: document.getElementById('mappingGrid'),
        reapplyMappingBtn: document.getElementById('reapplyMappingBtn'),
        parseWarnings: document.getElementById('parseWarnings')
    };

    bindEvents();
    loadLogs();
    renderPreview();
    renderSummary();

    function bindEvents() {
        refs.chooseFileBtn.addEventListener('click', function () {
            refs.fileInput.click();
        });

        refs.fileInput.addEventListener('change', function () {
            var files = Array.prototype.slice.call(refs.fileInput.files || []);
            if (!files.length) {
                return;
            }
            setFiles(files);
        });

        refs.dropZone.addEventListener('dragover', function (event) {
            event.preventDefault();
            refs.dropZone.classList.add('dragover');
        });

        refs.dropZone.addEventListener('dragleave', function () {
            refs.dropZone.classList.remove('dragover');
        });

        refs.dropZone.addEventListener('drop', function (event) {
            event.preventDefault();
            refs.dropZone.classList.remove('dragover');
            var files = Array.prototype.slice.call(event.dataTransfer.files || []);
            if (files.length) {
                setFiles(files);
            }
        });

        refs.uploadBtn.addEventListener('click', uploadAndPreview);
        refs.confirmImportBtn.addEventListener('click', confirmImport);
        refs.rollbackLastBtn.addEventListener('click', rollbackLastImport);
        refs.reapplyMappingBtn.addEventListener('click', reapplyMapping);
        refs.previewSearch.addEventListener('input', function () {
            state.search = refs.previewSearch.value.trim().toLowerCase();
            renderPreview();
        });
    }

    function setFiles(files) {
        state.files = files.slice(0);
        refs.uploadFileName.textContent = files.length + ' file(s) selected';
        refs.jobStatus.textContent = files.length + ' file(s) ready';
        refs.selectedFilesList.innerHTML = '<ul>' + files.map(function (file) {
            return '<li>' + escapeHtml(file.name) + ' (' + Math.round(file.size / 1024) + ' KB)</li>';
        }).join('') + '</ul>';
    }

    function uploadAndPreview() {
        if (!state.files.length || state.uploading) {
            toast('Select one or more CSV or XLSX files first', 'error');
            return;
        }

        uploadFileWithMapping(false);
    }

    function uploadFileWithMapping(useManualMapping) {
        var formData = new FormData();
        state.files.forEach(function (file) {
            formData.append('files', file);
        });
        formData.append('duplicateStrategy', refs.duplicateStrategy.value);
        formData.append('rollbackOnFailure', refs.rollbackOnFailure.value);

        if (useManualMapping) {
            formData.append('mappingJson', JSON.stringify(state.columnMapping || {}));
        }

        state.uploading = true;
        refs.uploadBtn.disabled = true;
        refs.reapplyMappingBtn.disabled = true;
        refs.jobStatus.textContent = 'Uploading...';
        setProgress(5);

        var xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/admin/import/students', true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.responseType = 'json';

        xhr.upload.onprogress = function (event) {
            if (!event.lengthComputable) {
                return;
            }
            var percent = Math.round((event.loaded / event.total) * 100);
            setProgress(percent);
        };

        xhr.onload = function () {
            state.uploading = false;
            refs.uploadBtn.disabled = false;
            refs.reapplyMappingBtn.disabled = false;
            if (xhr.status >= 200 && xhr.status < 300) {
                applyPreviewPayload(xhr.response || {});
                setProgress(100);
                refs.jobStatus.textContent = 'Preview ready';
                toast('File validated successfully', 'success');
            } else {
                refs.jobStatus.textContent = 'Upload failed';
                toast(errorMessage(xhr.response) || 'Upload failed', 'error');
                setProgress(0);
            }
        };

        xhr.onerror = function () {
            state.uploading = false;
            refs.uploadBtn.disabled = false;
            refs.reapplyMappingBtn.disabled = false;
            refs.jobStatus.textContent = 'Upload failed';
            toast('Upload failed', 'error');
            setProgress(0);
        };

        xhr.send(formData);
    }

    function reapplyMapping() {
        if (!state.files.length) {
            toast('Select a file before applying mapping', 'error');
            return;
        }
        uploadFileWithMapping(true);
    }

    function applyPreviewPayload(payload) {
        state.jobId = payload.jobId || null;
        state.rows = (payload.rows || []).map(normalizeRow);
        state.mergedStudents = payload.mergedStudents || [];
        state.suggestions = payload.smartSuggestions || [];
        state.sourceFiles = payload.sourceFiles || [];
        state.errorReportUrl = payload.errorReport || null;
        state.availableHeaders = payload.availableHeaders && payload.availableHeaders.length ? payload.availableHeaders : state.availableHeaders;
        state.fieldLabels = payload.fieldLabels && Object.keys(payload.fieldLabels).length ? payload.fieldLabels : state.fieldLabels;
        state.columnMapping = payload.columnMapping && Object.keys(payload.columnMapping).length ? payload.columnMapping : state.columnMapping;
        state.parseWarnings = payload.parseWarnings || [];
        state.missingRequiredFields = payload.missingRequiredFields || [];

        renderSummary(payload);
        renderMergePreview();
        renderSuggestions();
        renderMappingPanel();
        renderPreview();
        renderLogsBadge(payload);
        refs.confirmImportBtn.disabled = !state.jobId;
        refs.mergePanel.hidden = !state.mergedStudents.length;
        refs.suggestionsPanel.hidden = !state.suggestions.length;
        if (state.errorReportUrl) {
            refs.downloadErrorsBtn.href = state.errorReportUrl;
            refs.downloadErrorsBtn.hidden = false;
        } else {
            refs.downloadErrorsBtn.hidden = true;
        }
    }

    function normalizeRow(row) {
        return {
            id: row.id,
            rowIndex: row.rowIndex,
            fullName: row.fullName || '',
            enrollmentNumber: row.enrollmentNumber || '',
            email: row.email || '',
            phone: row.phone || '',
            course: row.course || '',
            semester: row.semester || '',
            department: row.department || '',
            section: row.section || '',
            className: row.className || '',
            house: row.house || '',
            joiningYear: row.joiningYear || '',
            leavingYear: row.leavingYear || '',
            rollNumber: row.rollNumber || '',
            program: row.program || '',
            school: row.school || '',
            sourceFileName: row.sourceFileName || '',
            confidenceScore: row.confidenceScore || 0,
            dateOfBirth: row.dateOfBirth || '',
            gender: row.gender || '',
            address: row.address || '',
            bloodGroup: row.bloodGroup || '',
            guardianName: row.guardianName || '',
            classGroup: row.classGroup || '',
            batchGroup: row.batchGroup || '',
            status: row.status || 'PENDING',
            errorMessage: row.errorMessage || ''
        };
    }

    function renderMappingPanel() {
        if (!state.availableHeaders.length) {
            refs.mappingPanel.hidden = true;
            return;
        }

        refs.mappingPanel.hidden = false;
        refs.mappingGrid.innerHTML = FIELD_ORDER.map(function (field) {
            var label = state.fieldLabels[field] || field;
            var selected = state.columnMapping[field] || '';
            var missing = REQUIRED_FIELDS.indexOf(field) >= 0 && !selected;

            return '<div class="mapping-item ' + (REQUIRED_FIELDS.indexOf(field) >= 0 ? 'required' : '') + ' ' + (missing ? 'missing' : '') + '" data-field="' + field + '">' +
                '<label for="mapping-' + field + '">' + escapeHtml(label) + '</label>' +
                '<select id="mapping-' + field + '" data-mapping-field="' + field + '">' +
                '<option value="">-- Not mapped --</option>' +
                state.availableHeaders.map(function (header) {
                    var isSelected = selected && header.toLowerCase() === selected.toLowerCase();
                    return '<option value="' + escapeAttr(header) + '" ' + (isSelected ? 'selected' : '') + '>' + escapeHtml(header) + '</option>';
                }).join('') +
                '</select>' +
                '</div>';
        }).join('');

        Array.from(refs.mappingGrid.querySelectorAll('select[data-mapping-field]')).forEach(function (select) {
            select.addEventListener('change', function () {
                var field = select.getAttribute('data-mapping-field');
                var value = select.value.trim();
                if (value) {
                    state.columnMapping[field] = value;
                } else {
                    delete state.columnMapping[field];
                }
                renderMappingPanel();
            });
        });

        if (state.parseWarnings.length) {
            refs.parseWarnings.hidden = false;
            refs.parseWarnings.innerHTML = state.parseWarnings.map(function (warning) {
                return '<div>' + escapeHtml(warning) + '</div>';
            }).join('');
        } else {
            refs.parseWarnings.hidden = true;
            refs.parseWarnings.innerHTML = '';
        }
    }

    function renderSummary(payload) {
        var totalRows = payload && typeof payload.totalRows !== 'undefined' ? payload.totalRows : state.rows.length;
        var validRows = payload && typeof payload.validRows !== 'undefined' ? payload.validRows : state.rows.filter(isValidRow).length;
        var invalidRows = payload && typeof payload.invalidRows !== 'undefined' ? payload.invalidRows : state.rows.length - validRows;
        var fusedStudents = payload && typeof payload.fusedStudentCount !== 'undefined' ? payload.fusedStudentCount : state.mergedStudents.length;
        refs.totalRowsCount.textContent = String(totalRows || 0);
        refs.validRowsCount.textContent = String(validRows || 0);
        refs.invalidRowsCount.textContent = String(invalidRows || 0);
        refs.fusedStudentsCount.textContent = String(fusedStudents || 0);
        if (!state.jobId) {
            refs.jobStatus.textContent = 'Idle';
        }
    }

    function renderMergePreview() {
        if (!state.mergedStudents.length) {
            refs.mergeBody.innerHTML = '<tr><td colspan="11" class="empty-state">No merged students to display.</td></tr>';
            return;
        }

        refs.mergeBody.innerHTML = state.mergedStudents.map(function (student) {
            var sourceFiles = (student.sources || []).map(escapeHtml).join('<br>') || '-';
            var conflicts = (student.conflicts || []).map(function (conflict) {
                return '<div><strong>' + escapeHtml(conflict.field || '-') + ':</strong> ' + escapeHtml((conflict.values || []).join(', ')) + '</div>';
            }).join('') || '-';
            return '<tr>' +
                '<td><strong>' + escapeHtml(student.confidenceScore || 0) + '%</strong></td>' +
                '<td>' + escapeHtml(student.fullName || [student.firstName, student.middleName, student.lastName].filter(Boolean).join(' ')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.enrollmentNumber, student.rollNumber, student.identityKey, '-')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.program, student.course, '-')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.department, '-')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.school, '-')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.joiningYear, student.leavingYear, '-')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.className, student.section, '-')) + '</td>' +
                '<td>' + escapeHtml(firstNonBlank(student.house, '-')) + '</td>' +
                '<td>' + sourceFiles + '</td>' +
                '<td>' + conflicts + '</td>' +
                '</tr>';
        }).join('');
    }

    function renderSuggestions() {
        if (!state.suggestions.length) {
            refs.suggestionsList.innerHTML = '<div class="suggestion-item">No merge suggestions right now.</div>';
            return;
        }

        refs.suggestionsList.innerHTML = state.suggestions.map(function (suggestion) {
            return '<article class="suggestion-item">' +
                '<div class="suggestion-head">' +
                '<strong>' + escapeHtml(suggestion.type || 'suggestion') + '</strong>' +
                '<span>' + escapeHtml((suggestion.confidence || suggestion.confidenceScore || 0)) + '%</span>' +
                '</div>' +
                '<div>' + escapeHtml(suggestion.message || '') + '</div>' +
                '</article>';
        }).join('');
    }

    function renderPreview() {
        var filtered = state.rows.filter(function (row) {
            if (!state.search) {
                return true;
            }
            var haystack = [row.fullName, row.enrollmentNumber, row.email, row.course, row.department].join(' ').toLowerCase();
            return haystack.indexOf(state.search) >= 0;
        });

        if (!filtered.length) {
            refs.previewBody.innerHTML = '<tr><td colspan="17" class="empty-state">No preview rows to display.</td></tr>';
            return;
        }

        refs.previewBody.innerHTML = filtered.map(function (row) {
            var rowClass = isValidRow(row) ? 'row-valid' : 'row-invalid';
            return '<tr class="' + rowClass + '" data-row-id="' + row.id + '">' +
                cell(row.rowIndex) +
                editableCell(row, 'fullName') +
                editableCell(row, 'enrollmentNumber') +
                editableCell(row, 'email') +
                editableCell(row, 'phone') +
                editableCell(row, 'course') +
                editableCell(row, 'semester') +
                editableCell(row, 'department') +
                editableCell(row, 'section') +
                editableCell(row, 'dateOfBirth') +
                editableCell(row, 'gender') +
                editableCell(row, 'address') +
                cell(escapeHtml(row.classGroup || '-')) +
                cell(escapeHtml(row.batchGroup || '-')) +
                cell(statusBadge(row)) +
                cell('<div class="error-text">' + escapeHtml(row.errorMessage || '') + '</div>') +
                cell('<div class="row-actions"><button type="button" class="btn-remove-row" data-remove-row="' + row.id + '">Remove</button></div>') +
                '</tr>';
        }).join('');

        Array.from(refs.previewBody.querySelectorAll('.cell-input')).forEach(function (input) {
            input.addEventListener('change', function () {
                var rowId = Number(input.closest('tr').getAttribute('data-row-id'));
                updateRowLocally(rowId, input.name, input.value);
            });
        });

        Array.from(refs.previewBody.querySelectorAll('button[data-remove-row]')).forEach(function (button) {
            button.addEventListener('click', function () {
                var rowId = Number(button.getAttribute('data-remove-row'));
                removeRow(rowId);
            });
        });
    }

    function editableCell(row, field) {
        return '<td><input class="cell-input' + (row.errorMessage ? ' invalid' : '') + '" data-field="' + field + '" name="' + field + '" value="' + escapeAttr(row[field] || '') + '" /></td>';
    }

    function cell(content) {
        return '<td>' + content + '</td>';
    }

    function statusBadge(row) {
        if (row.status === 'VALID') {
            return '<span style="color:#166534;font-weight:700;">Valid</span>';
        }
        if (row.status === 'IMPORTED') {
            return '<span style="color:#0f766e;font-weight:700;">Imported</span>';
        }
        if (row.status === 'SKIPPED') {
            return '<span style="color:#92400e;font-weight:700;">Skipped</span>';
        }
        return '<span style="color:#b91c1c;font-weight:700;">Invalid</span>';
    }

    function updateRowLocally(rowId, field, value) {
        var row = state.rows.find(function (item) { return item.id === rowId; });
        if (!row) {
            return;
        }
        row[field] = value || '';
        row.errorMessage = validateLocalRow(row);
        row.status = row.errorMessage ? 'INVALID' : 'VALID';
        renderSummary();
        renderPreview();

        if (state.updateTimers[rowId]) {
            clearTimeout(state.updateTimers[rowId]);
        }

        state.updateTimers[rowId] = window.setTimeout(function () {
            saveRowToServer(rowId);
        }, 300);
    }

    function saveRowToServer(rowId) {
        var row = state.rows.find(function (item) { return item.id === rowId; });
        if (!row || !state.jobId || state.pendingSaves[rowId]) {
            return;
        }

        state.pendingSaves[rowId] = true;
        api.request('/api/admin/import/rows/' + encodeURIComponent(rowId) + '?jobId=' + encodeURIComponent(state.jobId), {
            method: 'PUT',
            body: JSON.stringify({
                fullName: row.fullName,
                enrollmentNumber: row.enrollmentNumber,
                email: row.email,
                phone: row.phone,
                course: row.course,
                semester: row.semester,
                department: row.department,
                section: row.section,
                dateOfBirth: row.dateOfBirth,
                gender: row.gender,
                address: row.address,
                bloodGroup: row.bloodGroup,
                guardianName: row.guardianName
            })
        }).then(function (payload) {
            applyPreviewPayload(payload || {});
        }).catch(function (error) {
            toast(error.message || 'Row update failed', 'error');
        }).finally(function () {
            state.pendingSaves[rowId] = false;
        });
    }

    function removeRow(rowId) {
        if (!state.jobId) {
            return;
        }

        api.request('/api/admin/import/rows/' + encodeURIComponent(rowId) + '?jobId=' + encodeURIComponent(state.jobId), {
            method: 'DELETE'
        }).then(function (payload) {
            applyPreviewPayload(payload || {});
            toast('Row removed from preview', 'success');
        }).catch(function (error) {
            toast(error.message || 'Unable to remove row', 'error');
        });
    }

    function validateLocalRow(row) {
        var errors = [];
        if (!row.fullName.trim()) errors.push('Full Name is required');
        if (!row.enrollmentNumber.trim()) errors.push('Enrollment Number is required');
        if (row.email.trim() && !/^\S+@\S+\.\S+$/.test(row.email.trim())) errors.push('Email is invalid');
        if (row.phone.trim() && !/^[0-9]{7,15}$/.test(row.phone.trim().replace(/[^0-9]/g, ''))) errors.push('Phone is invalid');

        var dupCount = state.rows.filter(function (item) {
            return item.enrollmentNumber.trim().toLowerCase() === row.enrollmentNumber.trim().toLowerCase();
        }).length;
        if (dupCount > 1) {
            errors.push('Duplicate enrollment number in file');
        }
        return errors.join('; ');
    }

    function confirmImport() {
        if (!state.jobId) {
            toast('Upload a file first', 'error');
            return;
        }
        if (state.rows.some(function (row) { return row.status === 'INVALID'; })) {
            toast('Fix invalid rows before confirming', 'error');
            return;
        }

        api.request('/api/admin/import/confirm', {
            method: 'POST',
            body: JSON.stringify({
                jobId: state.jobId,
                duplicateStrategy: refs.duplicateStrategy.value,
                rollbackOnFailure: refs.rollbackOnFailure.value === 'true'
            })
        }).then(function (response) {
            refs.jobStatus.textContent = response.status || 'Confirmed';
            toast((response.successCount || 0) + ' students imported', 'success');
            if (response.errorReport) {
                refs.downloadErrorsBtn.href = response.errorReport;
                refs.downloadErrorsBtn.hidden = false;
            }
            loadLogs();
            refs.confirmImportBtn.disabled = true;
        }).catch(function (error) {
            toast(error.message || 'Import confirmation failed', 'error');
        });
    }

    function rollbackLastImport() {
        api.request('/api/admin/import/rollback-last', { method: 'POST' })
            .then(function (response) {
                toast(response.message || 'Last import rolled back', 'success');
                loadLogs();
            })
            .catch(function (error) {
                toast(error.message || 'Rollback failed', 'error');
            });
    }

    function loadLogs() {
        api.request('/api/admin/import/logs')
            .then(function (items) {
                state.logs = items || [];
                renderLogs();
            })
            .catch(function () {
                refs.logsList.innerHTML = '<div class="log-item"><strong>Unable to load logs</strong></div>';
            });
    }

    function renderLogs() {
        if (!state.logs.length) {
            refs.logsList.innerHTML = '<div class="log-item"><strong>No import logs yet.</strong></div>';
            return;
        }

        refs.logsList.innerHTML = state.logs.map(function (log) {
            var errorDownload = log.errorReportPath
                ? '<div class="meta"><a href="' + escapeAttr(log.errorReportPath) + '" target="_blank" rel="noopener">Download error report</a></div>'
                : '';
            return '<div class="log-item">' +
                '<strong>' + escapeHtml(log.fileName || 'Import') + '</strong>' +
                '<div class="meta">' + escapeHtml(log.uploadedBy || '-') + ' | ' + escapeHtml(log.uploadedAt || '-') + '</div>' +
                '<div class="meta">Files: ' + escapeHtml(log.sourceFileCount || 0) + ' | Fused: ' + escapeHtml(log.fusedStudentCount || 0) + ' | Rows: ' + escapeHtml(log.totalRows || 0) + ' | Success: ' + escapeHtml(log.successCount || 0) + ' | Failure: ' + escapeHtml(log.failureCount || 0) + ' | ' + escapeHtml(log.status || '-') + '</div>' +
                errorDownload +
                '</div>';
        }).join('');
    }

    function renderLogsBadge(payload) {
        if (payload && payload.status) {
            refs.jobStatus.textContent = payload.status;
        }
    }

    function isValidRow(row) {
        return row.status === 'VALID' || row.status === 'IMPORTED';
    }

    function setProgress(value) {
        var percent = Math.max(0, Math.min(100, Number(value) || 0));
        refs.uploadProgressBar.style.width = percent + '%';
        refs.uploadProgressLabel.textContent = percent + '%';
    }

    function toast(message, type) {
        window.alert(message);
    }

    function errorMessage(response) {
        if (!response) return '';
        return response.message || response.error || 'Unexpected import error';
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function escapeAttr(value) {
        return escapeHtml(value).replace(/"/g, '&quot;');
    }

    function firstNonBlank() {
        for (var i = 0; i < arguments.length; i++) {
            var value = arguments[i];
            if (value !== null && value !== undefined && String(value).trim()) {
                return value;
            }
        }
        return '';
    }
})();
