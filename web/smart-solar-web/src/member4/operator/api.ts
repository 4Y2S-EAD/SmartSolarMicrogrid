/* Member 4: operator reservation API, using the shared HTTP client and existing login token. */
import { fetchApi } from '@/lib/api';
export type ReservationFilters = { reservationId: string; prosumerNic: string; station: string; bookingDate: string; status: string };
export type OperatorReservation = {
  reservationId: string; prosumerNic: string; stationId: string; stationName: string | null;
  slotId: string; slotNumber: number | null; bookingDate: string; startTime: string; endTime: string;
  status: string; canApprove: boolean; cancellationReason: string | null; completedAt: string | null;
};
export type ReservationPage = {
  items: OperatorReservation[]; currentPage: number; pageSize: number; totalRecords: number; totalPages: number;
  summary: { activeCount: number; pendingCount: number; approvedCount: number; completedCount: number };
  statusOptions: string[];
};
function authorization(): Record<string, string> {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}
export async function getOperatorReservations(view: string, filters: ReservationFilters, page: number, signal: AbortSignal): Promise<ReservationPage> {
  const suffix = view === 'pending' || view === 'history' ? `/${view}` : view === 'all' ? '' : '/search';
  const params = new URLSearchParams({ page: String(page), pageSize: '20' });
  if (view === 'search') Object.entries(filters).forEach(([key, value]) => { if (value.trim()) params.set(key, value.trim()); });
  if (view === 'approved' || view === 'completed') params.set('status', view === 'approved' ? 'Approved' : 'Completed');
  const result = await fetchApi<ReservationPage>(`/operator/reservations${suffix}?${params}`, { signal, headers: authorization() });
  if (!result || !Array.isArray(result.items) || !result.summary || !Array.isArray(result.statusOptions)
    || !Object.values(result.summary).every(v => typeof v === 'number')
    || ![result.currentPage, result.totalPages, result.totalRecords].every(v => typeof v === 'number')
    || !result.items.every(x => x && typeof x.reservationId === 'string' && typeof x.status === 'string' && typeof x.bookingDate === 'string' && typeof x.canApprove === 'boolean')) {
    throw new Error('The server returned an invalid reservation response. Please retry.');
  }
  return result;
}
export async function approveOperatorReservation(id: string) {
  return fetchApi(`/reservations/${encodeURIComponent(id)}/approve`, { method: 'PUT', headers: authorization() });
}
