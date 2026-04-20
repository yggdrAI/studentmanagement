import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

export default function ClassesChart({ data, palette }) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 12, right: 8, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="classesGradient" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor={palette.blue} />
            <stop offset="100%" stopColor={palette.cyan} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke="rgba(148, 163, 184, 0.16)" vertical={false} />
        <XAxis dataKey="day" stroke="rgba(148, 163, 184, 0.85)" tickLine={false} axisLine={false} />
        <YAxis stroke="rgba(148, 163, 184, 0.85)" tickLine={false} axisLine={false} width={42} />
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
        <Bar dataKey="classes" radius={[14, 14, 6, 6]} fill="url(#classesGradient)" />
      </BarChart>
    </ResponsiveContainer>
  );
}
