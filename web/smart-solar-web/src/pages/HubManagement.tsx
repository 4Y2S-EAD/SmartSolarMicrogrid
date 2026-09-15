import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { Hub, Reservation } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { Zap, Plus, Search, MapPin, Pencil, Trash2, AlertTriangle, Eye, Power } from 'lucide-react';

type HubForm = {
  name: string;
  location_name: string;
  latitude: string;
  longitude: string;
  capacity_kw: string;
};

const emptyForm: HubForm = { name: '', location_name: '', latitude: '', longitude: '', capacity_kw: '' };

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
    try {
      const [hubRes, resRes] = await Promise.all([
        ApiService.getHubs(),
        ApiService.getReservations(), // Assuming getReservations filters appropriately or we filter client-side
      ]);
      
      setHubs(hubRes || []);
      setReservations(resRes?.filter(r => r.status === 'pending' || r.status === 'confirmed') || []);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = hubs.filter(
    (h) => h.name.toLowerCase().includes(search.toLowerCase()) || h.location_name.toLowerCase().includes(search.toLowerCase())
  );

  const hasActiveReservations = (hubId: string) =>
    reservations.some((r) => r.hub_id === hubId && r.status !== 'cancelled' && r.status !== 'completed');

  const openAdd = () => { setForm(emptyForm); setFormError(null); setAddOpen(true); };
  const openEdit = (h: Hub) => {
    setForm({ name: h.name, location_name: h.location_name, latitude: String(h.latitude), longitude: String(h.longitude), capacity_kw: String(h.capacity_kw) });
    setFormError(null);
    setEditHub(h);
  };

  const handleSave = async () => {
    setFormError(null);
    if (!form.name || !form.location_name || !form.latitude || !form.longitude) {
      setFormError('Name, location, and coordinates are required.');
      return;
    }
    const payload = {
      name: form.name,
      location_name: form.location_name,
      latitude: parseFloat(form.latitude),
      longitude: parseFloat(form.longitude),
      capacity_kw: parseFloat(form.capacity_kw) || 0,
    };
    setSaving(true);
    try {
      if (editHub) {
         // Assuming ApiService handles update logic via standard fetch PUT/PATCH
         await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:5224/api'}/hubs/${editHub.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
         });
      } else {
         await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:5224/api'}/hubs`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ...payload, status: 'active' })
         });
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

  const toggleStatus = async (h: Hub) => {
    if (h.status === 'active' && hasActiveReservations(h.id)) {
      setConfirmDelete(h);
      return;
    }
    const newStatus = h.status === 'active' ? 'deactivated' : 'active';
    try {
      await ApiService.updateHubStatus(h.id, newStatus);
    } catch (e) {}
    load();
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await ApiService.updateHubStatus(confirmDelete.id, 'deactivated');
    } catch (e) {}
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
            const activeRes = reservations.filter((r) => r.hub_id === h.id && r.status !== 'cancelled' && r.status !== 'completed').length;
            return (
              <div key={h.id} className="group rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                      <Zap className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="font-semibold text-gray-900">{h.name}</div>
                      <StatusBadge status={h.status} />
                    </div>
                  </div>
                </div>
                <div className="mt-4 space-y-2 text-sm">
                  <div className="flex items-center gap-2 text-gray-600">
                    <MapPin className="h-4 w-4 text-gray-400" />
                    {h.location_name}
                  </div>
                  <div className="text-xs text-gray-400">
                    {h.latitude.toFixed(4)}°, {h.longitude.toFixed(4)}°
                  </div>
                  <div className="flex items-center justify-between border-t border-gray-50 pt-3">
                    <div>
                      <div className="text-xs text-gray-400">Capacity</div>
                      <div className="text-sm font-semibold text-gray-900">{h.capacity_kw} kW</div>
                    </div>
                    <div>
                      <div className="text-xs text-gray-400">Active Res.</div>
                      <div className="text-sm font-semibold text-gray-900">{activeRes}</div>
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
                    className={h.status === 'active' ? 'text-rose-500' : 'text-sky-500'}
                  >
                    <Power className="h-3.5 w-3.5" />
                    {h.status === 'active' ? 'Deactivate' : 'Activate'}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add/Edit modal */}
      <Modal
        open={addOpen || !!editHub}
        onClose={() => { setAddOpen(false); setEditHub(null); setForm(emptyForm); setFormError(null); }}
        title={editHub ? 'Edit Hub' : 'Create New Hub'}
        description="Configure the solar station location and capacity."
        footer={
          <>
            <Button variant="secondary" onClick={() => { setAddOpen(false); setEditHub(null); setForm(emptyForm); setFormError(null); }}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save Hub'}</Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input label="Hub Name *" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Central Solar Hub" />
          <Input label="Location Name *" value={form.location_name} onChange={(e) => setForm({ ...form, location_name: e.target.value })} placeholder="Colombo 07" />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Latitude *" type="number" step="any" value={form.latitude} onChange={(e) => setForm({ ...form, latitude: e.target.value })} placeholder="6.9271" />
            <Input label="Longitude *" type="number" step="any" value={form.longitude} onChange={(e) => setForm({ ...form, longitude: e.target.value })} placeholder="79.8612" />
          </div>
          <Input label="Capacity (kW)" type="number" step="any" value={form.capacity_kw} onChange={(e) => setForm({ ...form, capacity_kw: e.target.value })} placeholder="50.00" />
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
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
                <div className="text-lg font-semibold text-gray-900">{viewHub.name}</div>
                <StatusBadge status={viewHub.status} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 rounded-xl bg-gray-50 p-4">
              <div><div className="text-xs text-gray-400">Location</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.location_name}</div></div>
              <div><div className="text-xs text-gray-400">Capacity</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.capacity_kw} kW</div></div>
              <div><div className="text-xs text-gray-400">Latitude</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.latitude}</div></div>
              <div><div className="text-xs text-gray-400">Longitude</div><div className="mt-0.5 text-sm text-gray-900">{viewHub.longitude}</div></div>
              <div><div className="text-xs text-gray-400">Created</div><div className="mt-0.5 text-sm text-gray-900">{new Date(viewHub.created_at).toLocaleDateString()}</div></div>
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
            Hub <strong>{confirmDelete?.name}</strong> has active reservations.
            Deactivation is blocked until all pending or confirmed reservations are cancelled or completed.
          </p>
        </div>
      </Modal>
    </div>
  );
}
