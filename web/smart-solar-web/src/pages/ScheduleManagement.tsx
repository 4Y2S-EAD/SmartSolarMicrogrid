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
  stationId: string;
  slotNumber: string;
  startTime: string;
  endTime: string;
  capacityKwh: string;
};

const emptyForm: SlotForm = { stationId: '', slotNumber: '1', startTime: '', endTime: '', capacityKwh: '' };

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

  // 1. Load Data from Backend APIs (Changed to match Member 2 endpoints)
  const load = useCallback(async () => {
    setLoading(true);
    try {
      // get hubs from our API
      const hubsData = await ApiService.getHubs();
      const activeHubs = hubsData.filter(h => h.status === 'Active');

      // Load slots for each active hub
      let allSlots: BookingSlot[] = [];
      for (const h of activeHubs) {
        try {
          const slotsData = await ApiService.getBookingSlots(h.stationId);
          // Attach hub reference to slot so UI can show hub name
          const slotsWithHub = slotsData.map((s: any) => ({ ...s, hub: h }));
          allSlots = [...allSlots, ...slotsWithHub];
        } catch (e) {
          console.error("Failed to load slots for hub " + h.stationId);
        }
      }

      setHubs(activeHubs);
      setSlots(allSlots);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = hubFilter === 'all' ? slots : slots.filter((s) => s.stationId === hubFilter);

  const openAdd = () => { setForm({ ...emptyForm, stationId: hubs[0]?.stationId ?? '' }); setFormError(null); setAddOpen(true); };

  const openEdit = (s: BookingSlot) => {
    setForm({
      stationId: s.stationId,
      slotNumber: String(s.slotNumber || 1),
      startTime: toLocalInput(s.startTime),
      endTime: toLocalInput(s.endTime),
      capacityKwh: String(s.capacityKwh),
    });
    setFormError(null);
    setEditSlot(s);
  };

  // 2. Save Slot to Backend Database
  const handleSave = async () => {
    setFormError(null);
    if (!form.stationId) {
      setFormError('Please select a hub.');
      return;
    }
    if (!form.startTime || !form.endTime) {
      setFormError('Start time and end time are required.');
      return;
    }

    try {
      const start = new Date(form.startTime);
      const end = new Date(form.endTime);

      if (isNaN(start.getTime()) || isNaN(end.getTime())) {
        setFormError('Invalid date format. Please pick valid start and end times.');
        return;
      }
      if (end <= start) {
        setFormError('End time must be after start time.');
        return;
      }

      // Create payload matching CreateSlotDto in C# Backend
      const payload = {
        slotNumber: parseInt(form.slotNumber) || 1,
        bookingDate: start.toISOString(),
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        capacityKwh: parseFloat(form.capacityKwh) || 0,
        status: "Available"
      };

      setSaving(true);

      if (editSlot) {
        // Update slot using API
        await ApiService.updateBookingSlot(editSlot.slotId, payload);
      } else {
        // Create slot using API
        await ApiService.createBookingSlot(form.stationId, payload);
      }

      setSaving(false);
      setAddOpen(false);
      setEditSlot(null);
      setForm(emptyForm);
      load();
    } catch (err: any) {
      setSaving(false);
      console.error('Slot save exception:', err);
      setFormError(err.message || 'An unexpected error occurred while saving.');
    }
  };

  // 3. Delete Slot using Backend API
  const handleDelete = async (s: BookingSlot) => {
    try {
      await ApiService.deleteBookingSlot(s.slotId);
      load();
    } catch (err: any) {
      alert("Cannot delete slot: " + err.message);
    }
  };

  const toLocalInput = (iso: string) => {
    if (!iso) return '';
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
          {hubs.map((h) => <option key={h.stationId} value={h.stationId}>{h.stationName}</option>)}
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
            return (
              <div key={s.slotId} className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-sky-50 text-sky-600">
                      <CalendarDays className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-gray-900">{s.hub?.stationName ?? 'Unknown hub'}</div>
                      <StatusBadge status={s.status} />
                    </div>
                  </div>
                  <div className="text-sm font-bold text-gray-500">Slot #{s.slotNumber}</div>
                </div>
                <div className="mt-4 space-y-3">
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Clock className="h-4 w-4 text-gray-400" />
                    {new Date(s.startTime).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
                  </div>
                  <div className="text-xs text-gray-400">
                    until {new Date(s.endTime).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
                  </div>
                  <div className="border-t border-gray-50 pt-3">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-gray-500">Slot Capacity</span>
                      <span className="font-semibold text-gray-900">{s.capacityKwh} kWh</span>
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
              value={form.stationId}
              onChange={(e) => setForm({ ...form, stationId: e.target.value })}
              className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
            >
              <option value="">Select a hub...</option>
              {hubs.map((h) => <option key={h.stationId} value={h.stationId}>{h.stationName}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label="Slot Number *" type="number" value={form.slotNumber} onChange={(e) => setForm({ ...form, slotNumber: e.target.value })} />
            <Input label="Capacity (kWh) *" type="number" step="any" value={form.capacityKwh} onChange={(e) => setForm({ ...form, capacityKwh: e.target.value })} placeholder="20.00" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label="Start Time *" type="datetime-local" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
            <Input label="End Time *" type="datetime-local" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
          </div>
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
        </div>
      </Modal>
    </div>
  );
}
