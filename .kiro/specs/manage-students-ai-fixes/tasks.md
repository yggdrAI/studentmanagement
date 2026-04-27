# Implementation Plan

- [x] 1. Write bug condition exploration tests
  - **Property 1: Bug Condition** - Unassigned Students Not Visible & AI Insights Double-Init
  - **CRITICAL**: These tests MUST FAIL on unfixed code — failure confirms the bugs exist
  - **DO NOT attempt to fix the tests or the code when they fail**
  - **NOTE**: These tests encode the expected behavior — they will validate the fix when they pass after implementation
  - **GOAL**: Surface counterexamples that demonstrate both bugs exist
  - **Scoped PBT Approach**: Scope each property to the concrete failing cases for reproducibility

  - Bug 1 — Hierarchy rendering with all-unassigned students:
    - Seed the DB (or mock the hierarchy endpoint response) with students where `academicClass = null` for all entries
    - Assert that the rendered hierarchy DOM contains a visible class card for the "Unassigned" group (id === 0)
    - Assert that a "Regenerate Structure" prompt/banner is visible when only the Unassigned node is present
    - Run on UNFIXED code — expect FAILURE (confirms the Unassigned node is hidden/absent)
    - Document counterexample: "Hierarchy DOM contains no visible class cards even though API returned Unassigned node"

  - Bug 2 — AI Insights double-init:
    - In a Jest/Vitest test harness, execute the `ai-insights.js` IIFE body twice with a mocked `window.smsApi`
    - Assert `refreshSummary` mock is called exactly once (not twice)
    - Assert each event listener is registered exactly once
    - Run on UNFIXED code — expect FAILURE (confirms `init()` runs twice)
    - Document counterexample: "`refreshSummary` called twice; event listeners fire twice per user interaction"

  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests FAIL (this is correct — it proves the bugs exist)
  - Mark task complete when tests are written, run, and failures are documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [-] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Hierarchy & Normal AI Insights Load Unchanged
  - **IMPORTANT**: Follow observation-first methodology — observe UNFIXED code behavior for non-buggy inputs first

  - Preservation for Bug 1 — Hierarchy with assigned students:
    - Observe: with students having valid `AcademicClass` assignments, the hierarchy endpoint returns the full Program → Class → Batch tree
    - Write property-based test: for all random sets of students with non-null `academicClass`, the hierarchy response is identical before and after the fix (same class nodes, same batch nodes)
    - Also assert: "Regenerate Structure" button still calls `POST /api/admin/grouping/regenerate` and refreshes the view
    - Also assert: drag-and-drop reassignment endpoint still accepts and processes batch reassignment requests

  - Preservation for Bug 2 — AI Insights single-load:
    - Observe: with a single script inclusion and no prior initialization, `init()` runs once and all analytics render correctly
    - Write property-based test: for all page configurations where `isBugCondition_AiInsightsNotLoading` returns false (script included exactly once), behavior is identical before and after the fix
    - Also assert: changing a filter still triggers exactly one `refreshSummary()` call

  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 3. Fix for classes not showing in Manage Students and AI insights not loading

  - [~] 3.1 Fix Bug 1 — Render the Unassigned node and add a "no structure" banner
    - In `src/main/resources/static/js/admin-students-saas.js`, locate the hierarchy rendering logic that iterates over class nodes
    - Remove any condition that filters out or skips class nodes with `id === 0` or `label === "Unassigned"`
    - Ensure the Unassigned node is rendered as a visible, clearly labelled class card in the hierarchy view
    - When the hierarchy response contains only the Unassigned node (or `totalClasses === 1` and the sole class is Unassigned), display a banner/inline prompt informing the admin that the academic structure has not been generated yet, with a button that calls `POST /api/admin/grouping/regenerate`
    - No changes to `AdminHierarchyController` — the back-end already correctly groups unassigned students under key `0`
    - _Bug_Condition: isBugCondition_ClassesNotShowing(context) where context.allStudentsHaveNoAcademicClassAssignment = true_
    - _Expected_Behavior: result.unassignedNodeVisible = true OR result.regeneratePromptVisible = true_
    - _Preservation: Hierarchy with assigned students must produce identical response before and after fix (Property 3)_
    - _Requirements: 2.1, 2.2, 3.1, 3.3, 3.5_

  - [~] 3.2 Fix Bug 2 — Add idempotency guard to `ai-insights.js`
    - Open `src/main/resources/static/js/ai-insights.js`
    - Locate the unconditional `init()` call at the bottom of the IIFE (line ~783)
    - Replace the bare `init()` call with the idempotency guard:
      ```javascript
      if (!window.smsAiInsightsInitialized) {
          window.smsAiInsightsInitialized = true;
          init();
      }
      ```
    - No other changes required — the rest of `ai-insights.js` is correct
    - Verify `ai-insights.html` has only one `<script>` tag each for `api-client.js` and `ai-insights.js` (the guard is a defensive measure against future regressions)
    - _Bug_Condition: isBugCondition_AiInsightsNotLoading(page) where window.smsAiInsightsInitialized = true AND init() is called again_
    - _Expected_Behavior: result.initCallCount = 1 AND result.refreshSummaryCallCount = 1 AND result.insightsRendered = true_
    - _Preservation: Single-load page behavior must be identical before and after fix (Property 4)_
    - _Requirements: 2.3, 2.4, 3.2, 3.4_

  - [~] 3.3 Verify bug condition exploration tests now pass
    - **Property 1: Expected Behavior** - Unassigned Students Visible & AI Insights Init Runs Once
    - **IMPORTANT**: Re-run the SAME tests from task 1 — do NOT write new tests
    - The tests from task 1 encode the expected behavior
    - When these tests pass, it confirms the expected behavior is satisfied for both bugs
    - Run bug condition exploration tests from step 1
    - **EXPECTED OUTCOME**: Tests PASS (confirms both bugs are fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [~] 3.4 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Hierarchy & Normal AI Insights Load Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 — do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [~] 4. Checkpoint — Ensure all tests pass
  - Ensure all tests pass; ask the user if questions arise.
