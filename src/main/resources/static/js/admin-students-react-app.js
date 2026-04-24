import React, { useCallback, useEffect, useMemo, useRef, useState } from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid
} from "https://esm.sh/recharts@2.15.4?deps=react@18.3.1";

const h = React.createElement;
const PAGE_SIZE = 100;
const CLASS_BATCH_COUNT = 4;
const INITIAL_CLASSES_VISIBLE = 200;
const CLASS_PAGE_SIZE = 50;
const CACHE_PREFIX = "sms:hierarchy:cache:";
const CACHE_TTL = 5 * 60 * 1000;

function api(path, options = {}) {
  if (window.smsApi && typeof window.smsApi.request === "function") {
    return window.smsApi.request(path, options);
  }

  const requestOptions = { ...options };
  const headers = { Accept: "application/json", ...(requestOptions.headers || {}) };
  const isFormData = typeof FormData !== "undefined" && requestOptions.body instanceof FormData;

  if (requestOptions.body !== undefined && requestOptions.body !== null && !isFormData && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  return fetch(path, { ...requestOptions, headers }).then(async (response) => {
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json") ? await response.json() : await response.text();

    if (!response.ok) {
      const message = typeof payload === "string"
        ? payload
        : payload?.message || payload?.error || `Request failed (${response.status})`;
      throw new Error(message);
    }

    return payload;
  });
}

function routeFromPath(pathname) {
  const normalized = (pathname || "/").replace(/\/+$/, "") || "/";

  if (normalized === "/admin/students" || normalized === "/admin/students/react" || normalized === "/admin/students/hierarchy") {
    return { type: "dashboard" };
  }

  const classMatch = normalized.match(/^\/classes\/(\d+)$/i);
  if (classMatch) {
    return { type: "class", classNumber: Number(classMatch[1]) };
  }

  const batchMatch = normalized.match(/^\/batches\/(\d+)$/i);
  if (batchMatch) {
    return { type: "batch", batchNumber: Number(batchMatch[1]) };
  }

  return { type: "dashboard" };
}

function navigate(path, replace = false) {
  if (replace) {
    window.history.replaceState({}, "", path);
  } else {
    window.history.pushState({}, "", path);
  }
  window.dispatchEvent(new PopStateEvent("popstate"));
}

function getClassNumber(classItem) {
  const raw = classItem?.number ?? classItem?.classNumber ?? classItem?.classId ?? classItem?.id;
  const value = Number(raw);
  return Number.isFinite(value) ? value : 0;
}

function getBatchNumber(batchItem) {
  const raw = batchItem?.number ?? batchItem?.batchNumber ?? batchItem?.globalNumber ?? batchItem?.id;
  const value = Number(raw);
  return Number.isFinite(value) ? value : 0;
}

function classNumberFromBatch(batchNumber) {
  const value = Number(batchNumber);
  if (!Number.isFinite(value) || value <= 0) {
    return 1;
  }
  return Math.floor((value - 1) / CLASS_BATCH_COUNT) + 1;
}

function formatNumber(value, digits = 0) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return digits > 0 ? (0).toFixed(digits) : "0";
  }
  return digits > 0 ? numeric.toFixed(digits) : Math.round(numeric).toString();
}

function initials(name) {
  const parts = String(name || "Student").trim().split(/\s+/).filter(Boolean);
  return (parts[0]?.[0] || "S") + (parts[1]?.[0] || "");
}

function readCache(signature) {
  try {
    const raw = localStorage.getItem(`${CACHE_PREFIX}${signature}`);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    if (!parsed || !parsed.timestamp || Date.now() - parsed.timestamp > CACHE_TTL) {
      return null;
    }
    return parsed.payload || null;
  } catch (_error) {
    return null;
  }
}

function writeCache(signature, payload) {
  try {
    localStorage.setItem(`${CACHE_PREFIX}${signature}`, JSON.stringify({ timestamp: Date.now(), payload }));
  } catch (_error) {
    // Ignore storage failures.
  }
}

function sortClasses(classes) {
  return [...(classes || [])].sort((left, right) => getClassNumber(left) - getClassNumber(right));
}

function sortBatches(batches) {
  return [...(batches || [])].sort((left, right) => getBatchNumber(left) - getBatchNumber(right));
}

function sortStudents(students) {
  return [...(students || [])].sort((left, right) => {
    const leftEnrollment = String(left?.enrollmentNumber || left?.enrollment || left?.id || "");
    const rightEnrollment = String(right?.enrollmentNumber || right?.enrollment || right?.id || "");
    return leftEnrollment.localeCompare(rightEnrollment, undefined, { numeric: true, sensitivity: "base" });
  });
}

function safeText(value) {
  return String(value ?? "");
}

function classHealthScore(classItem) {
  const analytics = classItem?.analytics || {};
  const direct = Number(analytics.healthScore);
  if (Number.isFinite(direct)) {
    return Math.max(0, Math.min(100, direct));
  }

  const avgMarks = Number(analytics.avgMarks ?? analytics.averageMarks ?? 0);
  const attendance = Number(analytics.attendance ?? analytics.averageAttendance ?? 0);
  const riskStudents = Number(analytics.riskStudents ?? 0);
  const totalStudents = Math.max(1, Number(classItem?.totalStudents ?? 1));
  const score = (avgMarks * 0.55) + (attendance * 0.45) - ((riskStudents / totalStudents) * 20);
  return Math.max(0, Math.min(100, score));
}

function determineStatusBadge(score) {
  if (score >= 85) {
    return { tone: "healthy", icon: "●", label: "Healthy" };
  }
  if (score >= 65) {
    return { tone: "stable", icon: "◐", label: "Stable" };
  }
  return { tone: "warning", icon: "!", label: "Watch" };
}

function buildQuery(route, filters) {
  const params = new URLSearchParams();
  if (filters.course) params.set("course", filters.course);
  if (filters.semester) params.set("semester", filters.semester);
  if (filters.performance) params.set("performance", filters.performance);

  if (route.type === "class") {
    params.set("classNumber", String(route.classNumber));
  }

  if (route.type === "batch") {
    params.set("classNumber", String(classNumberFromBatch(route.batchNumber)));
    params.set("batchNumber", String(route.batchNumber));
  }

  return params;
}

function normalizeHierarchy(payload) {
  const classes = sortClasses(Array.isArray(payload?.classes) ? payload.classes : []);
  const summary = payload?.summary || {};

  const totalClasses = Number(summary.totalClasses);
  const totalBatches = Number(summary.totalBatches);
  const totalStudents = Number(summary.totalStudents);

  return {
    summary: {
      totalClasses: Number.isFinite(totalClasses) ? totalClasses : classes.length,
      totalBatches: Number.isFinite(totalBatches) ? totalBatches : classes.reduce((count, classItem) => count + (Array.isArray(classItem.batches) ? classItem.batches.length : 0), 0),
      totalStudents: Number.isFinite(totalStudents) ? totalStudents : classes.reduce((count, classItem) => count + Number(classItem.totalStudents || 0), 0)
    },
    classes
  };
}

function filterClassesForSearch(classes, search) {
  const query = search.trim().toLowerCase();
  if (!query) {
    return classes;
  }

  return classes
    .map((classItem) => {
      const batches = (classItem.batches || []).filter((batch) => {
        const students = batch.students || [];
        const batchMatches = String(batch.label || batch.batchLabel || batch.number || "").toLowerCase().includes(query);
        const studentMatches = students.some((student) => {
          const name = String(student.name || "").toLowerCase();
          const enrollment = String(student.enrollmentNumber || student.enrollment || student.id || "").toLowerCase();
          return name.includes(query) || enrollment.includes(query);
        });
        return batchMatches || studentMatches;
      });

      const classMatches = String(classItem.label || classItem.number || "").toLowerCase().includes(query)
        || batches.length > 0
        || (classItem.batches || []).some((batch) => String(batch.label || batch.batchLabel || "").toLowerCase().includes(query));

      return classMatches ? { ...classItem, batches } : null;
    })
    .filter(Boolean);
}

function filterStudentsForSearch(students, search) {
  const query = search.trim().toLowerCase();
  if (!query) {
    return students;
  }

  return students.filter((student) => {
    const name = String(student.name || "").toLowerCase();
    const enrollment = String(student.enrollmentNumber || student.enrollment || student.id || "").toLowerCase();
    const email = String(student.email || "").toLowerCase();
    return name.includes(query) || enrollment.includes(query) || email.includes(query);
  });
}

function MetricCard({ label, value, hint, tone }) {
  return h(
    "article",
    { className: `rh-metric ${tone || ""}` },
    h("div", { className: "rh-metric-label" }, label),
    h("div", { className: "rh-metric-value" }, value),
    hint ? h("div", { className: "rh-metric-hint" }, hint) : null
  );
}

function SectionCard({ title, subtitle, action, children, className = "" }) {
  return h(
    "section",
    { className: `rh-section ${className}`.trim() },
    h(
      "div",
      { className: "rh-section-head" },
      h("div", null, h("h2", { className: "rh-section-title" }, title), subtitle ? h("p", { className: "rh-section-subtitle" }, subtitle) : null),
      action || null
    ),
    children
  );
}

function Breadcrumbs({ route, classItem, batchItem, onNavigate }) {
  const items = [
    { label: "Home", path: "/admin/students" },
    { label: "Classes", path: "/admin/students" }
  ];

  if (route.type === "class" && classItem) {
    items.push({ label: `Class ${getClassNumber(classItem)}` });
  }

  if (route.type === "batch" && classItem && batchItem) {
    items.push({ label: `Class ${getClassNumber(classItem)}` });
    items.push({ label: `Batch ${getBatchNumber(batchItem)}` });
  }

  return h(
    "nav",
    { className: "rh-breadcrumbs", "aria-label": "Breadcrumb" },
    items.map((item, index) => {
      if (item.path) {
        return h(
          "button",
          {
            key: `${item.label}-${index}`,
            className: "rh-breadcrumb-link",
            type: "button",
            onClick: () => onNavigate(item.path)
          },
          item.label
        );
      }

      return h("span", { key: `${item.label}-${index}`, className: "rh-breadcrumb-current" }, item.label);
    })
  );
}

function ClassCard({ classItem, onOpenClass, onOpenBatch }) {
  const classNumber = getClassNumber(classItem);
  const analytics = classItem.analytics || {};
  const batches = sortBatches(classItem.batches || []);
  const totalStudents = Number(classItem.totalStudents || 0);
  const avgMarks = Number(analytics.avgMarks ?? analytics.averageMarks ?? 0);
  const attendance = Number(analytics.attendance ?? analytics.averageAttendance ?? 0);
  const score = classHealthScore(classItem);
  const badge = determineStatusBadge(score);
  const radius = 22;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (score / 100) * circumference;

  return h(
    "article",
    {
      className: `rh-class-card ${badge.tone}`,
      onClick: () => onOpenClass(classNumber),
      role: "button",
      tabIndex: 0,
      onKeyDown: (event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onOpenClass(classNumber);
        }
      }
    },
    h(
      "div",
      { className: "rh-class-card-head" },
      h(
        "div",
        null,
        h("div", { className: "rh-class-title" }, `Class ${classNumber}`),
        h("div", { className: "rh-class-meta" }, `${totalStudents} students • ${batches.length} batches`),
        h(
          "div",
          { className: "rh-class-badges" },
          h("span", { className: `rh-status-pill ${badge.tone}` }, `${badge.icon} ${badge.label}`),
          h("span", { className: "rh-status-pill neutral" }, `${formatNumber(score)}% health`)
        )
      ),
      h(
        "div",
        { className: "rh-ring" },
        h(
          "svg",
          { width: 58, height: 58, viewBox: "0 0 58 58", "aria-hidden": "true" },
          h("circle", { cx: 29, cy: 29, r: radius, className: "rh-ring-track" }),
          h("circle", {
            cx: 29,
            cy: 29,
            r: radius,
            className: "rh-ring-progress",
            style: { strokeDasharray: circumference, strokeDashoffset: offset }
          })
        ),
        h("div", { className: "rh-ring-value" }, `${formatNumber(score)}%`)
      )
    ),
    h(
      "div",
      { className: "rh-class-grid" },
      h(MetricCard, { label: "Average Marks", value: formatNumber(avgMarks), hint: "Class-wide average" }),
      h(MetricCard, { label: "Attendance", value: `${formatNumber(attendance)}%`, hint: "Roll call average" }),
      h(MetricCard, { label: "Batches", value: "4", hint: "Fixed distribution" })
    ),
    h(
      "div",
      { className: "rh-batch-preview" },
      batches.map((batch) => {
        const batchNumber = getBatchNumber(batch);
        const studentsCount = Number(batch.studentsCount || batch.totalStudents || (batch.students || []).length || 0);
        return h(
          "button",
          {
            key: String(batchNumber),
            type: "button",
            className: "rh-batch-chip",
            onClick: (event) => {
              event.stopPropagation();
              onOpenBatch(batchNumber);
            }
          },
          h("span", { className: "rh-batch-chip-label" }, `Batch ${batchNumber}`),
          h("span", { className: "rh-batch-chip-sub" }, `${studentsCount} students`)
        );
      })
    )
  );
}

const MemoClassCard = React.memo(ClassCard);

function BatchCard({ batchItem, onOpenBatch }) {
  const batchNumber = getBatchNumber(batchItem);
  const studentsCount = Number(batchItem.studentsCount || batchItem.totalStudents || (batchItem.students || []).length || 0);
  const analytics = batchItem.analytics || {};
  const avgMarks = Number(analytics.avgMarks ?? analytics.averageMarks ?? 0);
  const attendance = Number(analytics.attendance ?? analytics.averageAttendance ?? 0);

  return h(
    "button",
    {
      type: "button",
      className: "rh-batch-card",
      onClick: () => onOpenBatch(batchNumber)
    },
    h("div", { className: "rh-batch-card-label" }, `Batch ${batchNumber}`),
    h("div", { className: "rh-batch-card-count" }, `${studentsCount} students`),
    h(
      "div",
      { className: "rh-batch-card-meta" },
      h("span", null, `Avg ${formatNumber(avgMarks)}`),
      h("span", null, `${formatNumber(attendance)}% attendance`)
    )
  );
}

function StudentRow({ student, onViewProfile, onEdit, onDelete, onUploadPhoto, onRowOpen }) {
  const enrollment = String(student.enrollmentNumber || student.enrollment || student.id || "");
  const initialsValue = initials(student.name);
  const marks = Number(student.marks ?? student.averageMarks ?? 0);
  const attendance = Number(student.attendance ?? 0);
  const band = String(student.performanceBand || student.performance?.status || "average");

  return h(
    "article",
    { className: "rh-student-row", onClick: onRowOpen },
    h(
      "div",
      { className: "rh-student-main" },
      h("div", { className: "rh-avatar" }, initialsValue),
      h(
        "div",
        { className: "rh-student-copy" },
        h("div", { className: "rh-student-name" }, student.name || "Unnamed Student"),
        h("div", { className: "rh-student-meta" }, `${enrollment}${student.email ? ` • ${student.email}` : ""}`),
        h(
          "div",
          { className: "rh-student-tags" },
          h("span", { className: `rh-performance-tag ${band}` }, String(student.performanceBand || band)),
          h("span", { className: "rh-performance-tag neutral" }, `${formatNumber(marks)} marks`),
          h("span", { className: "rh-performance-tag neutral" }, `${formatNumber(attendance)}% attendance`)
        )
      )
    ),
    h(
      "div",
      { className: "rh-student-actions", onClick: (event) => event.stopPropagation() },
      h("button", { type: "button", className: "rh-action-btn", onClick: onViewProfile }, "View Profile"),
      h("button", { type: "button", className: "rh-action-btn", onClick: onEdit }, "Edit Details"),
      h("button", { type: "button", className: "rh-action-btn", onClick: onUploadPhoto }, "Upload Photo"),
      h("button", { type: "button", className: "rh-action-btn danger", onClick: onDelete }, "Delete Student")
    )
  );
}

function DashboardView({ summary, classes, searchQuery, setSearchQuery, filters, setFilters, onOpenClass, onOpenBatch, onRefresh, teachers, timetables, loading }) {
  const [visibleClassCount, setVisibleClassCount] = useState(INITIAL_CLASSES_VISIBLE);
  const sentinelRef = useRef(null);

  const filteredClasses = useMemo(() => filterClassesForSearch(classes, searchQuery), [classes, searchQuery]);
  const visibleClasses = useMemo(
    () => filteredClasses.slice(0, visibleClassCount),
    [filteredClasses, visibleClassCount]
  );
  const hasMoreClasses = visibleClasses.length < filteredClasses.length;

  useEffect(() => {
    setVisibleClassCount(INITIAL_CLASSES_VISIBLE);
  }, [searchQuery, filters.course, filters.semester, filters.performance, classes.length]);

  useEffect(() => {
    if (!hasMoreClasses || !sentinelRef.current) {
      return undefined;
    }

    const observer = new IntersectionObserver((entries) => {
      const [entry] = entries;
      if (entry && entry.isIntersecting) {
        setVisibleClassCount((current) => Math.min(current + CLASS_PAGE_SIZE, filteredClasses.length));
      }
    }, { rootMargin: "180px 0px" });

    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasMoreClasses, filteredClasses.length]);

  return h(
    React.Fragment,
    null,
    h(
      "section",
      { className: "rh-dashboard-controls" },
      h("input", {
        className: "rh-input rh-search",
        type: "search",
        placeholder: "Search classes, batches, or students",
        value: searchQuery,
        onChange: (event) => setSearchQuery(event.target.value)
      }),
      h(
        "select",
        {
          className: "rh-select",
          value: filters.course,
          onChange: (event) => setFilters((prev) => ({ ...prev, course: event.target.value }))
        },
        h("option", { value: "" }, "All Courses"),
        h("option", { value: "B.Tech" }, "B.Tech"),
        h("option", { value: "BBA" }, "BBA"),
        h("option", { value: "MBA" }, "MBA"),
        h("option", { value: "M.Tech" }, "M.Tech")
      ),
      h(
        "select",
        {
          className: "rh-select",
          value: filters.semester,
          onChange: (event) => setFilters((prev) => ({ ...prev, semester: event.target.value }))
        },
        h("option", { value: "" }, "All Semesters"),
        h("option", { value: "Semester 1" }, "Semester 1"),
        h("option", { value: "Semester 2" }, "Semester 2"),
        h("option", { value: "Semester 3" }, "Semester 3"),
        h("option", { value: "Semester 4" }, "Semester 4"),
        h("option", { value: "Semester 5" }, "Semester 5"),
        h("option", { value: "Semester 6" }, "Semester 6"),
        h("option", { value: "Semester 7" }, "Semester 7"),
        h("option", { value: "Semester 8" }, "Semester 8")
      ),
      h(
        "select",
        {
          className: "rh-select",
          value: filters.performance,
          onChange: (event) => setFilters((prev) => ({ ...prev, performance: event.target.value }))
        },
        h("option", { value: "" }, "All Performance"),
        h("option", { value: "excellent" }, "Excellent"),
        h("option", { value: "good" }, "Good"),
        h("option", { value: "average" }, "Average"),
        h("option", { value: "poor" }, "Poor")
      ),
      h("button", { type: "button", className: "rh-button primary", onClick: onRefresh }, "Refresh")
    ),
    h(
      "section",
      { className: "rh-kpi-grid" },
      h(MetricCard, { label: "Total Classes", value: formatNumber(summary.totalClasses), hint: "Sorted numerically" }),
      h(MetricCard, { label: "Total Batches", value: formatNumber(summary.totalBatches), hint: "4 per class" }),
      h(MetricCard, { label: "Total Students", value: formatNumber(summary.totalStudents), hint: "All enrolled students" }),
      h(MetricCard, { label: "Teachers", value: formatNumber(teachers.length), hint: "Roster available" })
    ),
    h(
      SectionCard,
      {
        title: "Classes",
        subtitle: `Structured class cards with ${filteredClasses.length} total result${filteredClasses.length === 1 ? "" : "s"}`,
        className: "rh-panel-classes"
      },
      loading && classes.length === 0
        ? h(
            "div",
            { className: "rh-class-grid-layout rh-skeleton-grid" },
            Array.from({ length: INITIAL_CLASSES_VISIBLE }).map((_, index) =>
              h("article", { key: `skeleton-${index}`, className: "rh-class-card rh-skeleton-card", "aria-hidden": "true" })
            )
          )
        : h(
            React.Fragment,
            null,
            h(
              "div",
              { className: "rh-class-grid-layout" },
              visibleClasses.map((classItem) => h(MemoClassCard, {
                key: String(getClassNumber(classItem)),
                classItem,
                onOpenClass,
                onOpenBatch
              }))
            ),
            hasMoreClasses
              ? h(
                  "div",
                  { className: "rh-pager" },
                  h("button", {
                    type: "button",
                    className: "rh-button",
                    onClick: () => setVisibleClassCount((current) => Math.min(current + CLASS_PAGE_SIZE, filteredClasses.length))
                  }, "Load more classes")
                )
              : null,
            hasMoreClasses ? h("div", { ref: sentinelRef, className: "rh-scroll-sentinel", "aria-hidden": "true" }) : null
          )
    ),
    h(
      "section",
      { className: "rh-side-slab-grid" },
      h(
        SectionCard,
        {
          title: "Teachers assigned",
          subtitle: "Current staff roster and linkage point for future assignments"
        },
        teachers.length
          ? h(
              "div",
              { className: "rh-chip-list" },
              teachers.slice(0, 8).map((teacher) => h("span", { key: teacher.id, className: "rh-chip" }, teacher.name || `Teacher ${teacher.id}`))
            )
          : h("div", { className: "rh-empty-copy" }, "No teacher roster loaded yet.")
      ),
      h(
        SectionCard,
        {
          title: "Timetable",
          subtitle: "Structured timetable entry point"
        },
        timetables.length
          ? h(
              "div",
              { className: "rh-timetable-list" },
              timetables.slice(0, 4).map((item, index) => h(
                "div",
                { key: item.id ?? index, className: "rh-timetable-item" },
                h("strong", null, item.courseName || item.timetableCode || `Timetable ${index + 1}`),
                h("span", null, item.section || item.academicYear || "Linked schedule")
              ))
            )
          : h("div", { className: "rh-empty-copy" }, "Use the timetable workspace to assign class schedules.")
      )
    )
  );
}

function ClassView({ classItem, teachers, timetables, onOpenBatch, onNavigate }) {
  if (!classItem) {
    return h(
      "section",
      { className: "rh-empty-state" },
      h("h2", null, "Class not found"),
      h("p", null, "The selected class could not be loaded."),
      h("button", { type: "button", className: "rh-button primary", onClick: () => onNavigate("/admin/students") }, "Back to classes")
    );
  }

  const classNumber = getClassNumber(classItem);
  const batches = sortBatches(classItem.batches || []);
  const analytics = classItem.analytics || {};
  const trend = Array.isArray(analytics.trend) ? analytics.trend : [];
  const avgMarks = Number(analytics.avgMarks ?? analytics.averageMarks ?? 0);
  const attendance = Number(analytics.attendance ?? analytics.averageAttendance ?? 0);
  const riskStudents = Number(analytics.riskStudents ?? 0);
  const totalStudents = Number(classItem.totalStudents || 0);
  const classHealth = classHealthScore(classItem);
  const topBatch = Number(analytics.topPerformingBatch || 0);
  const lowestAttendanceBatch = Number(analytics.lowestAttendanceBatch || 0);

  return h(
    React.Fragment,
    null,
    h(
      "section",
      { className: "rh-class-hero" },
      h(
        "div",
        { className: "rh-class-hero-main" },
        h("div", { className: "rh-hero-kicker" }, `Class ${classNumber}`),
        h("h1", { className: "rh-hero-title" }, `Class ${classNumber} Overview`),
        h("p", { className: "rh-hero-subtitle" }, "Analytics, teachers, timetable, attendance, and batch navigation in a single route."),
        h(
          "div",
          { className: "rh-hero-actions" },
          h("button", { type: "button", className: "rh-button", onClick: () => onNavigate("/admin/students") }, "Back to Classes"),
          h("button", { type: "button", className: "rh-button primary", onClick: () => onOpenBatch(batches[0] ? getBatchNumber(batches[0]) : ((classNumber - 1) * CLASS_BATCH_COUNT) + 1) }, "Open First Batch")
        )
      ),
      h(
        "div",
        { className: "rh-hero-stats" },
        h(MetricCard, { label: "Total Students", value: formatNumber(totalStudents), hint: "Current class population" }),
        h(MetricCard, { label: "Total Batches", value: formatNumber(batches.length || CLASS_BATCH_COUNT), hint: "Always 4" }),
        h(MetricCard, { label: "Average Marks", value: formatNumber(avgMarks), hint: "Class average" }),
        h(MetricCard, { label: "Attendance", value: `${formatNumber(attendance)}%`, hint: "Average attendance" })
      )
    ),
    h(
      "section",
      { className: "rh-two-column-grid" },
      h(
        SectionCard,
        {
          title: "Analytics Overview",
          subtitle: "Performance trend and class health"
        },
        h(
          "div",
          { className: "rh-analytics-stack" },
          h("div", { className: "rh-analytics-summary" },
            h("span", null, `Health: ${formatNumber(classHealth)}%`),
            h("span", null, `Risk students: ${formatNumber(riskStudents)}`),
            h("span", null, `Top batch: B${topBatch || "-"}`),
            h("span", null, `Lowest attendance: B${lowestAttendanceBatch || "-"}`)
          ),
          h(
            "div",
            { className: "rh-chart-shell" },
            h(
              ResponsiveContainer,
              { width: "100%", height: 240 },
              h(
                LineChart,
                { data: trend },
                h(CartesianGrid, { strokeDasharray: "3 3", stroke: "rgba(148, 163, 184, 0.18)" }),
                h(XAxis, { dataKey: "batch", stroke: "#94a3b8" }),
                h(YAxis, { stroke: "#94a3b8" }),
                h(Tooltip, null),
                h(Line, { type: "monotone", dataKey: "marks", stroke: "#22d3ee", strokeWidth: 2 }),
                h(Line, { type: "monotone", dataKey: "attendance", stroke: "#f59e0b", strokeWidth: 2 })
              )
            )
          )
        )
      ),
      h(
        SectionCard,
        {
          title: "Teachers assigned",
          subtitle: "Structured roster block for class ownership"
        },
        teachers.length
          ? h(
              "div",
              { className: "rh-chip-list" },
              teachers.slice(0, 8).map((teacher) => h("span", { key: teacher.id, className: "rh-chip" }, teacher.name || `Teacher ${teacher.id}`))
            )
          : h("div", { className: "rh-empty-copy" }, "No class-teacher assignment data is currently linked.")
      ),
      h(
        SectionCard,
        {
          title: "Timetable",
          subtitle: "Published timetable entries and schedule shortcuts"
        },
        timetables.length
          ? h(
              "div",
              { className: "rh-timetable-list" },
              timetables.slice(0, 4).map((item, index) => h(
                "div",
                { key: item.id ?? index, className: "rh-timetable-item" },
                h("strong", null, item.courseName || item.timetableCode || `Timetable ${index + 1}`),
                h("span", null, item.section || item.academicYear || "Published schedule")
              ))
            )
          : h("div", { className: "rh-empty-copy" }, "Timetable entries can be managed from the timetable workspace.")
      ),
      h(
        SectionCard,
        {
          title: "Attendance insights",
          subtitle: "Attendance level and risk indicators"
        },
        h(
          "div",
          { className: "rh-insight-grid" },
          h(MetricCard, { label: "Attendance", value: `${formatNumber(attendance)}%`, hint: "Class average" }),
          h(MetricCard, { label: "At Risk", value: formatNumber(riskStudents), hint: "Below threshold" }),
          h(MetricCard, { label: "Health", value: `${formatNumber(classHealth)}%`, hint: "Composite score" })
        )
      )
    ),
    h(
      SectionCard,
      {
        title: "Batch Grid",
        subtitle: "Exactly four batches per class"
      },
      h(
        "div",
        { className: "rh-batch-grid" },
        batches.map((batch) => h(BatchCard, { key: String(getBatchNumber(batch)), batchItem: batch, onOpenBatch }))
      )
    )
  );
}

function BatchView({ classItem, batchItem, onNavigate, onViewProfile, onEdit, onDelete, onUploadPhoto }) {
  const [page, setPage] = useState(1);

  useEffect(() => {
    setPage(1);
  }, [batchItem?.id, batchItem?.batchNumber]);

  if (!classItem || !batchItem) {
    return h(
      "section",
      { className: "rh-empty-state" },
      h("h2", null, "Batch not found"),
      h("p", null, "The selected batch could not be loaded."),
      h("button", { type: "button", className: "rh-button primary", onClick: () => onNavigate("/admin/students") }, "Back to classes")
    );
  }

  const classNumber = getClassNumber(classItem);
  const batchNumber = getBatchNumber(batchItem);
  const students = sortStudents(filterStudentsForSearch(batchItem.students || [], ""));
  const analytics = batchItem.analytics || {};
  const avgMarks = Number(analytics.avgMarks ?? analytics.averageMarks ?? 0);
  const attendance = Number(analytics.attendance ?? analytics.averageAttendance ?? 0);
  const riskStudents = Number(analytics.riskStudents ?? 0);
  const performanceGraph = students.map((student, index) => ({
    batch: String(index + 1),
    marks: Number(student.marks ?? 0),
    attendance: Number(student.attendance ?? 0)
  }));
  const visibleStudents = students.slice(0, page * PAGE_SIZE);
  const hasMore = visibleStudents.length < students.length;

  return h(
    React.Fragment,
    null,
    h(
      "section",
      { className: "rh-class-hero rh-batch-hero" },
      h(
        "div",
        { className: "rh-class-hero-main" },
        h("div", { className: "rh-hero-kicker" }, `Batch ${batchNumber}`),
        h("h1", { className: "rh-hero-title" }, `Batch ${batchNumber}`),
        h("p", { className: "rh-hero-subtitle" }, `Class ${classNumber} • ${students.length} students`),
        h(
          "div",
          { className: "rh-hero-actions" },
          h("button", { type: "button", className: "rh-button", onClick: () => onNavigate(`/classes/${classNumber}`) }, "Back to Class"),
          h("button", { type: "button", className: "rh-button primary", onClick: () => onViewProfile(students[0]?.id || "") }, "Open First Student")
        )
      ),
      h(
        "div",
        { className: "rh-hero-stats" },
        h(MetricCard, { label: "Students", value: formatNumber(students.length), hint: "Batch population" }),
        h(MetricCard, { label: "Average Marks", value: formatNumber(avgMarks), hint: "Batch average" }),
        h(MetricCard, { label: "Attendance", value: `${formatNumber(attendance)}%`, hint: "Attendance rate" }),
        h(MetricCard, { label: "At Risk", value: formatNumber(riskStudents), hint: "Students under pressure" })
      )
    ),
    h(
      "section",
      { className: "rh-two-column-grid" },
      h(
        SectionCard,
        {
          title: "Batch analytics",
          subtitle: "Overview and comparative trend"
        },
        h(
          "div",
          { className: "rh-insight-grid" },
          h(MetricCard, { label: "Avg Marks", value: formatNumber(avgMarks), hint: "Current batch" }),
          h(MetricCard, { label: "Attendance", value: `${formatNumber(attendance)}%`, hint: "Current batch" }),
          h(MetricCard, { label: "Risk", value: formatNumber(riskStudents), hint: "Students below threshold" })
        )
      ),
      h(
        SectionCard,
        {
          title: "Performance graph",
          subtitle: "Student-by-student marks and attendance"
        },
        h(
          "div",
          { className: "rh-chart-shell" },
          h(
            ResponsiveContainer,
            { width: "100%", height: 240 },
            h(
              LineChart,
              { data: performanceGraph },
              h(CartesianGrid, { strokeDasharray: "3 3", stroke: "rgba(148, 163, 184, 0.18)" }),
              h(XAxis, { dataKey: "batch", stroke: "#94a3b8" }),
              h(YAxis, { stroke: "#94a3b8" }),
              h(Tooltip, null),
              h(Line, { type: "monotone", dataKey: "marks", stroke: "#22d3ee", strokeWidth: 2 }),
              h(Line, { type: "monotone", dataKey: "attendance", stroke: "#f59e0b", strokeWidth: 2 })
            )
          )
        )
      ),
      h(
        SectionCard,
        {
          title: "Attendance stats",
          subtitle: "Current batch attendance and performance split"
        },
        h(
          "div",
          { className: "rh-insight-grid" },
          h(MetricCard, { label: "Attendance", value: `${formatNumber(attendance)}%`, hint: "Average attendance" }),
          h(MetricCard, { label: "Present Today", value: formatNumber(analytics.presentToday ?? 0), hint: "Snapshot value" }),
          h(MetricCard, { label: "Total Students", value: formatNumber(students.length), hint: "Batch headcount" })
        )
      )
    ),
    h(
      SectionCard,
      {
        title: "Student list",
        subtitle: `Sorted by enrollment number • ${visibleStudents.length}/${students.length} shown`
      },
      h(
        "div",
        { className: "rh-student-list" },
        visibleStudents.map((student) => h(StudentRow, {
          key: String(student.id),
          student,
          onViewProfile: () => onViewProfile(student.id),
          onEdit: () => onEdit(student.id),
          onDelete: () => onDelete(student.id),
          onUploadPhoto: () => onUploadPhoto(student.id),
          onRowOpen: () => onViewProfile(student.id)
        }))
      ),
      hasMore
        ? h(
            "div",
            { className: "rh-pager" },
            h("button", { type: "button", className: "rh-button", onClick: () => setPage((current) => current + 1) }, "Load more students")
          )
        : null
    )
  );
}

function App() {
  const [route, setRoute] = useState(() => routeFromPath(window.location.pathname));
  const [data, setData] = useState({ summary: { totalClasses: 0, totalBatches: 0, totalStudents: 0 }, classes: [] });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filters, setFilters] = useState({ course: "", semester: "", performance: "" });
  const [searchQuery, setSearchQuery] = useState("");
  const [teachers, setTeachers] = useState([]);
  const [timetables, setTimetables] = useState([]);
  const [refreshTick, setRefreshTick] = useState(0);
  const [pendingUploadStudentId, setPendingUploadStudentId] = useState("");
  const uploadInputRef = useRef(null);

  useEffect(() => {
    const onPopState = () => setRoute(routeFromPath(window.location.pathname));
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [route.type, route.classNumber, route.batchNumber]);

  useEffect(() => {
    let active = true;
    api("/api/admin/teachers")
      .then((payload) => {
        if (active) {
          setTeachers(Array.isArray(payload) ? payload : []);
        }
      })
      .catch(() => {
        if (active) {
          setTeachers([]);
        }
      });

    api("/api/admin/timetables")
      .then((payload) => {
        if (active) {
          setTimetables(Array.isArray(payload) ? payload : []);
        }
      })
      .catch(() => {
        if (active) {
          setTimetables([]);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const querySignature = useMemo(() => {
    const query = buildQuery(route, filters).toString();
    return `${route.type}:${route.classNumber || ""}:${route.batchNumber || ""}:${query}:${refreshTick}`;
  }, [route, filters, refreshTick]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    const params = buildQuery(route, filters);
    const cacheKey = params.toString() || "dashboard";
    const useCache = refreshTick === 0;
    const cached = useCache ? readCache(cacheKey) : null;

    if (cached) {
      setData(normalizeHierarchy(cached));
      setLoading(false);
      return () => {
        active = false;
      };
    }

    api(`/api/admin/students-hierarchy?${params.toString()}`)
      .then((payload) => {
        if (!active) {
          return;
        }
        const normalized = normalizeHierarchy(payload);
        setData(normalized);
        writeCache(cacheKey, payload);
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.message || "Failed to load hierarchy");
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [querySignature]);

  const classes = data.classes || [];
  const currentClass = route.type === "class"
    ? classes.find((classItem) => getClassNumber(classItem) === route.classNumber)
    : route.type === "batch"
      ? classes.find((classItem) => getClassNumber(classItem) === classNumberFromBatch(route.batchNumber))
      : null;

  const currentBatch = currentClass && route.type === "batch"
    ? (currentClass.batches || []).find((batchItem) => getBatchNumber(batchItem) === route.batchNumber)
    : null;

  const summary = data.summary || { totalClasses: 0, totalBatches: 0, totalStudents: 0 };

  const refresh = useCallback(() => setRefreshTick((current) => current + 1), []);

  const openClass = useCallback((classNumber) => navigate(`/classes/${classNumber}`), []);
  const openBatch = useCallback((batchNumber) => navigate(`/batches/${batchNumber}`), []);
  const openProfile = useCallback((studentId) => {
    if (!studentId) {
      return;
    }
    window.location.href = `/students/${encodeURIComponent(studentId)}`;
  }, []);
  const editStudent = useCallback((studentId) => {
    if (!studentId) {
      return;
    }
    window.location.href = `/students/${encodeURIComponent(studentId)}?edit=1`;
  }, []);
  const deleteStudent = useCallback(async (studentId) => {
    if (!studentId) {
      return;
    }

    const confirmed = window.confirm(`Delete student ${studentId}? This cannot be undone.`);
    if (!confirmed) {
      return;
    }

    await api(`/api/admin/students/${encodeURIComponent(studentId)}`, { method: "DELETE" });
    refresh();
  }, [refresh]);
  const uploadPhoto = useCallback((studentId) => {
    setPendingUploadStudentId(studentId);
    if (uploadInputRef.current) {
      uploadInputRef.current.value = "";
      uploadInputRef.current.click();
    }
  }, []);
  const handleUploadChange = useCallback(async (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file || !pendingUploadStudentId) {
      return;
    }

    try {
      const formData = new FormData();
      formData.append("studentId", pendingUploadStudentId);
      formData.append("file", file);
      await api("/api/admin/upload-face", { method: "POST", body: formData });
      refresh();
    } catch (uploadError) {
      setError(uploadError?.message || "Failed to upload image");
    } finally {
      setPendingUploadStudentId("");
      event.target.value = "";
    }
  }, [pendingUploadStudentId, refresh]);

  return h(
    "div",
    { className: "rh-app-shell" },
    h(
      "header",
      { className: "rh-topbar" },
      h(
        "div",
        { className: "rh-title-block" },
        h("div", { className: "rh-kicker" }, "Student Identity Workspace"),
        h("h1", { className: "rh-title" }, route.type === "class" ? `Class ${route.classNumber}` : route.type === "batch" ? `Batch ${route.batchNumber}` : "Class → Batch → Student"),
        h("p", { className: "rh-subtitle" }, "Routed hierarchy navigation with sorted classes, fixed batch mapping, and full-page transitions.")
      ),
      h(
        "div",
        { className: "rh-topbar-actions" },
        h("button", { type: "button", className: "rh-button", onClick: refresh }, "Refresh"),
        h("button", { type: "button", className: "rh-button primary", onClick: () => navigate("/admin/students") }, "Dashboard")
      )
    ),
    h(Breadcrumbs, {
      route,
      classItem: currentClass,
      batchItem: currentBatch,
      onNavigate: navigate
    }),
    h(
      "main",
      { className: "rh-main" },
      h(
        "div",
        { className: "rh-main-inner" },
        error ? h("div", { className: "rh-error" }, error) : null,
        loading ? h("div", { className: "rh-loading" }, "Loading hierarchy...") : null,
        route.type === "dashboard"
          ? h(DashboardView, {
              summary,
              classes,
              searchQuery,
              setSearchQuery,
              filters,
              setFilters,
              onOpenClass: openClass,
              onOpenBatch: openBatch,
              onRefresh: refresh,
              teachers,
              timetables,
              loading
            })
          : route.type === "class"
            ? h(ClassView, {
                classItem: currentClass,
                teachers,
                timetables,
                onOpenBatch: openBatch,
                onNavigate: navigate
              })
            : h(BatchView, {
                classItem: currentClass,
                batchItem: currentBatch,
                onNavigate: navigate,
                onViewProfile: openProfile,
                onEdit: editStudent,
                onDelete: deleteStudent,
                onUploadPhoto: uploadPhoto
              })
      )
    ),
    h("input", {
      ref: uploadInputRef,
      type: "file",
      accept: "image/*",
      hidden: true,
      onChange: handleUploadChange
    })
  );
}

const rootElement = document.getElementById("reactHierarchyRoot");
if (rootElement) {
  createRoot(rootElement).render(h(App));
}
