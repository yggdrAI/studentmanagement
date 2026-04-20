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
                    <td>${escapeHtml(teacher.name)}</td>
                    <td>${escapeHtml(teacher.email)}</td>
                    <td>${escapeHtml(teacher.username)}</td>
                    <td>
                        <div style="display:flex; gap:8px;">
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
