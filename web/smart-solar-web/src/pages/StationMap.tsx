import { useEffect, useState } from 'react';
import { ApiService } from '@/lib/api';
import type { Hub } from '@/lib/api';
import StationGoogleMap from '@/components/StationGoogleMap';
import { MapPin, Zap, BatteryCharging, Radio, RefreshCw } from 'lucide-react';

export default function StationMap() {
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    setLoading(true);
    ApiService.getHubs()
      .then((data) => {
        setHubs(data || []);
      })
      .catch((err) => {
        console.error('Failed to load hubs for map:', err);
      })
      .finally(() => setLoading(false));
  }, [revision]);

  const activeCount = hubs.filter((h) => (h.status || '').toLowerCase() === 'active').length;
  const totalSlots = hubs.reduce((sum, h) => sum + (h.batterySlotCount || 0), 0);
  const availableSlots = hubs.reduce((sum, h) => sum + (h.availableSlotCount ?? h.batterySlotCount ?? 0), 0);

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="h-12 w-12 rounded-xl bg-emerald-500/10 text-emerald-600 flex items-center justify-center">
            <MapPin className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Microgrid Station Map</h1>
            <p className="text-sm text-gray-500">
              Interactive Google Maps geographic explorer for all microgrid solar hubs
            </p>
          </div>
        </div>

        {/* Quick summary telemetry pills */}
        <div className="flex items-center gap-3">
          <div className="bg-emerald-50 text-emerald-800 px-3.5 py-1.5 rounded-xl border border-emerald-200/60 text-xs font-semibold flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>{activeCount} / {hubs.length} Active Hubs</span>
          </div>

          <div className="bg-amber-50 text-amber-800 px-3.5 py-1.5 rounded-xl border border-amber-200/60 text-xs font-semibold flex items-center gap-1.5">
            <BatteryCharging className="h-3.5 w-3.5 text-amber-600" />
            <span>{availableSlots} / {totalSlots} Slots Free</span>
          </div>

          <button
            onClick={() => setRevision((r) => r + 1)}
            disabled={loading}
            className="p-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-600 transition cursor-pointer"
            title="Refresh Stations"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Main Interactive Map Component */}
      {loading ? (
        <div className="flex h-96 items-center justify-center bg-white rounded-2xl border border-gray-200 shadow-sm">
          <div className="text-sm font-medium text-gray-400 animate-pulse flex items-center gap-2">
            <RefreshCw className="h-4 w-4 animate-spin text-emerald-600" />
            Loading Google Maps station explorer...
          </div>
        </div>
      ) : (
        <StationGoogleMap
          stations={hubs}
          height="540px"
          showSidebarList={true}
        />
      )}
    </div>
  );
}
