import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

export default function StudentsChart({ data, palette }) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <AreaChart data={data} margin={{ top: 12, right: 8, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="studentsGradient" x1="0" x2="1" y1="0" y2="0">
            <stop offset="0%" stopColor={palette.blue} />
            <stop offset="100%" stopColor={palette.violet} />
          </linearGradient>
          <linearGradient id="studentsFill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor={palette.blue} stopOpacity={0.34} />
            <stop offset="100%" stopColor={palette.violet} stopOpacity={0.04} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke="rgba(148, 163, 184, 0.16)" vertical={false} />
        <XAxis dataKey="week" stroke="rgba(148, 163, 184, 0.85)" tickLine={false} axisLine={false} />
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
        <Area type="monotone" dataKey="students" stroke="url(#studentsGradient)" strokeWidth={3} fill="url(#studentsFill)" />
        <Area type="monotone" dataKey="teachers" stroke={palette.green} strokeWidth={2} fill="transparent" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
