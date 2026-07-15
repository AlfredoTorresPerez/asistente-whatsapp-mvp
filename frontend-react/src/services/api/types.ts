export type AuthUserResponse = {
  id: string
  firstName: string
  lastName: string
  email: string
  role: string
  businessId: string
  businessName: string
  timezone: string
  permissions: string[]
}

export type LoginResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: AuthUserResponse
}

export type ForgotPasswordResponse = {
  status: 'ACCEPTED'
  message: string
}

export type ResetPasswordValidationResponse = {
  valid: boolean
  expiresAt: string | null
}

export type StatusResponse = {
  status: string
}

export type UserProfileResponse = {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  timezone: string
  role: string
  businessName: string
}

export type UpdateProfileRequest = {
  firstName: string
  lastName: string
  phone: string
  timezone: string
}

export type ChangePasswordRequest = {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export type AuditLogResponse = {
  id: string
  actorUserId: string | null
  actionType: string
  entityType: string
  entityId: string | null
  summary: string
  metadata: Record<string, unknown>
  occurredAt: string
}

export type PagedResponse<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type DashboardKpisResponse = {
  openConversations: number
  newProspects: number
  openOrders: number
  pendingAppointments: number
}

export type DashboardSeriesPointResponse = {
  label: string
  value: number
}

export type DashboardAppointmentResponse = {
  id: string
  subject: string
  status: string
  customerName: string
  startsAt: string
  durationMinutes: number
  location: string | null
}

export type DashboardActivityResponse = {
  entityType: string
  entityId: string
  title: string
  body: string
  status: string
  occurredAt: string
}

export type DashboardSummaryResponse = {
  kpis: DashboardKpisResponse
  conversationSeries: DashboardSeriesPointResponse[]
  orderSeries: DashboardSeriesPointResponse[]
  todayAppointments: DashboardAppointmentResponse[]
  recentActivity: DashboardActivityResponse[]
}

export type ReportsKpiItem = {
  label: string
  currentValue: number
  previousValue: number
  variationPercent: number | null
  help: string
}

export type ReportsChannelResponse = {
  channel: string
  count: number
  percentage: number
}

export type ReportsConversationPerformancePoint = {
  date: string
  received: number
  aiAnswered: number
  humanAnswered: number
  unanswered: number
}

export type ReportsAppointmentPerformancePoint = {
  date: string
  solicitada: number
  confirmada: number
  completada: number
  cancelada: number
  ausencia: number
}

export type ReportsAppointmentDistributionPoint = {
  status: string
  label: string
  count: number
  percentage: number
}

export type ReportsFunnelStageResponse = {
  name: string
  count: number
  conversionFromPrevious: number | null
  conversionFromFirst: number | null
}

export type ReportsProspectRowResponse = {
  id: string
  name: string
  phone: string
  lastContact: string
  stage: string
  responsible: string | null
  nextAppointment: string | null
  location: string | null
  serviceInterest: string | null
  attentionStatus: string
}

export type ReportsProspectsResponse = {
  items: ReportsProspectRowResponse[]
  total: number
  page: number
  size: number
}

export type ReportsSummaryResponse = {
  kpis: ReportsKpiItem[]
  channelDistribution: ReportsChannelResponse[]
  conversationPerformance: ReportsConversationPerformancePoint[]
  appointmentDistribution: ReportsAppointmentDistributionPoint[]
  appointmentPerformance: ReportsAppointmentPerformancePoint[]
  conversionFunnel: ReportsFunnelStageResponse[]
  prospects: ReportsProspectsResponse
}

export type NotificationResponse = {
  id: string
  type: string
  status: string
  title: string
  body: string
  relatedEntityType: string | null
  relatedEntityId: string | null
  createdAt: string
  readAt: string | null
}

export type NotificationReadResponse = {
  id: string
  status: string
  readAt: string | null
}

export type NotificationsReadAllResponse = {
  updatedCount: number
}

export type AdminSummaryResponse = {
  company: {
    id: string
    companyName: string
  }
  users: {
    total: number
    active: number
  }
  whatsappWeb: {
    status: string
  }
  security: {
    sessionTimeoutMinutes: number
  }
}

export type AdminRoleResponse = {
  id: string
  code: string
  name: string
  description: string
  permissionCount: number
}

export type AdminUserResponse = {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  role: string
  status: string
  timezone: string
  lastLoginAt: string | null
  failedLoginAttempts: number
  createdAt: string
  updatedAt: string
}

export type AdminUserRequest = {
  firstName: string
  lastName: string
  email: string
  phone: string | null
  role: string
  status: string
  timezone: string
  temporaryPassword: string | null
}

export type SecurityPolicyResponse = {
  id: string
  sessionTimeoutMinutes: number
  passwordMinLength: number
  requireUppercase: boolean
  requireNumber: boolean
  requireSymbol: boolean
  maxFailedLoginAttempts: number
  activeUsers: number
  lockedUsers: number
  auditEventsLast7Days: number
  updatedAt: string
}

export type SecurityPolicyRequest = {
  sessionTimeoutMinutes: number
  passwordMinLength: number
  requireUppercase: boolean
  requireNumber: boolean
  requireSymbol: boolean
  maxFailedLoginAttempts: number
}

export type CompanySettingsRequest = {
  companyName: string
  businessName: string
  timezone: string
  currency: string
  contactEmail: string
  supportPhone: string
  address: string
}

export type CompanySettingsResponse = {
  id: string
  companyName: string
  businessName: string
  timezone: string
  currency: string
  contactEmail: string
  supportPhone: string | null
  address: string | null
}

export type WhatsAppWebRecentEventResponse = {
  deliveryId: string
  eventType: string
  processingStatus: string
  receivedAt: string
  processedAt: string | null
}

export type WhatsAppWebStatusResponse = {
  sessionStatus: string
  phoneNumber: string | null
  qrCode: string | null
  lastEventAt: string | null
  adapterReachable: boolean
  adapterMode: string
  warningMessage: string
  recentEvents: WhatsAppWebRecentEventResponse[]
}

export type WhatsAppWebQrResponse = {
  qrCode: string | null
  sessionStatus: string
  expiresAt: string | null
  lastQrAt: string | null
}

export type WhatsAppWebActionResponse = {
  sessionStatus: string
  phoneNumber: string | null
  qrCode: string | null
  acceptedAt: string
  adapterReachable: boolean
  adapterMode: string
}

export type WhatsAppWebTestMessageRequest = {
  recipientPhone: string
  body: string
}

export type WhatsAppWebTestMessageResponse = {
  conversationId: string
  messageId: string
  externalMessageId: string
  deliveryStatus: string
  acceptedAt: string
}

export type ConversationMetricsResponse = {
  activeConversations: number
  unattendedConversations: number
  newProspects: number
  activeOrders: number
}

export type ConversationAiReplyResponse = {
  suggestedBody: string
  confidence: number
  source: string
}

export type ConversationSummaryResponse = {
  id: string
  customerName: string
  customerPhone: string
  status: string
  unreadCount: number
  lastMessagePreview: string | null
  lastMessageAt: string | null
  channelType: string
  assignedUserId: string | null
  assignedUserName: string | null
  prospectId: string | null
  locationId: string | null
  locationName: string | null
}

export type ConversationCustomerResponse = {
  id: string
  firstName: string
  lastName: string
  displayName: string
  phone: string
  email: string | null
}

export type ConversationMessageResponse = {
  id: string
  direction: string
  messageType: string
  body: string
  status: string
  externalMessageId: string | null
  sentByUserId: string | null
  sentByUserName: string | null
  sentAt: string | null
  receivedAt: string | null
  failedAt: string | null
  createdAt: string
}

export type ConversationDetailResponse = {
  id: string
  status: string
  channelType: string
  unreadCount: number
  lastMessagePreview: string | null
  lastMessageAt: string | null
  openedAt: string | null
  closedAt: string | null
  assignedUserId: string | null
  assignedUserName: string | null
  prospectId: string | null
  locationId: string | null
  locationName: string | null
  customer: ConversationCustomerResponse
  messages: ConversationMessageResponse[]
}

export type CreateConversationRequest = {
  customerId?: string
  customerName?: string
  customerPhone?: string
  customerEmail?: string
  ownerUserId?: string
  initialMessage?: string
}

export type SendConversationMessageRequest = {
  body?: string
  templateId?: string
  idempotencyKey?: string
  aiSource?: string
}

export type AssignConversationRequest = {
  userId: string
}

export type ResponseTemplateResponse = {
  id: string
  name: string
  category: string
  body: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export type CreateResponseTemplateRequest = {
  name: string
  category: string
  body: string
  active?: boolean
}

export type UpdateResponseTemplateRequest = {
  name: string
  category: string
  body: string
}

export type UpdateTemplateStatusRequest = {
  active: boolean
}

export type LeadSummaryResponse = {
  id: string
  customerId: string
  conversationId: string | null
  firstName: string
  lastName: string
  displayName: string
  phone: string
  email: string | null
  stage: string
  sourceType: string
  assignedUserId: string | null
  assignedUserName: string | null
  createdAt: string
  updatedAt: string
}

export type LeadNoteResponse = {
  id: string
  authorUserId: string
  authorUserName: string
  noteText: string
  createdAt: string
  updatedAt: string
}

export type LeadDetailResponse = {
  id: string
  customerId: string
  conversationId: string | null
  firstName: string
  lastName: string
  displayName: string
  phone: string
  email: string | null
  stage: string
  sourceType: string
  notes: string | null
  assignedUserId: string | null
  assignedUserName: string | null
  active: boolean
  createdAt: string
  updatedAt: string
  noteHistory: LeadNoteResponse[]
}

export type CreateLeadRequest = {
  firstName: string
  lastName: string
  phone: string
  email?: string
  notes?: string
  stage?: string
  assignedUserId?: string
}

export type UpdateLeadRequest = CreateLeadRequest

export type CreateLeadFromConversationRequest = {
  firstName?: string
  lastName?: string
  phone?: string
  email?: string
  notes?: string
  stage?: string
  assignedUserId?: string
}

export type AddLeadNoteRequest = {
  noteText: string
}

export type UpdateLeadStageRequest = {
  stage: string
}

export type BookingSummaryResponse = {
  id: string
  subject: string
  status: string
  startsAt: string
  durationMinutes: number
  locationId: string | null
  location: string | null
  locationName: string | null
  customerId: string
  customerName: string
  customerPhone: string
  leadId: string | null
  conversationId: string | null
  assignedUserId: string | null
  assignedUserName: string | null
  requiresDeposit: boolean
  depositAmount: number
  paymentStatus: string
  calendarSyncStatus: string
}

export type BookingDetailResponse = {
  id: string
  subject: string
  status: string
  startsAt: string
  durationMinutes: number
  locationId: string | null
  location: string | null
  locationName: string | null
  notes: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
  customerId: string
  customerName: string
  customerPhone: string
  customerEmail: string | null
  leadId: string | null
  conversationId: string | null
  assignedUserId: string | null
  assignedUserName: string | null
  requiresDeposit: boolean
  depositAmount: number
  paymentStatus: string
  statusHistory: BookingStatusHistoryResponse[]
  publicLinks: BookingPublicLinkSummaryResponse[]
  reminders: BookingReminderResponse[]
  emailLogs: BookingEmailLogResponse[]
  payments: BookingPaymentResponse[]
}

export type BookingPaymentResponse = {
  id: string
  bookingId: string
  provider: string
  providerPaymentId: string | null
  idempotencyKey: string | null
  amount: number
  currency: string
  status: string
  checkoutUrl: string | null
  checkoutExpiresAt: string | null
  manual: boolean
  approvedAt: string | null
  rejectedAt: string | null
  expiredAt: string | null
  refundedAt: string | null
  createdAt: string
}

export type PublicBookingPaymentDetailResponse = {
  id: string
  bookingId: string
  provider: string
  providerPaymentId: string | null
  amount: number
  currency: string
  status: string
  checkoutUrl: string | null
  checkoutExpiresAt: string | null
  manual: boolean
  approvedAt: string | null
  rejectedAt: string | null
  expiredAt: string | null
  refundedAt: string | null
  createdAt: string
  bookingStatus: string
  bookingPaymentStatus: string
  subject: string
  serviceName: string
  professionalName: string | null
  roomName: string | null
  startsAt: string
  durationMinutes: number
  locationName: string | null
  customerName: string
}

export type BookingStatusHistoryResponse = {
  id: string
  previousStatus: string | null
  newStatus: string
  reason: string | null
  actorUserId: string | null
  source: string
  createdAt: string
}

export type BookingPublicLinkSummaryResponse = {
  id: string
  type: string
  status: string
  url: string
  expiresAt: string
  usedAt: string | null
  createdAt: string
}

export type BookingReminderResponse = {
  id: string
  reminderType: string
  channelType: string
  scheduledAt: string
  sentAt: string | null
  status: string
  failureReason: string | null
  templateKey: string | null
}

export type BookingEmailLogResponse = {
  id: string
  recipientEmail: string
  subject: string
  templateKey: string
  status: string
  simulation: boolean
  failureReason: string | null
  sentAt: string | null
  createdAt: string
}

export type CreateBookingRequest = {
  subject: string
  customerId?: string
  customerFirstName?: string
  customerLastName?: string
  customerName?: string
  customerPhone?: string
  customerEmail?: string
  status?: string
  assignedUserId?: string
  startsAt: string
  durationMinutes?: number
  locationId?: string
  location?: string
  notes?: string
}

export type UpdateBookingRequest = {
  subject: string
  status?: string
  assignedUserId?: string
  startsAt: string
  durationMinutes?: number
  locationId?: string
  location?: string
  notes?: string
}

export type RescheduleBookingRequest = {
  startsAt: string
  durationMinutes?: number
  locationId?: string
  location?: string
  notes?: string
}

export type CancelBookingRequest = {
  reason?: string
}

export type CreateBookingPaymentLinkRequest = {
  provider?: string
  paymentPurpose?: 'DEPOSIT' | 'FULL' | 'MANUAL'
  amount?: number
  currency?: string
  expirationMinutes?: number
  sendWhatsApp?: boolean
  sendEmail?: boolean
  metadata?: Record<string, unknown>
}

export type RegisterBookingManualPaymentRequest = {
  provider?: string
  providerPaymentId?: string
  idempotencyKey?: string
  amount?: number
  currency?: string
  status?: string
  occurredAt?: string
  notes?: string
  metadata?: Record<string, unknown>
}

export type RefundBookingPaymentRequest = {
  reason?: string
}

export type CreateBookingFromConversationRequest = {
  subject: string
  leadId?: string
  assignedUserId?: string
  status?: string
  startsAt: string
  durationMinutes?: number
  locationId?: string
  location?: string
  notes?: string
}

export type CreateBookingFromLeadRequest = {
  subject: string
  assignedUserId?: string
  status?: string
  startsAt: string
  durationMinutes?: number
  locationId?: string
  location?: string
  notes?: string
}

export type CreateBookingConfirmationLinkRequest = {
  expirationMinutes?: number
  sendWhatsApp?: boolean
}

export type BookingConfirmationLinkResponse = {
  id: string
  bookingId: string
  status: string
  confirmationUrl: string
  expiresAt: string
  sentAt: string | null
}

export type CreateBookingRescheduleLinkRequest = {
  locationId: string
  serviceId?: string
  professionalId?: string
  roomId?: string
  startsAt: string
  reason?: string
  expirationMinutes?: number
  sendWhatsApp?: boolean
  sendEmail?: boolean
}

export type CreateBookingCancellationLinkRequest = {
  reason?: string
  expirationMinutes?: number
  sendWhatsApp?: boolean
  sendEmail?: boolean
}

export type BookingPublicActionLinkResponse = {
  id: string
  bookingId: string
  type: string
  status: string
  publicUrl: string
  expiresAt: string
  whatsappSentAt: string | null
  emailSentAt: string | null
}

export type PublicBookingConfirmationResponse = {
  bookingId: string
  bookingStatus: string
  linkStatus: string
  subject: string
  serviceName: string | null
  professionalName: string | null
  roomName: string | null
  startsAt: string
  durationMinutes: number
  locationId: string | null
  location: string | null
  locationName: string | null
  customerName: string
  maskedCustomerPhone: string
  requiresDeposit: boolean
  depositAmount: number
  paymentStatus: string
  expiresAt: string
  confirmedAt: string | null
}

export type PublicBookingCancellationFromConfirmationRequest = {
  reason: string
}

export type PublicBookingRescheduleFromConfirmationRequest = {
  startsAt: string
  professionalId?: string | null
  roomId?: string | null
  reason?: string
}

export type PublicBookingRescheduleResponse = {
  bookingId: string
  bookingStatus: string
  linkStatus: string
  subject: string
  serviceName: string | null
  currentLocationName: string | null
  proposedLocationName: string | null
  currentProfessionalName: string | null
  proposedProfessionalName: string | null
  currentRoomName: string | null
  proposedRoomName: string | null
  currentStartsAt: string
  proposedStartsAt: string
  proposedEndsAt: string
  customerName: string
  maskedCustomerPhone: string
  expiresAt: string
  usedAt: string | null
  reason: string | null
  bookings: CustomerBookingItemResponse[]
  services: Array<{
    id: string
    name: string
    categoryName: string
    durationMinutes: number
    requiresRoom: boolean
  }>
  locations: Array<{
    id: string
    name: string
    address: string | null
    commune: string | null
  }>
}

export type PublicBookingRescheduleRequest = {
  bookingId: string
  serviceId: string
  locationId: string
  professionalId?: string | null
  roomId?: string | null
  date: string
  startsAt: string
  reason?: string
}

export type PublicBookingCancellationResponse = {
  bookingId: string
  bookingStatus: string
  linkStatus: string
  subject: string
  serviceName: string | null
  locationName: string | null
  professionalName: string | null
  roomName: string | null
  startsAt: string
  endsAt: string
  customerName: string
  maskedCustomerPhone: string
  expiresAt: string
  usedAt: string | null
  cancellationReason: string | null
}

export type PublicCategoryResponse = {
  id: string
  code: string
  name: string
  description: string | null
  active: boolean
}

export type PublicServiceItemResponse = {
  id: string
  code: string
  name: string
  description: string | null
  durationMinutes: number
  priceBase: number
  categoryCode: string
  categoryName: string
  active: boolean
  requiresPriorEvaluation: boolean
  requiresInformedConsent: boolean
}

export type PublicServiceDetailResponse = {
  id: string
  code: string
  name: string
  description: string | null
  categoryCode: string
  categoryName: string
  durationMinutes: number
  priceBase: number
  requiresPriorEvaluation: boolean
  requiresInformedConsent: boolean
  professionalRequired: string
  supplies: string | null
  contraindications: string | null
  aftercareRecommendations: string | null
  requiresDeposit: boolean
  depositAmount: number
  active: boolean
}

export type PublicServiceBranchResponse = {
  id: string
  code: string
  name: string
  address: string | null
  city: string | null
  commune: string | null
  phone: string | null
}

export type CreatePublicBookingRequest = {
  locationId: string
  serviceId: string
  professionalId?: string
  roomId?: string
  startsAt: string
  customerName: string
  customerPhone: string
  customerEmail?: string
}

export type PublicCustomerInfoResponse = {
  customerName: string | null
  customerPhone: string | null
  customerEmail: string | null
  lastLocationId: string | null
  lastLocationName: string | null
}

export type CreatePublicBookingResponse = {
  bookingId: string
  status: string
  startsAt: string
  durationMinutes: number
  locationName: string
  serviceName: string
  professionalName: string | null
  roomName: string | null
  customerName: string
  message: string
}

export type AestheticServiceResponse = {
  id: string
  code: string
  name: string
  description: string
  categoryCode: string
  categoryName: string
  durationMinutes: number
  priceBase: number
  professionalRequired: string
  supplies: string | null
  contraindications: string | null
  availabilityRules: string | null
  bookingRules: string | null
  cancellationRules: string | null
  aftercareRecommendations: string | null
  requiresPriorEvaluation: boolean
  requiresInformedConsent: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type AestheticProductResponse = {
  id: string
  code: string
  name: string
  description: string
  categoryCode: string
  categoryName: string
  price: number
  stock: number
  stockMinimum: number
  supplier: string | null
  expirationDate: string | null
  compatibleServices: string | null
  recommendationRules: string | null
  crossSellRules: string | null
  usageRestrictions: string | null
  lowStock: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export type AestheticBusinessRuleResponse = {
  id: string
  code: string
  name: string
  ruleType: string
  description: string
  priority: number
  active: boolean
  rulePayload: string
  createdAt: string
  updatedAt: string
}

export type IntentEntitiesResponse = {
  servicio: string | null
  producto: string | null
  fecha: string | null
  hora: string | null
  profesional: string | null
  cliente: string | null
}

export type IntentAnalysisRequest = {
  customerId?: string | null
  conversationId?: string | null
  message: string
}

export type IntentAnalysisResponse = {
  intencion: string
  confianza: number
  entidades: IntentEntitiesResponse
  requiereConsultaBaseDatos: boolean
  requiereDerivacionHumana: boolean
  motivoDerivacion: string | null
  respuestaSugerida: string
  modelo: string
}

export type AestheticIntentLogResponse = {
  id: string
  sourceMessage: string
  intent: string
  confidence: number
  entities: string
  requiresDatabaseLookup: boolean
  requiresHumanHandoff: boolean
  handoffReason: string | null
  suggestedResponse: string | null
  modelName: string
  createdAt: string
}

export type AestheticCategoryResponse = {
  id: string
  code: string
  name: string
  description: string | null
  active: boolean
}

export type UpsertAestheticServiceRequest = {
  code?: string | null
  categoryCode: string
  name: string
  description: string
  durationMinutes: number
  priceBase: number
  professionalRequired: string
  supplies?: string | null
  contraindications?: string | null
  availabilityRules?: string | null
  bookingRules?: string | null
  cancellationRules?: string | null
  aftercareRecommendations?: string | null
  requiresPriorEvaluation: boolean
  requiresInformedConsent: boolean
  active: boolean
}

export type UpsertAestheticProductRequest = {
  code?: string | null
  categoryCode: string
  name: string
  description: string
  price: number
  stock: number
  stockMinimum: number
  supplier?: string | null
  expirationDate?: string | null
  compatibleServices?: string | null
  recommendationRules?: string | null
  crossSellRules?: string | null
  usageRestrictions?: string | null
  active: boolean
}

export type UpsertAestheticBusinessRuleRequest = {
  code?: string | null
  name: string
  ruleType: string
  description: string
  priority: number
  active: boolean
  rulePayload: string
}

export type CatalogCategoryResponse = {
  id: string
  code: string
  name: string
  description: string | null
  active: boolean
}

export type CatalogProductResponse = {
  id: string
  categoryId: string
  categoryCode: string
  categoryName: string
  sku: string
  name: string
  description: string | null
  price: number
  stock: number
  stockMinimum: number
  lowStock: boolean
  supplier: string | null
  expiresAt: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export type UpsertCatalogCategoryRequest = {
  code: string
  name: string
  description?: string | null
  active?: boolean
}

export type UpsertCatalogProductRequest = {
  categoryCode: string
  sku?: string | null
  name: string
  description?: string | null
  price: number
  stock?: number
  stockMinimum?: number
  supplier?: string | null
  expiresAt?: string | null
  active?: boolean
}

export type OrderItemResponse = {
  id: string
  productId: string
  productName: string
  sku: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export type OrderPaymentResponse = {
  id: string
  amount: number
  method: string
  paidAt: string
  reference: string | null
  notes: string | null
}

export type OrderSummaryResponse = {
  id: string
  orderNumber: string
  customerId: string
  customerName: string
  customerPhone: string
  leadId: string | null
  conversationId: string | null
  status: string
  paymentStatus: string
  subtotalAmount: number
  discountAmount: number
  totalAmount: number
  paidAmount: number
  balanceDue: number
  currency: string
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

export type OrderDetailResponse = OrderSummaryResponse & {
  notes: string | null
  items: OrderItemResponse[]
  payments: OrderPaymentResponse[]
  receiptPreview: string
}

export type CreateOrderItemRequest = {
  productId: string
  quantity: number
}

export type CreateOrderRequest = {
  customerId?: string | null
  leadId?: string | null
  conversationId?: string | null
  customerName?: string | null
  customerPhone?: string | null
  customerEmail?: string | null
  status?: string | null
  discountAmount?: number
  dueDate?: string | null
  notes?: string | null
  items?: CreateOrderItemRequest[]
}

export type UpdateOrderRequest = {
  status?: string | null
  discountAmount?: number
  dueDate?: string | null
  notes?: string | null
  items?: CreateOrderItemRequest[]
}

export type RegisterPaymentRequest = {
  amount: number
  method?: string | null
  paidAt?: string | null
  reference?: string | null
  notes?: string | null
}

export type SendOrderSummaryResponse = {
  status: string
  externalMessageId: string
  acceptedAt: string
  body: string
}

export type WhatsAppConfigurationPreferencesResponse = {
  newMessageNotifications: boolean
  autoReassignment: boolean
  agentSignature: boolean
  outOfHoursMessage: boolean
}

export type UpdateWhatsAppConfigurationPreferencesRequest = WhatsAppConfigurationPreferencesResponse

export type WhatsAppConfigurationLinkedDeviceResponse = {
  id: string
  deviceName: string
  operatorName: string
  location: string
  browser: string
  status: string
  lastActivityAt: string | null
}

export type WhatsAppConfigurationChannelResponse = {
  channelName: string
  phoneNumber: string | null
  channelType: string
  businessHours: string
  automaticResponsesEnabled: boolean
}

export type WhatsAppConfigurationSessionHistoryResponse = {
  id: string
  title: string
  actor: string
  tone: 'success' | 'warning' | 'danger' | 'neutral' | 'info' | string
  occurredAt: string | null
}

export type WhatsAppConfigurationResponse = {
  sessionStatus: string
  phoneNumber: string | null
  businessName: string
  lastSynchronizationAt: string | null
  activeSessionHours: number
  connectedFrom: string
  qrCode: string | null
  adapterReachable: boolean
  adapterMode: string | null
  warningMessage: string | null
  preferences: WhatsAppConfigurationPreferencesResponse
  mainChannel: WhatsAppConfigurationChannelResponse
  linkedDevices: WhatsAppConfigurationLinkedDeviceResponse[]
  sessionHistory: WhatsAppConfigurationSessionHistoryResponse[]
}

export type BusinessLocationResponse = {
  id: string
  code: string
  name: string
  address: string | null
  city: string | null
  commune: string | null
  phone: string | null
  whatsappNumber: string | null
  timezone: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export type UpsertBusinessLocationRequest = {
  code: string
  name: string
  address?: string | null
  city?: string | null
  commune?: string | null
  phone?: string | null
  whatsappNumber?: string | null
  timezone?: string | null
  active?: boolean
}

export type MultisiteLocationSummaryResponse = {
  locationId: string
  locationCode: string
  locationName: string
  active: boolean
  conversations: number
  leads: number
  bookings: number
  orders: number
  productsWithStock: number
  professionals: number
}

export type MultisiteCatalogAvailabilityResponse = {
  itemId: string
  type: string
  name: string
  sku: string | null
  basePrice: number
  locationId: string
  locationName: string
  available: boolean
  priceOverride: number | null
  durationOverrideMinutes: number | null
  stockEnabled: boolean | null
  stockQuantity: number | null
  stockMinimum: number | null
}

export type UpsertCatalogAvailabilityRequest = {
  productServiceId: string
  locationId: string
  active?: boolean
  priceOverride?: number | null
  durationOverrideMinutes?: number | null
  stockEnabled?: boolean
  stockQuantity?: number | null
  stockMinimum?: number | null
}

export type ProfessionalLocationAssignmentResponse = {
  locationId: string
  locationName: string
  active: boolean
}

export type MultisiteProfessionalResponse = {
  professionalId: string
  fullName: string
  specialty: string | null
  active: boolean
  locations: ProfessionalLocationAssignmentResponse[]
}

export type ProfessionalScheduleResponse = {
  id: string
  professionalId: string
  professionalName: string
  locationId: string
  locationName: string
  dayOfWeek: number
  startTime: string
  endTime: string
  active: boolean
}

export type UpsertProfessionalScheduleRequest = {
  professionalId: string
  locationId: string
  dayOfWeek: number
  startTime: string
  endTime: string
  active?: boolean
}

export type UserLocationAccessResponse = {
  userId: string
  userName: string
  email: string
  locationId: string
  locationName: string
  roleScope: string
  canViewConversations: boolean
  canManageBookings: boolean
  canManageOrders: boolean
  canManageCatalog: boolean
  canViewReports: boolean
  active: boolean
}

export type UpsertUserLocationAccessRequest = {
  userId: string
  locationId: string
  roleScope?: string
  canViewConversations?: boolean
  canManageBookings?: boolean
  canManageOrders?: boolean
  canManageCatalog?: boolean
  canViewReports?: boolean
  active?: boolean
}

export type MultisiteChannelResponse = {
  channelId: string
  channelType: string
  providerName: string
  status: string
  phoneNumber: string | null
  locationId: string | null
  locationName: string | null
  routingMode: string
  active: boolean
}

export type UpdateChannelLocationRequest = {
  locationId?: string | null
  routingMode?: string | null
}

export type AgendaAvailabilityRequest = {
  locationId: string
  serviceId: string
  professionalId?: string
  roomId?: string
  date: string
  preference?: string
  maxSlots?: number
}

export type AgendaSlotResponse = {
  startsAt: string
  endsAt: string
  locationId: string
  locationName: string
  serviceId: string
  serviceName: string
  durationMinutes: number
  professionalId: string | null
  professionalName: string | null
  roomId: string | null
  roomName: string | null
  available: boolean
  reason: string
}

export type AgendaAvailabilityResponse = {
  locationId: string
  locationName: string
  serviceId: string
  serviceName: string
  date: string
  durationMinutes: number
  requiresRoom: boolean
  requiresDeposit: boolean
  slots: AgendaSlotResponse[]
}

export type CreateTemporaryAgendaBookingRequest = {
  locationId: string
  serviceId: string
  professionalId?: string
  roomId?: string
  startsAt: string
  customerName: string
  customerPhone: string
  customerEmail?: string
  customerId?: string
  conversationId?: string
  leadId?: string
  notes?: string
  expirationMinutes?: number
  generateConfirmationLink?: boolean
  sendWhatsApp?: boolean
}

export type AgendaCalendarItemResponse = {
  bookingId: string
  subject: string
  status: string
  startsAt: string
  endsAt: string
  durationMinutes: number
  locationId: string | null
  locationName: string | null
  serviceId: string | null
  serviceName: string | null
  professionalId: string | null
  professionalName: string | null
  roomId: string | null
  roomName: string | null
  customerName: string
  customerPhone: string
  sourceChannel: string
  startsAtLocal?: string | null
  endsAtLocal?: string | null
  dateLocal?: string | null
  startTimeLocal?: string | null
  endTimeLocal?: string | null
  timezone?: string | null
  type?: string | null
}

export type AgendaCalendarResponse = {
  from: string
  to: string
  items: AgendaCalendarItemResponse[]
}

export type AgendaFilterOptionResponse = {
  id: string
  name: string
  detail: string | null
  locationId: string | null
  active: boolean
}

export type BusinessHoursResponse = {
  dayOfWeek: number
  startTime: string
  endTime: string
}

export type SaveBusinessHoursRequest = {
  locationId: string
  hours: { dayOfWeek: number; startTime: string; endTime: string }[]
}

export type SaveProfessionalHoursRequest = {
  locationId: string
  professionalId: string
  hours: { dayOfWeek: number; startTime: string; endTime: string }[]
}

export type AgendaFilterOptionsResponse = {
  services: AgendaFilterOptionResponse[]
  professionals: AgendaFilterOptionResponse[]
  rooms: AgendaFilterOptionResponse[]
}

export type AgendaBlockRequest = {
  locationId?: string
  professionalId?: string
  roomId?: string
  startsAt: string
  endsAt: string
  reason: string
}

export type AgendaBlockResponse = {
  id: string
  locationId: string | null
  professionalId: string | null
  roomId: string | null
  startsAt: string
  endsAt: string
  reason: string
  active: boolean
}

export type AgendaRescheduleRequest = {
  locationId: string
  serviceId: string
  professionalId?: string
  roomId?: string
  startsAt: string
  reason: string
}

export type AgendaCancelRequest = {
  reason: string
}

export type CustomerBookingItemResponse = {
  bookingId: string
  locationId: string | null
  serviceId: string | null
  professionalId: string | null
  roomId: string | null
  serviceName: string
  locationName: string
  professionalName: string
  startsAt: string
  endsAt: string
  durationMinutes: number
  status: string
  customerName: string
  maskedPhone: string
}

export type CustomerBookingReschedulePreviewResponse = {
  booking: CustomerBookingItemResponse
  services: Array<{
    id: string
    name: string
    categoryName: string
    durationMinutes: number
    requiresRoom: boolean
  }>
  locations: Array<{
    id: string
    name: string
    address: string | null
    commune: string | null
  }>
}

export type CustomerBookingRescheduleRequest = {
  serviceId: string
  locationId: string
  professionalId?: string
  roomId?: string
  date: string
  startsAt: string
  reason?: string
}

// Calendar Integration
export type CalendarAccountResponse = {
  id: string
  provider: string
  emailMasked: string | null
  calendarId: string | null
  calendarSummary: string | null
  active: boolean
  connectedAt: string | null
  lastSyncAt: string | null
  requiresReconnect: boolean
  revokedAt: string | null
  authorizationStatus: string
}

export type CalendarListEntry = {
  id: string
  summary: string
  primary: boolean
  accessRole: string
}

export type BookingSyncStatusResponse = {
  id: string
  bookingId: string
  provider: string
  externalEventId: string | null
  syncStatus: string
  syncAction: string
  errorMessage: string | null
  retryCount: number
  lastSyncAttemptAt: string | null
  lastSuccessfulSyncAt: string | null
}
