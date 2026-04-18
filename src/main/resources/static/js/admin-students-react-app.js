import React, { useEffect, useMemo, useState } from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";
import { motion, AnimatePresence } from "https://esm.sh/framer-motion@11.11.17?deps=react@18.3.1";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid
} from "https://esm.sh/recharts@2.15.4?deps=react@18.3.1";
import { DndContext, useDraggable, useDroppable } from "https://esm.sh/@dnd-kit/core@6.1.0?deps=react@18.3.1";

function api(path, options = {}) {
  return fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  }).then(async (res) => {
    if (!res.ok) {
      const body = await res.text();
      throw new Error(body || `Request failed: ${res.status}`);
    }
    const contentType = res.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      return res.json();
    }
    return null;
  });
}

function performanceColor(band) {
  if (band === "excellent") return "#86efac";
  if (band === "good") return "#67e8f9";
  if (band === "average") return "#fde68a";
  return "#fca5a5";
}

function StudentCard({ student }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: student.id,
    data: {
      type: "student",
      studentId: student.id,
      classNumber: student.classNumber,
      batchNumber: student.batchNumber
    }
  });

  return React.createElement(
    "div",
    {
      ref: setNodeRef,
      className: "rh-student",
      style: {
        opacity: isDragging ? 0.45 : 1,
        transform: isDragging ? "scale(1.02)" : "scale(1)"
      },
      ...attributes,
      ...listeners
    },
    React.createElement(
      "div",
      null,
      React.createElement("div", { className: "rh-student-name" }, student.name),
      React.createElement("div", { className: "rh-student-meta" }, `${student.enrollment} | ${student.email}`),
      React.createElement("div", { className: "rh-actions" },
        React.createElement("button", { className: "rh-btn", type: "button" }, "View Profile"),
        React.createElement("button", { className: "rh-btn", type: "button" }, "Upload Face"),
        React.createElement("button", { className: "rh-btn", type: "button" }, "Edit"),
        React.createElement("button", { className: "rh-btn rh-btn-danger", type: "button" }, "Delete")
      )
    ),
    React.createElement(
      "div",
      { className: "rh-status", style: { borderColor: performanceColor(student.performanceBand), color: performanceColor(student.performanceBand) } },
      `${student.marks} / ${student.attendance}%`
    )
  );
}

function BatchColumn({ batch, onToggle, expanded }) {
  const droppableId = `drop-class-${batch.classNumber}-batch-${batch.number}`;
  const { setNodeRef, isOver } = useDroppable({
    id: droppableId,
    data: { classNumber: batch.classNumber, batchNumber: batch.number }
  });

  return React.createElement(
    motion.div,
    {
      className: `rh-batch rh-batch-${batch.number} ${isOver ? "rh-drop-target" : ""}`,
      ref: setNodeRef,
      layout: true,
      initial: { opacity: 0, y: 8 },
      animate: { opacity: 1, y: 0 }
    },
    React.createElement(
      "div",
      { className: "rh-batch-head" },
      React.createElement(
        "div",
        null,
        React.createElement("strong", null, batch.label),
        React.createElement("div", { className: "rh-student-meta" }, `${batch.studentsCount} students | Avg ${batch.analytics.avgMarks}`)
      ),
      React.createElement("button", { className: "rh-btn", type: "button", onClick: onToggle }, expanded ? "Hide" : "Show")
    ),
    React.createElement(
      AnimatePresence,
      null,
      expanded && React.createElement(
        motion.div,
        {
          className: "rh-students",
          initial: { opacity: 0, height: 0 },
          animate: { opacity: 1, height: "auto" },
          exit: { opacity: 0, height: 0 }
        },
        batch.students.map((student) => React.createElement(StudentCard, { key: student.id, student }))
      )
    )
  );
}

function ClassCard({ cls, expanded, onToggle, expandedBatches, onToggleBatch }) {
  return React.createElement(
    motion.div,
    {
      className: "rh-card",
      initial: { opacity: 0, y: 12 },
      animate: { opacity: 1, y: 0 },
      whileHover: { scale: 1.01 }
    },
    React.createElement(
      "div",
      { className: "rh-class-head" },
      React.createElement(
        "div",
        null,
        React.createElement("h3", { style: { margin: 0 } }, `Class ${cls.number}`),
        React.createElement("div", { className: "rh-subtitle" }, `${cls.totalStudents} Students | ${cls.batches.length} Batches`)
      ),
      React.createElement("button", { className: "rh-btn", type: "button", onClick: onToggle }, expanded ? "Collapse" : "Expand")
    ),
    React.createElement(
      "div",
      { className: "rh-analytics" },
      React.createElement("span", null, `Avg Marks: ${cls.analytics.avgMarks}`),
      React.createElement("span", null, `Attendance: ${cls.analytics.attendance}%`),
      React.createElement("span", null, `Top Batch: B${cls.analytics.topPerformingBatch}`),
      React.createElement("span", null, `Risk: ${cls.analytics.riskStudents}`)
    ),
    React.createElement(
      "div",
      { className: "rh-chart" },
      React.createElement(
        ResponsiveContainer,
        { width: "100%", height: "100%" },
        React.createElement(
          LineChart,
          { data: cls.analytics.trend || [] },
          React.createElement(CartesianGrid, { strokeDasharray: "3 3", stroke: "rgba(148,163,184,0.15)" }),
          React.createElement(XAxis, { dataKey: "batch", stroke: "#94a3b8" }),
          React.createElement(YAxis, { stroke: "#94a3b8" }),
          React.createElement(Tooltip),
          React.createElement(Line, { dataKey: "marks", type: "monotone", stroke: "#22d3ee", strokeWidth: 2 }),
          React.createElement(Line, { dataKey: "attendance", type: "monotone", stroke: "#a78bfa", strokeWidth: 2 })
        )
      )
    ),
    React.createElement(
      AnimatePresence,
      null,
      expanded && React.createElement(
        motion.div,
        {
          className: "rh-batches",
          initial: { opacity: 0, height: 0 },
          animate: { opacity: 1, height: "auto" },
          exit: { opacity: 0, height: 0 }
        },
        cls.batches.map((batch) => React.createElement(BatchColumn, {
          key: batch.id,
          batch,
          expanded: expandedBatches.has(batch.id),
          onToggle: () => onToggleBatch(batch.id)
        }))
      )
    )
  );
}

function App() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState({ summary: { totalClasses: 0, totalBatches: 0, totalStudents: 0 }, classes: [] });
  const [expandedClasses, setExpandedClasses] = useState(new Set());
  const [expandedBatches, setExpandedBatches] = useState(new Set());
  const [filters, setFilters] = useState({ classNumber: "", batchNumber: "", course: "", performance: "", semester: "" });
  const [toast, setToast] = useState("");

  const params = useMemo(() => {
    const p = new URLSearchParams();
    if (filters.classNumber) p.set("classNumber", filters.classNumber);
    if (filters.batchNumber) p.set("batchNumber", filters.batchNumber);
    if (filters.course) p.set("course", filters.course);
    if (filters.performance) p.set("performance", filters.performance);
    if (filters.semester) p.set("semester", filters.semester);
    return p;
  }, [filters]);

  const load = () => {
    setLoading(true);
    api(`/api/admin/students-hierarchy?${params.toString()}`)
      .then((payload) => setData(payload))
      .catch((err) => {
        setToast(`Load failed: ${err.message}`);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [params.toString()]);

  useEffect(() => {
    if (!toast) return undefined;
    const t = setTimeout(() => setToast(""), 2800);
    return () => clearTimeout(t);
  }, [toast]);

  const toggleClass = (classId) => {
    setExpandedClasses((prev) => {
      const next = new Set(prev);
      if (next.has(classId)) next.delete(classId); else next.add(classId);
      return next;
    });
  };

  const toggleBatch = (batchId) => {
    setExpandedBatches((prev) => {
      const next = new Set(prev);
      if (next.has(batchId)) next.delete(batchId); else next.add(batchId);
      return next;
    });
  };

  const applyAiGrouping = () => {
    api("/api/admin/students-hierarchy/ai-grouping", {
      method: "POST",
      body: JSON.stringify({
        classNumber: filters.classNumber ? Number(filters.classNumber) : null,
        course: filters.course || null,
        semester: filters.semester || null,
        clusters: 4
      })
    }).then((payload) => {
      const changed = (payload.suggestions || []).filter((row) => row.changed).length;
      setToast(`AI suggestions ready: ${changed} students suggested for reassignment`);
    }).catch((err) => setToast(`AI grouping failed: ${err.message}`));
  };

  const onDragEnd = (event) => {
    const { active, over } = event;
    if (!active || !over) return;

    const activeData = active.data.current;
    const overData = over.data.current;
    if (!activeData || !overData || activeData.type !== "student") return;

    if (activeData.classNumber === overData.classNumber && activeData.batchNumber === overData.batchNumber) {
      return;
    }

    api("/api/admin/students-hierarchy/reassign", {
      method: "POST",
      body: JSON.stringify({
        studentId: activeData.studentId,
        classNumber: overData.classNumber,
        batchNumber: overData.batchNumber
      })
    }).then(() => {
      setToast(`Student moved to Class ${overData.classNumber}, Batch ${overData.batchNumber}`);
      load();
    }).catch((err) => setToast(`Reassign failed: ${err.message}`));
  };

  return React.createElement(
    DndContext,
    { onDragEnd },
    React.createElement(
      "div",
      { className: "react-hierarchy-shell" },
      React.createElement(
        "header",
        { className: "rh-topbar" },
        React.createElement(
          "div",
          null,
          React.createElement("h1", { className: "rh-title" }, "Structured Academic Hierarchy Dashboard"),
          React.createElement("p", { className: "rh-subtitle" }, "Class -> Batch -> Student with motion, analytics, AI grouping, and drag-drop reassignment")
        ),
        React.createElement(
          "div",
          { className: "rh-controls" },
          React.createElement("input", {
            placeholder: "Class",
            value: filters.classNumber,
            onChange: (e) => setFilters((prev) => ({ ...prev, classNumber: e.target.value }))
          }),
          React.createElement("input", {
            placeholder: "Batch",
            value: filters.batchNumber,
            onChange: (e) => setFilters((prev) => ({ ...prev, batchNumber: e.target.value }))
          }),
          React.createElement("input", {
            placeholder: "Course",
            value: filters.course,
            onChange: (e) => setFilters((prev) => ({ ...prev, course: e.target.value }))
          }),
          React.createElement("select", {
            value: filters.performance,
            onChange: (e) => setFilters((prev) => ({ ...prev, performance: e.target.value }))
          },
          React.createElement("option", { value: "" }, "Performance"),
          React.createElement("option", { value: "excellent" }, "Excellent"),
          React.createElement("option", { value: "good" }, "Good"),
          React.createElement("option", { value: "average" }, "Average"),
          React.createElement("option", { value: "poor" }, "Poor")
          ),
          React.createElement("button", { type: "button", onClick: applyAiGrouping }, "AI Grouping")
        )
      ),

      React.createElement(
        "section",
        { className: "rh-kpis" },
        React.createElement("div", { className: "rh-kpi" }, React.createElement("div", { className: "rh-kpi-label" }, "Total Classes"), React.createElement("div", { className: "rh-kpi-value" }, data.summary.totalClasses || 0)),
        React.createElement("div", { className: "rh-kpi" }, React.createElement("div", { className: "rh-kpi-label" }, "Total Batches"), React.createElement("div", { className: "rh-kpi-value" }, data.summary.totalBatches || 0)),
        React.createElement("div", { className: "rh-kpi" }, React.createElement("div", { className: "rh-kpi-label" }, "Total Students"), React.createElement("div", { className: "rh-kpi-value" }, data.summary.totalStudents || 0)),
        React.createElement("div", { className: "rh-kpi" }, React.createElement("div", { className: "rh-kpi-label" }, "Mode"), React.createElement("div", { className: "rh-kpi-value", style: { fontSize: 16, marginTop: 8 } }, "Drag to Reassign"))
      ),

      loading
        ? React.createElement("div", { className: "rh-subtitle" }, "Loading hierarchy...")
        : React.createElement(
            "section",
            { className: "rh-class-grid" },
            (data.classes || []).map((cls) => React.createElement(ClassCard, {
              key: cls.id,
              cls,
              expanded: expandedClasses.has(cls.id),
              expandedBatches,
              onToggle: () => toggleClass(cls.id),
              onToggleBatch: toggleBatch
            }))
          ),

      toast ? React.createElement("div", { className: "rh-toast" }, toast) : null
    )
  );
}

const rootElement = document.getElementById("reactHierarchyRoot");
if (rootElement) {
  createRoot(rootElement).render(React.createElement(App));
}
