import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { Hub, Reservation } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { Zap, Plus, Search, MapPin, Pencil, Trash2, AlertTriangle, Eye, Power } from 'lucide-react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

function MapEvents({ onLocationSelect }: { onLocationSelect: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onLocationSelect(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

type HubForm = {
  stationName: string;
  latitude: string;
  longitude: string;
  capacityKwh: string;
  batterySlotCount: string;
};

const emptyForm: HubForm = { stationName: '', latitude: '', longitude: '', capacityKwh: '', batterySlotCount: '10' };


export default function HubManagement() {
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [addOpen, setAddOpen] = useState(false);
  const [editHub, setEditHub] = useState<Hub | null>(null);
  const [viewHub, setViewHub] = useState<Hub | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Hub | null>(null);
  const [form, setForm] = useState<HubForm>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);

    // Load Hubs
    try {
      const hubRes = await ApiService.getHubs();
      setHubs(hubRes || []);
    } catch (err) {
      console.error("Failed to load hubs", err);
    }

    // Load Reservations (Ignore error if backend endpoint is not ready yet)
    try {
      const resRes = await ApiService.getReservations();
      setReservations(resRes?.filter(r => r.status === 'pending' || r.status === 'confirmed') || []);
    } catch (err) {
      console.error("Reservations API not ready yet, ignoring...", err);
      setReservations([]);
    }

    setLoading(false);
  }, []);


  useEffect(() => { load(); }, [load]);

  //3 removed filtered and openEdit functions 9/22
  const filtered = hubs.filter(
    (h) => h.stationName?.toLowerCase().includes(search.toLowerCase())
  );

  const hasActiveReservations = (hubId: string) =>
    reservations.some((r) => r.hub_id === hubId && r.status !== 'cancelled' && r.status !== 'completed');

  const openAdd = () => { setForm(emptyForm); setFormError(null); setAddOpen(true); };

  const openEdit = (h: Hub) => {
    setForm({
      stationName: h.stationName,
      latitude: String(h.location?.latitude || ''),
      longitude: String(h.location?.longitude || ''),
      capacityKwh: String(h.capacityKwh),
      batterySlotCount: String(h.batterySlotCount || '10')
    });
    setFormError(null);
    setEditHub(h);
  };


  //1 changed handelsave function 9/22 (this is a payload it create data we need to pass backend)

  const handleSave = async () => {
    setFormError(null);
    if (!form.stationName || !form.latitude || !form.longitude) {
      setFormError('Name and coordinates are required.');
      return;
    }

    const payload = {
      stationName: form.stationName,
      latitude: parseFloat(form.latitude),
      longitude: parseFloat(form.longitude),
      capacityKwh: parseFloat(form.capacityKwh) || 0,
      batterySlotCount: parseInt(form.batterySlotCount) || 10,
      status: 'Active',
      schedule: 'Standard 24/7'
    };

    setSaving(true);
    try {
      if (editHub) {
        // Update existing station
        await ApiService.updateHub(editHub.stationId, payload);
      } else {
        // Create new station
        await ApiService.createHub(payload);
      }
    } catch (err: any) {
      setFormError(err.message || 'Failed to save');
      setSaving(false);
      return;
    }
    setSaving(false);
    setAddOpen(false);
    setEditHub(null);
    setForm(emptyForm);
    load();
  };

  // 2 Deactivate (toggleStatus and handleDelete) Functions changed 22/9
  const toggleStatus = async (h: Hub) => {
    if (h.status === 'Active' && hasActiveReservations(h.stationId)) {
      setConfirmDelete(h);
      return;
    }

    try {
      if (h.status === 'Active') {
        await ApiService.deactivateHub(h.stationId);
      } else {
        await ApiService.updateHub(h.stationId, { status: 'Active' });
      }
    } catch (e) { }
    load();
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await ApiService.deactivateHub(confirmDelete.stationId);
    } catch (e) { }
    setConfirmDelete(null);
    load();
  };


  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            placeholder="Search hubs..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-lg border-0 py-2.5 pl-10 pr-3.5 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
          />
        </div>
        <Button onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Create Hub
        </Button>
      </div>

      {loading ? (
        <div className="py-16 text-center text-sm text-gray-400">Loading...</div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white shadow-sm">
          <EmptyState icon={<Zap className="h-7 w-7" />} title="No hubs found" description="Create a new solar hub to start managing energy distribution." />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map((h) => {
            const activeRes = reservations.filter((r) => r.hub_id === h.stationId && r.status !== 'cancelled' && r.status !== 'completed').length;
            return (
              <div key={h.stationId} className="group rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                      <Zap className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="font-semibold text-gray-900">{h.stationName}</div>
                      <StatusBadge status={h.status} />
                    </div>
                  </div>
                </div>
                <div className="mt-4 space-y-2 text-sm">
                  <div className="text-xs text-gray-400">
                    Location: {h.location?.latitude?.toFixed(4)}°, {h.location?.longitude?.toFixed(4)}°
                  </div>
                  <div className="flex items-center justify-between border-t border-gray-50 pt-3">
                    <div>
                      <div className="text-xs text-gray-400">Capacity</div>
                      <div className="text-sm font-semibold text-gray-900">{h.capacityKwh} kW</div>
                    </div>
                    <div>
                      <div className="text-xs text-gray-400">Battery Slots</div>
                      <div className="text-sm font-semibold text-gray-900">{h.batterySlotCount}</div>
                    </div>
                  </div>
                </div>
                <div className="mt-4 flex items-center gap-2 border-t border-gray-50 pt-4">
                  <Button variant="ghost" size="sm" onClick={() => setViewHub(h)}>
                    <Eye className="h-4 w-4" /> View
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => openEdit(h)}>
                    <Pencil className="h-3.5 w-3.5" /> Edit
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => toggleStatus(h)}
                    className={h.status === 'Active' ? 'text-rose-500' : 'text-sky-500'}
                  >
                    <Power className="h-3.5 w-3.5" />
                    {h.status === 'Active' ? 'Deactivate' : 'Activate'}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Modal
        open={addOpen || !!editHub}
        onClose={() => { setAddOpen(false); setEditHub(null); setForm(emptyForm); setFormError(null); }}
        title={editHub ? 'Edit Hub' : 'Create New Hub'}
        description="Configure the solar station location and capacity."
        size="xl"
        footer={
          <>
            <Button variant="secondary" onClick={() => { setAddOpen(false); setEditHub(null); setForm(emptyForm); setFormError(null); }}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save Hub'}</Button>
          </>
        }
      >
        <div className="grid grid-cols-1 gap-6 md:grid-cols-5">
          <div className="space-y-3 md:col-span-3">
             <h3 className="text-sm font-medium text-gray-900">Hub Location</h3>
             <div className="h-[300px] w-full overflow-hidden rounded-xl border border-gray-200">
               <MapContainer center={form.latitude && form.longitude ? [parseFloat(form.latitude), parseFloat(form.longitude)] : [6.9271, 79.8612]} zoom={10} style={{ height: '100%', width: '100%' }}>
                 <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                 <MapEvents onLocationSelect={(lat, lng) => setForm({ ...form, latitude: lat.toFixed(4), longitude: lng.toFixed(4) })} />
                 {form.latitude && form.longitude && (
                   <Marker position={[parseFloat(form.latitude), parseFloat(form.longitude)]} />
                 )}
               </MapContainer>
             </div>
             <div className="flex items-center justify-between rounded-lg bg-gray-50 p-4">
               <div className="flex items-center gap-3">
                 <div className="rounded-full bg-white p-2 shadow-sm"><MapPin className="h-5 w-5 text-gray-400" /></div>
                 <div>
                   <div className="text-sm font-medium text-gray-900">Selected location</div>
                   <div className="text-xs text-gray-500">Click on the map to select.</div>
                 </div>
               </div>
               <div className="flex gap-4 text-right">
                 <div><div className="text-xs text-gray-500">Latitude</div><div className="text-sm font-semibold">{form.latitude || '-'}</div></div>
                 <div><div className="text-xs text-gray-500">Longitude</div><div className="text-sm font-semibold">{form.longitude || '-'}</div></div>
               </div>
             </div>
          </div>
          <div className="space-y-4 md:col-span-2">
            <Input label="Hub Name *" value={form.stationName} onChange={(e) => setForm({ ...form, stationName: e.target.value })} placeholder="Central Solar Hub" />
            <Input label="Capacity (kW) *" type="number" step="any" value={form.capacityKwh} onChange={(e) => setForm({ ...form, capacityKwh: e.target.value })} placeholder="50.00" />
            <Input label="Battery Slots" type="number" value={form.batterySlotCount} onChange={(e) => setForm({ ...form, batterySlotCount: e.target.value })} placeholder="10" />
            {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
          </div>
        </div>
      </Modal>

      {/* View modal */}
      <Modal
        open={!!viewHub}
        onClose={() => setViewHub(null)}
        title="Hub Details"
        footer={<Button variant="secondary" onClick={() => setViewHub(null)}>Close</Button>}
      >
        {viewHub && (
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-600">
                <Zap className="h-7 w-7" />
              </div>
              <div>
                <div className="text-lg font-semibold text-gray-900">{viewHub.stationName}</div>
                <StatusBadge status={viewHub.status} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 rounded-xl bg-gray-50 p-4">
              <div><div className="text-xs text-gray-400">Capacity</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.capacityKwh} kW</div></div>
              <div><div className="text-xs text-gray-400">Battery Slots</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.batterySlotCount}</div></div>
              <div><div className="text-xs text-gray-400">Latitude</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.location?.latitude}</div></div>
              <div><div className="text-xs text-gray-400">Longitude</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.location?.longitude}</div></div>
              <div><div className="text-xs text-gray-400">Created</div><div className="mt-0.5 text-sm text-gray-900">{new Date(viewHub.createdAt).toLocaleDateString()}</div></div>
            </div>
          </div>
        )}
      </Modal>

      {/* Block deactivation confirmation */}
      <Modal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        title="Cannot Deactivate Hub"
        size="sm"
        footer={<Button onClick={() => setConfirmDelete(null)}>Understood</Button>}
      >
        <div className="flex flex-col items-center text-center">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-50 text-rose-500">
            <AlertTriangle className="h-7 w-7" />
          </div>
          <p className="text-sm text-gray-600">
            Hub <strong>{confirmDelete?.stationName}</strong> has active reservations.
            Deactivation is blocked until all pending or confirmed reservations are cancelled or completed.
          </p>
        </div>
      </Modal>
    </div>
  );
}
