import { useEffect, useState, useCallback } from 'react';
import { ApiService, type ReservationSummary, type Hub, type BookingSlot } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import QrCodeDisplay from '@/components/ui/QrCodeDisplay';
import {
  ArrowLeft,
  CalendarDays,
  Clock,
  Zap,
  QrCode,
  Pencil,
  Trash2,
  AlertTriangle,
  MapPin,
  BatteryCharging,
  Layers,
  ExternalLink
} from 'lucide-react';

type ViewReservationProps = {
  reservationId: string;
  onBack: () => void;
};

export default function ViewReservation({ reservationId, onBack }: ViewReservationProps) {
  const [reservation, setReservation] = useState<ReservationSummary | null>(null);
  const [currentStation, setCurrentStation] = useState<Hub | null>(null);
  const [currentSlot, setCurrentSlot] = useState<BookingSlot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Update modal state
  const [updateOpen, setUpdateOpen] = useState(false);
  const [stations, setStations] = useState<Hub[]>([]);
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [targetStationId, setTargetStationId] = useState('');
  const [targetSlotId, setTargetSlotId] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [targetStart, setTargetStart] = useState('');
  const [targetEnd, setTargetEnd] = useState('');
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [updating, setUpdating] = useState(false);

  // Cancel modal state
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);

  // QR Modal
  const [qrOpen, setQrOpen] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await ApiService.getReservationById(reservationId);
      setReservation(res);

      // Fetch station details for capacity, slot count, and map coordinates
      if (res?.stationId) {
        try {
          const hubs = await ApiService.getHubs();
          const matchedHub = hubs.find((h) => h.stationId === res.stationId);
          if (matchedHub) setCurrentStation(matchedHub);

          // Fetch slots to obtain slot capacity
          const slotList = await ApiService.getBookingSlots(res.stationId);
          const matchedSlot = slotList.find((s) => s.slotId === res.slotId);
          if (matchedSlot) setCurrentSlot(matchedSlot);
        } catch (hubErr) {
          console.error('Failed to load station/slot metadata:', hubErr);
        }
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch reservation.');
    }
    setLoading(false);
  }, [reservationId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Helper to parse "09:00 AM" into components
  function parse12HourTime(timeStr: string) {
    const match = timeStr?.trim().match(/^(0?[1-9]|1[0-2]):([0-5][0-9])\s?(AM|PM)$/i);
    if (match) {
      return {
        hour: match[1].padStart(2, '0'),
        minute: match[2],
        period: match[3].toUpperCase(),
      };
    }
    return { hour: '09', minute: '00', period: 'AM' };
  }

  // Helper to rebuild "09:00 AM" string
  function format12HourTime(hour: string, minute: string, period: string) {
    return `${hour.padStart(2, '0')}:${minute} ${period}`;
  }

  // Open Update Modal and prefill current values
  const handleOpenUpdate = async () => {
    if (!reservation) return;
    setUpdateError(null);
    setTargetStationId(reservation.stationId);
    setTargetDate(new Date(reservation.bookingDate).toISOString().split('T')[0]);
    setTargetStart(reservation.startTime);
    setTargetEnd(reservation.endTime);

    try {
      const hubs = await ApiService.getHubs();
      setStations(hubs.filter((h) => h.status?.toLowerCase() === 'active'));

      const slotList = await ApiService.getBookingSlots(reservation.stationId);
      setSlots(slotList);
      setTargetSlotId(reservation.slotId);
      setUpdateOpen(true);
    } catch (err) {
      setUpdateError('Failed to load stations or slots for update.');
    }
  };

  const handleStationChange = async (stationId: string) => {
    setTargetStationId(stationId);
    try {
      const slotList = await ApiService.getBookingSlots(stationId);
      setSlots(slotList);
      if (slotList.length > 0) {
        setTargetSlotId(slotList[0].slotId);
      }
    } catch {
      setSlots([]);
    }
  };

  const handleSaveUpdate = async () => {
    setUpdateError(null);
    setUpdating(true);
    try {
      await ApiService.updateReservation(reservationId, {
        stationId: targetStationId,
        slotId: targetSlotId,
        bookingDate: new Date(targetDate).toISOString(),
        startTime: targetStart,
        endTime: targetEnd,
      });

      try {
        await ApiService.generateQrCode(reservationId);
      } catch (e) { }

      setUpdateOpen(false);
      loadData();
    } catch (err: any) {
      setUpdateError(err.message || 'Failed to update reservation.');
    }
    setUpdating(false);
  };

  const handleConfirmCancel = async () => {
    setCancelError(null);
    setCancelling(true);
    try {
      await ApiService.cancelReservation(
        reservationId,
        cancelReason || 'Cancelled by prosumer from web portal'
      );
      setCancelOpen(false);
      loadData();
    } catch (err: any) {
      setCancelError(err.message || 'Failed to cancel reservation.');
    }
    setCancelling(false);
  };

  if (loading) {
    return <div className="py-20 text-center text-sm text-gray-500">Loading reservation details...</div>;
  }

  if (error || !reservation) {
    return (
      <div className="space-y-4">
        <button onClick={onBack} className="flex items-center gap-2 text-sm text-gray-500 hover:text-gray-800">
          <ArrowLeft className="h-4 w-4" /> Back
        </button>
        <div className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">
          {error || 'Reservation could not be found.'}
        </div>
      </div>
    );
  }

  const isCancelled = reservation.status.toLowerCase() === 'cancelled';
  const isEditable = reservation.status === 'Pending' || reservation.status === 'Approved';

  const latitude = currentStation?.location?.latitude;
  const longitude = currentStation?.location?.longitude;
  const hasCoordinates = typeof latitude === 'number' && typeof longitude === 'number';

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <button
        onClick={onBack}
        className="flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-gray-800 transition-colors"
      >
        <ArrowLeft className="h-4 w-4" /> Back to Dashboard
      </button>

      {/* Main Reservation Card */}
      <div className="rounded-2xl border border-gray-100 bg-white p-7 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-100 pb-5">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 mt-0.5">{reservation.stationName}</h1>
          </div>
          <StatusBadge status={reservation.status.toLowerCase()} />
        </div>

        {/* Reservation Details */}
        <div className="rounded-xl border border-gray-100 bg-gray-50/60 p-4 space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-gray-500">
            <BatteryCharging className="h-4 w-4 text-emerald-500" /> Reservation Information
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-2 gap-3 text-sm">
            <div>
              <span className="text-xs text-gray-500">Slot Number</span>
              <div className="font-semibold text-gray-800">Slot No. {reservation.slotNumber}</div>
            </div>
            <div>
              <span className="text-xs text-gray-500">Slot Capacity</span>
              <div className="font-semibold text-gray-800">
                {currentSlot ? `${currentSlot.capacityKwh} kWh` : '—'}
              </div>
            </div>

            <div>
              <span className="text-xs text-gray-500">Scheduled Date</span>
              <div className="font-semibold text-gray-800">
                {new Date(reservation.bookingDate).toLocaleDateString(undefined, {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-500">Scheduled Time</span>
              <div className="font-semibold text-gray-800">
                {reservation.startTime} - {reservation.endTime}
              </div>
            </div>
          </div>
        </div>

        {/* Cancellation Reason Notice */}
        {isCancelled && (
          <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50/80 p-4 text-rose-800">
            <AlertTriangle className="h-5 w-5 shrink-0 text-rose-500 mt-0.5" />
            <div>
              <div className="text-xs font-bold uppercase tracking-wider text-rose-700">Cancellation Reason</div>
              <p className="mt-1 text-sm font-medium">
                {reservation.cancellationReason || 'No specific reason provided.'}
              </p>
            </div>
          </div>
        )}

        {/* QR Section */}
        {!isCancelled && (
          <div>
            {reservation.qrToken ? (
              <div className="flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50/50 p-4">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-500 text-white shadow-sm">
                    <QrCode className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-gray-900">Reservation QR Code</div>
                    <div className="text-xs text-gray-500">To be scanned by Grid Operators</div>
                  </div>
                </div>
                <Button size="sm" onClick={() => setQrOpen(true)}>
                  View QR Code
                </Button>
              </div>
            ) : (
              <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-center text-xs text-gray-500">
                QR code is not generated for this reservation.
              </div>
            )}
          </div>
        )}

        {/* Station Details & Map */}
        <div className="rounded-xl border border-gray-100 bg-gray-50/60 p-4 space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-gray-500">
            <Zap className="h-4 w-4 text-amber-500" /> {currentStation ? `${currentStation.stationName} Station Information` : 'Station Information'}
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
            <div>
              <span className="text-xs text-gray-500">Station Capacity</span>
              <div className="font-semibold text-gray-800">
                {currentStation ? `${currentStation.capacityKwh} kWh` : '—'}
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-500">Total Battery Slots</span>
              <div className="font-semibold text-gray-800">
                {currentStation ? currentStation.batterySlotCount : '—'}
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-500">Available Slots</span>
              <div className="font-semibold text-gray-800">
                {currentStation ? currentStation.availableSlotCount : '—'}
              </div>
            </div>
          </div>

          {/* Map Preview */}
          {hasCoordinates && (
            <div className="mt-3 overflow-hidden rounded-lg border border-gray-200 bg-white">
              <iframe
                title="Station Location Map"
                width="100%"
                height="190"
                loading="lazy"
                className="border-0"
                src={`https://maps.google.com/maps?q=${latitude},${longitude}&hl=en&z=14&output=embed`}
              />
              <div className="flex items-center justify-between px-3 py-2 text-xs text-gray-500 bg-gray-50">
                <span className="flex items-center gap-1 font-mono">
                  <MapPin className="h-3.5 w-3.5 text-rose-500" />
                  {latitude?.toFixed(4)}°, {longitude?.toFixed(4)}°
                </span>
                <a
                  href={`https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-1 font-medium text-amber-600 hover:text-amber-700"
                >
                  Open in Google Maps <ExternalLink className="h-3 w-3" />
                </a>
              </div>
            </div>
          )}
        </div>

        {/* Timestamps Section */}
        <div className="rounded-xl border border-gray-100 bg-gray-50/40 p-4">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
            <Clock className="h-3.5 w-3.5" /> Activity Timeline
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-2 gap-3 text-sm text-gray-600">
            {reservation.createdAt && (
              <div>
                <span className="text-gray-500 block">Created At</span>
                <span className="font-medium text-gray-800">
                  {new Date(reservation.createdAt).toLocaleString()}
                </span>
              </div>
            )}
            {reservation.verifiedAt && (
              <div>
                <span className="text-gray-500 block">Verified At</span>
                <span className="font-medium text-gray-800">
                  {new Date(reservation.verifiedAt).toLocaleString()}
                </span>
              </div>
            )}
            {reservation.completedAt && (
              <div>
                <span className="text-gray-500 block">Completed At</span>
                <span className="font-medium text-gray-800">
                  {new Date(reservation.completedAt).toLocaleString()}
                </span>
              </div>
            )}
            {reservation.updatedAt && (
              <div>
                <span className="text-gray-500 block">Last Updated</span>
                <span className="font-medium text-gray-800">
                  {new Date(reservation.updatedAt).toLocaleString()}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center justify-end gap-3 border-t border-gray-100 pt-5">
          {isEditable ? (
            <>
              <Button variant="secondary" onClick={handleOpenUpdate}>
                <Pencil className="h-4 w-4" /> Update Schedule
              </Button>
              <Button variant="danger" onClick={() => setCancelOpen(true)}>
                <Trash2 className="h-4 w-4" /> Cancel Booking
              </Button>
            </>
          ) : (
            <p className="text-xs text-gray-500 italic">
              This reservation is {reservation.status} and can no longer be updated or cancelled.
            </p>
          )}
        </div>
      </div>

      {/* Update Schedule Modal */}
      <Modal
        open={updateOpen}
        onClose={() => setUpdateOpen(false)}
        title="Update Reservation"
        description="Reservations can be rescheduled at least 12 hours before the reserved time."
        footer={
          <>
            <Button variant="secondary" onClick={() => setUpdateOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleSaveUpdate}
              disabled={updating || slots.length === 0}
            >
              {updating ? 'Saving...' : 'Save Changes'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          {updateError && (
            <div className="rounded-lg bg-rose-50 px-3.5 py-2.5 text-xs text-rose-700">
              {updateError}
            </div>
          )}

          {/* Station Selector */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Grid Station</label>
            <select
              value={targetStationId}
              onChange={(e) => handleStationChange(e.target.value)}
              className="w-full rounded-lg border-0 py-2 px-3 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
            >
              {stations.map((s) => (
                <option key={s.stationId} value={s.stationId}>
                  {s.stationName}
                </option>
              ))}
            </select>
          </div>

          {/* Slot Selector or Empty Notice */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Slot</label>
            {slots.length === 0 ? (
              <div className="rounded-lg border border-dashed border-amber-300 bg-amber-50/60 p-3 text-center text-xs text-amber-800">
                No slots defined for this station. Please choose another station.
              </div>
            ) : (
              <select
                value={targetSlotId}
                onChange={(e) => {
                  setTargetSlotId(e.target.value);
                  const selectedSlot = slots.find((s) => s.slotId === e.target.value);
                  if (selectedSlot?.startTime && selectedSlot.startTime.includes(':')) {
                    setTargetStart(selectedSlot.startTime);
                  }
                  if (selectedSlot?.endTime && selectedSlot.endTime.includes(':')) {
                    setTargetEnd(selectedSlot.endTime);
                  }
                }}
                className="w-full rounded-lg border-0 py-2 px-3 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
              >
                {slots.map((s) => (
                  <option key={s.slotId} value={s.slotId}>
                    Slot #{s.slotNumber} ({s.capacityKwh} kWh) - {s.status}
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Date Picker */}
          <Input
            label="Date"
            type="date"
            value={targetDate}
            onChange={(e) => setTargetDate(e.target.value)}
          />

          {/* Start Time Picker */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Start Time</label>
            {(() => {
              const parsed = parse12HourTime(targetStart);
              return (
                <div className="flex gap-1.5">
                  {/* Hour */}
                  <select
                    value={parsed.hour}
                    onChange={(e) => setTargetStart(format12HourTime(e.target.value, parsed.minute, parsed.period))}
                    className="w-1/3 rounded-lg border-0 py-2 px-2 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                  >
                    {Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')).map((h) => (
                      <option key={h} value={h}>{h}</option>
                    ))}
                  </select>
                  {/* Minute */}
                  <select
                    value={parsed.minute}
                    onChange={(e) => setTargetStart(format12HourTime(parsed.hour, e.target.value, parsed.period))}
                    className="w-1/3 rounded-lg border-0 py-2 px-2 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                  >
                    {['00', '15', '30', '45'].map((m) => (
                      <option key={m} value={m}>{m}</option>
                    ))}
                  </select>
                  {/* AM/PM */}
                  <select
                    value={parsed.period}
                    onChange={(e) => setTargetStart(format12HourTime(parsed.hour, parsed.minute, e.target.value))}
                    className="w-1/3 rounded-lg border-0 py-2 px-2 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                  >
                    <option value="AM">AM</option>
                    <option value="PM">PM</option>
                  </select>
                </div>
              );
            })()}
          </div>

          {/* End Time Picker */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">End Time</label>
            {(() => {
              const parsed = parse12HourTime(targetEnd);
              return (
                <div className="flex gap-1.5">
                  {/* Hour */}
                  <select
                    value={parsed.hour}
                    onChange={(e) => setTargetEnd(format12HourTime(e.target.value, parsed.minute, parsed.period))}
                    className="w-1/3 rounded-lg border-0 py-2 px-2 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                  >
                    {Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')).map((h) => (
                      <option key={h} value={h}>{h}</option>
                    ))}
                  </select>
                  {/* Minute */}
                  <select
                    value={parsed.minute}
                    onChange={(e) => setTargetEnd(format12HourTime(parsed.hour, e.target.value, parsed.period))}
                    className="w-1/3 rounded-lg border-0 py-2 px-2 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                  >
                    {['00', '15', '30', '45'].map((m) => (
                      <option key={m} value={m}>{m}</option>
                    ))}
                  </select>
                  {/* AM/PM */}
                  <select
                    value={parsed.period}
                    onChange={(e) => setTargetEnd(format12HourTime(parsed.hour, parsed.minute, e.target.value))}
                    className="w-1/3 rounded-lg border-0 py-2 px-2 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                  >
                    <option value="AM">AM</option>
                    <option value="PM">PM</option>
                  </select>
                </div>
              );
            })()}
          </div>
        </div>
      </Modal>

      {/* Cancel Confirmation Modal */}
      <Modal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        title="Cancel Reservation?"
        size="sm"
        footer={
          <>
            <Button variant="danger" onClick={handleConfirmCancel} disabled={cancelling}>
              {cancelling ? 'Cancelling...' : 'Confirm Cancellation'}
            </Button>
            <Button variant="secondary" onClick={() => setCancelOpen(false)}>
              Close
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="flex items-center gap-3 rounded-lg bg-rose-50 p-3 text-xs text-rose-800">
            <AlertTriangle className="h-5 w-5 shrink-0 text-rose-500" />
            <span>
              Reservation cancellations need to be done at least 12 hours before the scheduled time.
            </span>
          </div>

          <Input
            label="Reason for Cancellation"
            placeholder="e.g., Change of travel schedule"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
          />

          {cancelError && <p className="text-xs text-rose-600">{cancelError}</p>}
        </div>
      </Modal>

      {/* QR Code Pass Modal */}
      <Modal
        open={qrOpen}
        onClose={() => setQrOpen(false)}
        title="Reservation QR Code"
        size="sm"
        footer={<Button onClick={() => setQrOpen(false)}>Close</Button>}
      >
        {reservation.qrToken && (
          <div className="flex flex-col items-center py-2 space-y-4">
            <QrCodeDisplay value={reservation.qrToken} size={240} />
            <p className="text-center text-xs text-gray-500">
              Show this code to the Grid Operator at <strong>{reservation.stationName}</strong>.
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}