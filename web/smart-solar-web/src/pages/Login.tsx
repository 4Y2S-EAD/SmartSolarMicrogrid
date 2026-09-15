import { useState, FormEvent } from 'react';
import { useAuth, type UserRole } from '@/context/AuthContext';
import { Sun, Loader2, Lock, Mail, User, Shield, Radio } from 'lucide-react';
import Button from '@/components/ui/Button';

export default function Login() {
  const { signIn, signUp } = useAuth();
  const [mode, setMode] = useState<'signin' | 'signup'>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [nic, setNic] = useState('');
  const [role, setRole] = useState<UserRole>('backoffice');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    if (mode === 'signin') {
      const { error } = await signIn(email, password);
      if (error) {
        setError(error);
        setLoading(false);
      }
    } else {
      if (!fullName.trim() || !nic.trim()) {
        setError('Please enter your NIC and full name.');
        setLoading(false);
        return;
      }
      const { error } = await signUp(nic, email, password, fullName, role);
      if (error) {
        setError(error);
        setLoading(false);
      }
    }
  };

  return (
    <div className="flex min-h-screen flex-col lg:flex-row">
      {/* Left brand panel */}
      <div className="relative flex flex-1 flex-col justify-between overflow-hidden bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-10 text-white">
        <div className="absolute inset-0 opacity-20">
          <div className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-amber-400 blur-3xl" />
          <div className="absolute bottom-0 left-1/4 h-64 w-64 rounded-full bg-sky-500 blur-3xl" />
        </div>
        <div className="relative z-10">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-500 text-white shadow-lg shadow-amber-500/30">
              <Sun className="h-6 w-6" />
            </div>
            <span className="text-xl font-bold tracking-tight">MicroGrid</span>
          </div>
        </div>
        <div className="relative z-10 max-w-md">
          <h1 className="text-4xl font-bold leading-tight tracking-tight">
            Smart Solar Microgrid
            <br />
            <span className="text-amber-400">Trading System</span>
          </h1>
          <p className="mt-4 text-lg text-slate-300">
            Manage hubs, prosumers, and energy reservations from a single backoffice platform.
          </p>
          <div className="mt-8 grid grid-cols-3 gap-4">
            {[
              { label: 'Hubs Online', value: '4' },
              { label: 'Active Prosumers', value: '120+' },
              { label: 'Energy Traded', value: '2.4 MWh' },
            ].map((stat) => (
              <div key={stat.label} className="rounded-xl bg-white/5 p-4 backdrop-blur-sm ring-1 ring-white/10">
                <div className="text-2xl font-bold text-amber-400">{stat.value}</div>
                <div className="mt-0.5 text-xs text-slate-400">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="relative z-10 text-sm text-slate-400">
          &copy; 2026 MicroGrid Systems
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex flex-1 items-center justify-center bg-gray-50 px-6 py-12">
        <div className="w-full max-w-sm">
          <h2 className="text-2xl font-bold text-gray-900">
            {mode === 'signin' ? 'Sign in to your account' : 'Create your account'}
          </h2>
          <p className="mt-2 text-sm text-gray-500">
            {mode === 'signin'
              ? 'Enter your credentials to access the dashboard.'
              : 'Choose your role and create an account to get started.'}
          </p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            {mode === 'signup' && (
              <>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-gray-700">NIC Number</label>
                  <div className="relative">
                    <User className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
                    <input
                      required
                      value={nic}
                      onChange={(e) => setNic(e.target.value)}
                      placeholder="991234567V"
                      className="w-full rounded-lg border-0 py-2.5 pl-11 pr-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
                    />
                  </div>
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-gray-700">Full Name</label>
                  <div className="relative">
                    <User className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
                    <input
                      required
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="John Doe"
                      className="w-full rounded-lg border-0 py-2.5 pl-11 pr-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
                    />
                  </div>
                </div>
              </>
            )}

            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">Email</label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@microgrid.io"
                  className="w-full rounded-lg border-0 py-2.5 pl-11 pr-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
                />
              </div>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">Password</label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
                <input
                  type="password"
                  required
                  minLength={6}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-lg border-0 py-2.5 pl-11 pr-3.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-amber-500"
                />
              </div>
            </div>

            {mode === 'signup' && (
              <div>
                <label className="mb-2 block text-sm font-medium text-gray-700">Role</label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setRole('backoffice')}
                    className={`flex items-center gap-3 rounded-lg border-0 p-3.5 text-left ring-1 ring-inset transition-all ${
                      role === 'backoffice'
                        ? 'bg-amber-50 ring-amber-500'
                        : 'bg-white ring-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    <Shield className={`h-5 w-5 ${role === 'backoffice' ? 'text-amber-600' : 'text-gray-400'}`} />
                    <div>
                      <div className={`text-sm font-medium ${role === 'backoffice' ? 'text-amber-900' : 'text-gray-700'}`}>Backoffice</div>
                      <div className="text-xs text-gray-400">Full admin access</div>
                    </div>
                  </button>
                  <button
                    type="button"
                    onClick={() => setRole('grid_operator')}
                    className={`flex items-center gap-3 rounded-lg border-0 p-3.5 text-left ring-1 ring-inset transition-all ${
                      role === 'grid_operator'
                        ? 'bg-indigo-50 ring-indigo-500'
                        : 'bg-white ring-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    <Radio className={`h-5 w-5 ${role === 'grid_operator' ? 'text-indigo-600' : 'text-gray-400'}`} />
                    <div>
                      <div className={`text-sm font-medium ${role === 'grid_operator' ? 'text-indigo-900' : 'text-gray-700'}`}>Grid Operator</div>
                      <div className="text-xs text-gray-400">QR scanning access</div>
                    </div>
                  </button>
                </div>
              </div>
            )}

            {error && (
              <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-inset ring-rose-600/20">
                {error}
              </div>
            )}

            <Button type="submit" size="lg" className="w-full" disabled={loading}>
              {loading && <Loader2 className="h-4 w-4 animate-spin" />}
              {mode === 'signin' ? 'Sign in' : 'Create account'}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            {mode === 'signin' ? "Don't have an account? " : 'Already have an account? '}
            <button
              onClick={() => { setMode(mode === 'signin' ? 'signup' : 'signin'); setError(null); }}
              className="font-semibold text-amber-600 hover:text-amber-700"
            >
              {mode === 'signin' ? 'Sign up' : 'Sign in'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
