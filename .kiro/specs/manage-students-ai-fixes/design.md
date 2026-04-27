# Manage Students & AI Insights Bugfix Design

## Overview

Two independent bugs affect the admin panel:

1. **Classes not showing in Manage Students** — When no academic structure has been generated (the `AcademicClass`/`AcademicBatch` tables are empty), every student has `academicClass = null`, so `classGroupingKey()` in `AdminHierarchyController` returns `UNASSIGNED_CLASS_NUMBER` (0) for all of them. The hierarchy response contains a single node labelled "Unassigned" with `id: 0`. The front-end hierarchy view does not render class key `0` as a visible, actionable class, so the admin sees no classes at all. The fix surfaces the Unassigned group visibly and/or auto-triggers structure regeneration.

2. **AI insights not loading** — `ai-insights.html` contains a single `<script>` tag each for `api-client.js` and `ai-insights.js`, but `ai-insights.js` is an IIFE that calls `init()` unconditionally at the bottom. Because `api-client.js` has an early-return guard (`if (window.smsApi) return`) but `ai-insights.js` does not, any future duplicate inclusion (or a browser quirk that re-executes the script) would call `init()` twice, re-running `refreshSummary()` and re-registering all event listeners. The fix adds an idempotency guard to `ai-insights.js` so `init()` can only ever run once per page load.

---

## Glossary

- **Bug_Condition (C)**: The condition that triggers a bug — either all students are unassigned (no academic structure) or `ai-insights.js` executes more than once.
- **Property (P)**: The desired correct behavior when the bug condition holds.
- **Preservation**: Existing behaviors that must remain unchanged after the fix.
- **`classGroupingKey(student, profile)`**: Helper in `AdminHierarchyController` that returns `UNASSIGNED_CLASS_NUMBER` (0) when a student has no `AcademicClass` assignment.
- **`UNASSIGNED_CLASS_NUMBER`**: The constant `0` used as a synthetic class key for students with no `AcademicClass`.
- **`buildClassNode(classNumber, ...)`**: Builds the JSON node for a class in the hierarchy response; when `classNumber == 0` it labels the node "Unassigned".
- **`init()`**: The bootstrap function in `ai-insights.js` that calls `bindEvents()`, `refreshSummary()`, and sets up realtime connections.
- **`window.smsAiInsightsInitialized`**: The proposed idempotency flag to prevent `init()` from running more than once.

---

## Bug Details

### Bug 1 — Classes Not Showing in Manage Students

#### Bug Condition

The bug manifests when the admin opens the Manage Students hierarchy view and no academic structure has been generated (i.e., every student's `academicClass` field is `null`). `classGroupingKey()` returns `0` for every student, producing a single hierarchy node with `id: 0` and `label: "Unassigned"`. The front-end either does not render class key `0` or renders it in a way that is not visible/actionable to the admin.

**Formal Specification:**
```
FUNCTION isBugCondition_ClassesNotShowing(context)
  INPUT: context of type HierarchyPageContext
  OUTPUT: boolean

  RETURN context.academicProgramTableIsEmpty = true
      OR context.allStudentsHaveNoAcademicClassAssignment = true
END FUNCTION
```

#### Examples

- Admin opens `/admin/students` with 200 students, none assigned to an `AcademicClass` → hierarchy shows zero visible classes; expected: "Unassigned" group is visible with a "Regenerate Structure" prompt.
- Admin opens the page after a fresh install with students imported via CSV but structure not yet generated → same empty-class symptom.
- Admin opens the page after structure has been generated → full Program → Class → Batch tree renders correctly (this is the preserved case).

---

### Bug 2 — AI Insights Not Loading

#### Bug Condition

The bug manifests when `ai-insights.js` executes more than once in the same page context. The IIFE calls `init()` unconditionally at line 783. A second execution re-runs `bindEvents()` (duplicating all event listeners) and `refreshSummary()` (firing a second API call), corrupting the rendering state.

**Formal Specification:**
```
FUNCTION isBugCondition_AiInsightsNotLoading(page)
  INPUT: page of type AiInsightsHtmlPage
  OUTPUT: boolean

  RETURN countOccurrences(page, "ai-insights.js") > 1
      OR window.smsAiInsightsInitialized = true
         AND init() is called again
END FUNCTION
```

#### Examples

- `ai-insights.html` gains a duplicate `<script th:src="@{/js/ai-insights.js}">` tag → `init()` runs twice, `refreshSummary()` fires twice, event listeners are doubled; expected: `init()` runs exactly once.
- A Thymeleaf fragment include accidentally injects the script a second time → same double-init symptom.
- Page loads normally with a single script tag and no prior initialization → `init()` runs once, insights render correctly (preserved case).

---

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- When the academic structure has already been generated and classes exist, the full Program → Class → Batch hierarchy MUST continue to render correctly in the Manage Students window (requirement 3.1).
- When the admin uses the "Regenerate Structure" button, it MUST continue to regenerate groupings and refresh the hierarchy (requirement 3.3).
- When a student is dragged and dropped to a different batch, the reassignment MUST continue to work without a full page reload (requirement 3.5).
- When the AI Insights page loads with a valid session and a single script inclusion, analytics metrics, smart cards, charts, and the activity feed MUST continue to render correctly (requirement 3.2).
- When AI Insights filters are changed, the page MUST continue to re-fetch and re-render the analytics summary (requirement 3.4).

**Scope:**
All inputs that do NOT trigger the bug conditions above must be completely unaffected by these fixes. This includes:
- Hierarchy views where students already have `AcademicClass` assignments.
- AI Insights page loads where `ai-insights.js` is included exactly once.
- All other admin pages and student-facing pages.

---

## Hypothesized Root Cause

### Bug 1 — Classes Not Showing

1. **Unassigned node not rendered by the front-end**: The hierarchy view JavaScript may skip or hide class nodes with `id === 0` or `label === "Unassigned"`, treating them as internal sentinel values rather than displayable groups.

2. **No prompt to regenerate structure**: When the hierarchy returns only an "Unassigned" group, the UI does not inform the admin that the academic structure needs to be generated, leaving them with a blank view and no call to action.

3. **Empty `classes` array returned**: If `filtered.isEmpty()` is true (no students at all), the endpoint returns `"classes": []` with no Unassigned node, so there is nothing to render regardless of front-end behavior.

4. **`classGroupingKey` always returns 0**: Because `isUnassigned(student)` checks `academicClass == null && academicBatch == null`, any student without a generated structure falls into key `0`, and the entire hierarchy collapses to a single invisible node.

### Bug 2 — AI Insights Not Loading

1. **No idempotency guard in `ai-insights.js`**: The IIFE calls `init()` unconditionally at the bottom (line 783). Unlike `api-client.js` which checks `if (window.smsApi) return`, `ai-insights.js` has no equivalent guard, so any second execution re-runs the full initialization.

2. **Duplicate event listener registration**: `bindEvents()` attaches listeners directly to DOM elements without checking whether they are already bound. A second call doubles every listener, causing double API calls and unpredictable UI state.

3. **`refreshSummary()` called twice on load**: Two calls to `refreshSummary()` in quick succession can result in a race condition where the second response overwrites the first mid-render, or the first response's render is interrupted.

---

## Correctness Properties

Property 1: Bug Condition — Unassigned Students Are Visible

_For any_ hierarchy page context where `isBugCondition_ClassesNotShowing` returns true (all students are unassigned), the fixed Manage Students page SHALL either display the "Unassigned" group as a visible, clearly labelled class node in the hierarchy view, OR display a prompt/banner that guides the admin to regenerate the academic structure so that classes become visible.

**Validates: Requirements 2.1, 2.2**

Property 2: Bug Condition — AI Insights Init Runs Exactly Once

_For any_ page load where `isBugCondition_AiInsightsNotLoading` returns true (the script executes more than once), the fixed `ai-insights.js` SHALL ensure `init()` is called exactly once, resulting in a single `refreshSummary()` call and a single registration of each event listener, so that AI insights render correctly without state corruption.

**Validates: Requirements 2.3, 2.4**

Property 3: Preservation — Existing Hierarchy Renders Correctly

_For any_ hierarchy page context where `isBugCondition_ClassesNotShowing` returns false (students already have `AcademicClass` assignments), the fixed code SHALL produce exactly the same Program → Class → Batch hierarchy response as the original code, preserving all existing class and batch nodes.

**Validates: Requirements 3.1, 3.3, 3.5**

Property 4: Preservation — AI Insights Normal Load Unchanged

_For any_ page load where `isBugCondition_AiInsightsNotLoading` returns false (script included exactly once, no prior initialization), the fixed `ai-insights.js` SHALL produce exactly the same behavior as the original code, preserving all analytics rendering, filter interactions, and realtime connections.

**Validates: Requirements 3.2, 3.4**

---

## Fix Implementation

### Bug 1 — Classes Not Showing in Manage Students

**File**: `src/main/resources/static/js/admin-students-saas.js` (hierarchy rendering logic) and/or the Thymeleaf template / hierarchy view component that consumes the `/api/admin/students-hierarchy` response.

**Specific Changes**:

1. **Render the Unassigned node**: In the front-end hierarchy rendering code, ensure that a class node with `id === 0` or `label === "Unassigned"` is rendered as a visible group rather than filtered out or silently skipped.

2. **Add a "no structure" banner**: When the hierarchy response contains only the Unassigned node (or `totalClasses === 1` and the sole class is Unassigned), display a banner or inline prompt telling the admin that the academic structure has not been generated yet, with a button that calls the `POST /api/admin/grouping/regenerate` endpoint.

3. **No changes to `AdminHierarchyController`**: The back-end already correctly groups unassigned students under key `0` and labels the node "Unassigned". The fix is purely in the front-end rendering layer.

### Bug 2 — AI Insights Not Loading

**File**: `src/main/resources/static/js/ai-insights.js`

**Function**: IIFE bottom — the unconditional `init()` call at line 783.

**Specific Changes**:

1. **Add an idempotency guard**: Before calling `init()`, check a flag on `window` (e.g., `window.smsAiInsightsInitialized`). If the flag is already `true`, skip the call. Set the flag to `true` immediately after calling `init()`.

   ```javascript
   // Replace the bare init() call at the bottom of the IIFE:
   if (!window.smsAiInsightsInitialized) {
       window.smsAiInsightsInitialized = true;
       init();
   }
   ```

2. **No other changes required**: The rest of `ai-insights.js` is correct. The duplicate `<script>` tags described in the bug report are not present in the current `ai-insights.html` (the template already has only one tag each for `api-client.js` and `ai-insights.js`), so the guard is a defensive measure against future regressions.

---

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate each bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

---

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bugs BEFORE implementing the fixes. Confirm or refute the root cause analysis.

**Bug 1 Test Plan**: Call `GET /api/admin/students-hierarchy` with a database state where all students have `academicClass = null`. Assert that the response contains a class node with `id: 0` and `label: "Unassigned"`. Then render the hierarchy view and assert that the Unassigned node is visible in the DOM. Run on unfixed code to observe whether the node is hidden or absent.

**Bug 1 Test Cases**:
1. **All-unassigned state**: Seed DB with students having `academicClass = null`, call the hierarchy endpoint, assert response has `classes[0].id === 0` and `classes[0].label === "Unassigned"` (will pass on back-end; front-end rendering assertion will fail on unfixed code).
2. **Empty DB state**: No students at all — assert `classes` is empty and a "no data" state is shown (edge case, may pass on unfixed code).
3. **Front-end render of Unassigned node**: Given a hierarchy response with only `id: 0`, assert the DOM contains a visible class card (will fail on unfixed front-end code).
4. **Regenerate prompt visibility**: Assert that a "Regenerate Structure" prompt is visible when only the Unassigned node is present (will fail on unfixed code).

**Bug 2 Test Plan**: Manually invoke the `ai-insights.js` IIFE twice in a test harness (or inject the script tag twice). Assert that `init()` is called only once, `refreshSummary()` is called only once, and event listeners are registered only once.

**Bug 2 Test Cases**:
1. **Double-init simulation**: Call the IIFE body twice in a Jest/Vitest test with a mocked `window.smsApi`. Assert `refreshSummary` mock is called exactly once (will fail on unfixed code — called twice).
2. **Event listener duplication**: After two IIFE executions, fire a filter `change` event and assert the API is called once, not twice (will fail on unfixed code).
3. **Single-init baseline**: Call the IIFE once. Assert `init()` runs and `refreshSummary` is called once (should pass on both fixed and unfixed code).
4. **Guard flag check**: After one execution, assert `window.smsAiInsightsInitialized === true` (will fail on unfixed code — flag does not exist).

**Expected Counterexamples**:
- Bug 1: The hierarchy DOM contains no visible class cards even though the API returned an Unassigned node.
- Bug 2: `refreshSummary` is called twice; event listeners fire twice per user interaction.

---

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed code produces the expected behavior.

**Bug 1 Pseudocode:**
```
FOR ALL context WHERE isBugCondition_ClassesNotShowing(context) DO
  result := loadManageStudentsPage_fixed(context)
  ASSERT result.unassignedNodeVisible = true
      OR result.regeneratePromptVisible = true
END FOR
```

**Bug 2 Pseudocode:**
```
FOR ALL page WHERE isBugCondition_AiInsightsNotLoading(page) DO
  result := executeAiInsightsScript_fixed(page)
  ASSERT result.initCallCount = 1
      AND result.refreshSummaryCallCount = 1
      AND result.eventListenerCount = expectedSingleRegistrationCount
END FOR
```

---

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed code produces the same result as the original code.

**Pseudocode:**
```
FOR ALL context WHERE NOT isBugCondition_ClassesNotShowing(context) DO
  ASSERT loadManageStudentsPage_original(context)
       = loadManageStudentsPage_fixed(context)
END FOR

FOR ALL page WHERE NOT isBugCondition_AiInsightsNotLoading(page) DO
  ASSERT executeAiInsightsScript_original(page)
       = executeAiInsightsScript_fixed(page)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because it generates many random hierarchy states and page configurations automatically, catching edge cases that manual tests might miss.

**Preservation Test Cases**:
1. **Existing hierarchy preserved**: Generate random sets of students with valid `AcademicClass` assignments; assert the hierarchy response is identical before and after the fix.
2. **Regenerate button still works**: After the fix, assert that clicking "Regenerate Structure" still calls `POST /api/admin/grouping/regenerate` and refreshes the view.
3. **Drag-and-drop reassignment preserved**: Assert that the reassign endpoint still accepts and processes batch reassignment requests correctly.
4. **AI Insights single-load preserved**: With a single script inclusion, assert that `init()` runs once and all analytics render as before.
5. **Filter change preserved**: Assert that changing a filter on the AI Insights page still triggers exactly one `refreshSummary()` call.

---

### Unit Tests

- Test `classGroupingKey()` returns `0` for students with `academicClass = null`.
- Test `buildClassNode()` with `classNumber = 0` produces a node with `label: "Unassigned"`.
- Test the front-end hierarchy renderer correctly displays a node with `id === 0`.
- Test the idempotency guard: calling the `ai-insights.js` IIFE twice results in `init()` being called once.
- Test `bindEvents()` does not duplicate listeners when called once.

### Property-Based Tests

- Generate random lists of students (mix of assigned and unassigned) and verify the hierarchy response always contains the correct number of class nodes, including the Unassigned node when applicable.
- Generate random page configurations (single vs. duplicate script inclusion) and verify `init()` call count is always exactly 1 after the fix.
- Generate random filter combinations for AI Insights and verify each filter change triggers exactly one API call.

### Integration Tests

- Full admin flow: fresh DB → open Manage Students → see Unassigned prompt → click Regenerate → see full hierarchy.
- Full admin flow: existing structure → open Manage Students → see full Program → Class → Batch tree unchanged.
- Full AI Insights flow: navigate to page → insights load → change filters → insights re-render → no duplicate API calls.
- Drag-and-drop reassignment: move a student to a different batch → hierarchy updates without page reload.
