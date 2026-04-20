import AnalyticsDashboard from './components/AnalyticsDashboard';
import { useLiveAnalytics } from './hooks/useLiveAnalytics';

export default function App() {
  const { snapshot, connectionState, liveSummary } = useLiveAnalytics();

  return (
    <AnalyticsDashboard
      snapshot={snapshot}
      connectionState={connectionState}
      liveSummary={liveSummary}
    />
  );
}
