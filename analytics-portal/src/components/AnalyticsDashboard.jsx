import { AnimatePresence, motion } from 'framer-motion';
import { getPalette } from '../lib/analytics';
import StudentsChart from './StudentsChart';
import AttendanceChart from './AttendanceChart';
import ClassesChart from './ClassesChart';
import TransferStudentDemoButton from "./TransferStudentDemoButton";

function MotionCard({ children, delay = 0, className = '' }) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 22 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45, delay }}
      whileHover={{ y: -4, scale: 1.01 }}
      className={`glass-card ${className}`.trim()}
    >
      {children}
    </motion.section>
  );
}

function MetricCard({ label, value, delta, tone }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
      className={`metric-card metric-${tone}`}
    >
      <span className="metric-label">{label}</span>
      <div className="metric-row">
        <strong>{value}</strong>
        <span>{delta}</span>
      </div>
    </motion.div>
  );
}

export default function AnalyticsDashboard({ snapshot, connectionState, liveSummary }) {
  const palette = getPalette();
  const totalStudents = snapshot.kpis.totalStudents;
  const activeStudents = snapshot.kpis.activeStudents;
  const atRiskStudents = snapshot.kpis.atRiskStudents;
  const highPerformers = snapshot.kpis.highPerformers;
  const latestGrowth = snapshot.studentsGrowth[snapshot.studentsGrowth.length - 1] || {};
  const previousGrowth = snapshot.studentsGrowth[Math.max(snapshot.studentsGrowth.length - 2, 0)] || latestGrowth;
  const growthDelta = Number(latestGrowth.students || 0) - Number(previousGrowth.students || 0);
  const attendanceLatest = snapshot.attendanceTrend[snapshot.attendanceTrend.length - 1] || {};
  const attendanceAverage = snapshot.attendanceTrend.reduce((sum, row) => sum + Number(row.attendance || 0), 0) / Math.max(snapshot.attendanceTrend.length, 1);

  // TEMP: Demo integration for transfer modal (replace with real studentId)
  const demoStudentId = snapshot.studentsList?.[0]?.id || "SAMPLE123";

  return (
    <main className="dashboard-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <motion.header
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.55 }}
        className="hero"
      >
        <div className="hero-copy">
          <div className="eyebrow-row">
            <span className={`live-pill ${connectionState}`}>{connectionState}</span>
            <span className="subtle-pill">Real-time analytics</span>
          </div>
          <h1>Smart Campus Analytics Command Center</h1>
          <p>
            Premium operational visibility for student growth, attendance velocity, and class throughput.
            Live metrics refresh automatically, and the charts stay usable on small screens.
          </p>
        </div>

        <div className="hero-status">
          <div>
            <span className="status-label">Last updated</span>
            <strong>{new Date(liveSummary.lastUpdated).toLocaleTimeString()}</strong>
          </div>
          <div>
            <span className="status-label">Stream source</span>
            <strong>{liveSummary.source}</strong>
          </div>
        </div>
      </motion.header>

      <section className="metrics-grid">
        <MetricCard label="Total Students" value={totalStudents.toLocaleString()} delta={`+${growthDelta.toLocaleString()} this window`} tone="blue" />
        <MetricCard label="Active Students" value={activeStudents.toLocaleString()} delta={`${Math.round((activeStudents / Math.max(totalStudents, 1)) * 100)}% engaged`} tone="green" />
        <MetricCard label="High Performers" value={highPerformers.toLocaleString()} delta="Consistent top-tier progress" tone="violet" />
        <MetricCard label="At Risk" value={atRiskStudents.toLocaleString()} delta="Requires intervention" tone="rose" />
      </section>

      <section className="charts-grid">
        <MotionCard delay={0.05} className="chart-card">
          <div className="card-head">
            <div>
              <span className="card-kicker">Growth</span>
              <h2>Students Growth</h2>
            </div>
            <span className="card-badge">Blue to Violet</span>
          </div>
          <StudentsChart data={snapshot.studentsGrowth} palette={palette} />
        </MotionCard>

        <MotionCard delay={0.1} className="chart-card">
          <div className="card-head">
            <div>
              <span className="card-kicker">Attendance</span>
              <h2>Attendance Trends</h2>
            </div>
            <span className="card-badge">Live benchmark {Math.round(attendanceAverage)}%</span>
          </div>
          <AttendanceChart data={snapshot.attendanceTrend} palette={palette} />
        </MotionCard>

        <MotionCard delay={0.15} className="chart-card">
          <div className="card-head">
            <div>
              <span className="card-kicker">Throughput</span>
              <h2>Classes Per Day</h2>
            </div>
            <span className="card-badge">Weekly density</span>
          </div>
          <ClassesChart data={snapshot.classesPerDay} palette={palette} />
        </MotionCard>
      </section>

      <section className="lower-grid">
        <MotionCard delay={0.2} className="feed-card">
          <div className="card-head">
            <div>
              <span className="card-kicker">Live feed</span>
              <h2>Realtime activity stream</h2>
            </div>
          </div>

          <AnimatePresence initial={false}>
            <motion.ul className="event-list" layout>
              {snapshot.liveEvents.map((event) => (
                <motion.li
                  key={event.id}
                  layout
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.28 }}
                  className={`event-item event-${event.type}`}
                >
                  <span className="event-time">{event.time}</span>
                  <strong>{event.title}</strong>
                  <p>{event.detail}</p>
                </motion.li>
              ))}
            </motion.ul>
          </AnimatePresence>
        </MotionCard>

        <MotionCard delay={0.25} className="insight-card">
          <div className="card-head">
            <div>
              <span className="card-kicker">Snapshot</span>
              <h2>Operational summary</h2>
            </div>
          </div>

          <div className="insight-stack">
            <div className="insight-block">
              <span>Growth lead</span>
              <strong>{latestGrowth.week || 'W4'} at {Number(latestGrowth.students || 0).toLocaleString()} students</strong>
            </div>
            <div className="insight-block">
              <span>Attendance trend</span>
              <strong>{Math.round(attendanceLatest.attendance || 0)}% latest session</strong>
            </div>
            <div className="insight-block">
              <span>Realtime readiness</span>
              <strong>{connectionState === 'live' ? 'Socket ready' : 'Polling fallback active'}</strong>
            </div>
          </div>
          <div style={{ marginTop: 24 }}>
            <TransferStudentDemoButton studentId={demoStudentId} onTransfer={() => window.location.reload()} />
          </div>
        </MotionCard>
      </section>
    </main>
  );
}
