import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { BookingSlot, Hub } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { CalendarDays, Plus, Clock, Zap, Pencil, Trash2 } from 'lucide-react';

type SlotForm = {
  hub_id: string;
  start_time: string;
  end_time: string;
  energy_available_kwh: string;
};

const emptyForm: SlotForm = { hub_id: '', start_time: '', end_time: '', energy_available_kwh: '' };

export default function ScheduleManagement() {
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [loading, setLoading] = useState(true);
  const [hubFilter, setHubFilter] = useState('all');
  const [addOpen, setAddOpen] = useState(false);
  const [editSlot, setEditSlot] = useState<BookingSlot | null>(null);
  const [form, setForm] = useState<SlotForm>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const [slotRes, hubRes] = await Promise.all([
      Promise.resolve({data: [], error: null}),
      ApiService.getHubs().eq('status', 'active').order('name'),
    ]);
    setSlots((slotRes.data as BookingSlot[]) ?? []);
    setHubs((hubRes.data as Hub[]) ?? []);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = hubFilter === 'all' ? slots : slots.filter((s) => s.hub_id === hubFilter);

  const openAdd = () => { setForm({ ...emptyForm, hub_id: hubs[0]?.id ?? '' }); setFormError(null); setAddOpen(true); };
  const openEdit = (s: BookingSlot) => {
    setForm({
      hub_id: s.hub_id,
      start_time: toLocalInput(s.start_time),
      end_time: toLocalInput(s.end_time),
      energy_available_kwh: String(s.energy_available_kwh),
    });
    setFormError(null);
    setEditSlot(s);
  };

  const handleSave = async () => {
    setFormError(null);
    if (!form.hub_id) {
      setFormError('Please select a hub.');
      return;
    }
    if (!form.start_time || !form.end_time) {
      setFormError('Start time and end time are required.');
      return;
    }
    try {
      const start = new Date(form.start_time);
      const end = new Date(form.end_time);
      if (isNaN(start.getTime()) || isNaN(end.getTime())) {
        setFormError('Invalid date format. Please pick valid start and end times.');
        return;
      }
      if (end <= start) {
        setFormError('End time must be after start time.');
        return;
      }
      const payload = {
        hub_id: form.hub_id,
        start_time: start.toISOString(),
        end_time: end.toISOString(),
        energy_available_kwh: parseFloat(form.energy_available_kwh) || 0,
      };
      setSaving(true);
      let result;
      if (editSlot) {
        result = await Promise.resolve({data: null, error: null});
      } else {
        result = await Promise.resolve({data: null, error: null});
      }
      setSaving(false);
      if (result.error) {
        console.error('Slot save error:', result.error);
        setFormError(result.error.message);
        return;
      }
      setAddOpen(false);
      setEditSlot(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      setSaving(false);
      console.error('Slot save exception:', err);
      setFormError(err instanceof Error ? err.message : 'An unexpected error occurred while saving.');
    }
  };

  const handleDelete = async (s: BookingSlot) => {
    await supabase.from('booking_slots').delete().eq('id', s.id);
    load();
  };

  const toLocalInput = (iso: string) => {
    const d = new Date(iso);
    const offset = d.getTimezoneOffset() * 60000;
    return new Date(d.getTime() - offset).toISOString().slice(0, 16);
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <select
          value={hubFilter}
          onChange={(e) => setHubFilter(e.target.value)}
          className="rounded-lg border-0 py-2.5 pl-3.5 pr-8 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
        >
          <option value="all">All Hubs</option>
          {hubs.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
        </select>
        <Button onClick={openAdd} disabled={hubs.length === 0}>
          <Plus className="h-4 w-4" />
          Create Slot
        </Button>
      </div>

      {loading ? (
        <div className="py-16 text-center text-sm text-gray-400">Loading...</div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white shadow-sm">
          <EmptyState icon={<CalendarDays className="h-7 w-7" />} title="No booking slots" description="Create time slots for hubs to enable energy reservations." />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map((s) => {
            const pct = s.energy_available_kwh > 0 ? (s.energy_booked_kwh / s.energy_available_kwh) * 100 : 0;
            return (
              <div key={s.id} className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-sky-50 text-sky-600">
                      <CalendarDays className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-gray-900">{s.hub?.name ?? 'Unknown hub'}</div>
                      <StatusBadge status={s.status} />
                    </div>
                  </div>
                </div>
                <div className="mt-4 space-y-3">
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Clock className="h-4 w-4 text-gray-400" />
                    {new Date(s.start_time).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
                  </div>
                  <div className="text-xs text-gray-400">
                    until {new Date(s.end_time).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
                  </div>
                  <div className="border-t border-gray-50 pt-3">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-gray-500">Energy booked</span>
                      <span className="font-semibold text-gray-900">{s.energy_booked_kwh}/{s.energy_available_kwh} kWh</span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-gray-100">
                      <div
                        className={`h-full rounded-full ${pct >= 100 ? 'bg-rose-500' : pct > 50 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                        style={{ width: `${Math.min(pct, 100)}%` }}
                      />
                    </div>
                  </div>
                </div>
                <div className="mt-4 flex items-center gap-2 border-t border-gray-50 pt-4">
                  <Button variant="ghost" size="sm" onClick={() => openEdit(s)}>
                    <Pencil className="h-3.5 w-3.5" /> Edit
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => handleDelete(s)} className="text-rose-500">
                    <Trash2 className="h-3.5 w-3.5" /> Delete
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Modal
        open={addOpen || !!editSlot}
        onClose={() => { setAddOpen(false); setEditSlot(null); setForm(emptyForm); setFormError(null); }}
        title={editSlot ? 'Edit Slot' : 'Create Booking Slot'}
        description="Define a time window and energy allocation for a hub."
        footer={
          <>
            <Button variant="secondary" onClick={() => { setAddOpen(false); setEditSlot(null); setForm(emptyForm); setFormError(null); }}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save Slot'}</Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Hub *</label>
            <select
              value={form.hub_id}
              onChange={(e) => setForm({ ...form, hub_id: e.target.value })}
              className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
            >
              <option value="">Select a hub...</option>
              {hubs.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label="Start Time *" type="datetime-local" value={form.start_time} onChange={(e) => setForm({ ...form, start_time: e.target.value })} />
            <Input label="End Time *" type="datetime-local" value={form.end_time} onChange={(e) => setForm({ ...form, end_time: e.target.value })} />
          </div>
          <Input label="Energy Available (kWh)" type="number" step="any" value={form.energy_available_kwh} onChange={(e) => setForm({ ...form, energy_available_kwh: e.target.value })} placeholder="20.00" />
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
        </div>
      </Modal>
    </div>
  );
}
