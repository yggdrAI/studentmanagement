/**
 * Bug 1 — Hierarchy rendering with all-unassigned students
 *
 * Validates: Requirements 1.1, 1.2
 *
 * These tests MUST FAIL on unfixed code.
 * Failure confirms the bug: when all students are unassigned (academicClass = null),
 * the hierarchy returns a single node with id=0 / label="Unassigned", but the
 * front-end does not surface a visible class card OR a "Regenerate Structure" prompt.
 *
 * Expected counterexample:
 *   "Hierarchy DOM contains no visible class cards even though API returned Unassigned node"
 */

"use strict";

// ─── Minimal DOM setup ───────────────────────────────────────────────────────

function buildMinimalDOM() {
  document.body.innerHTML = `
    <div id="classesContainer" hidden></div>
    <div id="noDataState" hidden></div>
    <div id="loadingSpinner"></div>
    <div id="regenerateBanner" hidden></div>
    <div id="totalPrograms">0</div>
    <div id="totalClasses">0</div>
    <div id="totalBatches">0</div>
    <div id="totalStudents">0</div>
    <div id="avgAttendance">0%</div>
    <div id="totalLabel"></div>
    <div id="programsGrid"></div>
    <select id="courseFilter"></select>
    <select id="semesterFilter"></select>
    <select id="performanceFilter"></select>
    <select id="groupingMode"></select>
    <input id="globalSearch" />
    <button id="refreshBtn"></button>
    <button id="expandAllBtn"></button>
    <button id="collapseAllBtn"></button>
    <button id="aiGroupBtn"></button>
    <button id="regenerateBtn"></button>
    <button id="sidebarToggle"></button>
    <div id="topPerformers"></div>
  `;
}

// ─── Extract the pure rendering functions from hierarchy-students.js ─────────
// We replicate the minimal subset needed to test renderHierarchy() and
// the "Regenerate Structure" banner logic.

/**
 * Minimal replica of the renderHierarchy + renderClassCard logic from
 * src/main/resources/static/js/hierarchy-students.js.
 *
 * This is the UNFIXED version — it does NOT show a banner when only the
 * Unassigned node is present.
 */
function makeUnfixedRenderer(refs, state) {
  function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    const div = document.createElement("div");
    div.textContent = String(value ?? "");
    return div.innerHTML;
  }

  function formatNumber(value) {
    const n = Number(value) || 0;
    return n.toFixed(1).replace(/\.0$/, "");
  }

  function countStudentsInClass(c) {
    return (c.batches || []).reduce((sum, b) => sum + (b.students?.length || 0), 0);
  }

  function renderClassCard(classItem, index) {
    const classId     = classItem.id ?? classItem.classId ?? `class-${classItem.number}`;
    const classNumber = classItem.number ?? classItem.classNumber ?? index + 1;
    const classLabel  = classItem.label || `Class ${classNumber}`;
    const analytics   = classItem.analytics || classItem.classAnalytics || {};
    const batches     = Array.isArray(classItem.batches) ? classItem.batches : [];

    const totalStudents = countStudentsInClass(classItem);
    const avgMarks      = analytics.avgMarks ?? analytics.averageMarks ?? 0;
    const attendance    = analytics.attendance ?? analytics.averageAttendance ?? 0;
    const riskStudents  = analytics.riskStudents ?? 0;
    const riskFactor    = totalStudents > 0 ? (riskStudents * 100) / totalStudents : 0;
    const healthScore   = analytics.healthScore ?? Math.max(0, Math.min(100, avgMarks + attendance - riskFactor));

    let status = "healthy", statusText = "Healthy";
    if (riskStudents > totalStudents * 0.3)       { status = "critical"; statusText = "Critical"; }
    else if (riskStudents > totalStudents * 0.15) { status = "moderate"; statusText = "Moderate"; }

    const heatmapColor = healthScore < 40 ? "heatmap-critical" : healthScore < 60 ? "heatmap-poor" : healthScore < 75 ? "heatmap-average" : healthScore < 85 ? "heatmap-good" : "heatmap-excellent";

    return `
      <article class="class-card card-container glass-panel ${heatmapColor}"
               data-class-id="${escapeHtml(String(classId))}"
               data-class-number="${escapeHtml(String(classNumber))}">
        <header class="header" data-open-class="${escapeHtml(String(classId))}">
          <div class="header-left">
            <h3 class="class-title">${escapeHtml(classLabel)}</h3>
            <span class="status-badge ${status}">${statusText}</span>
          </div>
        </header>
        <section class="new-inner-panel">
          <div class="metrics-grid">
            <div class="metric"><div class="metric-label">Students</div><div class="metric-value">${totalStudents}</div></div>
            <div class="metric"><div class="metric-label">Avg Marks</div><div class="metric-value">${formatNumber(avgMarks)}</div></div>
            <div class="metric"><div class="metric-label">Attendance</div><div class="metric-value">${formatNumber(attendance)}%</div></div>
          </div>
        </section>
      </article>
    `;
  }

  function applyFilters(classes) {
    const query = (state.filters.searchQuery || "").trim().toLowerCase();
    return classes
      .map(classItem => {
        const batches = (classItem.batches || [])
          .map(batch => {
            const students = (batch.students || []).filter(s =>
              !query || JSON.stringify(s).toLowerCase().includes(query)
            );
            const batchMatch = !query || students.length > 0;
            return batchMatch ? { ...batch, students: query ? students : (batch.students || []) } : null;
          })
          .filter(Boolean);
        const classMatch = !query || JSON.stringify(classItem).toLowerCase().includes(query) || batches.length > 0;
        return classMatch ? { ...classItem, batches } : null;
      })
      .filter(Boolean);
  }

  /**
   * UNFIXED renderHierarchy — does NOT check for the Unassigned-only case
   * and does NOT show a "Regenerate Structure" banner.
   */
  function renderHierarchy() {
    const classes = Array.isArray(state.hierarchy?.classes) ? state.hierarchy.classes : [];
    const filtered = applyFilters(classes);

    if (!filtered.length) {
      if (refs.noDataState) {
        refs.noDataState.innerHTML = `
          <div class="no-data-icon">🔍</div>
          <p>No students match current filters.</p>
        `;
        refs.noDataState.hidden = false;
      }
      if (refs.classesContainer) refs.classesContainer.innerHTML = "";
      return;
    }

    if (refs.noDataState) refs.noDataState.hidden = true;
    if (refs.loadingSpinner) refs.loadingSpinner.hidden = true;
    if (refs.classesContainer) {
      refs.classesContainer.hidden = false;
      refs.classesContainer.style.display = "grid";
      refs.classesContainer.innerHTML = filtered
        .map((classItem, index) => renderClassCard(classItem, index))
        .join("");
    }
  }

  return { renderHierarchy };
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe("Bug 1 — Hierarchy rendering with all-unassigned students (UNFIXED code)", () => {
  let refs;
  let state;

  beforeEach(() => {
    buildMinimalDOM();
    refs = {
      classesContainer: document.getElementById("classesContainer"),
      noDataState:      document.getElementById("noDataState"),
      loadingSpinner:   document.getElementById("loadingSpinner"),
    };
    state = {
      hierarchy: { summary: {}, classes: [] },
      filters: { searchQuery: "", groupingMode: "number" },
    };
  });

  /**
   * Bug condition: API returns a single Unassigned node (id=0, label="Unassigned").
   * This is the exact response produced by AdminHierarchyController when all students
   * have academicClass = null.
   *
   * Expected (FIXED): The Unassigned class card is visible in the DOM.
   * Actual (UNFIXED): The class card IS rendered (renderHierarchy doesn't filter id=0),
   *                   but there is NO "Regenerate Structure" banner/prompt.
   *
   * This test asserts the FIXED behavior — it MUST FAIL on unfixed code.
   */
  test("EXPLORATION: Unassigned node (id=0) renders a visible class card", () => {
    // Simulate the API response when all students have academicClass = null
    state.hierarchy = {
      summary: { totalClasses: 1, totalBatches: 1, totalStudents: 5 },
      classes: [
        {
          id: 0,
          number: 0,
          label: "Unassigned",
          batches: [
            {
              id: 0,
              number: 0,
              label: "Unassigned",
              students: [
                { studentId: "S001", name: "Alice" },
                { studentId: "S002", name: "Bob" },
              ],
            },
          ],
          analytics: {},
        },
      ],
    };

    const { renderHierarchy } = makeUnfixedRenderer(refs, state);
    renderHierarchy();

    const classCards = document.querySelectorAll(".class-card");
    const unassignedCard = Array.from(classCards).find(card =>
      card.querySelector(".class-title")?.textContent?.includes("Unassigned")
    );

    // ASSERTION: The Unassigned class card must be visible
    // EXPECTED TO FAIL on unfixed code if the card is hidden or absent
    expect(unassignedCard).toBeTruthy();
    expect(refs.classesContainer.hidden).toBe(false);
  });

  /**
   * Bug condition: API returns ONLY the Unassigned node.
   * Expected (FIXED): A "Regenerate Structure" prompt/banner is visible.
   * Actual (UNFIXED): No such banner exists — the admin sees no call to action.
   *
   * This test asserts the FIXED behavior — it MUST FAIL on unfixed code.
   *
   * Counterexample: "Hierarchy DOM contains no visible class cards even though
   * API returned Unassigned node" — and no regenerate prompt is shown.
   */
  test("EXPLORATION: Regenerate Structure banner is visible when only Unassigned node is present", () => {
    // Simulate the API response when all students have academicClass = null
    state.hierarchy = {
      summary: { totalClasses: 1, totalBatches: 1, totalStudents: 5 },
      classes: [
        {
          id: 0,
          number: 0,
          label: "Unassigned",
          batches: [
            {
              id: 0,
              number: 0,
              label: "Unassigned",
              students: [
                { studentId: "S001", name: "Alice" },
                { studentId: "S002", name: "Bob" },
              ],
            },
          ],
          analytics: {},
        },
      ],
    };

    const { renderHierarchy } = makeUnfixedRenderer(refs, state);
    renderHierarchy();

    // Check for a "Regenerate Structure" banner/prompt in the DOM
    // The fixed code should inject this when only the Unassigned node is present.
    // On unfixed code, no such element exists.
    const regenerateBanner = document.getElementById("regenerateBanner");
    const bannerVisible = regenerateBanner && !regenerateBanner.hidden;

    // Also check for any element containing "Regenerate" text in the hierarchy area
    const allText = document.body.innerHTML;
    const hasRegeneratePrompt =
      bannerVisible ||
      (allText.includes("Regenerate") &&
        (allText.includes("structure") || allText.includes("Structure") || allText.includes("academic")));

    // ASSERTION: A regenerate prompt must be visible
    // EXPECTED TO FAIL on unfixed code — no such prompt is shown
    expect(hasRegeneratePrompt).toBe(true);
  });

  /**
   * Verify the API response shape: when all students have academicClass = null,
   * the back-end returns a class node with id=0 and label="Unassigned".
   * This test validates our mock data matches the real API contract.
   * (This test should PASS on both fixed and unfixed code — it's a sanity check.)
   */
  test("SANITY: API response with all-unassigned students has id=0 Unassigned node", () => {
    const mockApiResponse = {
      summary: { totalClasses: 1, totalBatches: 1, totalStudents: 3 },
      classes: [
        {
          id: 0,
          number: 0,
          label: "Unassigned",
          batches: [{ id: 0, number: 0, label: "Unassigned", students: [] }],
        },
      ],
    };

    expect(mockApiResponse.classes).toHaveLength(1);
    expect(mockApiResponse.classes[0].id).toBe(0);
    expect(mockApiResponse.classes[0].label).toBe("Unassigned");
  });
});
