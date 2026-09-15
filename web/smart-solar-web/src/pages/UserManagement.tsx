import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import type { UserProfile, Hub } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { Users, Shield, Radio, Search, Power, Pencil, UserCog, Plus } from 'lucide-react';

type Tab = 'backoffice' | 'grid_operator';

type UserForm = {
  full_name: string;
  badge_id: string;
  assigned_hub_id: string;
};

const emptyForm: UserForm = { full_name: '', badge_id: '', assigned_hub_id: '' };

export default function UserManagement() {
  const { user, refreshProfile } = useAuth();
  const [profiles, setProfiles] = useState<UserProfile[]>([]);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('backoffice');
  const [search, setSearch] = useState('');
  const [editProfile, setEditProfile] = useState<UserProfile | null>(null);
  const [form, setForm] = useState<UserForm>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const [profRes, hubRes] = await Promise.all([
      Promise.resolve({data: [], error: null}),
      Promise.resolve({data: [], error: null}),
    ]);
    setProfiles((profRes.data as UserProfile[]) ?? []);
    setHubs((hubRes.data as Hub[]) ?? []);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = profiles.filter(
    (p) =>
      p.role === tab &&
      (search === '' ||
        p.full_name.toLowerCase().includes(search.toLowerCase()) ||
        p.email.toLowerCase().includes(search.toLowerCase()) ||
        (p.badge_id ?? '').toLowerCase().includes(search.toLowerCase()))
  );

  const openEdit = (p: UserProfile) => {
    setForm({
      full_name: p.full_name,
      badge_id: p.badge_id ?? '',
      assigned_hub_id: p.assigned_hub_id ?? '',
    });
    setFormError(null);
    setEditProfile(p);
  };

  const handleSave = async () => {
    if (!editProfile) return;
    setFormError(null);
    if (!form.full_name.trim()) {
      setFormError('Full name is required.');
      return;
    }
    if (tab === 'grid_operator' && !form.badge_id.trim()) {
      setFormError('Badge ID is required for grid operators.');
      return;
    }
    setSaving(true);
    const payload: Record<string, unknown> = {
      full_name: form.full_name,
      assigned_hub_id: form.assigned_hub_id || null,
    };
    if (tab === 'grid_operator') {
      payload.badge_id = form.badge_id;
    }
    const { error } = await Promise.resolve({data: null, error: null});
    setSaving(false);
    if (error) { setFormError(error.message); return; }
    if (editProfile.id === user?.id) await refreshProfile();
    setEditProfile(null);
    setForm(emptyForm);
    load();
  };

  const toggleStatus = async (p: UserProfile) => {
    const newStatus = p.status === 'active' ? 'deactivated' : 'active';
    await Promise.resolve({data: null, error: null});
    if (p.id === user?.id) await refreshProfile();
    load();
  };

  const tabs: { id: Tab; label: string; icon: typeof Shield; count: number }[] = [
    { id: 'backoffice', label: 'Backoffice Users', icon: Shield, count: profiles.filter((p) => p.role === 'backoffice').length },
    { id: 'grid_operator', label: 'Grid Operators', icon: Radio, count: profiles.filter((p) => p.role === 'grid_operator').length },
  ];

  return (
    <div className="space-y-5">
      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-gray-200">
        {tabs.map((t) => {
          const Icon = t.icon;
          const active = tab === t.id;
          return (
            <button
              key={t.id}
              onClick={() => { setTab(t.id); setSearch(''); }}
              className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-medium transition-colors ${
                active
                  ? 'border-amber-500 text-amber-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Icon className="h-4 w-4" />
              {t.label}
              <span className={`ml-1 rounded-full px-2 py-0.5 text-xs ${active ? 'bg-amber-100 text-amber-700' : 'bg-gray-100 text-gray-500'}`}>
                {t.count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Toolbar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            placeholder={`Search ${tab === 'backoffice' ? 'backoffice users' : 'grid operators'}...`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-lg border-0 py-2.5 pl-10 pr-3.5 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
          />
        </div>
        <p className="text-sm text-gray-500">
          {tab === 'backoffice'
            ? 'Full admin access to all management modules.'
            : 'Can access QR transaction flow and assigned hub operations.'}
        </p>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        {loading ? (
          <div className="py-16 text-center text-sm text-gray-400">Loading...</div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={tab === 'backoffice' ? <Shield className="h-7 w-7" /> : <Radio className="h-7 w-7" />}
            title={tab === 'backoffice' ? 'No backoffice users' : 'No grid operators'}
            description={tab === 'backoffice'
              ? 'Backoffice users will appear here after they sign up.'
              : 'Grid operators will appear here after they sign up with the operator role.'}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  <th className="px-6 py-3">Name</th>
                  <th className="px-6 py-3">Email</th>
                  {tab === 'grid_operator' && <th className="px-6 py-3">Badge ID</th>}
                  {tab === 'grid_operator' && <th className="px-6 py-3">Assigned Hub</th>}
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className={`flex h-9 w-9 items-center justify-center rounded-full text-xs font-bold text-white ${
                          p.role === 'backoffice' ? 'bg-gradient-to-br from-amber-400 to-amber-600' : 'bg-gradient-to-br from-indigo-400 to-indigo-600'
                        }`}>
                          {(p.full_name || p.email).split(' ').map((n) => n[0]).slice(0, 2).join('').toUpperCase() || '?'}
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">{p.full_name || '(no name set)'}</div>
                          {p.id === user?.id && <div className="text-xs text-amber-600">You</div>}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{p.email}</td>
                    {tab === 'grid_operator' && (
                      <td className="px-6 py-4 font-mono text-xs text-gray-600">{p.badge_id ?? '—'}</td>
                    )}
                    {tab === 'grid_operator' && (
                      <td className="px-6 py-4 text-gray-600">{p.assigned_hub?.name ?? 'Unassigned'}</td>
                    )}
                    <td className="px-6 py-4"><StatusBadge status={p.status} /></td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => openEdit(p)}
                          className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                          title="Edit"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => toggleStatus(p)}
                          className={`rounded-md p-1.5 hover:bg-gray-100 ${p.status === 'active' ? 'text-rose-400 hover:text-rose-600' : 'text-sky-500 hover:text-sky-600'}`}
                          title={p.status === 'active' ? 'Deactivate' : 'Activate'}
                        >
                          <Power className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Edit modal */}
      <Modal
        open={!!editProfile}
        onClose={() => { setEditProfile(null); setForm(emptyForm); setFormError(null); }}
        title={`Edit ${tab === 'backoffice' ? 'Backoffice User' : 'Grid Operator'}`}
        description={editProfile?.email}
        footer={
          <>
            <Button variant="secondary" onClick={() => { setEditProfile(null); setForm(emptyForm); setFormError(null); }}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save Changes'}</Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input label="Full Name *" value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} placeholder="John Doe" />
          {tab === 'grid_operator' && (
            <Input label="Badge ID *" value={form.badge_id} onChange={(e) => setForm({ ...form, badge_id: e.target.value })} placeholder="OP-001" />
          )}
          {tab === 'grid_operator' && (
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">Assigned Hub</label>
              <select
                value={form.assigned_hub_id}
                onChange={(e) => setForm({ ...form, assigned_hub_id: e.target.value })}
                className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500"
              >
                <option value="">Unassigned</option>
                {hubs.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
              </select>
            </div>
          )}
          {formError && <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">{formError}</div>}
        </div>
      </Modal>
    </div>
  );
}
