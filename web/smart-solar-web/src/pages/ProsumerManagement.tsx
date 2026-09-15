import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { Prosumer } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { UserPlus, Search, Users, Check, X, Power, Eye, UserCheck } from 'lucide-react';

type ProsumerForm = {
  nic: string;
  full_name: string;
  email: string;
  phone: string;
  address: string;
};

export default function ProsumerManagement() {
  const [prosumers, setProsumers] = useState<Prosumer[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [addOpen, setAddOpen] = useState(false);
  const [viewProsumer, setViewProsumer] = useState<Prosumer | null>(null);
  const [form, setForm] = useState<ProsumerForm>({ nic: '', full_name: '', email: '', phone: '', address: '' });
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    let query = ApiService.getProsumers().order('created_at', { ascending: false });
    if (statusFilter !== 'all') query = query.eq('status', statusFilter);
    if (search) query = query.or(`full_name.ilike.%${search}%,nic.ilike.%${search}%,email.ilike.%${search}%`);
    const { data } = await query;
    setProsumers((data as Prosumer[]) ?? []);
    setLoading(false);
  }, [search, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleAdd = async () => {
    setFormError(null);
    if (!form.nic || !form.full_name || !form.email) {
      setFormError('NIC, full name, and email are required.');
      return;
    }
    setSaving(true);
    const { error } = await Promise.resolve({data: null, error: null});
    setSaving(false);
    if (error) {
      setFormError(error.message);
      return;
    }
    setAddOpen(false);
    setForm({ nic: '', full_name: '', email: '', phone: '', address: '' });
    load();
  };

  const updateStatus = async (p: Prosumer, status: Prosumer['status'], deactivationRequested = false) => {
    const updates: Record<string, unknown> = { status };
    if (deactivationRequested !== undefined) updates.deactivation_requested = deactivationRequested;
    await Promise.resolve({data: null, error: null});
    load();
    setViewProsumer(null);
  };

  const filtered = prosumers;

  return (
    <div className="space-y-5">
      {/* Toolbar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-col gap-3 sm:flex-row">
          <div className="relative flex-1 max-w-sm">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              placeholder="Search by name, NIC, or email..."
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
            <option value="active">Active</option>
            <option value="deactivated">Deactivated</option>
          </select>
        </div>
        <Button onClick={() => setAddOpen(true)}>
          <UserPlus className="h-4 w-4" />
          Add Prosumer
        </Button>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        {loading ? (
          <div className="py-16 text-center text-sm text-gray-400">Loading...</div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={<Users className="h-7 w-7" />}
            title="No prosumers found"
            description="Add a new prosumer or adjust your search filters."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  <th className="px-6 py-3">Prosumer</th>
                  <th className="px-6 py-3">NIC</th>
                  <th className="px-6 py-3">Contact</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-amber-400 to-amber-600 text-xs font-bold text-white">
                          {p.full_name.split(' ').map((n) => n[0]).slice(0, 2).join('')}
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">{p.full_name}</div>
                          {p.deactivation_requested && (
                            <div className="text-xs text-amber-600">Deactivation requested</div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-gray-600">{p.nic}</td>
                    <td className="px-6 py-4">
                      <div className="text-gray-900">{p.email}</div>
                      {p.phone && <div className="text-xs text-gray-400">{p.phone}</div>}
                    </td>
                    <td className="px-6 py-4"><StatusBadge status={p.status} /></td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setViewProsumer(p)}
                          className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                          title="View details"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {p.status === 'pending' && (
                          <button
                            onClick={() => updateStatus(p, 'active')}
                            className="rounded-md p-1.5 text-emerald-500 hover:bg-emerald-50"
                            title="Approve activation"
                          >
                            <Check className="h-4 w-4" />
                          </button>
                        )}
                        {p.status === 'active' && !p.deactivation_requested && (
                          <button
                            onClick={() => updateStatus(p, 'deactivated')}
                            className="rounded-md p-1.5 text-rose-400 hover:bg-rose-50"
                            title="Deactivate"
                          >
                            <Power className="h-4 w-4" />
                          </button>
                        )}
                        {p.status === 'deactivated' && (
                          <button
                            onClick={() => updateStatus(p, 'active')}
                            className="rounded-md p-1.5 text-sky-500 hover:bg-sky-50"
                            title="Reactivate"
                          >
                            <Power className="h-4 w-4" />
                          </button>
                        )}
                        {p.deactivation_requested && p.status === 'active' && (
                          <button
                            onClick={() => updateStatus(p, 'deactivated', false)}
                            className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50"
                            title="Approve deactivation"
                          >
                            <X className="h-4 w-4" />
                          </button>
                        )}
                      </div>
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
        title="Add New Prosumer"
        description="Register a new energy prosumer in the system."
        footer={
          <>
            <Button variant="secondary" onClick={() => setAddOpen(false)}>Cancel</Button>
            <Button onClick={handleAdd} disabled={saving}>{saving ? 'Saving...' : 'Add Prosumer'}</Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input label="NIC (National ID) *" value={form.nic} onChange={(e) => setForm({ ...form, nic: e.target.value })} placeholder="199012345678" />
          <Input label="Full Name *" value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} placeholder="John Doe" />
          <Input label="Email *" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="john@example.com" />
          <Input label="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} placeholder="+94 77 123 4567" />
          <Input label="Address" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} placeholder="123 Main St, Colombo" />
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
        </div>
      </Modal>

      {/* View modal */}
      <Modal
        open={!!viewProsumer}
        onClose={() => setViewProsumer(null)}
        title="Prosumer Details"
        size="md"
        footer={
          viewProsumer && (
            <>
              {viewProsumer.status === 'pending' && (
                <Button onClick={() => updateStatus(viewProsumer, 'active')}>
                  <UserCheck className="h-4 w-4" /> Approve Activation
                </Button>
              )}
              {viewProsumer.status === 'deactivated' && (
                <Button onClick={() => updateStatus(viewProsumer, 'active')}>Reactivate</Button>
              )}
              {viewProsumer.status === 'active' && (
                <Button variant="danger" onClick={() => updateStatus(viewProsumer, 'deactivated')}>Deactivate</Button>
              )}
            </>
          )
        }
      >
        {viewProsumer && (
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 to-amber-600 text-lg font-bold text-white">
                {viewProsumer.full_name.split(' ').map((n) => n[0]).slice(0, 2).join('')}
              </div>
              <div>
                <div className="text-lg font-semibold text-gray-900">{viewProsumer.full_name}</div>
                <StatusBadge status={viewProsumer.status} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 rounded-xl bg-gray-50 p-4">
              <div>
                <div className="text-xs font-medium text-gray-400">NIC</div>
                <div className="mt-0.5 text-sm text-gray-900 font-mono">{viewProsumer.nic}</div>
              </div>
              <div>
                <div className="text-xs font-medium text-gray-400">Email</div>
                <div className="mt-0.5 text-sm text-gray-900">{viewProsumer.email}</div>
              </div>
              <div>
                <div className="text-xs font-medium text-gray-400">Phone</div>
                <div className="mt-0.5 text-sm text-gray-900">{viewProsumer.phone ?? '—'}</div>
              </div>
              <div>
                <div className="text-xs font-medium text-gray-400">Address</div>
                <div className="mt-0.5 text-sm text-gray-900">{viewProsumer.address ?? '—'}</div>
              </div>
              <div>
                <div className="text-xs font-medium text-gray-400">Registered</div>
                <div className="mt-0.5 text-sm text-gray-900">{new Date(viewProsumer.created_at).toLocaleDateString()}</div>
              </div>
              <div>
                <div className="text-xs font-medium text-gray-400">Deactivation Requested</div>
                <div className="mt-0.5 text-sm text-gray-900">{viewProsumer.deactivation_requested ? 'Yes' : 'No'}</div>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
