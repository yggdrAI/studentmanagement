const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

const MEALS_DATA = [
    {
        type: 'Breakfast',
        time: '07:30 AM - 09:30 AM',
        icon: 'sun',
        items: [
            { id: 'b1', name: 'Vada Pao', calories: 165 },
            { id: 'b2', name: 'Green Chutney', calories: 65 },
            { id: 'b3', name: 'Mix Fruits', calories: 105 },
            { id: 'b4', name: 'Veg Oats', calories: 115 }
        ]
    },
    {
        type: 'Lunch',
        time: '12:00 PM - 03:00 PM',
        icon: 'utensils',
        items: [
            { id: 'l1', name: 'Lauki Chana Masala', calories: 125 },
            { id: 'l2', name: 'Aloo Beans', calories: 145 },
            { id: 'l3', name: 'Amritsari Chole', calories: 185 },
            { id: 'l4', name: 'Jeera Rice', calories: 145 }
        ]
    },
    {
        type: 'Snack',
        time: '05:00 PM - 06:00 PM',
        icon: 'coffee',
        items: [
            { id: 's1', name: 'Jhalmuri', calories: 125 },
            { id: 's2', name: 'Sweet Chutney', calories: 125 },
            { id: 's3', name: 'Coffee', calories: 65 }
        ]
    },
    {
        type: 'Dinner',
        time: '07:30 PM - 09:30 PM',
        icon: 'moon',
        items: [
            { id: 'd1', name: 'Dal Tadka', calories: 130 },
            { id: 'd2', name: 'Steamed Rice', calories: 150 },
            { id: 'd3', name: 'Paneer Bhurji', calories: 190 },
            { id: 'd4', name: 'Salad Bowl', calories: 85 }
        ]
    }
];

let selectedDay = currentDay();
let expandedMeal = null;
let selectedItems = [];
let reusableToken = getOrCreateToken();
let weeklyCaloriesChart = null;
let healthScoreChart = null;
let syncTimerHandle = null;

function currentDay() {
    const short = new Intl.DateTimeFormat('en-US', { weekday: 'short' }).format(new Date());
    return DAYS.includes(short) ? short : 'Sat';
}

function getOrCreateToken() {
    const key = 'cafeteria-reusable-token';
    const existing = localStorage.getItem(key);
    if (existing) {
        return existing;
    }
    const created = createToken();
    localStorage.setItem(key, created);
    return created;
}

function createToken() {
    return 'CAF-' + Date.now().toString(36).toUpperCase() + '-' + Math.random().toString(36).slice(2, 10).toUpperCase();
}

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return '';
    }
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function iconSvg(type) {
    if (type === 'sun') {
        return '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><circle cx="12" cy="12" r="4" fill="currentColor"></circle><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"></path></svg>';
    }

    if (type === 'coffee') {
        return '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M4 6h13v7a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V6Z" fill="none" stroke="currentColor" stroke-width="1.6"></path><path d="M17 8h1a3 3 0 1 1 0 6h-1" fill="none" stroke="currentColor" stroke-width="1.6"></path><path d="M6 20h10" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"></path></svg>';
    }

    if (type === 'moon') {
        return '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M14.5 2.5a8.7 8.7 0 1 0 7 13.9A9.4 9.4 0 0 1 14.5 2.5Z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"></path></svg>';
    }

    return '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M3 6h18M6 6l1 12h10l1-12M9 10v4M12 10v4M15 10v4" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"></path></svg>';
}

function renderDaySelector() {
    const container = document.getElementById('daySelector');
    container.innerHTML = DAYS.map((day) => {
        const active = day === selectedDay ? ' active' : '';
        return '<button type="button" class="day-pill' + active + '" data-day="' + day + '" role="tab" aria-selected="' + (day === selectedDay) + '">' + day + '</button>';
    }).join('');

    container.querySelectorAll('[data-day]').forEach((button) => {
        button.addEventListener('click', () => {
            selectedDay = button.getAttribute('data-day') || 'Sat';
            renderDaySelector();
            renderMeals();
            updateSummary();
            refreshQrMetaOnly();
        });
    });
}

function filteredMeals() {
    const searchText = (document.getElementById('mealSearch').value || '').trim().toLowerCase();
    const mealType = document.getElementById('mealTypeFilter').value;

    return MEALS_DATA
        .filter((meal) => mealType === 'all' || meal.type === mealType)
        .map((meal) => {
            const items = meal.items.filter((item) => item.name.toLowerCase().includes(searchText));
            return { ...meal, items };
        })
        .filter((meal) => meal.items.length > 0);
}

function itemSelected(itemId) {
    return selectedItems.some((item) => item.id === itemId);
}

function toggleItem(item, mealType) {
    if (itemSelected(item.id)) {
        selectedItems = selectedItems.filter((entry) => entry.id !== item.id);
        showToast(item.name + ' removed');
    } else {
        selectedItems = [...selectedItems, { ...item, mealType }];
        showToast(item.name + ' added');
    }

    renderMeals();
    updateSummary();
    scheduleSelectionSync();
}

function scheduleSelectionSync() {
    if (syncTimerHandle) {
        clearTimeout(syncTimerHandle);
    }

    syncTimerHandle = window.setTimeout(() => {
        persistDietLog(buildPayload(), { silent: true });
    }, 500);
}

function renderMeals() {
    const meals = filteredMeals();
    const grid = document.getElementById('mealGrid');

    if (meals.length === 0) {
        grid.innerHTML = '<div class="empty-state">No meals match your current filters. Try clearing search to explore the full menu.</div>';
        return;
    }

    grid.innerHTML = meals.map((meal, index) => {
        const viewItems = expandedMeal === meal.type ? meal.items : meal.items.slice(0, 3);
        const total = meal.items.reduce((sum, item) => sum + item.calories, 0);
        const buttonLabel = expandedMeal === meal.type ? 'Hide' : 'View';

        return '<article class="meal-card" style="transition-delay:' + (index * 70) + 'ms">' +
            '<div class="meal-head">' +
                '<div class="meal-head-main">' +
                    '<span class="meal-icon">' + iconSvg(meal.icon) + '</span>' +
                    '<div><h3 class="meal-type">' + escapeHtml(meal.type) + '</h3><p class="meal-time">' + escapeHtml(meal.time) + '</p></div>' +
                '</div>' +
                '<button type="button" class="expand-btn" data-expand="' + escapeHtml(meal.type) + '">' + buttonLabel + '</button>' +
            '</div>' +
            '<div class="meal-items">' +
                viewItems.map((item) => {
                    const selectedClass = itemSelected(item.id) ? ' selected' : '';
                    const barWidth = Math.min(100, Math.round((item.calories / 250) * 100));
                    return '<div class="meal-item' + selectedClass + '" data-item-id="' + item.id + '" data-meal-type="' + escapeHtml(meal.type) + '">' +
                        '<div class="meal-item-top"><span>' + escapeHtml(item.name) + '</span><span class="kcal-tag">' + item.calories + ' kcal</span></div>' +
                        '<div class="kcal-bar"><div class="kcal-bar-fill" style="width:' + barWidth + '%;"></div></div>' +
                    '</div>';
                }).join('') +
            '</div>' +
            '<div class="meal-foot"><span class="day-chip">' + selectedDay + '</span><span class="meal-kcal-total">Total: ' + total + ' kcal</span></div>' +
        '</article>';
    }).join('');

    grid.querySelectorAll('[data-expand]').forEach((button) => {
        button.addEventListener('click', () => {
            const mealType = button.getAttribute('data-expand');
            expandedMeal = expandedMeal === mealType ? null : mealType;
            renderMeals();
        });
    });

    grid.querySelectorAll('[data-item-id]').forEach((row) => {
        row.addEventListener('click', () => {
            const itemId = row.getAttribute('data-item-id');
            const mealType = row.getAttribute('data-meal-type');
            const meal = MEALS_DATA.find((entry) => entry.type === mealType);
            if (!meal) {
                return;
            }

            const item = meal.items.find((entry) => entry.id === itemId);
            if (!item) {
                return;
            }

            toggleItem(item, mealType);
        });
    });

    requestAnimationFrame(() => {
        grid.querySelectorAll('.meal-card').forEach((card) => card.classList.add('in'));
    });
}

function summaryData() {
    const totalCalories = selectedItems.reduce((sum, item) => sum + item.calories, 0);
    const count = selectedItems.length;
    const progress = Math.min(100, Math.round((totalCalories / 1200) * 100));
    return { totalCalories, count, progress };
}

function updateHero(summary) {
    const heroValue = document.getElementById('heroRingValue');
    const heroDay = document.getElementById('heroDayText');
    const arc = document.getElementById('heroCalorieArc');
    if (heroValue) {
        heroValue.textContent = String(summary.totalCalories);
    }

    if (heroDay) {
        heroDay.textContent = selectedDay;
    }

    if (arc) {
        const circumference = 314;
        const progress = Math.max(0, Math.min(1, summary.totalCalories / 2000));
        const offset = circumference - (circumference * progress);
        arc.style.strokeDasharray = String(circumference);
        arc.style.strokeDashoffset = String(Math.round(offset));
    }
}

function updateSummary() {
    const summary = summaryData();
    document.getElementById('selectedCount').textContent = summary.count + (summary.count === 1 ? ' item' : ' items');
    document.getElementById('totalCalories').textContent = summary.totalCalories + ' kcal';
    document.getElementById('calorieProgress').style.width = summary.progress + '%';
    updateHero(summary);
}

function buildPayload() {
    const summary = summaryData();
    return {
        day: selectedDay,
        meals: selectedItems.map((item) => ({
            name: item.name,
            mealType: item.mealType,
            calories: item.calories
        })),
        totalCalories: summary.totalCalories,
        reusableToken,
        generatedAt: new Date().toISOString()
    };
}

function updateSuggestionCard(data) {
    const panel = document.querySelector('.ai-suggestion-panel');
    const title = document.getElementById('aiSuggestionTitle');
    const text = document.getElementById('aiSuggestionText');

    if (!panel || !title || !text || !data) {
        return;
    }

    panel.classList.remove('risk-low', 'risk-medium', 'risk-high');
    const normalizedRisk = (data.riskLevel || 'LOW').toString().toLowerCase();
    panel.classList.add('risk-' + normalizedRisk);

    title.textContent = 'AI Suggestion for ' + (data.nextMeal || 'next meal') + ' (' + (data.caloriesToday || 0) + ' kcal today)';
    text.textContent = data.suggestion || 'No suggestion available right now.';

    const mlPrediction = document.getElementById('mlPrediction');
    const mlScore = document.getElementById('mlScore');
    const mlSource = document.getElementById('mlSource');

    if (mlPrediction) {
        mlPrediction.textContent = String(data.mlPrediction || 'moderate').toUpperCase();
    }

    if (mlScore) {
        mlScore.textContent = String(Math.round(Number(data.mlScore || 0)));
    }

    if (mlSource) {
        const source = String(data.mlSource || 'heuristic').toLowerCase();
        mlSource.textContent = source.includes('local') ? 'local model' : 'python ml';
    }

    if (Array.isArray(data.weeklyCalories)) {
        renderWeeklyCaloriesChart(data.weeklyCalories);
    }

    renderHealthScoreChart(Number(data.mlScore || 0));
}

function showToast(message) {
    const toast = document.getElementById('cafeteriaToast');
    if (!toast) {
        return;
    }

    toast.textContent = message;
    toast.hidden = false;
    if (navigator.vibrate) {
        navigator.vibrate(40);
    }
    window.setTimeout(() => {
        toast.hidden = true;
    }, 2000);
}

function renderWeeklyCaloriesChart(points) {
    const canvas = document.getElementById('weeklyCaloriesChart');
    if (!canvas || typeof Chart === 'undefined') {
        return;
    }

    const labels = points.map((point) => point.day);
    const values = points.map((point) => Number(point.calories || 0));
    const maxValue = Math.max(...values, 0);
    const suggestedMax = Math.max(600, Math.ceil((maxValue || 1) / 200) * 200);
    const isFlatSeries = values.every((value) => value === 0);
    const weeklyChartEmpty = document.getElementById('weeklyChartEmpty');

    if (weeklyChartEmpty) {
        weeklyChartEmpty.hidden = !isFlatSeries;
    }

    const context = canvas.getContext('2d');
    const gradient = context.createLinearGradient(0, 0, 0, 220);
    gradient.addColorStop(0, 'rgba(34, 197, 94, 0.35)');
    gradient.addColorStop(1, 'rgba(34, 197, 94, 0.02)');

    if (weeklyCaloriesChart) {
        weeklyCaloriesChart.destroy();
    }

    weeklyCaloriesChart = new Chart(canvas, {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: 'Calories',
                    data: values,
                    borderColor: isFlatSeries ? '#94a3b8' : '#22c55e',
                    backgroundColor: gradient,
                    borderWidth: 3,
                    fill: true,
                    tension: 0.35,
                    pointRadius: isFlatSeries ? 0 : 3.5,
                    pointHoverRadius: 5,
                    pointBackgroundColor: '#ffffff',
                    pointBorderColor: isFlatSeries ? '#94a3b8' : '#22c55e',
                    pointBorderWidth: 2,
                    borderDash: isFlatSeries ? [7, 5] : []
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            aspectRatio: 2.35,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: (ctx) => `${ctx.parsed.y} kcal`
                    }
                }
            },
            scales: {
                x: {
                    ticks: { color: '#64748b', autoSkip: false },
                    grid: { color: 'rgba(148,163,184,0.12)', drawBorder: false }
                },
                y: {
                    min: 0,
                    suggestedMax,
                    ticks: {
                        color: '#64748b',
                        stepSize: suggestedMax <= 1000 ? 200 : 400,
                        callback: (value) => `${value}`
                    },
                    grid: { color: 'rgba(148,163,184,0.12)', drawBorder: false }
                }
            }
        }
    });
}

function renderHealthScoreChart(score) {
    const canvas = document.getElementById('healthScoreChart');
    if (!canvas || typeof Chart === 'undefined') {
        return;
    }

    const normalized = Math.max(0, Math.min(100, Number(score || 0)));
    const centerTextPlugin = {
        id: 'centerText',
        afterDraw(chart) {
            const { ctx } = chart;
            const meta = chart.getDatasetMeta(0);
            if (!meta || !meta.data || !meta.data[0]) {
                return;
            }

            const x = meta.data[0].x;
            const y = meta.data[0].y;
            ctx.save();
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillStyle = '#0f172a';
            ctx.font = '700 24px Outfit';
            ctx.fillText(String(Math.round(normalized)), x, y - 4);
            ctx.fillStyle = '#64748b';
            ctx.font = '600 11px Outfit';
            ctx.fillText('health score', x, y + 15);
            ctx.restore();
        }
    };

    if (healthScoreChart) {
        healthScoreChart.destroy();
    }

    healthScoreChart = new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['Health Score', 'Remaining'],
            datasets: [
                {
                    data: [normalized, 100 - normalized],
                    backgroundColor: ['#22c55e', 'rgba(148,163,184,0.18)'],
                    borderWidth: 0,
                    cutout: '76%'
                }
            ]
        },
        plugins: [centerTextPlugin],
        options: {
            responsive: true,
            maintainAspectRatio: true,
            aspectRatio: 1,
            plugins: {
                legend: { display: false },
                tooltip: { enabled: false }
            },
            animation: {
                animateRotate: true,
                duration: 650
            }
        }
    });
}

async function fetchSuggestion() {
    const panel = document.querySelector('.ai-suggestion-panel');
    const title = document.getElementById('aiSuggestionTitle');
    const text = document.getElementById('aiSuggestionText');
    if (panel) {
        panel.classList.add('skeleton');
    }

    if (title && text) {
        title.textContent = 'Analyzing today\'s meal behavior...';
        text.textContent = 'Pulling calorie trend and preparing AI guidance.';
    }

    try {
        const response = await fetch('/api/student/diet/suggestion', {
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) {
            return;
        }
        const payload = await response.json();
        updateSuggestionCard(payload);
    } catch (error) {
        console.error('Unable to load AI suggestion:', error);
        if (title && text) {
            title.textContent = 'AI insight unavailable right now';
            text.textContent = 'Your meal selection still works. We will refresh recommendations automatically.';
        }
    } finally {
        if (panel) {
            panel.classList.remove('skeleton');
        }
    }
}

async function persistDietLog(payload, options = {}) {
    const silent = options.silent === true;

    try {
        const response = await fetch('/api/student/diet/log-batch', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            return;
        }

        const suggestion = await response.json();
        updateSuggestionCard(suggestion);
        if (!silent) {
            showToast('Diet log updated and AI insights refreshed.');
        }
    } catch (error) {
        console.error('Unable to persist diet log:', error);
        if (!silent) {
            showToast('Saved locally, but diet API is currently unreachable.');
        }
    }
}

function refreshQrMetaOnly() {
    const meta = document.getElementById('qrMeta');
    if (!meta) {
        return;
    }

    if (selectedItems.length === 0) {
        meta.textContent = 'Select meal items, then generate QR.';
        return;
    }

    meta.textContent = 'Ready for ' + selectedItems.length + ' item(s) on ' + selectedDay + '.';
}

async function generateQr() {
    if (selectedItems.length === 0) {
        alert('Select at least one meal item before generating QR.');
        return;
    }

    const payload = buildPayload();
    const payloadText = JSON.stringify(payload);

    const drawer = document.getElementById('qrDrawer');
    const canvas = document.getElementById('qrCanvas');
    const preview = document.getElementById('qrPayloadPreview');
    const meta = document.getElementById('qrMeta');

    drawer.hidden = false;
    canvas.innerHTML = '';

    new QRCode(canvas, {
        text: payloadText,
        width: 92,
        height: 92,
        colorDark: '#0f172a',
        colorLight: '#ffffff',
        correctLevel: QRCode.CorrectLevel.H
    });

    preview.textContent = JSON.stringify(payload, null, 2);
    meta.textContent = 'QR valid for cafeteria verification window. Token: ' + reusableToken.slice(0, 10) + '...';

    await persistDietLog(payload, { silent: false });
}

function regenerateToken() {
    reusableToken = createToken();
    localStorage.setItem('cafeteria-reusable-token', reusableToken);
    refreshQrMetaOnly();
    document.getElementById('qrDrawer').hidden = true;
    showToast('Reusable token regenerated.');
}

function wireFilters() {
    const search = document.getElementById('mealSearch');
    const filter = document.getElementById('mealTypeFilter');
    const clear = document.getElementById('clearFiltersBtn');

    search.addEventListener('input', renderMeals);
    filter.addEventListener('change', renderMeals);

    clear.addEventListener('click', () => {
        search.value = '';
        filter.value = 'all';
        renderMeals();
    });
}

function initActions() {
    document.getElementById('generateQrBtn').addEventListener('click', generateQr);
    document.getElementById('regenerateTokenBtn').addEventListener('click', regenerateToken);
}

function initCafeteria() {
    renderDaySelector();
    wireFilters();
    initActions();
    renderMeals();
    updateSummary();
    refreshQrMetaOnly();
    fetchSuggestion();
}

document.addEventListener('DOMContentLoaded', initCafeteria);
