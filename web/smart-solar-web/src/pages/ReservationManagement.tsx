/*
  File Name   : ReservationManagement.tsx
  Description : Back Office – Reservations management UI.
                All data, counts, filters and pagination are driven by the C# backend.
                No hardcoded values. Reuses the operator/reservations endpoints which
                return data across all hubs/stations.
*/
import { FormEvent, useEffect, useRef, useState } from 'react';
import {
  CalendarCheck2,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Zap,
  XCircle,
  Check,
  AlertCircle,
} from 'lucide-react';
import {
  fetchBackOfficeReservations,
  approveBackOfficeReservation,
  type BackOfficeReservationFilters,
  type BackOfficeReservationPage,
  type BackOfficeReservationItem,
} from '@/lib/api';

// ─── Constants ───────────────────────────────────────────────────────────────
const TABS = [
  { key: 'all', label: 'All Reservations' },
  { key: 'pending', label: 'Pending' },
  { key: 'approved', label: 'Approved' },
  { key: 'completed', label: 'Completed' },
  { key: 'history', label: 'Booking History' },
  { key: 'search', label: 'Search & Filter' },
] as const;

type TabKey = (typeof TABS)[number]['key'];

const EMPTY_FILTERS: BackOfficeReservationFilters = {
  reservationId: '',
  prosumerNic: '',
  station: '',
  bookingDate: '',
  status: '',
};

// ─── Status helpers ───────────────────────────────────────────────────────────
const STATUS_CONFIG: Record<string, { label: string; dot: string; bg: string; text: string }> = {
  Pending:   { label: 'Pending',   dot: 'bg-amber-400',  bg: 'bg-amber-50',  text: 'text-amber-700'  },
  Approved:  { label: 'Approved',  dot: 'bg-emerald-400', bg: 'bg-emerald-50', text: 'text-emerald-700' },
  Completed: { label: 'Completed', dot: 'bg-indigo-400',  bg: 'bg-indigo-50',  text: 'text-indigo-700'  },
  Cancelled: { label: 'Cancelled', dot: 'bg-rose-400',    bg: 'bg-rose-50',    text: 'text-rose-700'    },
};

function StatusPill({ status }: { status: string }) {
  const cfg = STATUS_CONFIG[status] ?? { label: status, dot: 'bg-gray-400', bg: 'bg-gray-100', text: 'text-gray-600' };
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${cfg.bg} ${cfg.text}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  );
}

// ─── Metric card ─────────────────────────────────────────────────────────────
function MetricCard({ label, value, Icon, iconBg, iconColor, loading }: {
  label: string; value: number | undefined; Icon: React.ElementType;
  iconBg: string; iconColor: string; loading: boolean;
}) {
  return (
    <div className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm transition-shadow hover:shadow-md">
      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${iconBg}`}>
        <Icon className={`h-5 w-5 ${iconColor}`} />
      </div>
      <div className="min-w-0">
        <p className="truncate text-xs font-medium text-gray-500">{label}</p>
        <p className="mt-0.5 text-2xl font-bold tabular-nums text-gray-900">
          {loading || value == null
            ? <span className="inline-block h-7 w-10 animate-pulse rounded-md bg-gray-200" />
            : value.toLocaleString()}
        </p>
      </div>
    </div>
  );
}

// ─── Expanded row detail ──────────────────────────────────────────────────────
function ExpandedRow({ row, onClose, onApprove, approving }: {
  row: BackOfficeReservationItem;
  onClose: () => void;
  onApprove: (id: string) => void;
  approving: string | null;
}) {
  return (
    <tr className="bg-indigo-50/40">
      <td colSpan={7} className="px-6 py-4">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="grid grid-cols-2 gap-x-8 gap-y-2 text-sm sm:grid-cols-4">
            <div>
              <div className="text-xs text-gray-400">Reservation ID</div>
              <div className="mt-0.5 font-mono text-xs text-gray-700 break-all">{row.reservationId}</div>
            </div>
            <div>
              <div className="text-xs text-gray-400">Slot ID</div>
              <div className="mt-0.5 font-mono text-xs text-gray-700 break-all">{row.slotId}</div>
            </div>
            <div>
              <div className="text-xs text-gray-400">Created</div>
              <div className="mt-0.5 text-gray-700">{new Date(row.createdAt).toLocaleString()}</div>
            </div>
            <div>
              <div className="text-xs text-gray-400">Last updated</div>
              <div className="mt-0.5 text-gray-700">{new Date(row.updatedAt).toLocaleString()}</div>
            </div>
            {row.completedAt && (
              <div>
                <div className="text-xs text-gray-400">Completed at</div>
                <div className="mt-0.5 text-gray-700">{new Date(row.completedAt).toLocaleString()}</div>
              </div>
            )}
            {row.cancellationReason && (
              <div className="col-span-2">
                <div className="text-xs text-gray-400">Cancellation reason</div>
                <div className="mt-0.5 text-gray-700 italic">{row.cancellationReason}</div>
              </div>
            )}
          </div>
          <div className="flex items-center gap-2">
            {row.canApprove && (
              <button
                disabled={!!approving}
                onClick={() => onApprove(row.reservationId)}
                className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:opacity-60"
              >
                {approving === row.reservationId
                  ? <RefreshCw className="h-4 w-4 animate-spin" />
                  : <Check className="h-4 w-4" />}
                {approving === row.reservationId ? 'Approving…' : 'Approve'}
              </button>
            )}
            <button
              onClick={onClose}
              className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-medium text-gray-600 transition hover:bg-gray-50"
            >
              <XCircle className="h-4 w-4" /> Close
            </button>
          </div>
        </div>
      </td>
    </tr>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────
export default function ReservationManagement() {
  const [activeTab, setActiveTab] = useState<TabKey>('all');
  const [filters, setFilters] = useState<BackOfficeReservationFilters>(EMPTY_FILTERS);
  const [draft, setDraft] = useState<BackOfficeReservationFilters>(EMPTY_FILTERS);
  const [page, setPage] = useState(1);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState<BackOfficeReservationPage | null>(null);
  const [statusOptions, setStatusOptions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [feedbackType, setFeedbackType] = useState<'success' | 'error'>('success');
  const [approving, setApproving] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    abortRef.current?.abort();
    const ctrl = new AbortController();
    abortRef.current = ctrl;
    setLoading(true);
    setError('');
    fetchBackOfficeReservations(activeTab, filters, page, ctrl.signal)
      .then((result) => {
        if (!ctrl.signal.aborted) {
          setData(result);
          setStatusOptions(result.statusOptions ?? []);
          setExpandedId(null);
        }
      })
      .catch((err) => {
        if (!ctrl.signal.aborted)
          setError(err instanceof Error ? err.message : 'Unable to load reservations. Please retry.');
      })
      .finally(() => { if (!ctrl.signal.aborted) setLoading(false); });
    return () => ctrl.abort();
  }, [activeTab, filters, page, revision]);

  function switchTab(tab: TabKey) { setActiveTab(tab); setPage(1); setFeedback(''); setExpandedId(null); }

  function handleSearch(e: FormEvent) { e.preventDefault(); setFilters({ ...draft }); setPage(1); setExpandedId(null); }

  function clearSearch() { setDraft(EMPTY_FILTERS); setFilters(EMPTY_FILTERS); setPage(1); setExpandedId(null); }

  async function handleApprove(id: string) {
    setApproving(id); setFeedback('');
    try {
      await approveBackOfficeReservation(id);
      setFeedback('Reservation approved successfully.'); setFeedbackType('success');
      setRevision((r) => r + 1);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Approval failed. Please retry.');
      setFeedbackType('error');
    } finally { setApproving(null); }
  }

  function toggleExpand(id: string) { setExpandedId((prev) => (prev === id ? null : id)); }

  const metrics = [
    { label: 'Active Bookings',  value: data?.summary.activeCount,    Icon: Zap,          iconBg: 'bg-amber-50',   iconColor: 'text-amber-500'   },
    { label: 'Pending',          value: data?.summary.pendingCount,   Icon: Clock3,        iconBg: 'bg-sky-50',     iconColor: 'text-sky-500'     },
    { label: 'Approved',         value: data?.summary.approvedCount,  Icon: CalendarDays,  iconBg: 'bg-emerald-50', iconColor: 'text-emerald-500' },
    { label: 'Completed',        value: data?.summary.completedCount, Icon: CheckCircle2,  iconBg: 'bg-indigo-50',  iconColor: 'text-indigo-500'  },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-amber-500">Back Office / Reservations</p>
          <h2 className="mt-0.5 text-2xl font-bold text-gray-900">All Hubs &amp; Stations</h2>
          <p className="mt-1 text-sm text-gray-500">Review, approve and track energy reservations across every hub.</p>
        </div>
        <button
          disabled={loading || !!approving}
          onClick={() => setRevision((r) => r + 1)}
          className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 shadow-sm transition hover:bg-gray-50 disabled:opacity-60"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Refresh
        </button>
      </div>

      {/* Metric cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {metrics.map((m) => <MetricCard key={m.label} {...m} loading={loading} />)}
      </div>
      <p className="text-xs text-gray-400">Active bookings = Pending + Approved. All counts cover every hub.</p>

      {/* Panel */}
      <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">

        {/* Tabs */}
        <div className="flex overflow-x-auto border-b border-gray-100 bg-gray-50/50">
          {TABS.map(({ key, label }) => (
            <button
              key={key}
              onClick={() => switchTab(key as TabKey)}
              className={`relative flex shrink-0 items-center gap-2 px-5 py-3.5 text-sm font-medium transition-colors focus:outline-none ${
                activeTab === key ? 'text-amber-700' : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              {key === 'search' && <SlidersHorizontal className="h-3.5 w-3.5" />}
              {label}
              {activeTab === key && <span className="absolute inset-x-0 bottom-0 h-0.5 bg-amber-500" />}
            </button>
          ))}
        </div>

        {/* Search form */}
        {activeTab === 'search' && (
          <form onSubmit={handleSearch} className="grid grid-cols-1 gap-4 border-b border-gray-100 bg-white p-5 sm:grid-cols-2 lg:grid-cols-3">
            <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
              Booking ID (24-char hex)
              <input type="text" maxLength={24} value={draft.reservationId}
                onChange={(e) => setDraft({ ...draft, reservationId: e.target.value })}
                placeholder="e.g. 66a1b2c3d4e5f6a7b8c9d0e1"
                className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 placeholder-gray-300 focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-400/20" />
            </label>
            <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
              Prosumer NIC
              <input type="text" maxLength={32} value={draft.prosumerNic}
                onChange={(e) => setDraft({ ...draft, prosumerNic: e.target.value })}
                placeholder="e.g. 200012345678"
                className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 placeholder-gray-300 focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-400/20" />
            </label>
            <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
              Station name or ID
              <input type="text" maxLength={100} value={draft.station}
                onChange={(e) => setDraft({ ...draft, station: e.target.value })}
                placeholder="e.g. Colombo Hub"
                className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 placeholder-gray-300 focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-400/20" />
            </label>
            <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
              Booking date
              <input type="date" value={draft.bookingDate}
                onChange={(e) => setDraft({ ...draft, bookingDate: e.target.value })}
                className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-400/20" />
            </label>
            <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
              Status
              <select value={draft.status}
                onChange={(e) => setDraft({ ...draft, status: e.target.value })}
                className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-900 focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-400/20">
                <option value="">All statuses</option>
                {statusOptions.map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <div className="flex items-end gap-2">
              <button type="submit" disabled={loading}
                className="inline-flex items-center gap-2 rounded-lg bg-amber-500 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-amber-600 disabled:opacity-60">
                <Search className="h-4 w-4" /> Search
              </button>
              <button type="button" onClick={clearSearch}
                className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-600 transition hover:bg-gray-50">
                Clear
              </button>
            </div>
          </form>
        )}

        {/* Feedback banner */}
        {feedback && (
          <div className={`flex items-center gap-2 px-5 py-3 text-sm font-medium ${feedbackType === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
            {feedbackType === 'success' ? <CheckCircle2 className="h-4 w-4 shrink-0" /> : <AlertCircle className="h-4 w-4 shrink-0" />}
            {feedback}
          </div>
        )}

        {/* Content states */}
        {loading ? (
          <div className="flex flex-col items-center justify-center gap-3 py-20 text-gray-400">
            <RefreshCw className="h-7 w-7 animate-spin text-amber-400" />
            <span className="text-sm">Loading reservations…</span>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center gap-3 py-16 px-6 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-50 text-rose-400">
              <AlertCircle className="h-7 w-7" />
            </div>
            <p className="text-sm font-medium text-gray-700">{error}</p>
            <p className="text-xs text-gray-400">Check your session and connection, then retry.</p>
            <button onClick={() => setRevision((r) => r + 1)}
              className="mt-1 inline-flex items-center gap-2 rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700">
              <RefreshCw className="h-4 w-4" /> Retry
            </button>
          </div>
        ) : !data?.items.length ? (
          <div className="flex flex-col items-center gap-3 py-16 text-center text-gray-400">
            <CalendarCheck2 className="h-10 w-10" />
            <p className="text-sm font-medium text-gray-600">No reservations found</p>
            <p className="text-xs">Try a different tab, adjust your filters, or check back later.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/60 text-left text-xs font-semibold uppercase tracking-wider text-gray-400">
                  <th className="px-5 py-3">Prosumer NIC</th>
                  <th className="px-5 py-3">Station / Slot</th>
                  <th className="px-5 py-3">Booking Date</th>
                  <th className="px-5 py-3">Time Window</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Action</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {data.items.map((row) => (
                  <>
                    <tr
                      key={row.reservationId}
                      className={`cursor-pointer transition-colors hover:bg-amber-50/30 ${expandedId === row.reservationId ? 'bg-amber-50/40' : ''}`}
                      onClick={() => toggleExpand(row.reservationId)}
                    >
                      <td className="px-5 py-4 font-medium text-gray-900">{row.prosumerNic}</td>
                      <td className="px-5 py-4">
                        <div className="font-medium text-gray-900">
                          {row.stationName ?? <span className="italic text-gray-400">Station unavailable</span>}
                        </div>
                        <div className="mt-0.5 text-xs text-gray-400">
                          {row.slotNumber == null ? 'Slot unavailable' : `Slot ${row.slotNumber}`}
                        </div>
                      </td>
                      <td className="px-5 py-4 text-gray-600">{row.bookingDate.slice(0, 10)}</td>
                      <td className="px-5 py-4 text-gray-600">
                        <div>{row.startTime}</div>
                        <div className="text-xs text-gray-400">– {row.endTime}</div>
                      </td>
                      <td className="px-5 py-4">
                        <StatusPill status={row.status} />
                        {row.cancellationReason && <div className="mt-1 text-xs text-rose-400 italic">{row.cancellationReason}</div>}
                      </td>
                      <td className="px-5 py-4">
                        {row.canApprove ? (
                          <button
                            disabled={!!approving}
                            onClick={(e) => { e.stopPropagation(); handleApprove(row.reservationId); }}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:opacity-60"
                          >
                            {approving === row.reservationId ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Check className="h-3.5 w-3.5" />}
                            {approving === row.reservationId ? 'Approving…' : 'Approve'}
                          </button>
                        ) : (
                          <span className="text-xs text-gray-400">—</span>
                        )}
                      </td>
                      <td className="px-5 py-4 text-right text-xs text-gray-300">
                        {expandedId === row.reservationId ? '▲' : '▼'}
                      </td>
                    </tr>
                    {expandedId === row.reservationId && (
                      <ExpandedRow
                        key={`${row.reservationId}-expanded`}
                        row={row}
                        onClose={() => setExpandedId(null)}
                        onApprove={handleApprove}
                        approving={approving}
                      />
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 0 && (
          <div className="flex items-center justify-between border-t border-gray-100 px-5 py-3.5">
            <span className="text-xs text-gray-500">
              {data.totalRecords.toLocaleString()} result{data.totalRecords !== 1 ? 's' : ''} &mdash; Page {data.currentPage} of {Math.max(1, data.totalPages)}
            </span>
            <div className="flex items-center gap-1.5">
              <button
                disabled={loading || page <= 1}
                onClick={() => setPage((p) => p - 1)}
                className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 transition hover:bg-gray-50 disabled:opacity-40"
              >
                <ChevronLeft className="h-3.5 w-3.5" /> Previous
              </button>
              <button
                disabled={loading || page >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 transition hover:bg-gray-50 disabled:opacity-40"
              >
                Next <ChevronRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
