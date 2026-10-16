import { useAuth } from '@/context/AuthContext';
import DashboardLayout from '@/components/DashboardLayout';
import Dashboard from '@/pages/Dashboard';
import OperatorDashboard from '@/member4/operator/OperatorDashboard';
import OperatorReservations from '@/member4/operator/OperatorReservations';
import ProsumerManagement from '@/pages/ProsumerManagement';
import HubManagement from '@/pages/HubManagement';
import ScheduleManagement from '@/pages/ScheduleManagement';
import ReservationManagement from '@/pages/ReservationManagement';
import QrFlow from '@/pages/QrFlow';
import OperatorManagement from '@/pages/OperatorManagement';
import UserManagement from '@/pages/UserManagement';
import Login from '@/pages/Login';
import ProsumerDashboard from '@/pages/ProsumerDashboard';
import CreateReservation from '@/pages/CreateReservation';
import ViewReservation from '@/pages/ViewReservation';
import { Loader2 } from 'lucide-react';
import { useState } from 'react';
import logoImg from '@/assets/logo.png';

function App() {
  const { user, profile, role, loading } = useAuth();
  const [view, setView] = useState('dashboard');
  const [activeReservationId, setActiveReservationId] = useState<string | null>(null);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
      </div>
    );
  }

  if (!user) {
    return <Login />;
  }

  // If profile is still loading after auth is done, wait a bit more
  if (!profile) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
      </div>
    );
  }

  const navigateProsumer = (nextView: string, reservationId?: string) => {
    if (reservationId) setActiveReservationId(reservationId);
    setView(nextView);
  };

  // Prosumer views
  if (role === 'prosumer') {
    return (
      <div className="min-h-screen bg-gray-50/70">
        <header className="border-b border-gray-200 bg-white px-4 sm:px-8 py-3.5 shadow-sm sticky top-0 z-30">
          <div className="mx-auto flex max-w-6xl items-center justify-between">
            <button
              onClick={() => setView('dashboard')}
              className="flex items-center gap-3 text-left transition-opacity hover:opacity-90 cursor-pointer"
            >
              <img src={logoImg} alt="MicroGrid Logo" className="h-9 w-9 object-contain" />
              <div>
                <span className="text-lg font-bold text-gray-900 leading-none">MicroGrid</span>
                <span className="ml-2.5 inline-block text-xs font-semibold text-amber-700 bg-amber-50 px-2.5 py-0.5 rounded-full border border-amber-200/80">Prosumer Portal</span>
              </div>
            </button>
            <div className="text-sm font-medium text-gray-700 hidden sm:block">
              {profile?.full_name || user?.email}
            </div>
          </div>
        </header>
        <div className="p-4 sm:p-8">
          <div className="mx-auto max-w-6xl">
            {view === 'create-reservation' && (
              <CreateReservation
                onBack={() => setView('dashboard')}
                onSuccess={(newId) => {
                  setActiveReservationId(newId);
                  setView('view-reservation');
                }}
              />
            )}
            {view === 'view-reservation' && activeReservationId && (
              <ViewReservation
                reservationId={activeReservationId}
                onBack={() => setView('dashboard')}
              />
            )}
            {view === 'dashboard' && (
              <ProsumerDashboard onNavigate={navigateProsumer} />
            )}
          </div>
        </div>
      </div>
    );
  }

  // Role-based page access
  const backofficePages: Record<string, React.ReactNode> = {
    dashboard: <Dashboard />,
    prosumers: <ProsumerManagement />,
    hubs: <HubManagement />,
    schedule: <ScheduleManagement />,
    reservations: <ReservationManagement />,
    'qr-flow': <QrFlow />,
    operators: <OperatorManagement />,
    'user-management': <UserManagement />,
  };

  const operatorPages: Record<string, React.ReactNode> = {
    dashboard: <OperatorDashboard />,
    reservations: <OperatorReservations />,
    'qr-flow': <QrFlow />,
  };

  const allowedPages = role === 'backoffice' ? backofficePages : operatorPages;

  // If current view is not allowed for this role, redirect to dashboard
  const currentView = allowedPages[view] ? view : 'dashboard';

  return (
    <DashboardLayout activeView={currentView} onNavigate={setView}>
      {allowedPages[currentView] ?? <Dashboard />}
    </DashboardLayout>
  );
}

export default App;
