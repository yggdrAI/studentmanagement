/**
 * Bug 2 — AI Insights double-init
 *
 * Validates: Requirements 1.3, 1.4
 *
 * These tests MUST FAIL on unfixed code.
 * Failure confirms the bug: ai-insights.js calls init() unconditionally at the
 * bottom of its IIFE. When the script executes twice (e.g., duplicate <script> tag),
 * init() runs twice, refreshSummary() is called twice, and all event listeners
 * are registered twice.
 *
 * Expected counterexample:
 *   "refreshSummary called twice; event listeners fire twice per user interaction"
 */

"use strict";

const fs = require("fs");
const path = require("path");

// ─── Load the IIFE source ─────────────────────────────────────────────────────

const AI_INSIGHTS_PATH = path.resolve(
  __dirname,
  "../src/main/resources/static/js/ai-insights.js"
);

const aiInsightsSrc = fs.readFileSync(AI_INSIGHTS_PATH, "utf8");

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Build a minimal DOM environment and mock window.smsApi so the IIFE
 * doesn't bail out at the top-level guard.
 */
function setupMockEnvironment(refreshSummaryMock) {
  // Minimal DOM elements referenced by ai-insights.js
  document.body.innerHTML = `
    <div id="aiApp" data-role="ADMIN"></div>
    <select id="filterCourse"></select>
    <select id="filterSemester"></select>
    <select id="filterSection"></select>
    <select id="filterRange"></select>
    <input id="filterFrom" type="date" />
    <input id="filterTo" type="date" />
    <div id="customFromWrap" class="hidden"></div>
    <div id="customToWrap" class="hidden"></div>
    <div id="metricTotal">0</div>
    <div id="metricActive">0</div>
    <div id="metricRisk">0</div>
    <div id="metricHigh">0</div>
    <div id="smartCards"></div>
    <div id="heatmapGrid"></div>
    <div id="studentList"></div>
    <div id="listSentinel"></div>
    <div id="activityFeed"></div>
    <div id="recommendationList"></div>
    <div id="feedStatus"></div>
    <div id="insightModal" aria-hidden="true"></div>
    <div id="modalTitle"></div>
    <div id="modalSubtitle"></div>
    <ul id="modalDetails"></ul>
    <button id="modalClose"></button>
    <div id="commandPalette"></div>
    <button id="paletteBtn"></button>
    <input id="paletteInput" />
    <ul id="paletteList"></ul>
    <div id="notificationPanel"></div>
    <button id="notificationBtn"></button>
    <button id="notificationClose"></button>
    <div id="notificationList"></div>
    <button id="digestBtn"></button>
    <button id="liveStatusBtn"></button>
    <button id="fabAdd"></button>
    <div id="toastStack"></div>
  `;

  // Mock window.smsApi so the IIFE doesn't return early
  window.smsApi = {
    analytics: {
      summary: refreshSummaryMock,
      sendDigest: jest.fn().mockResolvedValue({}),
      live: jest.fn().mockResolvedValue({ activeStudents: 0 }),
    },
  };

  // Mock SockJS and Stomp to prevent real WebSocket connections
  window.SockJS = jest.fn().mockImplementation(() => ({}));
  window.Stomp = {
    over: jest.fn().mockReturnValue({
      debug: null,
      connect: jest.fn(),
    }),
  };

  // Mock Chart.js
  window.Chart = jest.fn().mockImplementation(() => ({
    destroy: jest.fn(),
  }));

  // Mock IntersectionObserver
  window.IntersectionObserver = jest.fn().mockImplementation(() => ({
    observe: jest.fn(),
    disconnect: jest.fn(),
  }));

  // Mock performance.now
  window.performance = { now: jest.fn().mockReturnValue(0) };

  // Mock requestAnimationFrame
  window.requestAnimationFrame = jest.fn();
}

/**
 * Execute the ai-insights.js IIFE body in the current window context.
 * This simulates what happens when the browser loads the script.
 */
function executeAiInsightsIIFE() {
  // eslint-disable-next-line no-new-func
  const fn = new Function(aiInsightsSrc);
  fn();
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe("Bug 2 — AI Insights double-init (UNFIXED code)", () => {
  beforeEach(() => {
    // Reset the idempotency flag between tests (simulates fresh page load)
    delete window.smsAiInsightsInitialized;
    // Reset smsApi
    delete window.smsApi;
  });

  afterEach(() => {
    jest.clearAllMocks();
    delete window.smsAiInsightsInitialized;
    delete window.smsApi;
  });

  /**
   * Bug condition: The IIFE executes twice (simulating a duplicate <script> tag).
   * Expected (FIXED): refreshSummary is called exactly once.
   * Actual (UNFIXED): refreshSummary is called twice.
   *
   * This test MUST FAIL on unfixed code.
   *
   * Counterexample: "refreshSummary called twice; event listeners fire twice per user interaction"
   */
  test("EXPLORATION: refreshSummary is called exactly once when IIFE executes twice", () => {
    const refreshSummaryMock = jest.fn().mockResolvedValue({
      metrics: {},
      smartCards: [],
      charts: {},
      recommendations: [],
      activityFeed: [],
      studentTags: [],
    });

    setupMockEnvironment(refreshSummaryMock);

    // Execute the IIFE twice — simulates duplicate <script> tag
    executeAiInsightsIIFE();
    executeAiInsightsIIFE();

    // ASSERTION: refreshSummary must be called exactly once
    // EXPECTED TO FAIL on unfixed code — it will be called twice
    expect(refreshSummaryMock).toHaveBeenCalledTimes(1);
  });

  /**
   * Bug condition: The IIFE executes twice.
   * Expected (FIXED): Each event listener is registered exactly once.
   * Actual (UNFIXED): Event listeners are registered twice (doubled).
   *
   * We test this by firing a 'change' event on filterCourse and asserting
   * that the debounced refreshSummary is triggered only once per event.
   *
   * This test MUST FAIL on unfixed code.
   */
  test("EXPLORATION: event listeners are registered exactly once when IIFE executes twice", async () => {
    let callCount = 0;
    const refreshSummaryMock = jest.fn().mockImplementation(() => {
      callCount++;
      return Promise.resolve({
        metrics: {},
        smartCards: [],
        charts: {},
        recommendations: [],
        activityFeed: [],
        studentTags: [],
      });
    });

    setupMockEnvironment(refreshSummaryMock);

    // Execute the IIFE twice — simulates duplicate <script> tag
    executeAiInsightsIIFE();
    executeAiInsightsIIFE();

    // Reset call count after init (we only care about event-triggered calls)
    refreshSummaryMock.mockClear();
    callCount = 0;

    // Fire a change event on filterCourse — should trigger refreshSummary once
    const filterCourse = document.getElementById("filterCourse");
    filterCourse.dispatchEvent(new Event("change", { bubbles: true }));

    // Wait for debounce (350ms) + a bit more
    await new Promise(resolve => setTimeout(resolve, 500));

    // ASSERTION: refreshSummary must be called exactly once per event
    // EXPECTED TO FAIL on unfixed code — it will be called twice (two listeners)
    expect(refreshSummaryMock).toHaveBeenCalledTimes(1);
  });

  /**
   * Bug condition: The IIFE executes twice.
   * Expected (FIXED): window.smsAiInsightsInitialized is true after first execution.
   * Actual (UNFIXED): window.smsAiInsightsInitialized does not exist (flag not set).
   *
   * This test MUST FAIL on unfixed code.
   */
  test("EXPLORATION: window.smsAiInsightsInitialized flag is set after first execution", () => {
    const refreshSummaryMock = jest.fn().mockResolvedValue({
      metrics: {},
      smartCards: [],
      charts: {},
      recommendations: [],
      activityFeed: [],
      studentTags: [],
    });

    setupMockEnvironment(refreshSummaryMock);

    // Execute the IIFE once
    executeAiInsightsIIFE();

    // ASSERTION: The idempotency flag must be set
    // EXPECTED TO FAIL on unfixed code — the flag does not exist
    expect(window.smsAiInsightsInitialized).toBe(true);
  });

  /**
   * Baseline / sanity check: Single execution should call refreshSummary exactly once.
   * This test should PASS on both fixed and unfixed code.
   */
  test("SANITY: refreshSummary is called once on single IIFE execution", () => {
    const refreshSummaryMock = jest.fn().mockResolvedValue({
      metrics: {},
      smartCards: [],
      charts: {},
      recommendations: [],
      activityFeed: [],
      studentTags: [],
    });

    setupMockEnvironment(refreshSummaryMock);

    // Execute the IIFE once
    executeAiInsightsIIFE();

    // ASSERTION: refreshSummary must be called exactly once
    expect(refreshSummaryMock).toHaveBeenCalledTimes(1);
  });
});
