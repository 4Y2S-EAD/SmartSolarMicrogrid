import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { Operator, Hub } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { Radio, Plus, Pencil, Power, User } from 'lucide-react';

type OperatorForm = {
  nic?: string;
  email?: string;
  password?: string;
  badge_id: string;
  full_name: string;
  assigned_hub_id: string;
};

const emptyForm: OperatorForm = { nic: '', email: '', password: '', badge_id: '', full_name: '', assigned_hub_id: '' };

export default function OperatorManagement() {
  const [operators, setOperators] = useState<Operator[]>([]);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [loading, setLoading] = useState(true);
  const [addOpen, setAddOpen] = useState(false);
  const [editOp, setEditOp] = useState<Operator | null>(null);
  const [form, setForm] = useState<OperatorForm>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [users, hubsList] = await Promise.all([
        ApiService.getUsers(),
        ApiService.getHubs(),
      ]);
      const activeHubs = (hubsList || [])
        .filter(h => h.status?.toLowerCase() === 'active')
        .sort((a, b) => (a.stationName || '').localeCompare(b.stationName || ''));
      setHubs(activeHubs);

      const ops: Operator[] = (users || [])
        .filter(u => u.role === 'grid_operator')
        .map(u => ({
          id: u.id || u.nic || '',
          user_id: u.id,
          badge_id: u.badge_id || '',
          full_name: u.full_name,
          assigned_hub_id: u.assigned_hub_id,
          status: u.status as 'active' | 'inactive',
          created_at: u.created_at,
          assigned_hub: activeHubs.find(h => h.stationId === u.assigned_hub_id)
        }));
      setOperators(ops);
    } catch (err) {
      console.error('Failed to load operators/hubs:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openAdd = () => { setForm(emptyForm); setFormError(null); setAddOpen(true); };
  const openEdit = (op: Operator) => {
    setForm({ badge_id: op.badge_id, full_name: op.full_name, assigned_hub_id: op.assigned_hub_id ?? '' });
    setFormError(null);
    setEditOp(op);
  };

  const handleSave = async () => {
    setFormError(null);
    if (!form.badge_id || !form.full_name) {
      setFormError('Badge ID and full name are required.');
      return;
    }
    
    if (!editOp && (!form.nic || !form.email || !form.password)) {
      setFormError('NIC, Email and Password are required for new operators.');
      return;
    }

    setSaving(true);
    try {
      if (editOp) {
        await ApiService.updateUser(editOp.id, {
          full_name: form.full_name,
          badge_id: form.badge_id,
          assigned_hub_id: form.assigned_hub_id || null,
        });
      } else {
        await ApiService.createUser({
          nic: form.nic,
          full_name: form.full_name,
          email: form.email,
          password: form.password,
          role: 'grid_operator',
          badge_id: form.badge_id,
          assigned_hub_id: form.assigned_hub_id || null,
        });
      }
      setAddOpen(false);
      setEditOp(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'An error occurred while saving');
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (op: Operator) => {
    const newStatus = op.status === 'active' ? 'deactivated' : 'active';
    try {
      await ApiService.updateUserStatus(op.id, newStatus);
      load();
    } catch (err) {
      console.error('Failed to toggle status:', err);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-gray-500">
          Manage grid operators who scan QR codes and finalize energy transactions.
          Assign each operator to a specific Microgrid Hub.
        </p>
        <Button onClick={openAdd}>
          <Plus className="h-4 w-4" />
          Add Operator
        </Button>
      </div>

      {loading ? (
        <div className="py-16 text-center text-sm text-gray-400">Loading...</div>
      ) : operators.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white shadow-sm">
          <EmptyState icon={<Radio className="h-7 w-7" />} title="No operators" description="Add operators who can scan QR codes and verify transactions." />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {operators.map((op) => (
            <div key={op.id} className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                    <User className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="font-semibold text-gray-900">{op.full_name}</div>
                    <div className="text-xs font-mono text-gray-400">{op.badge_id}</div>
                  </div>
                </div>
                <StatusBadge status={op.status} />
              </div>
              <div className="mt-4 border-t border-gray-50 pt-3">
                <div className="text-xs text-gray-400">Assigned Hub</div>
                <div className="mt-0.5 text-sm text-gray-900">{op.assigned_hub?.stationName ?? 'Unassigned'}</div>
              </div>
              <div className="mt-4 flex items-center gap-2 border-t border-gray-50 pt-4">
                <Button variant="ghost" size="sm" onClick={() => openEdit(op)}>
                  <Pencil className="h-3.5 w-3.5" /> Edit
                </Button>
                <Button variant="ghost" size="sm" onClick={() => toggleStatus(op)} className={op.status === 'active' ? 'text-rose-500' : 'text-sky-500'}>
                  <Power className="h-3.5 w-3.5" />
                  {op.status === 'active' ? 'Deactivate' : 'Activate'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={addOpen || !!editOp}
        onClose={() => { setAddOpen(false); setEditOp(null); setForm(emptyForm); setFormError(null); }}
        title={editOp ? 'Edit Operator' : 'Add New Operator'}
        footer={
          <>
            <Button variant="secondary" onClick={() => { setAddOpen(false); setEditOp(null); setForm(emptyForm); setFormError(null); }}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button>
          </>
        }
      >
        <div className="space-y-4">
          {!editOp && (
            <>
              <Input label="NIC *" value={form.nic || ''} onChange={(e) => setForm({ ...form, nic: e.target.value })} placeholder="123456789V" />
              <Input label="Email *" value={form.email || ''} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="operator@example.com" />
              <Input label="Password *" type="password" value={form.password || ''} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="••••••••" />
            </>
          )}
          <Input label="Badge ID *" value={form.badge_id} onChange={(e) => setForm({ ...form, badge_id: e.target.value })} placeholder="OP-003" />
          <Input label="Full Name *" value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} placeholder="John Smith" />
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">Assigned Hub</label>
            <select
              value={form.assigned_hub_id}
              onChange={(e) => setForm({ ...form, assigned_hub_id: e.target.value })}
              className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
            >
              <option value="">Unassigned</option>
              {hubs.map((h) => <option key={h.stationId} value={h.stationId}>{h.stationName}</option>)}
            </select>
          </div>
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
        </div>
      </Modal>
    </div>
  );
}
