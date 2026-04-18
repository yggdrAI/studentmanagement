(function () {
    "use strict";

    if (window.__SMS_THEME_MANAGER_READY) {
        return;
    }
    window.__SMS_THEME_MANAGER_READY = true;

    const STORAGE_KEY = "sms_theme";
    const DENSITY_KEY = "sms_density";
    const MOTION_KEY = "sms_motion";
    const THEMES = ["light", "dim", "dark", "premium"];
    const LABELS = {
        light: "☀️ Light",
        dim: "🌙 Dim",
        dark: "🌌 Dark",
        premium: "💎 Premium"
    };

    function preferredTheme() {
        if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
            return "dim";
        }
        return "light";
    }

    function getTheme() {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored && THEMES.includes(stored)) {
            return stored;
        }
        const current = document.documentElement.getAttribute("data-theme");
        if (current && THEMES.includes(current)) {
            return current;
        }
        return preferredTheme();
    }

    function applyTheme(theme, persist) {
        const next = THEMES.includes(theme) ? theme : preferredTheme();
        document.documentElement.setAttribute("data-theme", next);
        if (persist !== false) {
            localStorage.setItem(STORAGE_KEY, next);
        }
        window.dispatchEvent(new CustomEvent("theme:changed", { detail: { theme: next } }));
        updateSwitchers(next);
    }

    function cycleTheme() {
        const current = getTheme();
        const idx = THEMES.indexOf(current);
        const next = THEMES[(idx + 1) % THEMES.length];
        applyTheme(next, true);
    }

    function getDensity() {
        const stored = localStorage.getItem(DENSITY_KEY);
        return stored === "compact" ? "compact" : "comfortable";
    }

    function applyDensity(mode, persist) {
        const next = mode === "compact" ? "compact" : "comfortable";
        document.documentElement.setAttribute("data-density", next);
        if (persist !== false) {
            localStorage.setItem(DENSITY_KEY, next);
        }
    }

    function getMotion() {
        const stored = localStorage.getItem(MOTION_KEY);
        return stored === "reduced" ? "reduced" : "normal";
    }

    function applyMotion(mode, persist) {
        const next = mode === "reduced" ? "reduced" : "normal";
        document.documentElement.setAttribute("data-motion", next);
        if (persist !== false) {
            localStorage.setItem(MOTION_KEY, next);
        }
    }

    function buildSwitcher(currentTheme) {
        const wrap = document.createElement("div");
        wrap.className = "theme-switcher";
        wrap.setAttribute("role", "group");
        wrap.setAttribute("aria-label", "Theme Switcher");

        THEMES.forEach((theme) => {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "theme-option" + (theme === currentTheme ? " active" : "");
            btn.setAttribute("data-theme-option", theme);
            btn.textContent = LABELS[theme];
            btn.addEventListener("click", function () {
                applyTheme(theme, true);
            });
            wrap.appendChild(btn);
        });

        return wrap;
    }

    function updateSwitchers(currentTheme) {
        document.querySelectorAll(".theme-switcher").forEach((switcher) => {
            switcher.querySelectorAll("[data-theme-option]").forEach((node) => {
                const theme = node.getAttribute("data-theme-option");
                node.classList.toggle("active", theme === currentTheme);
            });
        });
    }

    function injectSwitcher() {
        // Theme selection is intentionally centralized in User Settings modal.
    }

    function bindGlobalToggles() {
        const themeToggle = document.getElementById("themeToggle");
        if (themeToggle) {
            themeToggle.addEventListener("click", cycleTheme);
            themeToggle.setAttribute("title", "Switch Theme");
        }

        const legacyDarkButton = document.getElementById("darkModeBtn");
        if (legacyDarkButton) {
            legacyDarkButton.textContent = "Switch Theme";
        }

        bindSidebarSettingsPanel();
    }

    function bindSidebarSettingsPanel() {
        const btn = document.getElementById("userSettingsBtn");
        const modal = document.getElementById("userSettingsModal");
        const panel = modal ? modal.querySelector(".user-settings-window") : null;
        const closeBtn = document.getElementById("userSettingsClose");
        const select = document.getElementById("sidebarThemeSelect");
        const densitySelect = document.getElementById("sidebarDensitySelect");
        const motionSelect = document.getElementById("sidebarMotionSelect");

        if (!btn || !modal || !panel || !closeBtn || !select || !densitySelect || !motionSelect) {
            return;
        }

        function syncSelect() {
            select.value = getTheme();
            densitySelect.value = getDensity();
            motionSelect.value = getMotion();
            updateSwitchers(getTheme());
        }

        function closePanel() {
            modal.hidden = true;
            btn.setAttribute("aria-expanded", "false");
        }

        function openPanel() {
            modal.hidden = false;
            btn.setAttribute("aria-expanded", "true");
            syncSelect();
        }

        btn.addEventListener("click", function (event) {
            event.stopPropagation();
            if (modal.hidden) {
                openPanel();
            } else {
                closePanel();
            }
        });

        closeBtn.addEventListener("click", closePanel);

        select.addEventListener("change", function () {
            applyTheme(select.value, true);
        });

        densitySelect.addEventListener("change", function () {
            applyDensity(densitySelect.value, true);
        });

        motionSelect.addEventListener("change", function () {
            applyMotion(motionSelect.value, true);
        });

        panel.addEventListener("click", function (event) {
            event.stopPropagation();
        });

        document.addEventListener("click", function (event) {
            if (!modal.hidden && !modal.contains(event.target) && event.target !== btn) {
                closePanel();
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closePanel();
            }
        });
        window.addEventListener("theme:changed", syncSelect);
        syncSelect();
    }

    function init() {
        applyTheme(getTheme(), false);
        applyDensity(getDensity(), false);
        applyMotion(getMotion(), false);
        document.querySelectorAll(".theme-switcher").forEach((node) => node.remove());
        injectSwitcher();
        bindGlobalToggles();
    }

    window.SMSTheme = {
        get: getTheme,
        set: function (theme) { applyTheme(theme, true); },
        cycle: cycleTheme,
        getDensity: getDensity,
        setDensity: function (density) { applyDensity(density, true); },
        getMotion: getMotion,
        setMotion: function (motion) { applyMotion(motion, true); }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
