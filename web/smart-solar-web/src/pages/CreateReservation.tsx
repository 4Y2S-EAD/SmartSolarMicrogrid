import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ApiService, type Hub, type BookingSlot } from '@/lib/api';
import Button from '@/components/ui/Button';
import { ArrowLeft, Zap, BatteryCharging, AlertCircle, CheckCircle2 } from 'lucide-react';

type CreateReservationProps = {
  onBack: () => void;
  onSuccess: (reservationId: string) => void;
};

// Helper parse "09:00 AM" into components
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

// Helper format back to 12-hour string
function format12HourTime(hour: string, minute: string, period: string) {
  return `${hour.padStart(2, '0')}:${minute} ${period}`;
}

export default function CreateReservation({ onBack, onSuccess }: CreateReservationProps) {
  const { user } = useAuth();
  const [stations, setStations] = useState<Hub[]>([]);
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [selectedStationId, setSelectedStationId] = useState('');
  const [selectedSlotId, setSelectedSlotId] = useState('');
  const [bookingDate, setBookingDate] = useState('');
  const [startTime, setStartTime] = useState('09:00 AM');
  const [endTime, setEndTime] = useState('10:00 AM');
  const [loadingStations, setLoadingStations] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitStatusText, setSubmitStatusText] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 7 day restriction rule
  const today = new Date();
  const minDate = today.toISOString().split('T')[0];
  const maxDate = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

  useEffect(() => {
    async function fetchStations() {
      try {
        const data = await ApiService.getHubs();
        const activeStations = (data || []).filter(
          (s) => s.status?.toLowerCase() === 'active'
        );
        setStations(activeStations);
        if (activeStations.length > 0) {
          setSelectedStationId(activeStations[0].stationId);
        }
      } catch (err: any) {
        setError('Failed to load solar stations.');
      }
      setLoadingStations(false);
    }
    fetchStations();
  }, []);

  useEffect(() => {
    if (!selectedStationId) return;

    async function fetchSlots() {
      setLoadingSlots(true);
      try {
        const data = await ApiService.getBookingSlots(selectedStationId);
        // Only show available slots
        const availableSlots = (data || []).filter(
          (s) => s.status?.toLowerCase() === 'available'
        );
        setSlots(availableSlots);
        if (availableSlots.length > 0) {
          setSelectedSlotId(availableSlots[0].slotId);
          if (availableSlots[0].startTime && availableSlots[0].startTime.includes(':')) {
            setStartTime(availableSlots[0].startTime);
          }
          if (availableSlots[0].endTime && availableSlots[0].endTime.includes(':')) {
            setEndTime(availableSlots[0].endTime);
          }
        } else {
          setSelectedSlotId('');
        }
      } catch (err) {
        setSlots([]);
      }
      setLoadingSlots(false);
    }

    fetchSlots();
  }, [selectedStationId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!selectedStationId || !selectedSlotId || !bookingDate || !startTime || !endTime) {
      setError('Please select a station, a battery slot, date, and times.');
      return;
    }

    if (!user?.id) {
      setError('User identity (NIC) not found. Please log in again.');
      return;
    }

    setSubmitting(true);
    setSubmitStatusText('Creating reservation record...');

    try {
      // Create Reservation
      const created = await ApiService.createReservation({
        prosumerNic: user.id,
        stationId: selectedStationId,
        slotId: selectedSlotId,
        bookingDate: new Date(bookingDate).toISOString(),
        startTime: startTime.trim(),
        endTime: endTime.trim(),
      });

      if (!created || !created.reservationId) {
        throw new Error('Reservation could not be initialized by the server.');
      }

      // Generate QR Code
      setSubmitStatusText('Generating and signing QR token...');
      const qrResponse = await ApiService.generateQrCode(created.reservationId);

      if (!qrResponse || !qrResponse.qrToken) {
        throw new Error('Reservation was booked, but QR code token generation failed.');
      }

      onSuccess(created.reservationId);
    } catch (err: any) {
      setError(err.message || 'Failed to complete reservation and QR generation.');
      setSubmitting(false);
      setSubmitStatusText(null);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <button
        onClick={onBack}
        className="flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-gray-800 transition-colors"
      >
        <ArrowLeft className="h-4 w-4" /> Back to Dashboard
      </button>

      <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
        <div className="flex items-center gap-3 border-b border-gray-100 pb-5">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
            <Zap className="h-6 w-6" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-gray-900">Create Energy Reservation</h2>
            <p className="text-xs text-gray-500">
              Reserve an available battery slot and generate your verified transaction pass.
            </p>
          </div>
        </div>

        {error && (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          {/* Station Selection */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">
              Solar Station *
            </label>
            {loadingStations ? (
              <div className="text-xs text-gray-400">Loading solar stations...</div>
            ) : (
              <select
                value={selectedStationId}
                onChange={(e) => setSelectedStationId(e.target.value)}
                className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500 text-sm"
              >
                {stations.map((s) => (
                  <option key={s.stationId} value={s.stationId}>
                    {s.stationName} ({s.capacityKwh} kWh Capacity)
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Available Battery Slot Card View Selection */}
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">
              Select Available Battery Slot *
            </label>

            {loadingSlots ? (
              <div className="text-xs text-gray-400 py-3">Loading available slots...</div>
            ) : slots.length === 0 ? (
              <div className="rounded-lg border border-dashed border-amber-300 bg-amber-50/60 p-4 text-center text-xs text-amber-800">
                No slots defined or available for this station. Please select a different solar station.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-56 overflow-y-auto p-1">
                {slots.map((slot) => {
                  const isSelected = selectedSlotId === slot.slotId;
                  return (
                    <label
                      key={slot.slotId}
                      className={`relative flex items-center gap-3.5 p-3.5 rounded-xl border cursor-pointer transition-all ${isSelected
                          ? 'border-amber-500 bg-amber-50/50 ring-2 ring-amber-500/20 shadow-sm'
                          : 'border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50/50'
                        }`}
                    >
                      <input
                        type="radio"
                        name="batterySlot"
                        value={slot.slotId}
                        checked={isSelected}
                        onChange={() => {
                          setSelectedSlotId(slot.slotId);
                          if (slot.startTime && slot.startTime.includes(':')) {
                            setStartTime(slot.startTime);
                          }
                          if (slot.endTime && slot.endTime.includes(':')) {
                            setEndTime(slot.endTime);
                          }
                        }}
                        className="h-4 w-4 text-amber-600 focus:ring-amber-500 border-gray-300"
                      />
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-semibold text-gray-900">
                            Slot No. {slot.slotNumber}
                          </span>
                        </div>
                        <div className="mt-1 flex items-center gap-1.5 text-sm text-gray-500">
                          <BatteryCharging className="h-4 w-4 text-amber-600" />
                          <span>Capacity : {slot.capacityKwh} kWh</span>
                        </div>
                      </div>
                    </label>
                  );
                })}
              </div>
            )}
          </div>

          {/* Date Picker (7-day rule enforced) */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">
              Booking Date (within 7 days) *
            </label>
            <input
              type="date"
              required
              min={minDate}
              max={maxDate}
              value={bookingDate}
              onChange={(e) => setBookingDate(e.target.value)}
              className="w-full rounded-lg border-0 py-2.5 px-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-amber-500 text-sm"
            />
            <p className="mt-1 text-xs text-gray-400">
              Reservations are restricted to the 7-day period ending on {maxDate}.
            </p>
          </div>

          {/* 12-Hour Dropdown Time Selectors */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-1">
            {/* Start Time Selection */}
            <div className="rounded-xl border border-gray-100 bg-gray-50 p-3.5">
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-gray-500">
                Start Time *
              </label>
              {(() => {
                const parsed = parse12HourTime(startTime);
                return (
                  <div className="flex items-center gap-2">
                    <select
                      value={parsed.hour}
                      onChange={(e) => setStartTime(format12HourTime(e.target.value, parsed.minute, parsed.period))}
                      className="flex-1 rounded-lg border-0 bg-white py-2 px-2.5 text-sm font-medium text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                    >
                      {Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')).map((h) => (
                        <option key={h} value={h}>{h}</option>
                      ))}
                    </select>
                    <span className="font-bold text-gray-400">:</span>
                    <select
                      value={parsed.minute}
                      onChange={(e) => setStartTime(format12HourTime(parsed.hour, e.target.value, parsed.period))}
                      className="flex-1 rounded-lg border-0 bg-white py-2 px-2.5 text-sm font-medium text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                    >
                      {['00', '15', '30', '45'].map((m) => (
                        <option key={m} value={m}>{m}</option>
                      ))}
                    </select>
                    <select
                      value={parsed.period}
                      onChange={(e) => setStartTime(format12HourTime(parsed.hour, parsed.minute, e.target.value))}
                      className="w-20 rounded-lg border-0 bg-white py-2 px-2.5 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                    >
                      <option value="AM">AM</option>
                      <option value="PM">PM</option>
                    </select>
                  </div>
                );
              })()}
            </div>

            {/* End Time Selection */}
            <div className="rounded-xl border border-gray-100 bg-gray-50 p-3.5">
              <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-gray-500">
                End Time *
              </label>
              {(() => {
                const parsed = parse12HourTime(endTime);
                return (
                  <div className="flex items-center gap-2">
                    <select
                      value={parsed.hour}
                      onChange={(e) => setEndTime(format12HourTime(e.target.value, parsed.minute, parsed.period))}
                      className="flex-1 rounded-lg border-0 bg-white py-2 px-2.5 text-sm font-medium text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                    >
                      {Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')).map((h) => (
                        <option key={h} value={h}>{h}</option>
                      ))}
                    </select>
                    <span className="font-bold text-gray-400">:</span>
                    <select
                      value={parsed.minute}
                      onChange={(e) => setEndTime(format12HourTime(parsed.hour, e.target.value, parsed.period))}
                      className="flex-1 rounded-lg border-0 bg-white py-2 px-2.5 text-sm font-medium text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                    >
                      {['00', '15', '30', '45'].map((m) => (
                        <option key={m} value={m}>{m}</option>
                      ))}
                    </select>
                    <select
                      value={parsed.period}
                      onChange={(e) => setEndTime(format12HourTime(parsed.hour, parsed.minute, e.target.value))}
                      className="w-20 rounded-lg border-0 bg-white py-2 px-2.5 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-amber-500"
                    >
                      <option value="AM">AM</option>
                      <option value="PM">PM</option>
                    </select>
                  </div>
                );
              })()}
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-between pt-4 border-t border-gray-100">
            <span className="text-xs text-amber-700 font-medium">
              {submitStatusText || ''}
            </span>
            <div className="flex gap-3">
              <Button variant="secondary" onClick={onBack} disabled={submitting}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitting || slots.length === 0}>
                {submitting ? 'Processing...' : 'Confirm & Generate Pass'}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}