const palette = {
  blue: '#3b82f6',
  indigo: '#6366f1',
  violet: '#8b5cf6',
  green: '#22c55e',
  emerald: '#10b981',
  amber: '#f59e0b',
  rose: '#f43f5e',
  cyan: '#06b6d4'
};

export function createSeedSnapshot() {
  return {
    studentsGrowth: [
      { week: 'W1', students: 4200, teachers: 200 },
      { week: 'W2', students: 4300, teachers: 210 },
      { week: 'W3', students: 4400, teachers: 220 },
      { week: 'W4', students: 4500, teachers: 230 }
    ],
    attendanceTrend: [
      { date: 'Mon', attendance: 84 },
      { date: 'Tue', attendance: 86 },
      { date: 'Wed', attendance: 89 },
      { date: 'Thu', attendance: 87 },
      { date: 'Fri', attendance: 90 },
      { date: 'Sat', attendance: 91 },
      { date: 'Sun', attendance: 88 }
    ],
    classesPerDay: [
      { day: 'Mon', classes: 6 },
      { day: 'Tue', classes: 8 },
      { day: 'Wed', classes: 7 },
      { day: 'Thu', classes: 9 },
      { day: 'Fri', classes: 5 },
      { day: 'Sat', classes: 3 }
    ],
    liveEvents: [
      { id: 1, type: 'growth', title: 'Student intake updated', detail: 'Fresh enrollment data was ingested into the analytics model.', time: 'Just now' },
      { id: 2, type: 'attendance', title: 'Attendance spike detected', detail: 'Friday attendance climbed above the weekly average.', time: '2m ago' },
      { id: 3, type: 'class', title: 'Schedule density normalized', detail: 'Midweek class throughput stabilized after a high-load window.', time: '7m ago' }
    ],
    kpis: {
      totalStudents: 4520,
      activeStudents: 3884,
      highPerformers: 918,
      atRiskStudents: 164
    },
    updatedAt: new Date().toISOString(),
    source: 'seed'
  };
}

export function normalizeSnapshot(payload, previousSnapshot = createSeedSnapshot()) {
  const normalized = createSeedSnapshot();
  const previousGrowth = previousSnapshot.studentsGrowth || [];
  const previousAttendance = previousSnapshot.attendanceTrend || [];
  const previousClasses = previousSnapshot.classesPerDay || [];
  const feedRows = Array.isArray(payload?.recentFeed)
    ? payload.recentFeed
    : Array.isArray(payload?.activityFeed)
      ? payload.activityFeed
      : Array.isArray(payload?.liveEvents)
        ? payload.liveEvents
        : [];

  normalized.studentsGrowth = Array.isArray(payload?.studentsGrowth) && payload.studentsGrowth.length > 0
    ? payload.studentsGrowth.map((row, index) => ({
      week: row.label || row.week || `W${index + 1}`,
      students: Number(row.students ?? row.value ?? 0),
      teachers: Number(row.teachers ?? Math.max(0, Number(row.students ?? 0) * 0.05))
    }))
    : previousGrowth;

  normalized.attendanceTrend = Array.isArray(payload?.attendanceTrend) && payload.attendanceTrend.length > 0
    ? payload.attendanceTrend.map((row, index) => ({
      date: row.date || row.label || `D${index + 1}`,
      attendance: Number(row.value ?? row.attendance ?? 0)
    }))
    : previousAttendance;

  normalized.classesPerDay = Array.isArray(payload?.classesPerDay) && payload.classesPerDay.length > 0
    ? payload.classesPerDay.map((row, index) => ({
      day: row.day || row.label || `D${index + 1}`,
      classes: Number(row.value ?? row.classes ?? 0)
    }))
    : previousClasses;

  normalized.kpis = {
    totalStudents: Number(payload?.metrics?.totalStudents ?? payload?.totalStudents ?? previousSnapshot.kpis?.totalStudents ?? 0),
    activeStudents: Number(payload?.metrics?.activeStudents ?? payload?.activeStudents ?? previousSnapshot.kpis?.activeStudents ?? 0),
    highPerformers: Number(payload?.metrics?.highPerformers ?? payload?.highPerformers ?? previousSnapshot.kpis?.highPerformers ?? 0),
    atRiskStudents: Number(payload?.metrics?.atRiskStudents ?? payload?.atRiskStudents ?? previousSnapshot.kpis?.atRiskStudents ?? 0)
  };

  normalized.liveEvents = buildLiveEvents({ ...payload, recentFeed: feedRows }, previousSnapshot.liveEvents || []);
  normalized.updatedAt = new Date().toISOString();
  normalized.source = payload?.source || 'live';
  return normalized;
}

export function mergeLiveEvent(previousSnapshot, event) {
  const snapshot = previousSnapshot || createSeedSnapshot();
  const feed = Array.isArray(snapshot.liveEvents) ? snapshot.liveEvents.slice() : [];
  const nextEvent = {
    id: event?.id || `${Date.now()}-${feed.length}`,
    type: event?.type || 'update',
    title: event?.title || event?.message || 'Analytics update',
    detail: event?.detail || event?.status || event?.studentId || '',
    time: event?.time || event?.timestamp || 'Just now'
  };

  return {
    ...snapshot,
    liveEvents: [nextEvent, ...feed].slice(0, 6),
    updatedAt: new Date().toISOString(),
    source: 'socket'
  };
}

export function createSimulatedSnapshot(previousSnapshot = createSeedSnapshot()) {
  const growth = (previousSnapshot.studentsGrowth || []).map((row, index) => ({
    week: row.week || `W${index + 1}`,
    students: Math.max(0, Math.round(Number(row.students || 0) + 6 + index)),
    teachers: Math.max(0, Math.round(Number(row.teachers || 0) + (index % 2 === 0 ? 1 : 0)))
  }));

  const attendance = (previousSnapshot.attendanceTrend || []).map((row, index) => ({
    date: row.date || `D${index + 1}`,
    attendance: Math.max(0, Math.min(100, Math.round(Number(row.attendance || 0) + (index % 3 === 0 ? 1 : -1))))
  }));

  const classes = (previousSnapshot.classesPerDay || []).map((row, index) => ({
    day: row.day || `D${index + 1}`,
    classes: Math.max(0, Math.round(Number(row.classes || 0) + (index % 2 === 0 ? 1 : 0)))
  }));

  const totalStudents = growth.reduce((sum, row) => sum + Number(row.students || 0), 0) / Math.max(growth.length, 1);
  const activeStudents = Math.round(totalStudents * 0.86);
  const highPerformers = Math.round(totalStudents * 0.21);
  const atRiskStudents = Math.max(0, Math.round(totalStudents * 0.04));

  return {
    studentsGrowth: growth,
    attendanceTrend: attendance,
    classesPerDay: classes,
    liveEvents: [
      { id: Date.now(), type: 'system', title: 'Live sync refreshed', detail: 'Analytics stream updated with the latest sample.', time: 'Now' },
      ...(previousSnapshot.liveEvents || []).slice(0, 4)
    ],
    kpis: {
      totalStudents: Math.round(totalStudents),
      activeStudents,
      highPerformers,
      atRiskStudents
    },
    updatedAt: new Date().toISOString(),
    source: 'simulated'
  };
}

function buildLiveEvents(payload, previousEvents) {
  const incoming = Array.isArray(payload?.events)
    ? payload.events
    : Array.isArray(payload?.recentFeed)
      ? payload.recentFeed
      : Array.isArray(payload?.activityFeed)
        ? payload.activityFeed
        : Array.isArray(payload?.liveEvents)
          ? payload.liveEvents
          : [];
  if (incoming.length > 0) {
    return incoming.slice(0, 6).map((event, index) => ({
      id: event.id || `${Date.now()}-${index}`,
      type: event.type || 'update',
      title: event.title || event.message || 'Analytics update',
      detail: event.detail || event.status || event.studentId || '',
      time: event.time || event.timestamp || 'Just now'
    }));
  }

  return previousEvents.length > 0 ? previousEvents : createSeedSnapshot().liveEvents;
}

export function getPalette() {
  return palette;
}
