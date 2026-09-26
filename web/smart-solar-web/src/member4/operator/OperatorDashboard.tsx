/* Member 4: operator dashboard – reservation metrics only (no duplication with Reservations tab). */
import { useEffect, useState } from 'react';
import { CalendarDays, CheckCircle2, Clock3, RefreshCw, Zap, Activity, TrendingUp, BarChart3 } from 'lucide-react';
import { fetchHubReservations, type HubReservationPage } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import './operator.css';

const emptyFilters = { reservationId: '', prosumerNic: '', station: '', bookingDate: '', status: '' };

export default function OperatorDashboard() {
  const { profile } = useAuth();
  const [data, setData] = useState<HubReservationPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(''); setData(null);
    fetchHubReservations('all', emptyFilters, 1, controller.signal)
      .then(result => { if (!controller.signal.aborted) setData(result); })
      .catch(err => { if (!controller.signal.aborted) setError(err instanceof Error ? err.message : 'Unable to load metrics.'); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision]);

  const metrics = [
    { label: 'Active Bookings', value: data?.summary.activeCount, icon: Zap, color: 'emerald', gradient: 'from-emerald-500 to-teal-600', bg: 'bg-emerald-50', border: 'border-emerald-100', text: 'text-emerald-700', sub: 'Pending + Approved' },
    { label: 'Pending', value: data?.summary.pendingCount, icon: Clock3, color: 'amber', gradient: 'from-amber-500 to-orange-600', bg: 'bg-amber-50', border: 'border-amber-100', text: 'text-amber-700', sub: 'Awaiting approval' },
    { label: 'Approved', value: data?.summary.approvedCount, icon: CalendarDays, color: 'blue', gradient: 'from-blue-500 to-indigo-600', bg: 'bg-blue-50', border: 'border-blue-100', text: 'text-blue-700', sub: 'Ready for service' },
    { label: 'Completed', value: data?.summary.completedCount, icon: CheckCircle2, color: 'violet', gradient: 'from-violet-500 to-purple-600', bg: 'bg-violet-50', border: 'border-violet-100', text: 'text-violet-700', sub: 'All time' },
  ];

  const stationName = data?.items?.find((i: any) => i.stationName)?.stationName;

  return (
    <section className="m4-dashboard">
      {/* Welcome banner */}
      <div className="m4-dash-banner m4-anim-fade-down">
        <div className="m4-dash-banner-content">
          <div className="m4-dash-banner-icon">
            <Activity size={28} />
          </div>
          <div>
            <h2>Welcome back, {profile?.full_name || 'Operator'}</h2>
            <p>Here's your reservation overview for {stationName || 'your assigned hub'}.</p>
          </div>
        </div>
        <button
          className="m4-button m4-dash-refresh"
          disabled={loading}
          onClick={() => setRevision(r => r + 1)}
        >
          <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      {/* Error state */}
      {error && (
        <div className="m4-dash-error m4-anim-fade-up">
          <p>{error}</p>
          <button className="m4-button" onClick={() => setRevision(r => r + 1)}>Retry</button>
        </div>
      )}

      {/* Metric cards */}
      <div className="m4-dash-grid">
        {metrics.map(({ label, value, icon: Icon, gradient, bg, border, text, sub }, index) => (
          <article
            key={label}
            className={`m4-dash-card ${bg} ${border} m4-anim-scale-in`}
            style={{ animationDelay: `${index * 100}ms` }}
          >
            <div className="m4-dash-card-header">
              <div className={`m4-dash-card-badge bg-gradient-to-br ${gradient}`}>
                <Icon size={20} color="white" />
              </div>
              <TrendingUp size={16} className={text} style={{ opacity: 0.5 }} />
            </div>
            <div className="m4-dash-card-value">
              <strong className={text}>
                {loading || value == null ? '--' : value.toLocaleString()}
              </strong>
              <span className="m4-dash-card-label">{label}</span>
            </div>
            <p className="m4-dash-card-sub">{sub}</p>
          </article>
        ))}
      </div>

      {/* Status bar */}
      <div className="m4-dash-status m4-anim-fade-up" style={{ animationDelay: '400ms' }}>
        <div className="m4-dash-status-row">
          <div className="m4-dash-status-indicator">
            <BarChart3 size={18} />
            <span>Reservation Metrics</span>
          </div>
          <div className="m4-dash-status-note">
            {loading ? 'Loading latest data…' : 'Active bookings include Pending and Approved reservations'}
          </div>
        </div>
        {loading && <div className="m4-dash-progress" />}
      </div>

      {/* Grid status card */}
      <div className="m4-dash-grid-status m4-anim-fade-up" style={{ animationDelay: '500ms' }}>
        <div className="m4-dash-grid-status-icon">
          <span className="m4-dash-pulse" />
          <span>✓</span>
        </div>
        <div>
          <h3>Microgrid System Operational</h3>
          <p>All core services are running normally</p>
        </div>
      </div>
    </section>
  );
}
