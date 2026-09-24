/* Member 4: backend-driven operator dashboard, search and existing approval integration. */
import { FormEvent, useEffect, useState } from 'react';
import { CalendarDays, CheckCircle2, Clock3, RefreshCw, Search, Zap } from 'lucide-react';
import { approveOperatorReservation, getOperatorReservations, ReservationFilters, ReservationPage } from './api';
import './operator.css';
const emptyFilters: ReservationFilters = { reservationId: '', prosumerNic: '', station: '', bookingDate: '', status: '' };
const tabs = [['all', 'All reservations'], ['pending', 'Pending'], ['approved', 'Approved'], ['completed', 'Completed'], ['history', 'Booking history'], ['search', 'Search & filter']];
export default function OperatorReservations() {
  const [view, setView] = useState('all');
  const [filters, setFilters] = useState(emptyFilters);
  const [draft, setDraft] = useState(emptyFilters);
  const [page, setPage] = useState(1);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState<ReservationPage | null>(null);
  const [statusOptions, setStatusOptions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [approving, setApproving] = useState<string | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(''); setData(null);
    getOperatorReservations(view, filters, page, controller.signal).then(result => {
      if (!controller.signal.aborted) { setData(result); setStatusOptions(result.statusOptions); }
    }).catch(err => {
      if (!controller.signal.aborted) setError(err instanceof Error ? err.message : 'Unable to load reservations. Please retry.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [view, filters, page, revision]);
  function changeView(next: string) { setView(next); setPage(1); setFeedback(''); }
  function search(event: FormEvent) { event.preventDefault(); setFilters({ ...draft }); setPage(1); }
  async function approve(id: string) {
    setApproving(id); setFeedback('');
    try { await approveOperatorReservation(id); setFeedback('Reservation approved. Latest records requested.'); setRevision(r => r + 1); }
    catch (err) { setFeedback(err instanceof Error ? err.message : 'Approval failed. Please retry.'); }
    finally { setApproving(null); }
  }
  const metrics = [
    { label: 'Active bookings', value: data?.summary.activeCount, icon: Zap },
    { label: 'Pending reservations', value: data?.summary.pendingCount, icon: Clock3 },
    { label: 'Approved reservations', value: data?.summary.approvedCount, icon: CalendarDays },
    { label: 'Completed reservations', value: data?.summary.completedCount, icon: CheckCircle2 },
  ];
  return <section className="m4-operator">
    <header className="m4-heading"><div><span className="m4-eyebrow">GRID OPERATOR / RESERVATIONS</span><h2>Keep every booking on track</h2><p>Review requests, approve reservations and browse booking history.</p></div>
      <button className="m4-button" disabled={loading || !!approving} onClick={() => setRevision(r => r + 1)}><RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Refresh</button></header>
    <div className="m4-metrics">{metrics.map(({ label, value, icon: Icon }) => <article key={label} className="m4-metric"><Icon size={22} /><p>{label}</p><strong>{loading || value == null ? '--' : value.toLocaleString()}</strong></article>)}</div>
    <p className="m4-caption">Active bookings include Pending and Approved reservations. Counts cover all reservations.</p>
    <div className="m4-panel">
      <nav className="m4-tabs" aria-label="Reservation views">{tabs.map(([key, title]) => <button key={key} aria-pressed={view === key} onClick={() => changeView(key)}>{title}</button>)}</nav>
      {view === 'search' && <form className="m4-filters" onSubmit={search}>
        {([['reservationId', 'Booking ID', 'text'], ['prosumerNic', 'Prosumer NIC', 'text'], ['station', 'Station name or ID', 'text'], ['bookingDate', 'Booking date', 'date']] as const).map(([key, label, type]) => <label key={key}>{label}<input type={type} value={draft[key]} maxLength={key === 'reservationId' ? 24 : key === 'prosumerNic' ? 32 : 100} onChange={e => setDraft({ ...draft, [key]: e.target.value })} /></label>)}
        <label>Status<select value={draft.status} onChange={e => setDraft({ ...draft, status: e.target.value })}><option value="">All statuses</option>{statusOptions.map(s => <option key={s}>{s}</option>)}</select></label>
        <div className="m4-filter-actions"><button className="m4-button" type="submit" disabled={loading}><Search size={16} />Search</button><button className="m4-button m4-secondary" type="button" onClick={() => { setDraft(emptyFilters); setFilters(emptyFilters); setPage(1); }}>Clear filters</button></div>
      </form>}
      {feedback && <p role="status" className="m4-notice">{feedback}</p>}
      {loading ? <div className="m4-state" role="status"><RefreshCw className="animate-spin" />Loading reservations...</div>
        : error ? <div className="m4-state m4-error" role="alert"><p>{error}</p><p>Check your connection and operator session, then retry.</p><button className="m4-button" onClick={() => setRevision(r => r + 1)}>Retry</button></div>
        : !data?.items.length ? <div className="m4-state"><CalendarDays size={32} /><h3>No reservations found</h3><p>Refresh for new bookings or adjust your search filters.</p></div>
        : <div className="m4-table-wrap"><table><thead><tr>{['Booking ID', 'Prosumer NIC', 'Station / slot', 'Date / time', 'Status', 'Action'].map(h => <th key={h} scope="col">{h}</th>)}</tr></thead><tbody>{data.items.map(row => <tr key={row.reservationId}>
          <td className="m4-id">{row.reservationId}</td><td>{row.prosumerNic}</td><td><strong>{row.stationName || 'Station unavailable'}</strong><small>{row.stationId}</small><small>{row.slotNumber == null ? 'Slot unavailable' : `Slot ${row.slotNumber}`}</small></td>
          <td>{row.bookingDate.slice(0, 10)}<small>{row.startTime} - {row.endTime}</small></td>
          <td><span className={`m4-status m4-status-${row.status.toLowerCase()}`}>{row.status}</span>{row.cancellationReason && <small>{row.cancellationReason}</small>}</td>
          <td>{row.canApprove ? <button className="m4-button" disabled={!!approving} onClick={() => approve(row.reservationId)}>{approving === row.reservationId ? 'Approving...' : 'Approve'}</button> : <span className="m4-caption">No action required</span>}</td>
        </tr>)}</tbody></table></div>}
      {data && <footer className="m4-pagination"><span>{data.totalRecords} result(s) | Page {data.currentPage} of {Math.max(1, data.totalPages)}</span><div><button disabled={loading || page <= 1} onClick={() => setPage(p => p - 1)}>Previous</button><button disabled={loading || page >= data.totalPages} onClick={() => setPage(p => p + 1)}>Next</button></div></footer>}
    </div>
  </section>;
}
