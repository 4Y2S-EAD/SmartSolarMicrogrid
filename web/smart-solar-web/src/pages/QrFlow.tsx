import { useEffect, useState, useCallback } from 'react';
import { fetchBackOfficeReservations, fetchHubReservations, type BackOfficeReservationPage } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import StatusBadge from '@/components/ui/StatusBadge';
import {
  ScanLine, CheckCircle2,
  Zap, TrendingUp,
} from 'lucide-react';

export default function QrFlow() {
  const { role, profile } = useAuth();
  const [data, setData] = useState<BackOfficeReservationPage | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const fetchApi = role === 'grid_operator' ? fetchHubReservations : fetchBackOfficeReservations;
      // Fetch all reservations. The backend enforces hub scoping for operators.
      const result = await fetchApi('all', { reservationId: '', prosumerNic: '', station: '', bookingDate: '', status: '' }, 1);
      setData(result);
    } catch (err) {
      console.error('Failed to load real metrics:', err);
    } finally {
      setLoading(false);
    }
  }, [role]);

  useEffect(() => { load(); }, [load]);

  // Use the real backend metrics
  const stats = {
    total: data?.summary.activeCount ? data.summary.activeCount + (data.summary.completedCount || 0) : 0,
    approved: data?.summary.approvedCount || 0,
    completed: data?.summary.completedCount || 0,
    // Transactions with VerifiedAt set but not yet completed
    verified: data?.items.filter(i => (i as any).verifiedAt != null && i.status !== 'Completed').length || 0,
  };

  const statCards = [
    { label: 'Total Reservations', value: stats.total, icon: TrendingUp, color: 'bg-gray-50 text-gray-600' },
    { label: 'Ready for Scan (Approved)', value: stats.approved, icon: ScanLine, color: 'bg-cyan-50 text-cyan-600' },
    { label: 'Verified', value: stats.completed, icon: ScanLine, color: 'bg-indigo-50 text-indigo-600' },
    { label: 'Completed Transfers', value: stats.completed, icon: CheckCircle2, color: 'bg-emerald-50 text-emerald-600' },
  ];

  // Show only relevant transactions (completed or verified)
  const transactions = data?.items.filter(i => i.status === 'Completed' || (i as any).verifiedAt != null) || [];

  return (
    <div className="space-y-6">
      {/* Stats */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.label} className="rounded-xl border border-gray-100 bg-white p-4 shadow-sm m4-anim-scale-in">
              <div className={`mb-2 flex h-8 w-8 items-center justify-center rounded-lg ${card.color}`}>
                <Icon className="h-4 w-4" />
              </div>
              <div className="text-2xl font-bold text-gray-900">{loading ? '--' : card.value}</div>
              <div className="text-xs text-gray-400">{card.label}</div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        {/* Mobile App Promotion Card (Replaces Scanner) */}
        <div className="lg:col-span-2 rounded-2xl border border-gray-100 bg-white p-8 shadow-sm flex flex-col items-center justify-center text-center space-y-6 m4-anim-fade-up">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600">
            <ScanLine className="h-8 w-8" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-gray-900">Mobile QR Scanning</h2>
            <p className="mt-2 text-sm text-gray-500 max-w-sm mx-auto">
              QR scanning is exclusively available on the Grid Operator mobile app to ensure secure physical energy transfers at the hub.
            </p>
          </div>
          <div className="w-full max-w-sm rounded-xl bg-gray-50 p-5 text-left border border-gray-100 shadow-inner">
            <h3 className="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
              Transfer Process
            </h3>
            <ol className="space-y-4 text-sm text-gray-600">
              <li className="flex gap-3 items-start">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600 mt-0.5">1</span>
                <span>Open the <strong>Grid Operator mobile app</strong> on your assigned device.</span>
              </li>
              <li className="flex gap-3 items-start">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600 mt-0.5">2</span>
                <span>Scan the Prosumer's reservation QR code.</span>
              </li>
              <li className="flex gap-3 items-start">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600 mt-0.5">3</span>
                <span>Verify the reservation details on-screen.</span>
              </li>
              <li className="flex gap-3 items-start">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-medium text-indigo-600 mt-0.5">4</span>
                <span>Confirm and finalize the energy transfer.</span>
              </li>
            </ol>
          </div>
          <p className="text-xs text-gray-400 mt-4">
            Completed transactions will automatically sync and appear in the log below.
          </p>
        </div>

        {/* Transaction table */}
        <div className="lg:col-span-3 rounded-2xl border border-gray-100 bg-white shadow-sm m4-anim-fade-up flex flex-col" style={{ animationDelay: '100ms' }}>
          <div className="border-b border-gray-100 px-6 py-5">
            <h2 className="text-base font-semibold text-gray-900">Recent Transactions</h2>
            <p className="text-sm text-gray-500">Live verified and completed transfers</p>
          </div>
          <div className="overflow-x-auto flex-1">
            {loading ? (
              <div className="py-12 text-center text-sm text-gray-400">Loading transactions...</div>
            ) : transactions.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full min-h-[300px] text-center px-6">
                <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-gray-50 text-gray-300 shadow-inner">
                  <CheckCircle2 className="h-7 w-7" />
                </div>
                <p className="text-sm text-gray-500 font-medium">No recent transactions</p>
                <p className="mt-1 text-xs text-gray-400">Verified and finalized reservations will appear here automatically.</p>
              </div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    <th className="px-5 py-3">Reservation</th>
                    <th className="px-5 py-3">Prosumer NIC</th>
                    <th className="px-5 py-3">Time Window</th>
                    <th className="px-5 py-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {transactions.map((tx) => (
                    <tr key={tx.reservationId} className="hover:bg-gray-50/50 transition-colors">
                      <td className="px-5 py-3 font-mono text-xs font-medium text-gray-900">{tx.reservationId}</td>
                      <td className="px-5 py-3 text-gray-600">{tx.prosumerNic}</td>
                      <td className="px-5 py-3 text-gray-600">
                        <div>{tx.bookingDate.slice(0, 10)}</div>
                        <div className="text-xs text-gray-400">{tx.startTime} – {tx.endTime}</div>
                      </td>
                      <td className="px-5 py-3">
                        <StatusBadge status={tx.status === 'Completed' ? 'completed' : 'verified'} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
