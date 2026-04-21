(function () {
    "use strict";

    const gridBody = document.getElementById("teacherGridBody");
    if (!gridBody || !window.smsApi || !window.smsApi.admin || !window.smsApi.admin.teachers) {
        return;
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

    async function loadTeachers() {
        try {
            const teachers = await window.smsApi.admin.teachers.list();
            if (!teachers || !teachers.length) {
                gridBody.innerHTML = "<tr><td colspan=\"5\">No teachers found.</td></tr>";
                return;
            }

            gridBody.innerHTML = teachers.map((teacher) => `
                <tr>
                    <td>${escapeHtml(teacher.id)}</td>
                    <td>
                        <div style="display:flex; align-items:center; gap:8px;">
                            <img src="${escapeHtml(teacher.profileImage || teacher.profilePhotoUrl || '/images/default-avatar.png')}" alt="Teacher" style="width:34px;height:34px;border-radius:50%;object-fit:cover;border:1px solid rgba(0,0,0,.12);" onerror="this.src='/images/default-avatar.png'" />
                            <div>
                                <div>${escapeHtml(teacher.name || teacher.fullName || '')}</div>
                                <div style="font-size:12px; opacity:.75;">${escapeHtml([teacher.designation, teacher.department].filter(Boolean).join(' · '))}</div>
                            </div>
                        </div>
                    </td>
                    <td>${escapeHtml(teacher.email)}</td>
                    <td>${escapeHtml(teacher.username)}</td>
                    <td>
                        <div style="display:flex; gap:8px;">
                            <button class="btn btn-outline" type="button" data-upload-photo="${escapeHtml(teacher.id)}">Upload Photo</button>
                            <button class="btn btn-outline" type="button" data-change-password="${escapeHtml(teacher.id)}">Change Password</button>
                            <button class="btn btn-outline" type="button" data-reset-password="${escapeHtml(teacher.id)}">Reset To Teacher ID</button>
                        </div>
                    </td>
                </tr>
            `).join("");

            bindActions();
        } catch (error) {
            gridBody.innerHTML = `<tr><td colspan="5">Unable to load teachers: ${escapeHtml(error.message || "Unknown error")}</td></tr>`;
        }
    }

    function bindActions() {
        Array.from(document.querySelectorAll("[data-upload-photo]"))
            .forEach((button) => {
                button.addEventListener("click", async () => {
                    const teacherId = button.getAttribute("data-upload-photo");
                    if (!teacherId) {
                        return;
                    }

                    const input = document.createElement("input");
                    input.type = "file";
                    input.accept = "image/*";
                    input.onchange = async () => {
                        const file = input.files && input.files[0] ? input.files[0] : null;
                        if (!file) {
                            return;
                        }
                        try {
                            await window.smsApi.admin.teachers.uploadProfilePicture(teacherId, file);
                            window.alert(`Profile picture updated for teacher ${teacherId}`);
                            loadTeachers();
                        } catch (error) {
                            window.alert(error.message || "Profile picture upload failed");
                        }
                    };
                    input.click();
                });
            });

        Array.from(document.querySelectorAll("[data-change-password]"))
            .forEach((button) => {
                button.addEventListener("click", async () => {
                    const teacherId = button.getAttribute("data-change-password");
                    if (!teacherId) {
                        return;
                    }

                    const newPassword = window.prompt(`Set new password for teacher ${teacherId}:`);
                    if (!newPassword) {
                        return;
                    }
                    const confirmPassword = window.prompt(`Confirm new password for teacher ${teacherId}:`);
                    if (!confirmPassword) {
                        return;
                    }

                    try {
                        await window.smsApi.admin.teachers.changePassword(teacherId, { newPassword, confirmPassword });
                        window.alert(`Password updated for teacher ${teacherId}`);
                    } catch (error) {
                        window.alert(error.message || "Password update failed");
                    }
                });
            });

        Array.from(document.querySelectorAll("[data-reset-password]"))
            .forEach((button) => {
                button.addEventListener("click", async () => {
                    const teacherId = button.getAttribute("data-reset-password");
                    if (!teacherId) {
                        return;
                    }

                    const confirmed = window.confirm(`Reset password for teacher ${teacherId} to teacher ID?`);
                    if (!confirmed) {
                        return;
                    }

                    try {
                        await window.smsApi.admin.teachers.resetPassword(teacherId);
                        window.alert(`Password reset for teacher ${teacherId}`);
                    } catch (error) {
                        window.alert(error.message || "Password reset failed");
                    }
                });
            });
    }

    loadTeachers();
})();
