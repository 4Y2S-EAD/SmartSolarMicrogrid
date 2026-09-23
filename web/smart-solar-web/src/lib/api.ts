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
  deactivation_reason?: string;
  created_at: string;
  updated_at: string;
};

export type Hub = {
  stationId: string;
  stationName: string;
  location: {
    latitude: number;
    longitude: number;
  };
  capacityKwh: number;
  batterySlotCount: number;
  availableSlotCount: number;
  status: string;
  schedule?: string;
  createdAt: string;
  updatedAt: string;
};

export type BookingSlot = {
  slotId: string;
  stationId: string;
  slotNumber: number;
  bookingDate: string;
  startTime: string;
  endTime: string;
  capacityKwh: number;
  status: string;
  reservationId?: string;
  updatedAt: string;
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


// member 03
export type ReservationSummary = {
  reservationId: string;
  prosumerNic: string;
  stationId: string;
  stationName: string;
  slotId: string;
  slotNumber: number;
  bookingDate: string;
  startTime: string;
  endTime: string;
  status: string;
  qrToken: string | null;
  createdAt: string;
  updatedAt: string;
  verifiedAt?: string | null;
  completedAt?: string | null;
  cancellationReason?: string | null;
};

export type UserDashboardStats = {
  prosumerNic: string;
  totalReservations: number;
  pendingCount: number;
  approvedCount: number;
  completedCount: number;
  cancelledCount: number;
};

export type PaginatedReservations = {
  currentPage: number;
  pageSize: number;
  totalRecords: number;
  totalPages: number;
  items: ReservationSummary[];
};

export type CreateReservationPayload = {
  prosumerNic: string;
  stationId: string;
  slotId: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
};

export type UpdateReservationPayload = {
  stationId: string;
  slotId: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
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
  updateProsumerStatus: (nic: string, status: string) =>
    fetchApi<Prosumer>(`/prosumers/${nic}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),

  // Hubs (Microgrid Nodes)
  getHubs: () => fetchApi<Hub[]>('/stations'),

  createHub: (payload: any) =>
    fetchApi<Hub>('/stations', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  updateHub: (id: string, payload: any) =>
    fetchApi<Hub>(`/stations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),

  // Deactivate hub (Our C# API uses DELETE for deactivation)
  deactivateHub: (id: string) =>
    fetchApi<any>(`/stations/${id}`, {
      method: 'DELETE'
    }),

  // Booking Slots
  getBookingSlots: (stationId: string) => fetchApi<BookingSlot[]>(`/stations/${stationId}/slots`),

  createBookingSlot: (stationId: string, slot: any) =>
    fetchApi<BookingSlot>(`/stations/${stationId}/slots`, {
      method: 'POST',
      body: JSON.stringify(slot)
    }),

  updateBookingSlot: (slotId: string, payload: any) =>
    fetchApi<BookingSlot>(`/stations/slots/${slotId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),

  deleteBookingSlot: (slotId: string) =>
    fetchApi<any>(`/stations/slots/${slotId}`, {
      method: 'DELETE'
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
  getUsers: () => fetchApi<UserProfile[]>('/member1/users'),
  
  createUser: (payload: any) => 
    fetchApi<any>('/member1/users', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  updateUser: (nic: string, payload: any) =>
    fetchApi<any>(`/member1/users/${nic}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),

  updateUserStatus: (nic: string, status: string) =>
    fetchApi<any>(`/member1/users/${nic}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),

  deleteUser: (nic: string) =>
    fetchApi<any>(`/member1/users/${nic}`, {
      method: 'DELETE'
    }),

  getUserReservationDashboard: (nic: string) =>
    fetchApi<UserDashboardStats>(`/reservations/user/${nic}/dashboard`),

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
  },

  // QR Transactions (Mocked since endpoints may not exist in C# API yet)
  getQrTransactions: () => fetchApi<QrTransaction[]>('/qr-transactions').catch(() => []),

  verifyQrToken: async (token: string) => {
    try {
      const res = await fetchApi<any>(`/qr-transactions/verify`, {
        method: 'POST',
        body: JSON.stringify({ token })
      });
      return { data: res, error: null };
    } catch (err: any) {
      // Return a mock error or handle properly when backend is ready
      return { data: null, error: err.message || 'Verification failed' };
    }
  },

  finalizeQrTransaction: async (token: string) => {
    try {
      const res = await fetchApi<any>(`/qr-transactions/finalize`, {
        method: 'POST',
        body: JSON.stringify({ token })
      });
      return { data: res, error: null };
    } catch (err: any) {
      return { data: null, error: err.message || 'Finalization failed' };
    }
  },

  // Member 03 - Prosumer Reservations
  getReservationsByStatus: (nic: string, status: 'pending' | 'approved' | 'completed' | 'cancelled', page = 1, pageSize = 10) =>
    fetchApi<PaginatedReservations>(`/reservations/user/${nic}/${status}?page=${page}&pageSize=${pageSize}`),

  getReservationById: (id: string) =>
    fetchApi<ReservationSummary>(`/reservations/${id}`),

  createReservation: (payload: CreateReservationPayload) =>
    fetchApi<ReservationSummary>('/reservations', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateReservation: (id: string, payload: UpdateReservationPayload) =>
    fetchApi<ReservationSummary>(`/reservations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  cancelReservation: (id: string, reason: string) =>
    fetchApi<{ message: string; reservationId: string; status: string }>(`/reservations/${id}/cancel`, {
      method: 'PUT',
      body: JSON.stringify({ cancellationReason: reason }),
    }),

  deleteReservationPermanent: (id: string) =>
    fetchApi<{ message: string; reservationId: string }>(`/reservations/${id}`, {
      method: 'DELETE',
    }),

  generateQrCode: (id: string) =>
    fetchApi<{ message: string; reservationId: string; qrToken: string; generatedAt: string }>(`/reservations/${id}/generate-qr`, {
      method: 'POST',
    }),
};
