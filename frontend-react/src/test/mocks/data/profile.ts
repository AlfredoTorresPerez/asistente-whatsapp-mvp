export const mockProfile = {
  id: '40000000-0000-0000-0000-000000000001',
  firstName: 'Carla',
  lastName: 'Mendez',
  email: 'admin@demo.cl',
  phone: '+56955550101',
  timezone: 'America/Santiago',
  role: 'OWNER',
  businessName: 'Centro Estetico Bella',
  businessId: '11111111-1111-1111-1111-111111111111',
}

export const mockAdminSummary = {
  companyName: 'Centro Estetico Bella',
  totalUsers: 5,
  activeUsers: 3,
  sessionTimeoutMinutes: 480,
  status: 'ACTIVE',
}

export const mockSecurityPolicy = {
  sessionTimeoutMinutes: 480,
  passwordMinLength: 8,
  passwordRequiresUppercase: true,
  passwordRequiresDigit: true,
  maxLoginAttempts: 5,
  lockoutDurationMinutes: 30,
}

export const mockCompanySettings = {
  legalName: 'Centro Estetico Bella SpA',
  businessName: 'Centro Estetico Bella',
  timezone: 'America/Santiago',
  currency: 'CLP',
  contactEmail: 'contacto@centroesteticobella.cl',
  supportPhone: '+56955550100',
  address: 'Av. Providencia 1234',
}

export const mockAdminUsers = {
  items: [
    {
      id: '40000000-0000-0000-0000-000000000001',
      firstName: 'Carla',
      lastName: 'Mendez',
      email: 'admin@demo.cl',
      role: 'OWNER',
      status: 'ACTIVE',
      lastLoginAt: '2026-07-14T10:00:00Z',
    },
    {
      id: '40000000-0000-0000-0000-000000000002',
      firstName: 'Pedro',
      lastName: 'Gonzalez',
      email: 'pedro@demo.cl',
      role: 'ADMIN',
      status: 'ACTIVE',
      lastLoginAt: '2026-07-13T15:30:00Z',
    },
  ],
  page: 0,
  size: 50,
  totalItems: 2,
  totalPages: 1,
}

export const mockRoles = [
  { id: 'OWNER', name: 'Propietario', description: 'Acceso completo' },
  { id: 'ADMIN', name: 'Administrador', description: 'Gestion del negocio' },
  { id: 'MANAGER', name: 'Encargado', description: 'Operacion diaria' },
]

export const mockWhatsAppConfig = {
  status: 'CONNECTED',
  phoneNumber: '+56999900001',
  businessName: 'Centro Estetico Bella',
  lastSyncAt: '2026-07-15T10:00:00Z',
  sessionActiveHours: 72,
  connectedFrom: 'Chrome en Windows',
  adapterMode: 'WEB',
  preferences: {
    newMessageNotifications: true,
    autoReassignment: false,
    agentSignature: true,
    outOfHoursMessage: false,
  },
  linkedDevices: [
    {
      deviceName: 'Chrome',
      operatorName: 'Carla Mendez',
      status: 'ACTIVE',
      lastActivityAt: '2026-07-15T10:30:00Z',
    },
  ],
  mainChannel: {
    name: 'WhatsApp Principal',
    type: 'WHATSAPP',
    phoneNumber: '+56999900001',
    businessHours: 'Lun-Vie 09:00-18:00',
    autoReplyEnabled: true,
  },
  history: [
    {
      eventType: 'CONNECTED',
      actorName: 'Carla Mendez',
      eventTitle: 'Sesion conectada',
      timestamp: '2026-07-15T10:00:00Z',
      tone: 'success',
    },
  ],
}

export const mockBusinessLocations = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    code: 'PROV',
    name: 'Sucursal Providencia',
    address: 'Av. Providencia 1234',
    city: 'Santiago',
    commune: 'Providencia',
    phone: '+56955550100',
    whatsappNumber: '+56999900001',
    timezone: 'America/Santiago',
    active: true,
  },
  {
    id: '11111111-1111-1111-1111-111111111112',
    code: 'LSC',
    name: 'Sucursal Las Condes',
    address: 'Av. Las Condes 5678',
    city: 'Santiago',
    commune: 'Las Condes',
    phone: '+56955550101',
    whatsappNumber: '+56999900002',
    timezone: 'America/Santiago',
    active: true,
  },
]

export const mockMultisiteSummary = {
  locations: [
    {
      locationId: '11111111-1111-1111-1111-111111111111',
      locationName: 'Sucursal Providencia',
      locationCode: 'PROV',
      active: true,
      metrics: {
        conversations: 45,
        leads: 12,
        bookings: 28,
        orders: 15,
        productsWithStock: 34,
        professionals: 6,
      },
    },
    {
      locationId: '11111111-1111-1111-1111-111111111112',
      locationName: 'Sucursal Las Condes',
      locationCode: 'LSC',
      active: true,
      metrics: {
        conversations: 30,
        leads: 8,
        bookings: 20,
        orders: 10,
        productsWithStock: 28,
        professionals: 4,
      },
    },
  ],
}
