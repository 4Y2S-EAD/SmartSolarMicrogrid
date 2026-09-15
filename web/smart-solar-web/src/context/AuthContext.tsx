import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { ApiService } from '@/lib/api';
import type { UserProfile } from '@/lib/api';

export type UserRole = 'backoffice' | 'grid_operator';

type AuthContextType = {
  user: { id: string, email: string } | null;
  profile: UserProfile | null;
  role: UserRole;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<{ error: string | null }>;
  signUp: (nic: string, email: string, password: string, fullName: string, role: UserRole) => Promise<{ error: string | null }>;
  signOut: () => Promise<void>;
  refreshProfile: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<{ id: string, email: string } | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  // In a real app, this would check for a stored token and validate it with the backend.
  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const storedProfile = localStorage.getItem('profile');
    
    if (storedUser && storedProfile) {
      try {
        setUser(JSON.parse(storedUser));
        setProfile(JSON.parse(storedProfile));
      } catch (e) {
        console.error('Failed to parse stored user data', e);
        localStorage.removeItem('user');
        localStorage.removeItem('profile');
        localStorage.removeItem('token');
      }
    }
    
    setLoading(false);
  }, []);

  const signIn = async (email: string, password: string) => {
    try {
      const result = await ApiService.login(email, password);
      const token = result.token;
      const u = result.user;

      const newUser = { id: u.nic, email: u.email };
      const mappedRole = u.role === 'gridoperator' ? 'grid_operator' : u.role as UserRole;
      const newProfile: UserProfile = {
        id: u.nic,
        email: u.email,
        full_name: u.fullName,
        role: mappedRole,
        status: 'active',
        badge_id: null,
        assigned_hub_id: null,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };
      
      setUser(newUser);
      setProfile(newProfile);
      
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(newUser));
      localStorage.setItem('profile', JSON.stringify(newProfile));
      
      return { error: null };
    } catch (err: any) {
      return { error: err.message || 'Login failed' };
    }
  };

  const signUp = async (nic: string, email: string, password: string, fullName: string, role: UserRole) => {
    try {
      await ApiService.signUp(nic, email, password, fullName, role);
      // Automatically sign in after sign up
      return await signIn(email, password);
    } catch (err: any) {
      return { error: err.message || 'Sign up failed' };
    }
  };

  const signOut = async () => {
    setUser(null);
    setProfile(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('profile');
  };

  const refreshProfile = async () => {
    // In a real app, fetch the latest profile from API
  };

  const role: UserRole = profile?.role ?? 'backoffice';

  return (
    <AuthContext.Provider value={{ user, profile, role, loading, signIn, signUp, signOut, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
