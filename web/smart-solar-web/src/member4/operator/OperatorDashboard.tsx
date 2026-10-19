/* Member 4: Operator Dashboard – Redesigned with prominent Station Identity & Modern Glassmorphism UI */
import { useEffect, useState } from 'react';
import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  RefreshCw,
  Zap,
  MapPin,
  BatteryCharging,
  QrCode,
  ArrowRight,
  ShieldCheck,
  Activity,
  Layers,
  Sparkles,
  Building2,
  AlertCircle,
  ExternalLink,
  ChevronRight,
  User,
  Radio
} from 'lucide-react';
import { fetchHubReservations, ApiService, type HubReservationPage, type Hub, type BookingSlot } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import StatusBadge from '@/components/ui/StatusBadge';
import './operator.css';

const emptyFilters = { reservationId: '', prosumerNic: '', station: '', bookingDate: '', status: '' };

type OperatorDashboardProps = {
  onNavigate?: (view: string) => void;
};

export default function OperatorDashboard({ onNavigate }: OperatorDashboardProps) {
  const { profile } = useAuth();
  const [data, setData] = useState<HubReservationPage | null>(null);
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');

    // Fetch hub reservations and available hubs concurrently
    Promise.all([
      fetchHubReservations('all', emptyFilters, 1, controller.signal),
      ApiService.getHubs().catch(() => [] as Hub[])
    ])
      .then(async ([reservationData, hubList]) => {
        if (controller.signal.aborted) return;
        setData(reservationData);
        setHubs(hubList || []);

        // Resolve station id to fetch exact real-time slots
        const itemStationId = reservationData?.items?.find((i: any) => i.stationId)?.stationId;
        const matched = (hubList || []).find(
          (h) =>
            (itemStationId && h.stationId === itemStationId) ||
            (profile?.assigned_hub_id && h.stationId === profile.assigned_hub_id) ||
            h.stationId === '66f000000000000000000001'
        );
        const resolvedStationId =
          matched?.stationId || itemStationId || profile?.assigned_hub_id || '66f000000000000000000001';

        try {
          const slotList = await ApiService.getBookingSlots(resolvedStationId);
          if (!controller.signal.aborted) {
            setSlots(slotList || []);
          }
        } catch {
          // Keep empty if slot endpoint fails
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : 'Unable to load metrics.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [revision]);

  // Resolve assigned station information
  const itemStationName = data?.items?.find((i: any) => i.stationName)?.stationName;
  const itemStationId = data?.items?.find((i: any) => i.stationId)?.stationId;

  const matchedHub = hubs.find(
    (h) =>
      (itemStationId && h.stationId === itemStationId) ||
      (profile?.assigned_hub_id && h.stationId === profile.assigned_hub_id) ||
      (itemStationName && h.stationName.toLowerCase() === itemStationName.toLowerCase()) ||
      (profile?.assigned_hub?.stationName &&
        h.stationName.toLowerCase() === profile.assigned_hub.stationName.toLowerCase())
  );

  const stationName =
    matchedHub?.stationName ||
    itemStationName ||
    profile?.assigned_hub?.stationName ||
    'Malabe Solar Station';
  const stationId =
    matchedHub?.stationId ||
    itemStationId ||
    profile?.assigned_hub_id ||
    '66f000000000000000000001';
  const capacityKwh = matchedHub?.capacityKwh ?? 100;
  // Calculate live slot counts from real-time EnergyBookingSlots collection
  const batterySlots = slots.length > 0 ? slots.length : (matchedHub?.batterySlotCount ?? 5);
  const availableSlots = slots.length > 0
    ? slots.filter((s) => (s.status?.toLowerCase() === 'available' || !s.status) && !s.reservationId).length
    : (matchedHub?.availableSlotCount ?? batterySlots);
  const schedule = matchedHub?.schedule || 'Standard 24/7';
  const location = matchedHub?.location || { latitude: 6.9147, longitude: 79.9729 };

  const metrics = [
    {
      label: 'Active Bookings',
      value: data?.summary.activeCount,
      icon: Zap,
      gradient: 'from-emerald-500 to-teal-600',
      badgeBg: 'bg-emerald-50',
      borderColor: 'border-emerald-100 hover:border-emerald-300',
      textColor: 'text-emerald-700',
      sub: 'Pending + Approved in hub',
      tag: 'Current Queue'
    },
    {
      label: 'Pending Approvals',
      value: data?.summary.pendingCount,
      icon: Clock3,
      gradient: 'from-amber-500 to-orange-600',
      badgeBg: 'bg-amber-50',
      borderColor: 'border-amber-100 hover:border-amber-300',
      textColor: 'text-amber-700',
      sub: (data?.summary.pendingCount ?? 0) > 0 ? 'Requires operator review' : 'All requests reviewed',
      tag: (data?.summary.pendingCount ?? 0) > 0 ? 'Action Needed' : 'All Clear',
      clickable: true,
      onClick: () => onNavigate?.('reservations')
    },
    {
      label: 'Ready for Service',
      value: data?.summary.approvedCount,
      icon: CalendarDays,
      gradient: 'from-blue-500 to-indigo-600',
      badgeBg: 'bg-blue-50',
      borderColor: 'border-blue-100 hover:border-blue-300',
      textColor: 'text-blue-700',
      sub: 'Confirmed prosumer arrivals',
      tag: 'Ready to Swap'
    },
    {
      label: 'Completed Services',
      value: data?.summary.completedCount,
      icon: CheckCircle2,
      gradient: 'from-violet-500 to-purple-600',
      badgeBg: 'bg-violet-50',
      borderColor: 'border-violet-100 hover:border-violet-300',
      textColor: 'text-violet-700',
      sub: 'All-time fulfilled energy sessions',
      tag: 'Fulfilled'
    }
  ];

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-10">

      {/* 1. COMMAND CENTER HERO BANNER – PROMINENT STATION IDENTITY     */}

      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-950 via-teal-900 to-emerald-900 text-white p-6 sm:p-8 shadow-xl border border-emerald-800/40 m4-anim-fade-down">
        {/* Ambient atmospheric lighting */}
        <div className="absolute -right-20 -top-20 w-80 h-80 rounded-full bg-emerald-500/10 blur-3xl pointer-events-none" />
        <div className="absolute -left-20 -bottom-20 w-80 h-80 rounded-full bg-teal-500/10 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-4 flex-1">
            {/* Main Station Name - Clean, Eye Catching & Professional */}
            <div>
              <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white tracking-tight drop-shadow-sm">
                {stationName}
              </h1>
              <p className="text-emerald-200/80 text-sm font-medium mt-1">
                Logged in Duty Operator:{' '}
                <strong className="text-white font-semibold">{profile?.full_name || 'Grid Operator'}</strong>
                {profile?.email && <span className="opacity-70 ml-1">({profile.email})</span>}
              </p>
            </div>

            {/* Station Specs & Live Telemetry Badges */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-1 max-w-4xl">
              <div className="bg-white/10 backdrop-blur-md border border-white/10 rounded-xl px-3.5 py-2.5">
                <div className="text-[11px] uppercase tracking-wider text-emerald-200/70 font-semibold">
                  Storage Capacity
                </div>
                <div className="text-base font-bold text-white mt-0.5">{capacityKwh} kWh</div>
              </div>

              <div className="bg-white/10 backdrop-blur-md border border-white/10 rounded-xl px-3.5 py-2.5">
                <div className="text-[11px] uppercase tracking-wider text-emerald-200/70 font-semibold">
                  Battery Slots
                </div>
                <div className="text-base font-bold text-white mt-0.5">
                  <span className="text-emerald-300">{availableSlots}</span> / {batterySlots} Free
                </div>
              </div>

              <div className="bg-white/10 backdrop-blur-md border border-white/10 rounded-xl px-3.5 py-2.5">
                <div className="text-[11px] uppercase tracking-wider text-emerald-200/70 font-semibold">
                  Station Hours
                </div>
                <div className="text-base font-bold text-white mt-0.5 truncate">{schedule}</div>
              </div>
            </div>
          </div>

          {/* Quick Action Buttons */}
          <div className="flex sm:flex-row lg:flex-col gap-2.5 flex-shrink-0 justify-end pt-2 lg:pt-0">
            <button
              onClick={() => onNavigate?.('qr-flow')}
              className="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white font-semibold text-sm shadow-lg shadow-emerald-950/40 transition-all hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
            >
              <QrCode size={18} />
              Scan Prosumer QR
            </button>

            <button
              disabled={loading}
              onClick={() => setRevision((r) => r + 1)}
              className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 border border-white/15 text-white/90 font-medium text-sm transition-all backdrop-blur-sm cursor-pointer disabled:opacity-50"
            >
              <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
              {loading ? 'Refreshing...' : 'Refresh Hub'}
            </button>
          </div>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 sm:p-5 flex items-center gap-3 text-rose-800 m4-anim-fade-up">
          <AlertCircle size={20} className="text-rose-600 flex-shrink-0" />
          <p className="flex-1 text-sm font-medium">{error}</p>
          <button
            onClick={() => setRevision((r) => r + 1)}
            className="px-3 py-1.5 bg-rose-600 text-white text-xs font-semibold rounded-lg hover:bg-rose-700 transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* 2. MODERN OPERATIONS METRICS CARDS                            */}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {metrics.map(({ label, value, icon: Icon, gradient, borderColor, textColor, sub, tag, clickable, onClick }, index) => (
          <article
            key={label}
            onClick={clickable ? onClick : undefined}
            className={`group bg-white rounded-2xl p-5 border ${borderColor} shadow-sm hover:shadow-md transition-all duration-200 relative overflow-hidden m4-anim-scale-in ${clickable ? 'cursor-pointer' : 'cursor-default'
              }`}
            style={{ animationDelay: `${index * 80}ms` }}
          >
            <div className="flex items-center justify-between mb-3">
              <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center text-white shadow-sm shadow-emerald-900/10 group-hover:scale-105 transition-transform`}>
                <Icon size={20} />
              </div>
              <span className="text-[11px] font-semibold tracking-wide uppercase px-2.5 py-1 rounded-full bg-gray-100 text-gray-600 group-hover:bg-gray-200 transition-colors">
                {tag}
              </span>
            </div>

            <div className="space-y-1">
              <div className={`text-3xl font-extrabold ${textColor} tracking-tight`}>
                {loading || value == null ? (
                  <span className="text-gray-300 animate-pulse">--</span>
                ) : (
                  value.toLocaleString()
                )}
              </div>
              <h3 className="text-sm font-bold text-gray-800">{label}</h3>
              <p className="text-xs text-gray-500 font-medium leading-relaxed">{sub}</p>
            </div>

            {clickable && (
              <div className="mt-3 pt-2.5 border-t border-gray-100 flex items-center justify-between text-xs font-semibold text-amber-600 group-hover:text-amber-700">
                <span>Review requests</span>
                <ChevronRight size={14} className="group-hover:translate-x-1 transition-transform" />
              </div>
            )}
          </article>
        ))}
      </div>


      {/* 3. LOWER OPERATIONS HUB: RECENT QUEUE & STATION DIAGNOSTICS   */}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Recent Station Reservation Queue (2 cols wide) */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
          <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-gray-50/60">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl text-emerald-700">
                <CalendarDays size={18} />
              </div>
              <div>
                <h3 className="text-sm font-bold text-gray-900 leading-tight">Recent Station Reservations</h3>
                <p className="text-xs text-gray-500 mt-0.5">Live prosumer queue for {stationName}</p>
              </div>
            </div>

            <button
              onClick={() => onNavigate?.('reservations')}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700 hover:text-emerald-800 bg-emerald-50 hover:bg-emerald-100 px-3 py-1.5 rounded-lg border border-emerald-200/60 transition-colors cursor-pointer"
            >
              View All ({data?.totalRecords ?? 0})
              <ArrowRight size={13} />
            </button>
          </div>

          <div className="p-0 flex-1 overflow-x-auto">
            {loading ? (
              <div className="p-8 text-center text-sm text-gray-400 space-y-2">
                <RefreshCw size={24} className="animate-spin mx-auto text-emerald-600" />
                <p>Loading station queue...</p>
              </div>
            ) : data?.items && data.items.length > 0 ? (
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/70 text-[11px] uppercase tracking-wider text-gray-500 font-semibold">
                    <th className="py-3 px-6">Prosumer NIC</th>
                    <th className="py-3 px-4">Booking Date & Time</th>
                    <th className="py-3 px-4">Slot</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-6 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data.items.slice(0, 5).map((item) => (
                    <tr key={item.reservationId} className="hover:bg-gray-50/70 transition-colors">
                      <td className="py-3.5 px-6">
                        <div className="flex items-center gap-2">
                          <div className="h-7 w-7 rounded-full bg-gray-100 text-gray-600 flex items-center justify-center font-bold text-xs">
                            <User size={13} />
                          </div>
                          <div>
                            <span className="font-semibold text-gray-900 font-mono text-xs block">
                              {item.prosumerNic}
                            </span>
                            <span className="text-[10px] text-gray-400 font-mono">
                              ID: {item.reservationId.slice(-6)}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="text-xs font-medium text-gray-800">
                          {new Date(item.bookingDate).toLocaleDateString(undefined, {
                            month: 'short',
                            day: 'numeric',
                            year: 'numeric'
                          })}
                        </div>
                        <div className="text-[11px] text-gray-500">
                          {item.startTime} - {item.endTime}
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-700">
                          Slot {item.slotNumber ?? '01'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={item.status.toLowerCase()} />
                      </td>
                      <td className="py-3.5 px-6 text-right">
                        <button
                          onClick={() => onNavigate?.('reservations')}
                          className="text-xs font-semibold text-emerald-700 hover:text-emerald-900 hover:underline cursor-pointer"
                        >
                          Manage
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="py-12 px-6 text-center text-gray-500 space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center">
                  <CheckCircle2 size={24} />
                </div>
                <div>
                  <h4 className="font-semibold text-gray-800 text-sm">Station Queue is Clear</h4>
                  <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
                    There are currently no prosumer reservations waiting at {stationName}.
                  </p>
                </div>
                <button
                  onClick={() => onNavigate?.('qr-flow')}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-lg transition-colors cursor-pointer"
                >
                  <QrCode size={14} />
                  Scan Walk-In QR
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Station Hardware & Grid Telemetry (1 col wide) */}
        <div>
          {/* Station Diagnostics Card */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2 text-gray-900 font-bold text-sm">
                <Activity size={16} className="text-emerald-600" />
                <span>Station Health & Diagnostics</span>
              </div>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50">
                <div className="flex items-center gap-2 text-gray-700">
                  <Radio size={14} className="text-emerald-600" />
                  <span className="font-medium">Grid Frequency</span>
                </div>
                <span className="font-bold text-gray-900 font-mono">50.02 Hz (Stable)</span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50">
                <div className="flex items-center gap-2 text-gray-700">
                  <Zap size={14} className="text-amber-500" />
                  <span className="font-medium">Inverter Efficiency</span>
                </div>
                <span className="font-bold text-emerald-700 font-mono">98.4% (Optimal)</span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50">
                <div className="flex items-center gap-2 text-gray-700">
                  <BatteryCharging size={14} className="text-teal-600" />
                  <span className="font-medium">Battery Bank Health</span>
                </div>
                <span className="font-bold text-emerald-700 font-mono">100% Ready</span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50">
                <div className="flex items-center gap-2 text-gray-700">
                  <QrCode size={14} className="text-blue-600" />
                  <span className="font-medium">QR Scanning Node</span>
                </div>
                <span className="font-bold text-blue-700 font-mono">Online & Armed</span>
              </div>
            </div>

            {/* Live Station Battery Slots Matrix */}
            {slots.length > 0 && (
              <div className="pt-3 border-t border-gray-100 space-y-2.5">
                <div className="flex items-center justify-between text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                  <span>Battery Slot Matrix</span>
                  <span className="text-emerald-700 font-mono font-bold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    {availableSlots} / {batterySlots} Free
                  </span>
                </div>
                <div className="grid grid-cols-5 gap-1.5">
                  {slots.map((s) => {
                    const isAvailable = (s.status?.toLowerCase() === 'available' || !s.status) && !s.reservationId;
                    return (
                      <div
                        key={s.slotId}
                        className={`p-2 rounded-xl text-center border transition-all ${
                          isAvailable
                            ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
                            : 'bg-amber-50/70 border-amber-200 text-amber-900'
                        }`}
                        title={`Slot ${s.slotNumber}: ${s.status} (${s.capacityKwh} kWh)`}
                      >
                        <div className="text-[10px] uppercase font-bold text-gray-500">Slot {s.slotNumber}</div>
                        <div className="text-xs font-black font-mono mt-0.5">{s.capacityKwh}k</div>
                        <div className="text-[9px] font-semibold mt-0.5 capitalize truncate">
                          {isAvailable ? 'Free' : 'Booked'}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
