(function () {
    if (window.__SMS_SMART_CAMPUS_UI_READY) {
        return;
    }
    window.__SMS_SMART_CAMPUS_UI_READY = true;

    function addEnterAnimations() {
        const targets = document.querySelectorAll(
            '.content-wrapper section, .content-wrapper article, .content-wrapper .glass-panel, .content-wrapper .generic-card, .content-wrapper .student-info-card'
        );

        targets.forEach((el, idx) => {
            el.setAttribute('data-ui-enter', 'true');
            el.style.transitionDelay = Math.min(idx * 35, 220) + 'ms';
            requestAnimationFrame(() => {
                el.classList.add('entered');
            });
        });
    }

    function addRipple(event) {
        const target = event.currentTarget;
        const rect = target.getBoundingClientRect();
        const ripple = document.createElement('span');
        ripple.className = 'sc-ripple';
        ripple.style.left = (event.clientX - rect.left) + 'px';
        ripple.style.top = (event.clientY - rect.top) + 'px';
        target.appendChild(ripple);
        window.setTimeout(() => ripple.remove(), 600);
    }

    function bindInteractiveFeedback() {
        const clickables = document.querySelectorAll('.btn, .control-btn, .cafeteria-btn, .icon-btn, .day-pill, .nav-item, .tool-btn');
        clickables.forEach((el) => {
            el.addEventListener('pointerdown', addRipple);
            el.addEventListener('click', () => {
                if (navigator.vibrate && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                    navigator.vibrate(24);
                }
            });
        });
    }

    function ensureToastStack() {
        let stack = document.querySelector('.sc-toast-stack');
        if (!stack) {
            stack = document.createElement('div');
            stack.className = 'sc-toast-stack';
            document.body.appendChild(stack);
        }
        return stack;
    }

    window.smsToast = function (message, type) {
        const stack = ensureToastStack();
        const toast = document.createElement('div');
        toast.className = 'sc-toast ' + (type || 'info');
        toast.textContent = message;
        stack.appendChild(toast);
        window.setTimeout(() => toast.remove(), 2200);
    };

    function wireScannerStatusToasts() {
        const status = document.getElementById('statusMessage');
        if (!status) {
            return;
        }

        const observer = new MutationObserver(() => {
            if (status.classList.contains('success')) {
                window.smsToast('Attendance Marked', 'success');
            } else if (status.classList.contains('error')) {
                window.smsToast('Face mismatch or invalid QR', 'error');
            } else if (status.classList.contains('warning')) {
                window.smsToast('Suspicious activity detected', 'warning');
            }
        });

        observer.observe(status, { attributes: true, attributeFilter: ['class'] });
    }

    function init() {
        document.body.classList.add('smart-campus-ui');
        addEnterAnimations();
        bindInteractiveFeedback();
        ensureToastStack();
        wireScannerStatusToasts();
    }

    document.addEventListener('DOMContentLoaded', init);
})();
