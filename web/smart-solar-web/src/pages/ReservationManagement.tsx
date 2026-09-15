import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { Reservation, Hub, Prosumer, BookingSlot } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import EmptyState from '@/components/ui/EmptyState';
import Input from '@/components/ui/Input';
import QrCodeDisplay from '@/components/ui/QrCodeDisplay';
import { Gauge, Plus, Search, QrCode, Eye, X, Check, AlertTriangle, Clock, Loader2 } from 'lucide-react';

type ReservationForm = {
  prosumer_id: string;
  slot_id: string;
  hub_id: string;
  energy_kwh: string;
  notes: string;
};

const emptyForm: ReservationForm = { prosumer_id: '', slot_id: '', hub_id: '', energy_kwh: '', notes: '' };

export default function ReservationManagement() {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [prosumers, setProsumers] = useState<Prosumer[]>([]);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [addOpen, setAddOpen] = useState(false);
  const [viewRes, setViewRes] = useState<Reservation | null>(null);
  const [cancelRes, setCancelRes] = useState<Reservation | null>(null);
  const [form, setForm] = useState<ReservationForm>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [ruleWarning, setRuleWarning] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [qrResult, setQrResult] = useState<{ transaction_id: string; token: string } | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await ApiService.getReservations();
      const hRes = await ApiService.getHubs();
      const sRes = await ApiService.getBookingSlots();
      setReservations(res || []);
      setHubs(hRes || []);
      setSlots(sRes || []);
    } catch(e) {}
    setLoading(false);
  }, [statusFilter, search]);

  useEffect(() => { load(); }, [load]);

  const checkRules = (startTime: string, action: 'create' | 'update' | 'cancel'): string | null => {
    const now = new Date();
    const start = new Date(startTime);
    const hoursUntil = (start.getTime() - now.getTime()) / (1000 * 60 * 60);
    if (action === 'update' && hoursUntil < 12) return 'Update blocked: reservation starts within 12 hours.';
    if (action === 'cancel' && hoursUntil < 12) return 'Cancellation blocked: reservation starts within 12 hours.';
    if (action === 'create' && hoursUntil < 24) return 'Warning: reservation is less than 24 hours away.';
    return null;
  };

  const openAdd = () => { setForm(emptyForm); setFormError(null); setRuleWarning(null); setAddOpen(true); };

  const handleSave = async () => {
    setFormError(null);
    setRuleWarning(null);
    if (!form.prosumer_id || !form.slot_id || !form.hub_id || !form.energy_kwh) {
      setFormError('All fields are required.');
      return;
    }
    const energy = parseFloat(form.energy_kwh);
    if (energy <= 0) { setFormError('Energy amount must be positive.'); return; }

    const slot = slots.find((s) => s.id === form.slot_id);
    if (!slot) { setFormError('Invalid slot.'); return; }
    const remaining = slot.energy_available_kwh - slot.energy_booked_kwh;
    if (energy > remaining) { setFormError(`Only ${remaining} kWh available for this slot.`); return; }

    const rule = checkRules(slot.start_time, 'create');
    if (rule) setRuleWarning(rule);

    setSaving(true);
    const { error } = await Promise.resolve({data: null, error: null});
    setSaving(false);
    if (error) { setFormError(error.message); return; }
    setAddOpen(false);
    setForm(emptyForm);
    load();
  };

  const confirmReservation = async (r: Reservation) => {
    setConfirming(true);
    await Promise.resolve({data: null, error: null});

    // Generate QR transaction via server API
    const { data, error } = await generateQrTransaction(r.id);
    if (error) {
      setQrResult(null);
    } else if (data) {
      setQrResult({ transaction_id: data.transaction_id, token: data.token });
    }

    setConfirming(false);
    load();
    setViewRes(null);
  };

  const cancelReservation = async (r: Reservation) => {
    const slot = r.slot;
    const rule = slot ? checkRules(slot.start_time, 'cancel') : null;
    if (rule) { setCancelRes(r); return; }
    await Promise.resolve({data: null, error: null});
    load();
    setViewRes(null);
  };

  const completeReservation = async (r: Reservation) => {
    await Promise.resolve({data: null, error: null});
    load();
    setViewRes(null);
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-col gap-3 sm:flex-row">
          <div className="relative flex-1 max-w-sm">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              placeholder="Search by QR token..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-lg border-0 py-2.5 pl-10 pr-3.5 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-lg border-0 py-2.5 pl-3.5 pr-8 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
          >
            <option value="all">All statuses</option>
            <option value="pending">Pending</option>
            <option value="confirmed">Confirmed</option>
            <option value="cancelled">Cancelled</option>
            <option value="completed">Completed</option>
            <option value="no_show">No Show</option>
          </select>
        </div>
        <Button onClick={openAdd}>
          <Plus className="h-4 w-4" />
          New Reservation
        </Button>
      </div>

      <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        {loading ? (
          <div className="py-16 text-center text-sm text-gray-400">Loading...</div>
        ) : reservations.length === 0 ? (
          <EmptyState icon={<Gauge className="h-7 w-7" />} title="No reservations found" description="Create a new reservation or adjust filters." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  <th className="px-6 py-3">Prosumer</th>
                  <th className="px-6 py-3">Hub</th>
                  <th className="px-6 py-3">Energy</th>
                  <th className="px-6 py-3">QR Token</th>
                  <th className="px-6 py-3">Date</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {reservations.map((r) => (
                  <tr key={r.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-medium text-gray-900">{r.prosumer?.full_name ?? 'Unknown'}</div>
                      <div className="text-xs text-gray-400">{r.prosumer?.nic}</div>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{r.hub?.name ?? '—'}</td>
                    <td className="px-6 py-4"><span className="font-semibold text-gray-900">{r.energy_kwh}</span> kWh</td>
                    <td className="px-6 py-4">
                      <code className="rounded bg-gray-100 px-2 py-0.5 text-xs font-mono text-gray-600">{r.qr_token.slice(0, 12)}...</code>
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-500">{new Date(r.created_at).toLocaleDateString()}</td>
                    <td className="px-6 py-4"><StatusBadge status={r.status} /></td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => setViewRes(r)}
                        className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add modal */}
      <Modal
        open={addOpen}
        onClose={() => setAddOpen(false)}
        title="New Reservation"
        description="Book an energy slot for a prosumer."
        footer={
          <>
            <Button variant="secondary" onClick={() => setAddOpen(false)}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Create'}</Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Prosumer *</label>
            <select
              value={form.prosumer_id}
              onChange={(e) => setForm({ ...form, prosumer_id: e.target.value })}
              className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
            >
              <option value="">Select prosumer...</option>
              {prosumers.map((p) => <option key={p.id} value={p.id}>{p.full_name} ({p.nic})</option>)}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Slot *</label>
            <select
              value={form.slot_id}
              onChange={(e) => {
                const slot = slots.find((s) => s.id === e.target.value);
                setForm({ ...form, slot_id: e.target.value, hub_id: slot?.hub_id ?? '' });
              }}
              className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
            >
              <option value="">Select slot...</option>
              {slots.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.hub?.name} - {new Date(s.start_time).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })} ({s.energy_available_kwh - s.energy_booked_kwh} kWh avail)
                </option>
              ))}
            </select>
          </div>
          <Input label="Energy (kWh) *" type="number" step="any" value={form.energy_kwh} onChange={(e) => setForm({ ...form, energy_kwh: e.target.value })} placeholder="5.00" />
          <Input label="Notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} placeholder="Optional notes" />
          {ruleWarning && (
            <div className="flex items-center gap-2 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-700">
              <AlertTriangle className="h-4 w-4 shrink-0" /> {ruleWarning}
            </div>
          )}
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
        </div>
      </Modal>

      {/* View modal */}
      <Modal
        open={!!viewRes}
        onClose={() => setViewRes(null)}
        title="Reservation Details"
        footer={
          viewRes && (
            <div className="flex w-full items-center justify-between">
              <Button variant="secondary" onClick={() => setViewRes(null)}>Close</Button>
              <div className="flex gap-2">
                {viewRes.status === 'pending' && (
                  <Button onClick={() => confirmReservation(viewRes)} disabled={confirming}>
                    {confirming ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
                    {confirming ? 'Confirming...' : 'Confirm & Generate QR'}
                  </Button>
                )}
                {(viewRes.status === 'pending' || viewRes.status === 'confirmed') && (
                  <>
                    <Button variant="secondary" onClick={() => completeReservation(viewRes)}>
                      <Check className="h-4 w-4" /> Complete
                    </Button>
                    <Button variant="danger" onClick={() => cancelReservation(viewRes)}>
                      <X className="h-4 w-4" /> Cancel
                    </Button>
                  </>
                )}
              </div>
            </div>
          )
        }
      >
        {viewRes && (
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-teal-50 text-teal-600">
                <Gauge className="h-7 w-7" />
              </div>
              <div>
                <div className="text-lg font-semibold text-gray-900">{viewRes.prosumer?.full_name}</div>
                <StatusBadge status={viewRes.status} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 rounded-xl bg-gray-50 p-4">
              <div><div className="text-xs text-gray-400">Hub</div><div className="mt-0.5 text-sm text-gray-900">{viewRes.hub?.name}</div></div>
              <div><div className="text-xs text-gray-400">Energy</div><div className="mt-0.5 text-sm text-gray-900">{viewRes.energy_kwh} kWh</div></div>
              <div><div className="text-xs text-gray-400">Created</div><div className="mt-0.5 text-sm text-gray-900">{new Date(viewRes.created_at).toLocaleString()}</div></div>
              {viewRes.slot && <div><div className="text-xs text-gray-400">Slot Start</div><div className="mt-0.5 text-sm text-gray-900">{new Date(viewRes.slot.start_time).toLocaleString()}</div></div>}
              {viewRes.cancelled_at && <div><div className="text-xs text-gray-400">Cancelled</div><div className="mt-0.5 text-sm text-gray-900">{new Date(viewRes.cancelled_at).toLocaleString()}</div></div>}
              {viewRes.completed_at && <div><div className="text-xs text-gray-400">Completed</div><div className="mt-0.5 text-sm text-gray-900">{new Date(viewRes.completed_at).toLocaleString()}</div></div>}
            </div>
            <div className="flex items-center gap-3 rounded-xl border-2 border-dashed border-gray-200 p-4">
              <QrCode className="h-10 w-10 text-gray-300" />
              <div>
                <div className="text-xs text-gray-400">QR Token</div>
                <code className="text-sm font-mono text-gray-700">{viewRes.qr_token}</code>
              </div>
            </div>
            {viewRes.notes && <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-600">{viewRes.notes}</div>}
          </div>
        )}
      </Modal>

      {/* Cancel blocked modal */}
      <Modal
        open={!!cancelRes}
        onClose={() => setCancelRes(null)}
        title="Cancellation Blocked"
        size="sm"
        footer={<Button onClick={() => setCancelRes(null)}>Understood</Button>}
      >
        <div className="flex flex-col items-center text-center">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-50 text-rose-500">
            <Clock className="h-7 w-7" />
          </div>
          <p className="text-sm text-gray-600">
            This reservation starts within 12 hours and cannot be cancelled per the 12-hour cancellation rule.
          </p>
        </div>
      </Modal>

      {/* QR generated modal */}
      <Modal
        open={!!qrResult}
        onClose={() => setQrResult(null)}
        title="QR Transaction Generated"
        description="A secure QR token has been generated by the server"
        size="sm"
        footer={<Button onClick={() => setQrResult(null)}>Done</Button>}
      >
        {qrResult && (
          <div className="flex flex-col items-center space-y-4">
            <QrCodeDisplay value={qrResult.token} size={200} />
            <div className="w-full space-y-2 rounded-xl bg-gray-50 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Transaction ID</span>
                <span className="font-mono text-sm font-semibold text-gray-900">{qrResult.transaction_id}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Token</span>
                <span className="font-mono text-xs text-gray-600">{qrResult.token.slice(0, 20)}...</span>
              </div>
            </div>
            <p className="text-xs text-gray-400 text-center">
              The prosumer can now display this QR code. An operator can scan it to verify and finalize the energy transfer.
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}
