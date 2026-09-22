import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ApiService, type ReservationSummary, type PaginatedReservations } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import QrCodeDisplay from '@/components/ui/QrCodeDisplay';
import EmptyState from '@/components/ui/EmptyState';
import {
  CalendarDays,
  Clock,
  Zap,
  Plus,
  QrCode,
  ChevronLeft,
  ChevronRight,
  User,
  LogOut,
} from 'lucide-react';

type TabStatus = 'approved' | 'pending' | 'completed' | 'cancelled';

type ProsumerDashboardProps = {
  onNavigate: (view: string, reservationId?: string) => void;
};

export default function ProsumerDashboard({ onNavigate }: ProsumerDashboardProps) {
  const { user, profile, signOut } = useAuth();
  const [activeTab, setActiveTab] = useState<TabStatus>('approved');
  const [data, setData] = useState<PaginatedReservations>({
    currentPage: 1,
    pageSize: 10,
    totalRecords: 0,
    totalPages: 1,
    items: []
  });
  const [loading, setLoading] = useState(true);
  const [qrModalItem, setQrModalItem] = useState<ReservationSummary | null>(null);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);

  const loadReservations = useCallback(async (tab: TabStatus, page: number) => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const res = await ApiService.getReservationsByStatus(user.id, tab, page, 10);
      setData(res);
    } catch (err) {
      console.error('Failed to load reservations:', err);
      setData({
        currentPage: page,
        pageSize: 10,
        totalRecords: 0,
        totalPages: 1,
        items: []
      });
    }
    setLoading(false);
  }, [user?.id]);

  useEffect(() => {
    loadReservations(activeTab, 1);
  }, [activeTab, loadReservations]);

  const handleTabChange = (tab: TabStatus) => {
    setActiveTab(tab);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= data.totalPages) {
      loadReservations(activeTab, newPage);
    }
  };

  const tabs: { id: TabStatus; label: string }[] = [
    { id: 'approved', label: 'Approved' },
    { id: 'pending', label: 'Pending' },
    { id: 'completed', label: 'Completed' },
    { id: 'cancelled', label: 'Cancelled' },
  ];

  return (
    <div className="space-y-6">
      {/* Top Welcome Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between rounded-2xl border border-amber-100 bg-gradient-to-br from-amber-50/70 via-white to-amber-50/40 p-6 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-gray-900">
              Welcome, {profile?.full_name || 'Prosumer'}
            </h1>
          </div>
          <p className="mt-1 text-sm text-gray-500">
            Manage your solar battery charging slot reservations.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2.5">
          <Button variant="secondary">
            <User className="h-4 w-4" />
            My Profile
          </Button>
          <Button variant="ghost" onClick={() => setLogoutConfirmOpen(true)} className="text-gray-500 hover:text-rose-600">
            <LogOut className="h-4 w-4" />
            Sign Out
          </Button>
        </div>
      </div>

      {/* Tabs Bar with New Reservation Button aligned right */}
      <div className="flex items-center justify-between border-b border-gray-200">
        <div className="flex items-center gap-2">
          {tabs.map((t) => {
            const active = activeTab === t.id;
            return (
              <button
                key={t.id}
                onClick={() => handleTabChange(t.id)}
                className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-all ${active
                    ? 'border-amber-500 text-amber-700 bg-amber-50/40 rounded-t-lg'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
              >
                {t.label}
              </button>
            );
          })}
        </div>

        {/* Location matched to screenshot */}
        <div className="pb-2">
          <Button onClick={() => onNavigate('create-reservation')}>
            <Plus className="h-4 w-4" />
            New Reservation
          </Button>
        </div>
      </div>

      {/* Reservation Cards List */}
      {loading ? (
        <div className="py-20 text-center text-sm text-gray-400">Loading reservations...</div>
      ) : data.items.length === 0 ? (
        <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
          <EmptyState
            icon={<CalendarDays className="h-8 w-8 text-amber-500" />}
            title={`No ${activeTab} reservations found`}
            description={
              activeTab === 'approved'
                ? "You don't have any ongoing approved bookings. Click 'New Reservation' to reserve a slot."
                : `There are currently no reservations marked as ${activeTab}.`
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-2">
          {data.items.map((r) => (
            <div
              key={r.reservationId}
              className="group flex flex-col justify-between rounded-2xl border border-gray-100 bg-white p-5 shadow-sm transition-all hover:shadow-md"
            >
              <div>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                      <Zap className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900 line-clamp-1">{r.stationName}</h3>
                      <p className="text-xs text-gray-400">Slot No : {r.slotNumber}</p>
                    </div>
                  </div>
                  <StatusBadge status={r.status.toLowerCase()} />
                </div>

                <div className="mt-4 space-y-2 rounded-xl bg-gray-50 p-3.5 text-sm">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-gray-500 flex items-center gap-1.5">
                      <CalendarDays className="h-3.5 w-3.5 text-gray-400" /> Date
                    </span>
                    <span className="font-medium text-gray-900">
                      {new Date(r.bookingDate).toLocaleDateString()}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-gray-500 flex items-center gap-1.5">
                      <Clock className="h-3.5 w-3.5 text-gray-400" /> Time Slot
                    </span>
                    <span className="font-medium text-gray-900">
                      {r.startTime} - {r.endTime}
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="mt-5 flex items-center justify-end gap-2 border-t border-gray-50 pt-3.5">
                {r.qrToken && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setQrModalItem(r)}
                    className="text-amber-700 border-amber-200 hover:bg-amber-50"
                  >
                    <QrCode className="h-3.5 w-3.5" />
                    QR Code
                  </Button>
                )}
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => onNavigate('view-reservation', r.reservationId)}
                >
                  More Details
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Footer */}
      {!loading && data.totalPages > 1 && (
        <div className="flex items-center justify-between rounded-xl border border-gray-100 bg-white px-5 py-3 shadow-sm">
          <span className="text-xs text-gray-500">
            Page <span className="font-semibold text-gray-900">{data.currentPage}</span> of{' '}
            <span className="font-semibold text-gray-900">{data.totalPages}</span> ({data.totalRecords} total items)
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => handlePageChange(data.currentPage - 1)}
              disabled={data.currentPage <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => handlePageChange(data.currentPage + 1)}
              disabled={data.currentPage >= data.totalPages}
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      {/* QR Code Popup Modal */}
      <Modal
        open={!!qrModalItem}
        onClose={() => setQrModalItem(null)}
        title="Reservation QR Pass"
        description="Present this QR code at the station upon request."
        size="sm"
        footer={<Button onClick={() => setQrModalItem(null)}>Done</Button>}
      >
        {qrModalItem?.qrToken && (
          <div className="flex flex-col items-center space-y-4 py-2">
            <QrCodeDisplay value={qrModalItem.qrToken} size={240} />
            <div className="w-full space-y-2 rounded-xl bg-gray-50 p-3.5 text-xs">
              <div className="flex justify-between">
                <span className="text-gray-500">Station : </span>
                <span className="font-semibold text-gray-900">{qrModalItem.stationName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Slot No : </span>
                <span className="font-semibold text-gray-900">{qrModalItem.slotNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Scheduled Date : </span>
                <span className="font-semibold text-gray-900">
                  {new Date(qrModalItem.bookingDate).toLocaleDateString()}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Scheduled Time : </span>
                <span className="font-semibold text-gray-900">
                  {qrModalItem.startTime} - {qrModalItem.endTime}
                </span>
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Logout Confirmation Modal */}
      <Modal
        open={logoutConfirmOpen}
        onClose={() => setLogoutConfirmOpen(false)}
        title="Confirm Sign Out"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setLogoutConfirmOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                setLogoutConfirmOpen(false);
                signOut();
              }}
            >
              Sign Out
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          Are you sure you want to sign out of your account?
        </p>
      </Modal>
    </div>
  );
}