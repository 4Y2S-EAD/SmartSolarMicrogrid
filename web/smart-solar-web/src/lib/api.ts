export const API_BASE_URL = 'http://localhost:5224/api';

// --- Type Definitions ---
export type Prosumer = {
  id: string;
  nic: string;
  full_name: string;
  email: string;
  phone: string | null;
  address: string | null;
  status: 'pending' | 'active' | 'deactivated';
  deactivation_requested: boolean;
  created_at: string;
  updated_at: string;
};

export type Hub = {
  id: string;
  name: string;
  location_name: string;
  latitude: number;
  longitude: number;
  capacity_kw: number;
  status: 'active' | 'deactivated';
  created_at: string;
  updated_at: string;
};

export type BookingSlot = {
  id: string;
  hub_id: string;
  start_time: string;
  end_time: string;
  energy_available_kwh: number;
  energy_booked_kwh: number;
  status: 'open' | 'full' | 'closed';
  created_at: string;
  hub?: Hub;
};

export type Reservation = {
  id: string;
  prosumer_id: string;
  slot_id: string;
  hub_id: string;
  energy_kwh: number;
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed' | 'no_show';
  qr_token: string;
  transaction_id: string | null;
  reservation_id_human: string | null;
  notes: string | null;
  created_at: string;
  updated_at: string;
  cancelled_at: string | null;
  completed_at: string | null;
  prosumer?: Prosumer;
  hub?: Hub;
  slot?: BookingSlot;
};

export type QrTransactionStatus =
  | 'generated' | 'approved' | 'scanned' | 'verified'
  | 'finalized' | 'completed' | 'cancelled' | 'expired' | 'invalid';

export type QrTransaction = {
  id: string;
  transaction_id: string | null;
  reservation_id: string;
  operator_id: string | null;
  token: string;
  status: QrTransactionStatus;
  prosumer_nic: string | null;
  hub_id: string | null;
  slot_id: string | null;
  energy_amount: number | null;
  expiry_time: string | null;
  generated_at: string | null;
  scanned_at: string;
  verified_at: string | null;
  finalized_at: string | null;
  notes: string | null;
  reservation?: Reservation;
  operator?: Operator;
};

export type Operator = {
  id: string;
  user_id: string | null;
  badge_id: string;
  full_name: string;
  assigned_hub_id: string | null;
  status: 'active' | 'inactive';
  created_at: string;
  assigned_hub?: Hub;
};

export type UserProfile = {
  id: string;
  email: string;
  full_name: string;
  role: 'backoffice' | 'grid_operator';
  status: 'active' | 'deactivated';
  badge_id: string | null;
  assigned_hub_id: string | null;
  created_at: string;
  updated_at: string;
  assigned_hub?: Hub;
};

// --- Helper Functions ---
async function fetchApi<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers || {}),
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `API error: ${response.statusText}`);
  }

  return response.json();
}

// --- API Service Methods ---

export const ApiService = {
  // Prosumers
  getProsumers: () => fetchApi<Prosumer[]>('/prosumers'),
  updateProsumerStatus: (id: string, status: string) => 
    fetchApi<Prosumer>(`/prosumers/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),

  // Hubs
  getHubs: () => fetchApi<Hub[]>('/hubs'),
  updateHubStatus: (id: string, status: string) =>
    fetchApi<Hub>(`/hubs/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),

  // Booking Slots
  getBookingSlots: () => fetchApi<BookingSlot[]>('/slots'),
  createBookingSlot: (slot: Partial<BookingSlot>) =>
    fetchApi<BookingSlot>('/slots', {
      method: 'POST',
      body: JSON.stringify(slot)
    }),

  // Reservations
  getReservations: () => fetchApi<Reservation[]>('/reservations'),
  updateReservationStatus: (id: string, status: string) =>
    fetchApi<Reservation>(`/reservations/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),
    
  // Operators
  getOperators: () => fetchApi<Operator[]>('/operators'),
  updateOperatorStatus: (id: string, status: string) =>
    fetchApi<Operator>(`/operators/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),

  // Users
  getUsers: () => fetchApi<UserProfile[]>('/users'),
  updateUserRole: (id: string, role: string) =>
    fetchApi<UserProfile>(`/users/${id}/role`, {
      method: 'PUT',
      body: JSON.stringify({ role })
    }),
    
  // Auth Dummy logic
  signUp: async (nic: string, email: string, password: string, fullName: string, role: string) => {
    const res = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nic, email, password, fullName, role: role === 'backoffice' ? 0 : (role === 'grid_operator' ? 1 : 2) })
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.message || 'Registration failed');
    }
    return res.json();
  },

  login: async (email: string, password: string) => {
    const res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.message || 'Invalid credentials');
    }
    return res.json();
  },
  
  // Dashboard specific (for the dashboard component graphs etc)
  getDashboardStats: async () => {
    return fetchApi<any>('/dashboard/stats');
  }
};
