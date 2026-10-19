import React, { useState } from 'react';
import type { Hub } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import { MapPin, ExternalLink, Navigation, Zap, Battery, Clock, Search, Layers } from 'lucide-react';

interface StationGoogleMapProps {
  stations: Hub[];
  initialSelectedId?: string;
  height?: string;
  showSidebarList?: boolean;
}

export default function StationGoogleMap({
  stations,
  initialSelectedId,
  height = '380px',
  showSidebarList = false,
}: StationGoogleMapProps) {
  const [selectedId, setSelectedId] = useState<string>(
    initialSelectedId || stations[0]?.stationId || ''
  );
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'All' | 'Active' | 'Inactive'>('All');

  const validStations = stations.filter(
    (s) => s.location && typeof s.location.latitude === 'number' && typeof s.location.longitude === 'number'
  );

  const filteredStations = validStations.filter((s) => {
    const matchesSearch = s.stationName.toLowerCase().includes(search.toLowerCase());
    const matchesStatus =
      statusFilter === 'All' || (s.status || '').toLowerCase() === statusFilter.toLowerCase();
    return matchesSearch && matchesStatus;
  });

  const selectedHub =
    validStations.find((s) => s.stationId === selectedId) ||
    filteredStations[0] ||
    validStations[0];

  const lat = selectedHub?.location?.latitude ?? 6.9147;
  const lng = selectedHub?.location?.longitude ?? 79.9729;

  return (
    <div className="flex flex-col rounded-2xl border border-gray-200 bg-white shadow-sm overflow-hidden">
      {/* Top Header / Station Quick Selector */}
      <div className="border-b border-gray-100 bg-gray-50/80 px-4 py-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-lg bg-emerald-500/10 text-emerald-600 flex items-center justify-center">
            <MapPin className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-gray-900 leading-tight">
              {selectedHub ? selectedHub.stationName : 'Microgrid Station Map'}
            </h3>
            <p className="text-xs text-gray-500 font-mono">
              {lat.toFixed(4)}° N, {lng.toFixed(4)}° E
            </p>
          </div>
        </div>

        {/* Station Dropdown Selector for quick switching */}
        <div className="flex items-center gap-2">
          <select
            value={selectedHub?.stationId || ''}
            onChange={(e) => setSelectedId(e.target.value)}
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-700 shadow-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500 max-w-[200px] truncate"
          >
            {validStations.map((hub) => (
              <option key={hub.stationId} value={hub.stationId}>
                {hub.stationName} ({hub.status || 'Active'})
              </option>
            ))}
          </select>

          {selectedHub && (
            <a
              href={`https://www.google.com/maps/search/?api=1&query=${lat},${lng}`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-emerald-700 transition-colors"
              title="Open full Google Maps in new tab"
            >
              <span>Google Maps</span>
              <ExternalLink className="h-3 w-3" />
            </a>
          )}
        </div>
      </div>

      {/* Main Map Content Area */}
      <div className={`flex flex-col ${showSidebarList ? 'lg:flex-row' : ''}`}>
        {/* Optional Sidebar Station List */}
        {showSidebarList && (
          <div className="w-full lg:w-72 border-b lg:border-b-0 lg:border-r border-gray-100 flex flex-col max-h-96 lg:max-h-none overflow-hidden">
            <div className="p-3 border-b border-gray-100 space-y-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search station..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 pl-8 pr-3 py-1.5 text-xs focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
              <div className="flex gap-1 text-[11px]">
                {(['All', 'Active', 'Inactive'] as const).map((st) => (
                  <button
                    key={st}
                    onClick={() => setStatusFilter(st)}
                    className={`flex-1 py-1 rounded text-center font-medium transition-colors ${
                      statusFilter === st
                        ? 'bg-amber-100 text-amber-800 font-bold'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {st}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex-1 overflow-y-auto divide-y divide-gray-50 p-1">
              {filteredStations.map((hub) => {
                const isSelected = hub.stationId === selectedHub?.stationId;
                return (
                  <button
                    key={hub.stationId}
                    onClick={() => setSelectedId(hub.stationId)}
                    className={`w-full text-left p-2.5 rounded-lg transition-all flex flex-col gap-1 cursor-pointer ${
                      isSelected
                        ? 'bg-emerald-50 text-emerald-950 font-semibold ring-1 ring-emerald-500/20'
                        : 'hover:bg-gray-50 text-gray-700'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs">
                      <span className="truncate font-bold">{hub.stationName}</span>
                      <StatusBadge status={(hub.status || 'Active').toLowerCase()} />
                    </div>
                    <div className="flex items-center gap-3 text-[11px] text-gray-500">
                      <span>{hub.capacityKwh} kWh</span>
                      <span>•</span>
                      <span>{hub.availableSlotCount ?? hub.batterySlotCount} slots</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Embedded Google Map IFrame */}
        <div className="flex-1 relative bg-gray-100" style={{ height }}>
          {selectedHub ? (
            <iframe
              title={`Google Map - ${selectedHub.stationName}`}
              width="100%"
              height="100%"
              loading="lazy"
              className="border-0 w-full h-full"
              src={`https://maps.google.com/maps?q=${lat},${lng}&hl=en&z=15&output=embed`}
            />
          ) : (
            <div className="flex h-full items-center justify-center text-gray-400 text-sm">
              No station selected
            </div>
          )}

          {/* Floating Telemetry Overlay Badge on Map */}
          {selectedHub && (
            <div className="absolute bottom-3 left-3 right-3 sm:right-auto sm:max-w-sm bg-white/95 backdrop-blur-md rounded-xl p-3 shadow-lg border border-gray-200/80 text-xs space-y-1.5 pointer-events-auto">
              <div className="flex items-center justify-between gap-2">
                <span className="font-bold text-gray-900 truncate">{selectedHub.stationName}</span>
                <StatusBadge status={(selectedHub.status || 'Active').toLowerCase()} />
              </div>
              <div className="grid grid-cols-2 gap-2 pt-1 border-t border-gray-100 text-[11px] text-gray-600">
                <div className="flex items-center gap-1.5">
                  <Zap className="h-3.5 w-3.5 text-amber-500" />
                  <span>Capacity: <b>{selectedHub.capacityKwh} kWh</b></span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Battery className="h-3.5 w-3.5 text-emerald-500" />
                  <span>Slots: <b>{selectedHub.availableSlotCount ?? selectedHub.batterySlotCount} Free</b></span>
                </div>
                <div className="flex items-center gap-1.5 col-span-2 text-gray-500">
                  <Clock className="h-3.5 w-3.5 text-blue-500" />
                  <span className="truncate">{selectedHub.schedule || 'Standard 24/7'}</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Quick Station Badges Carousel / Row */}
      {!showSidebarList && validStations.length > 1 && (
        <div className="border-t border-gray-100 bg-gray-50/50 p-2.5 flex items-center gap-2 overflow-x-auto">
          <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider pl-1 flex-shrink-0">
            Quick Hubs:
          </span>
          {validStations.slice(0, 8).map((h) => {
            const isSel = h.stationId === selectedHub?.stationId;
            return (
              <button
                key={h.stationId}
                onClick={() => setSelectedId(h.stationId)}
                className={`flex-shrink-0 px-2.5 py-1 rounded-lg text-xs font-medium transition-all cursor-pointer ${
                  isSel
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-100'
                }`}
              >
                {h.stationName}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
