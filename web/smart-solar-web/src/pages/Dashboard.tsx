import { useEffect, useState } from 'react';
import { ApiService } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import type { Hub, Prosumer, Reservation } from '@/lib/api';
import { Zap, Users, Gauge, CheckCircle2, TrendingUp, MapPin, Clock, QrCode, ScanLine } from 'lucide-react';
import StatusBadge from '@/components/ui/StatusBadge';

export default function Dashboard() {
  const { role, profile } = useAuth();
  const [stats, setStats] = useState({
    hubs: 0,
    activeHubs: 0,
    prosumers: 0,
    activeProsumers: 0,
    pendingProsumers: 0,
    reservations: 0,
    confirmedReservations: 0,
    completedReservations: 0,
  });
  const [recentReservations, setRecentReservations] = useState<Reservation[]>([]);
  const [recentQrTx, setRecentQrTx] = useState<{ id: string; status: string; scanned_at: string; token: string }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      if (role === 'grid_operator') {
        // Operators see QR-focused dashboard
        const [resRes, qrRes] = await Promise.all([
          Promise.resolve({data: [], error: null}),
          Promise.resolve({data: [], error: null}),
        ]);
        setRecentReservations((resRes.data as Reservation[]) ?? []);
        setRecentQrTx((qrRes.data as { id: string; status: string; scanned_at: string; token: string }[]) ?? []);
        setLoading(false);
        return;
      }

      const [hubs, prosumers, reservations] = await Promise.all([
        ApiService.getHubs(),
        ApiService.getProsumers(),
        Promise.resolve({data: [], error: null}),
      ]);

      const h = (hubs.data as Hub[]) ?? [];
      const p = (prosumers.data as Prosumer[]) ?? [];
      const r = (reservations.data as Reservation[]) ?? [];

      setStats({
        hubs: h.length,
        activeHubs: h.filter((x) => x.status === 'active').length,
        prosumers: p.length,
        activeProsumers: p.filter((x) => x.status === 'active').length,
        pendingProsumers: p.filter((x) => x.status === 'pending').length,
        reservations: r.length,
        confirmedReservations: r.filter((x) => x.status === 'confirmed').length,
        completedReservations: r.filter((x) => x.status === 'completed').length,
      });
      setRecentReservations(r);
      setLoading(false);
    }
    load();
  }, [role]);

  const colorMap: Record<string, string> = {
    amber: 'bg-amber-50 text-amber-600',
    sky: 'bg-sky-50 text-sky-600',
    teal: 'bg-teal-50 text-teal-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    indigo: 'bg-indigo-50 text-indigo-600',
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="animate-pulse text-gray-400">Loading dashboard...</div>
      </div>
    );
  }

  // Grid Operator dashboard
  if (role === 'grid_operator') {
    const opCards = [
      { label: 'Pending Scans', value: recentReservations.filter((r) => r.status === 'confirmed').length, sub: 'Ready to scan', icon: QrCode, color: 'indigo' },
      { label: 'Completed Today', value: recentQrTx.filter((t) => t.status === 'finalized').length, sub: 'Transactions', icon: CheckCircle2, color: 'emerald' },
      { label: 'Recent Scans', value: recentQrTx.length, sub: 'Last 5 activities', icon: ScanLine, color: 'amber' },
    ];
    return (
      <div className="space-y-6">
        <div className="rounded-2xl border border-indigo-100 bg-gradient-to-br from-indigo-50 to-white p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-500 text-white shadow-lg shadow-indigo-500/20">
              <ScanLine className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900">Welcome, {profile?.full_name || 'Operator'}</h2>
              <p className="text-sm text-gray-500">Your assigned hub: {profile?.assigned_hub?.name ?? 'Unassigned'}</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {opCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium text-gray-500">{card.label}</div>
                    <div className="mt-2 text-3xl font-bold text-gray-900">{card.value}</div>
                    <div className="mt-1 text-xs text-gray-400">{card.sub}</div>
                  </div>
                  <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${colorMap[card.color]}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="rounded-2xl border border-gray-100 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-6 py-4">
            <h2 className="text-base font-semibold text-gray-900">Recent QR Transactions</h2>
            <p className="text-sm text-gray-500">Your latest scan activity</p>
          </div>
          <div className="divide-y divide-gray-50">
            {recentQrTx.length === 0 && (
              <div className="px-6 py-12 text-center text-sm text-gray-400">No transactions yet. Go to QR Transaction Flow to start scanning.</div>
            )}
            {recentQrTx.map((tx) => (
              <div key={tx.id} className="flex items-center justify-between px-6 py-4 hover:bg-gray-50/50 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gray-50 text-gray-400">
                    <QrCode className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm font-mono text-gray-600">{tx.token.slice(0, 20)}...</div>
                    <div className="text-xs text-gray-400">{new Date(tx.scanned_at).toLocaleString()}</div>
                  </div>
                </div>
                <StatusBadge status={tx.status} />
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Backoffice dashboard
  const cards = [
    { label: 'Total Hubs', value: stats.hubs, sub: `${stats.activeHubs} active`, icon: Zap, color: 'amber' },
    { label: 'Total Prosumers', value: stats.prosumers, sub: `${stats.pendingProsumers} pending`, icon: Users, color: 'sky' },
    { label: 'Reservations', value: stats.reservations, sub: `${stats.confirmedReservations} confirmed`, icon: Gauge, color: 'teal' },
    { label: 'Completed', value: stats.completedReservations, sub: 'All time', icon: CheckCircle2, color: 'emerald' },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.label} className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-medium text-gray-500">{card.label}</div>
                  <div className="mt-2 text-3xl font-bold text-gray-900">{card.value}</div>
                  <div className="mt-1 text-xs text-gray-400">{card.sub}</div>
                </div>
                <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${colorMap[card.color]}`}>
                  <Icon className="h-6 w-6" />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 rounded-2xl border border-gray-100 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-6 py-4">
            <h2 className="text-base font-semibold text-gray-900">Recent Reservations</h2>
            <p className="text-sm text-gray-500">Latest energy booking activity</p>
          </div>
          <div className="divide-y divide-gray-50">
            {recentReservations.length === 0 && (
              <div className="px-6 py-12 text-center text-sm text-gray-400">No reservations yet</div>
            )}
            {recentReservations.map((r) => (
              <div key={r.id} className="flex items-center justify-between px-6 py-4 hover:bg-gray-50/50 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gray-50 text-gray-400">
                    <TrendingUp className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-900">{r.prosumer?.full_name ?? 'Unknown'}</div>
                    <div className="text-xs text-gray-400">{r.hub?.name ?? 'Unknown hub'} - {r.energy_kwh} kWh</div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-gray-400">
                    {new Date(r.created_at).toLocaleDateString()}
                  </span>
                  <StatusBadge status={r.status} />
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-4">
          <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
            <h2 className="text-base font-semibold text-gray-900">Network Status</h2>
            <div className="mt-4 space-y-3">
              <div className="flex items-center justify-between rounded-lg bg-emerald-50 px-4 py-3">
                <div className="flex items-center gap-2">
                  <span className="h-2 w-2 rounded-full bg-emerald-500" />
                  <span className="text-sm font-medium text-emerald-800">Grid Online</span>
                </div>
                <span className="text-sm font-bold text-emerald-600">{stats.activeHubs}/{stats.hubs}</span>
              </div>
              <div className="flex items-center justify-between rounded-lg bg-sky-50 px-4 py-3">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-sky-600" />
                  <span className="text-sm font-medium text-sky-800">Active Prosumers</span>
                </div>
                <span className="text-sm font-bold text-sky-600">{stats.activeProsumers}</span>
              </div>
              <div className="flex items-center justify-between rounded-lg bg-amber-50 px-4 py-3">
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-amber-600" />
                  <span className="text-sm font-medium text-amber-800">Pending Approvals</span>
                </div>
                <span className="text-sm font-bold text-amber-600">{stats.pendingProsumers}</span>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
            <h2 className="text-base font-semibold text-gray-900">Hub Locations</h2>
            <div className="mt-4 space-y-2">
              {recentReservations[0]?.hub && (
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <MapPin className="h-4 w-4 text-gray-400" />
                  {recentReservations[0].hub.location_name}
                </div>
              )}
              <p className="text-xs text-gray-400">
                Map integration available on the Android application with Google Maps.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
