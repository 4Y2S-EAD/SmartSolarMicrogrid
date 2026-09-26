import { ReactNode, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import {
  Sun, LayoutDashboard, Users, Zap, CalendarDays, QrCode, Radio, LogOut, Menu, X, Gauge, UserCog, Shield, ScanLine,
} from 'lucide-react';
import type { UserRole } from '@/context/AuthContext';

export type NavItem = {
  id: string;
  label: string;
  icon: typeof LayoutDashboard;
  roles: UserRole[];
};

const navItems: NavItem[] = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ['backoffice', 'grid_operator'] },
  { id: 'prosumers', label: 'Prosumer Management', icon: Users, roles: ['backoffice'] },
  { id: 'hubs', label: 'Hub Management', icon: Zap, roles: ['backoffice'] },
  { id: 'schedule', label: 'Schedule & Slots', icon: CalendarDays, roles: ['backoffice'] },
  { id: 'reservations', label: 'Reservations', icon: Gauge, roles: ['backoffice', 'grid_operator'] },
  { id: 'qr-flow', label: 'QR Transaction Flow', icon: QrCode, roles: ['backoffice', 'grid_operator'] },
  { id: 'operators', label: 'Operators', icon: Radio, roles: ['backoffice'] },
  { id: 'user-management', label: 'User Management', icon: UserCog, roles: ['backoffice'] },
];

type DashboardLayoutProps = {
  activeView: string;
  onNavigate: (view: string) => void;
  children: ReactNode;
};

export default function DashboardLayout({ activeView, onNavigate, children }: DashboardLayoutProps) {
  const { user, profile, role, signOut } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const visibleItems = navItems.filter((n) => n.roles.includes(role));
  const currentLabel = visibleItems.find((n) => n.id === activeView)?.label ?? 'Dashboard';

  const roleConfig: Record<UserRole, { label: string; icon: typeof Shield; color: string }> = {
    backoffice: { label: 'Backoffice Admin', icon: Shield, color: 'text-amber-600' },
    grid_operator: { label: 'Grid Operator', icon: ScanLine, color: 'text-indigo-600' },
  };
  const roleInfo = roleConfig[role];
  const RoleIcon = roleInfo.icon;

  const nav = (
    <nav className="flex flex-1 flex-col gap-1 px-3 py-4">
      {visibleItems.map((item) => {
        const Icon = item.icon;
        const active = activeView === item.id;
        return (
          <button
            key={item.id}
            onClick={() => { onNavigate(item.id); setMobileOpen(false); }}
            className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all ${
              active
                ? 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20'
                : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
            }`}
          >
            <Icon className={`h-5 w-5 ${active ? 'text-amber-600' : 'text-gray-400'}`} />
            {item.label}
          </button>
        );
      })}
    </nav>
  );

  const header = (
    <div className="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 className="text-xl font-bold text-gray-900">{currentLabel}</h1>
        <p className="text-sm text-gray-500">Smart Solar Microgrid Trading System</p>
      </div>
      <div className="flex items-center gap-3">
        <div className="hidden text-right sm:block">
          <div className="text-sm font-medium text-gray-900">{profile?.full_name || user?.email}</div>
          <div className={`flex items-center justify-end gap-1 text-xs ${roleInfo.color}`}>
          
            {roleInfo.label}
          </div>
        </div>
        <button
          onClick={signOut}
          className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-100"
        >
          <LogOut className="h-4 w-4" />
          <span className="hidden sm:inline">Sign out</span>
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Desktop sidebar */}
      <aside className="hidden w-64 flex-col border-r border-gray-200 bg-white lg:flex">
        <div className="flex items-center gap-3 border-b border-gray-200 px-5 py-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500 text-white shadow-md shadow-amber-500/30">
            <Sun className="h-5 w-5" />
          </div>
          <div>
            <div className="text-base font-bold text-gray-900">MicroGrid</div>
            <div className={`text-xs ${roleInfo.color}`}>{roleInfo.label}</div>
          </div>
        </div>
        {nav}
        <div className="border-t border-gray-200 p-4">
          <div className="rounded-lg bg-slate-900 p-3 text-xs text-slate-300">
            <div className="font-semibold text-white">System Status</div>
            <div className="mt-1.5 flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-emerald-400" />
            All systems operational
            </div>
          </div>
        </div>
      </aside>

      {/* Mobile sidebar */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-gray-900/40 backdrop-blur-sm" onClick={() => setMobileOpen(false)} />
          <aside className="absolute left-0 top-0 flex h-full w-64 flex-col bg-white">
            <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500 text-white">
                  <Sun className="h-5 w-5" />
                </div>
                <div className="text-base font-bold text-gray-900">MicroGrid</div>
              </div>
              <button onClick={() => setMobileOpen(false)} className="text-gray-400">
                <X className="h-5 w-5" />
              </button>
            </div>
            {nav}
          </aside>
        </div>
      )}

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <div className="flex items-center border-b border-gray-200 bg-white px-4 py-3 lg:hidden">
          <button onClick={() => setMobileOpen(true)} className="text-gray-500">
            <Menu className="h-6 w-6" />
          </button>
          <span className="ml-3 text-base font-bold text-gray-900">MicroGrid</span>
        </div>
        {header}
        <main className="flex-1 overflow-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
