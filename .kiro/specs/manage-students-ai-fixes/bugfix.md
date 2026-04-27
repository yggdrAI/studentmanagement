# Bugfix Requirements Document

## Introduction

Two bugs affect the admin panel:

1. **Classes not showing in the "Manage Students" window** — The hierarchy view (`/admin/students`) renders a Program → Class → Batch tree. When no academic structure has been generated (i.e., the `AcademicProgram`, `AcademicClass`, and `AcademicBatch` tables are empty), the program cards section shows nothing and the class tree is empty. Students without `AcademicClass`/`AcademicBatch` assignments are silently bucketed under an "Unassigned" group (class key `0`) rather than being surfaced as a visible class. The result is that the admin sees no classes at all in the Manage Students window.

2. **AI insights not loading** — The `ai-insights.html` template includes both `api-client.js` and `ai-insights.js` twice each (duplicate `<script>` tags). Because `api-client.js` has an early-return guard (`if (window.smsApi) return`), the second load is a no-op. However, `ai-insights.js` has no such guard, so its IIFE executes twice. The second execution re-runs `init()`, which calls `refreshSummary()` a second time and re-registers all event listeners, causing double API calls, duplicate event handlers, and potential state corruption that prevents the insights from rendering correctly.

---

## Bug Analysis

### Current Behavior (Defect)

**Bug 1 — Classes not showing in Manage Students:**

1.1 WHEN the admin opens the Manage Students window and no academic structure has been regenerated (AcademicProgram/Class/Batch tables are empty) THEN the system shows an empty program cards section and an empty class tree with no classes visible

1.2 WHEN students exist in the database but have no `AcademicClass` assignment THEN the system groups them under an internal "Unassigned" bucket (class key `0`) that is not clearly surfaced as a usable class in the hierarchy view

**Bug 2 — AI insights not loading:**

1.3 WHEN the admin navigates to the AI Insights page THEN the system loads `api-client.js` and `ai-insights.js` twice each due to duplicate `<script>` tags in `ai-insights.html`

1.4 WHEN `ai-insights.js` executes a second time (due to the duplicate script tag) THEN the system calls `init()` again, which re-calls `refreshSummary()` and re-registers all DOM event listeners, causing double API requests and duplicate event handlers that corrupt the insights rendering state

### Expected Behavior (Correct)

**Bug 1 — Classes not showing in Manage Students:**

2.1 WHEN the admin opens the Manage Students window and no academic structure has been regenerated THEN the system SHALL display a clear prompt or auto-trigger the regeneration so that classes derived from student enrollment numbers become visible

2.2 WHEN students exist but have no `AcademicClass` assignment THEN the system SHALL surface those students in the hierarchy view under a clearly labeled "Unassigned" group so the admin can see and act on them

**Bug 2 — AI insights not loading:**

2.3 WHEN the admin navigates to the AI Insights page THEN the system SHALL load `api-client.js` and `ai-insights.js` exactly once each (no duplicate `<script>` tags)

2.4 WHEN `ai-insights.js` executes THEN the system SHALL call `init()` exactly once, resulting in a single `refreshSummary()` call and a single registration of each event listener, so that AI insights render correctly without state corruption

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the admin has already regenerated the academic structure and classes exist THEN the system SHALL CONTINUE TO display the full Program → Class → Batch hierarchy correctly in the Manage Students window

3.2 WHEN the admin navigates to the AI Insights page with a valid session THEN the system SHALL CONTINUE TO load and display analytics metrics, smart cards, charts, and the activity feed as before

3.3 WHEN the admin uses the "Regenerate Structure" button in the Manage Students window THEN the system SHALL CONTINUE TO regenerate the Program → Class → Batch groupings from enrollment numbers and refresh the hierarchy view

3.4 WHEN the AI Insights page filters (course, semester, section, date range) are changed THEN the system SHALL CONTINUE TO re-fetch and re-render the analytics summary with the updated filters

3.5 WHEN a student is dragged and dropped to a different batch in the hierarchy view THEN the system SHALL CONTINUE TO reassign the student and update the hierarchy without requiring a full page reload

---

## Bug Condition Pseudocode

### Bug 1 — Classes not showing

```pascal
FUNCTION isBugCondition_ClassesNotShowing(context)
  INPUT: context of type HierarchyPageContext
  OUTPUT: boolean

  RETURN context.academicProgramTableIsEmpty = true
      OR context.allStudentsHaveNoAcademicClassAssignment = true
END FUNCTION

// Property: Fix Checking
FOR ALL context WHERE isBugCondition_ClassesNotShowing(context) DO
  result ← loadManageStudentsPage'(context)
  ASSERT result.classesVisible = true
      OR result.promptToRegenerateVisible = true
END FOR

// Property: Preservation Checking
FOR ALL context WHERE NOT isBugCondition_ClassesNotShowing(context) DO
  ASSERT loadManageStudentsPage(context) = loadManageStudentsPage'(context)
END FOR
```

### Bug 2 — AI insights not loading

```pascal
FUNCTION isBugCondition_AiInsightsNotLoading(page)
  INPUT: page of type AiInsightsHtmlPage
  OUTPUT: boolean

  RETURN countOccurrences(page, "api-client.js") > 1
      OR countOccurrences(page, "ai-insights.js") > 1
END FUNCTION

// Property: Fix Checking
FOR ALL page WHERE isBugCondition_AiInsightsNotLoading(page) DO
  result ← renderAiInsightsPage'(page)
  ASSERT result.initCallCount = 1
      AND result.refreshSummaryCallCount = 1
      AND result.insightsRendered = true
END FOR

// Property: Preservation Checking
FOR ALL page WHERE NOT isBugCondition_AiInsightsNotLoading(page) DO
  ASSERT renderAiInsightsPage(page) = renderAiInsightsPage'(page)
END FOR
```
