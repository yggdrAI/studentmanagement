import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

export default function AttendanceChart({ data, palette }) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <LineChart data={data} margin={{ top: 12, right: 8, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="attendanceGradient" x1="0" x2="1" y1="0" y2="0">
            <stop offset="0%" stopColor={palette.green} />
            <stop offset="100%" stopColor={palette.cyan} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke="rgba(148, 163, 184, 0.16)" vertical={false} />
        <XAxis dataKey="date" stroke="rgba(148, 163, 184, 0.85)" tickLine={false} axisLine={false} />
        <YAxis stroke="rgba(148, 163, 184, 0.85)" tickLine={false} axisLine={false} width={42} domain={[70, 100]} />
        <Tooltip
          contentStyle={{
            background: 'rgba(8, 15, 30, 0.94)',
            border: '1px solid rgba(148, 163, 184, 0.18)',
            borderRadius: 14,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.28)',
            color: '#f8fafc'
          }}
          labelStyle={{ color: '#cbd5e1' }}
        />
        <Line
          type="monotone"
          dataKey="attendance"
          stroke="url(#attendanceGradient)"
          strokeWidth={3}
          dot={{ r: 4, strokeWidth: 2, fill: '#0f172a' }}
          activeDot={{ r: 7, strokeWidth: 0 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
