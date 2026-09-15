import { useEffect, useState, useCallback } from 'react';
import { ApiService } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import type { QrTransaction, QrTransactionStatus } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import QrCodeDisplay from '@/components/ui/QrCodeDisplay';
import TransactionTimeline from '@/components/TransactionTimeline';
import {
  QrCode, Search, ScanLine, ShieldCheck, CheckCircle2, XCircle, Clock,
  Zap, TrendingUp, Eye, AlertTriangle, Loader2,
} from 'lucide-react';

type ScanState = {
  step: 'idle' | 'scanning' | 'verified' | 'completed' | 'error';
  token: string;
  verifyData?: {
    transaction_id: string;
    reservation_id_human: string;
    prosumer_name: string;
    prosumer_nic: string;
    hub_name: string;
    energy_amount: number;
    reservation_date: string;
  };
  errorMessage?: string;
};

export default function QrFlow() {
  const { role, profile } = useAuth();
  const [transactions, setTransactions] = useState<QrTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [tokenInput, setTokenInput] = useState('');
  const [scanState, setScanState] = useState<ScanState>({ step: 'idle', token: '' });
  const [busy, setBusy] = useState(false);
  const [viewTx, setViewTx] = useState<QrTransaction | null>(null);
  const [viewQrTx, setViewQrTx] = useState<QrTransaction | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    const { data } = await supabase
      .from('qr_transactions')
      .select('*, reservation:reservations!reservation_id(*, prosumer:prosumers!prosumer_id(*), hub:hubs!hub_id(*), slot:booking_slots!slot_id(*)), operator:operators!operator_id(*)')
      .order('scanned_at', { ascending: false })
      .limit(50);
    setTransactions((data as QrTransaction[]) ?? []);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const stats = {
    total: transactions.length,
    generated: transactions.filter((t) => t.status === 'generated').length,
    scanned: transactions.filter((t) => t.status === 'scanned').length,
    verified: transactions.filter((t) => t.status === 'verified').length,
    completed: transactions.filter((t) => t.status === 'completed' || t.status === 'finalized').length,
    cancelled: transactions.filter((t) => t.status === 'cancelled' || t.status === 'invalid').length,
    expired: transactions.filter((t) => t.status === 'expired').length,
  };

  const handleScan = async () => {
    if (!tokenInput.trim()) return;
    setBusy(true);
    setScanState({ step: 'scanning', token: tokenInput.trim() });

    const { data, error } = await verifyQrToken(tokenInput.trim());

    if (error) {
      setScanState({ step: 'error', token: tokenInput.trim(), errorMessage: error });
      setBusy(false);
      return;
    }

    if (data) {
      setScanState({
        step: 'verified',
        token: tokenInput.trim(),
        verifyData: {
          transaction_id: data.transaction_id,
          reservation_id_human: data.reservation_id_human,
          prosumer_name: data.prosumer_name,
          prosumer_nic: data.prosumer_nic,
          hub_name: data.hub_name,
          energy_amount: data.energy_amount,
          reservation_date: data.reservation_date,
        },
      });
      load();
    }
    setBusy(false);
  };

  const handleFinalize = async () => {
    if (!scanState.token) return;
    setBusy(true);
    const { data, error } = await finalizeQrTransaction(scanState.token);
    if (error) {
      setScanState({ ...scanState, step: 'error', errorMessage: error });
      setBusy(false);
      return;
    }
    if (data) {
      setScanState({ ...scanState, step: 'completed' });
      load();
    }
    setBusy(false);
  };

  const resetScanner = () => {
    setTokenInput('');
    setScanState({ step: 'idle', token: '' });
  };

  const timelineSteps = (tx: QrTransaction) => [
    { label: 'QR Generated', status: 'generated' as QrTransactionStatus, time: tx.generated_at ?? tx.scanned_at },
    { label: 'Approved', status: 'approved' as QrTransactionStatus, time: tx.reservation?.created_at ?? null },
    { label: 'Scanned by Operator', status: 'scanned' as QrTransactionStatus, time: tx.scanned_at },
    { label: 'Verified', status: 'verified' as QrTransactionStatus, time: tx.verified_at },
    { label: 'Completed', status: 'completed' as QrTransactionStatus, time: tx.finalized_at },
  ];

  const statCards = [
    { label: 'Total', value: stats.total, icon: TrendingUp, color: 'bg-gray-50 text-gray-600' },
    { label: 'Generated', value: stats.generated, icon: QrCode, color: 'bg-cyan-50 text-cyan-600' },
    { label: 'Scanned', value: stats.scanned, icon: ScanLine, color: 'bg-sky-50 text-sky-600' },
    { label: 'Verified', value: stats.verified, icon: ShieldCheck, color: 'bg-indigo-50 text-indigo-600' },
    { label: 'Completed', value: stats.completed, icon: CheckCircle2, color: 'bg-emerald-50 text-emerald-600' },
    { label: 'Expired', value: stats.expired, icon: Clock, color: 'bg-orange-50 text-orange-600' },
  ];

  return (
    <div className="space-y-6">
      {/* Stats */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.label} className="rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
              <div className={`mb-2 flex h-8 w-8 items-center justify-center rounded-lg ${card.color}`}>
                <Icon className="h-4 w-4" />
              </div>
              <div className="text-2xl font-bold text-gray-900">{card.value}</div>
              <div className="text-xs text-gray-400">{card.label}</div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        {/* Scanner panel */}
        <div className="lg:col-span-2 rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
              <QrCode className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-gray-900">QR Scanner</h2>
              <p className="text-sm text-gray-500">
                {role === 'grid_operator'
                  ? `Scan prosumer QR to verify & finalize (${profile?.badge_id ?? 'No badge'})`
                  : 'Verify and finalize transactions'}
              </p>
            </div>
          </div>

          {/* Step indicator */}
          <div className="mt-6 flex items-center justify-between">
            {[
              { label: 'Scan', icon: ScanLine, active: scanState.step !== 'idle' },
              { label: 'Verify', icon: ShieldCheck, active: scanState.step === 'verified' || scanState.step === 'completed' },
              { label: 'Finalize', icon: CheckCircle2, active: scanState.step === 'completed' },
            ].map((step, i) => {
              const Icon = step.icon;
              return (
                <div key={step.label} className="flex flex-1 items-center">
                  <div className="flex flex-col items-center gap-1.5">
                    <div className={`flex h-10 w-10 items-center justify-center rounded-full transition-all ${
                      step.active ? 'bg-indigo-500 text-white' : 'bg-gray-100 text-gray-400'
                    } ${scanState.step === 'scanning' && i === 0 ? 'animate-pulse' : ''}`}>
                      {scanState.step === 'scanning' && i === 0 ? (
                        <Loader2 className="h-5 w-5 animate-spin" />
                      ) : (
                        <Icon className="h-5 w-5" />
                      )}
                    </div>
                    <span className={`text-xs font-medium ${step.active ? 'text-gray-900' : 'text-gray-400'}`}>{step.label}</span>
                  </div>
                  {i < 2 && <div className={`mx-2 h-0.5 flex-1 rounded ${step.active ? 'bg-indigo-500' : 'bg-gray-200'}`} />}
                </div>
              );
            })}
          </div>

          {/* Input */}
          <div className="mt-6">
            <label className="mb-1.5 block text-sm font-medium text-gray-700">QR Token</label>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  placeholder="Paste or scan QR token..."
                  value={tokenInput}
                  onChange={(e) => setTokenInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleScan()}
                  className="w-full rounded-lg border-0 py-2.5 pl-10 pr-3.5 text-sm font-mono text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-500"
                />
              </div>
              <Button onClick={handleScan} disabled={busy || !tokenInput.trim()}>
                Scan
              </Button>
            </div>
            <p className="mt-1.5 text-xs text-gray-400">
              Tokens are generated server-side when a reservation is confirmed.
            </p>
          </div>

          {/* Results */}
          {scanState.step === 'error' && (
            <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4">
              <div className="flex items-center gap-2 text-rose-700">
                <XCircle className="h-5 w-5" />
                <span className="font-medium">Verification Failed</span>
              </div>
              <p className="mt-1.5 text-sm text-rose-600">{scanState.errorMessage}</p>
              <Button variant="secondary" size="sm" className="mt-3" onClick={resetScanner}>Try Again</Button>
            </div>
          )}

          {scanState.step === 'verified' && scanState.verifyData && (
            <div className="mt-4 space-y-3 rounded-xl border border-indigo-200 bg-indigo-50 p-4">
              <div className="flex items-center gap-2 text-indigo-700">
                <ShieldCheck className="h-5 w-5" />
                <span className="font-medium">Verified by Server</span>
              </div>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <div className="text-xs text-indigo-600/60">Transaction ID</div>
                  <div className="font-mono text-gray-900">{scanState.verifyData.transaction_id}</div>
                </div>
                <div>
                  <div className="text-xs text-indigo-600/60">Reservation</div>
                  <div className="font-mono text-gray-900">{scanState.verifyData.reservation_id_human}</div>
                </div>
                <div>
                  <div className="text-xs text-indigo-600/60">Prosumer</div>
                  <div className="text-gray-900">{scanState.verifyData.prosumer_name}</div>
                </div>
                <div>
                  <div className="text-xs text-indigo-600/60">NIC</div>
                  <div className="font-mono text-gray-900">{scanState.verifyData.prosumer_nic}</div>
                </div>
                <div>
                  <div className="text-xs text-indigo-600/60">Hub</div>
                  <div className="text-gray-900">{scanState.verifyData.hub_name}</div>
                </div>
                <div>
                  <div className="text-xs text-indigo-600/60">Energy</div>
                  <div className="text-gray-900">{scanState.verifyData.energy_amount} kWh</div>
                </div>
              </div>
              <Button className="w-full" onClick={handleFinalize} disabled={busy}>
                {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                Confirm & Finalize Energy Transfer
              </Button>
            </div>
          )}

          {scanState.step === 'completed' && (
            <div className="mt-4 space-y-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
              <div className="flex items-center gap-2 text-emerald-700">
                <CheckCircle2 className="h-5 w-5" />
                <span className="font-medium">Energy transfer completed successfully</span>
              </div>
              <p className="text-sm text-emerald-600">
                The transaction has been finalized. Status updated to COMPLETED in the central database.
              </p>
              <Button variant="secondary" className="w-full" onClick={resetScanner}>Scan Next</Button>
            </div>
          )}
        </div>

        {/* Transaction table */}
        <div className="lg:col-span-3 rounded-2xl border border-gray-100 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-6 py-4">
            <h2 className="text-base font-semibold text-gray-900">Transaction Log</h2>
            <p className="text-sm text-gray-500">All QR transactions with server-verified status</p>
          </div>
          <div className="overflow-x-auto">
            {loading ? (
              <div className="py-12 text-center text-sm text-gray-400">Loading transactions...</div>
            ) : transactions.length === 0 ? (
              <div className="flex flex-col items-center py-12 text-center">
                <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-gray-50 text-gray-300">
                  <QrCode className="h-7 w-7" />
                </div>
                <p className="text-sm text-gray-500">No transactions yet</p>
                <p className="mt-1 text-xs text-gray-400">Confirm a reservation to generate a QR transaction.</p>
              </div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    <th className="px-4 py-3">Transaction</th>
                    <th className="px-4 py-3">Prosumer</th>
                    <th className="px-4 py-3">Hub</th>
                    <th className="px-4 py-3">Energy</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-gray-50/50 transition-colors">
                      <td className="px-4 py-3">
                        <div className="font-mono text-xs font-medium text-gray-900">{tx.transaction_id ?? '—'}</div>
                        <div className="font-mono text-xs text-gray-400">{tx.reservation?.reservation_id_human ?? '—'}</div>
                      </td>
                      <td className="px-4 py-3 text-gray-600">{tx.reservation?.prosumer?.full_name ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-600">{tx.reservation?.hub?.name ?? '—'}</td>
                      <td className="px-4 py-3"><span className="font-semibold text-gray-900">{tx.energy_amount ?? tx.reservation?.energy_kwh ?? '—'}</span> kWh</td>
                      <td className="px-4 py-3"><StatusBadge status={tx.status} /></td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => setViewTx(tx)}
                            className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                            title="View details"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                          {tx.token && (tx.status === 'generated' || tx.status === 'approved' || tx.status === 'scanned' || tx.status === 'verified') && (
                            <button
                              onClick={() => setViewQrTx(tx)}
                              className="rounded-md p-1.5 text-indigo-400 hover:bg-indigo-50 hover:text-indigo-600"
                              title="View QR"
                            >
                              <QrCode className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {/* Transaction details modal */}
      <Modal
        open={!!viewTx}
        onClose={() => setViewTx(null)}
        title="QR Transaction Details"
        size="lg"
        footer={<Button variant="secondary" onClick={() => setViewTx(null)}>Close</Button>}
      >
        {viewTx && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <div className="space-y-4">
              <div className="rounded-xl bg-gray-50 p-4">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Transaction ID</span>
                    <span className="font-mono text-sm font-semibold text-gray-900">{viewTx.transaction_id ?? '—'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Reservation ID</span>
                    <span className="font-mono text-sm text-gray-900">{viewTx.reservation?.reservation_id_human ?? '—'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Prosumer</span>
                    <span className="text-sm text-gray-900">{viewTx.reservation?.prosumer?.full_name ?? '—'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Prosumer NIC</span>
                    <span className="font-mono text-sm text-gray-900">{viewTx.reservation?.prosumer?.nic ?? '—'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Hub</span>
                    <span className="text-sm text-gray-900">{viewTx.reservation?.hub?.name ?? '—'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Energy</span>
                    <span className="text-sm font-semibold text-gray-900">{viewTx.energy_amount ?? viewTx.reservation?.energy_kwh ?? '—'} kWh</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-400">Operator</span>
                    <span className="text-sm text-gray-900">{viewTx.operator?.full_name ?? '—'}</span>
                  </div>
                  <div className="flex items-center justify-between border-t border-gray-200 pt-3">
                    <span className="text-xs text-gray-400">Status</span>
                    <StatusBadge status={viewTx.status} />
                  </div>
                  {viewTx.expiry_time && (
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-gray-400">Expires</span>
                      <span className="text-xs text-gray-600">{new Date(viewTx.expiry_time).toLocaleString()}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div>
              <h3 className="mb-4 text-sm font-semibold text-gray-900">Transaction Timeline</h3>
              <TransactionTimeline
                currentStatus={viewTx.status}
                steps={timelineSteps(viewTx)}
              />
            </div>
          </div>
        )}
      </Modal>

      {/* QR viewer modal */}
      <Modal
        open={!!viewQrTx}
        onClose={() => setViewQrTx(null)}
        title="QR Code Display"
        description="Reference QR for this transaction"
        footer={<Button variant="secondary" onClick={() => setViewQrTx(null)}>Close</Button>}
      >
        {viewQrTx && (
          <div className="flex flex-col items-center space-y-4">
            <QrCodeDisplay value={viewQrTx.token} size={240} />
            <div className="w-full space-y-2 rounded-xl bg-gray-50 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Transaction ID</span>
                <span className="font-mono text-sm font-semibold text-gray-900">{viewQrTx.transaction_id}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Reservation</span>
                <span className="font-mono text-sm text-gray-900">{viewQrTx.reservation?.reservation_id_human ?? '—'}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Hub</span>
                <span className="text-sm text-gray-900">{viewQrTx.reservation?.hub?.name ?? '—'}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Energy</span>
                <span className="text-sm text-gray-900">{viewQrTx.energy_amount ?? viewQrTx.reservation?.energy_kwh} kWh</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Status</span>
                <StatusBadge status={viewQrTx.status} />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Created</span>
                <span className="text-xs text-gray-600">{new Date(viewQrTx.generated_at ?? viewQrTx.scanned_at).toLocaleString()}</span>
              </div>
            </div>
            <p className="text-xs text-gray-400 text-center">
              This QR is for reference. Authoritative verification happens through the server API.
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}
