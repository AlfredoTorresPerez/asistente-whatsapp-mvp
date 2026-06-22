import type { RouteObject } from 'react-router-dom'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import type { ReactNode } from 'react'
import {
  AdminLocationsPage,
  AdminSecurityPage,
  AdminUserFormPage,
  AdminUsersPage,
  AdministrationPage,
  CompanySettingsPage,
  WhatsAppWebConnectionPage,
} from '../modules/administration'
import {
  AppointmentDetailPage,
  AppointmentsPage,
  BookingCancellationPage,
  BookingConfirmationPage,
  BookingReschedulePage,
  EditAppointmentPage,
  NewAppointmentFromConversationPage,
  NewAppointmentFromProspectPage,
  NewAppointmentPage,
  RescheduleAppointmentPage,
} from '../modules/bookings'
import { DashboardPage } from '../modules/dashboard/pages/DashboardPage'
import { ForgotPasswordPage } from '../modules/auth/pages/ForgotPasswordPage'
import { ForgotPasswordSentPage } from '../modules/auth/pages/ForgotPasswordSentPage'
import { LoginPage } from '../modules/auth/pages/LoginPage'
import {
  ConversationsPage,
  NewConversationPage,
  NewTemplatePage,
  TemplatesPage,
} from '../modules/conversations'
import {
  EditLeadPage,
  LeadDetailPage,
  NewLeadFromConversationPage,
  NewLeadPage,
  ProspectsPage,
} from '../modules/leads'
import { NotificationsPage } from '../modules/notifications/pages/NotificationsPage'
import { ResetPasswordPage } from '../modules/auth/pages/ResetPasswordPage'
import { ChangePasswordPage } from '../modules/security/pages/ChangePasswordPage'
import { ProfilePage } from '../modules/security/pages/ProfilePage'
import { CatalogFormPage, CatalogPage } from '../modules/catalog'
import { BusinessAiPage } from '../modules/business-ai'
import { CompleteAgendaPage } from '../modules/agenda'
import { MultisiteOperationsPage } from '../modules/multisite'
import { ConfigurationPage } from '../modules/configuration'
import { AutomationRuleFormPage, AutomationRuleRunPage, AutomationRulesPage } from '../modules/rules'
import { ReportsPage } from '../modules/visual-contract'
import {
  ConversationOrderCreatePage,
  OrderCreatePage,
  OrderDetailPage,
  OrderPaymentPage,
  OrdersPage,
  ProspectOrderCreatePage,
} from '../modules/orders'
import { PrivateRouteShell, PublicRouteShell } from './RouteShells'
import { useShellSession } from '../lib/shellSession'

export const appRoutes: RouteObject[] = [
  { path: '/reservas/confirmar/:token', element: <BookingConfirmationPage /> },
  { path: '/reservas/reprogramar/:token', element: <BookingReschedulePage /> },
  { path: '/reservas/cancelar/:token', element: <BookingCancellationPage /> },
  {
    path: '/',
    element: <PublicRouteShell />,
    children: [
      { index: true, element: <Navigate replace to="/login" /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'forgot-password', element: <ForgotPasswordPage /> },
      { path: 'forgot-password/sent', element: <ForgotPasswordSentPage /> },
      { path: 'reset-password', element: <ResetPasswordPage /> },
    ],
  },
  {
    path: '/',
    element: <PrivateRouteShell />,
    children: [
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'notifications', element: <NotificationsPage /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'profile/change-password', element: <ChangePasswordPage /> },
      { path: 'conversations', element: <ConversationsPage /> },
      { path: 'conversations/new', element: <NewConversationPage /> },
      { path: 'conversations/:conversationId', element: <ConversationsPage /> },
      { path: 'conversations/:conversationId/prospects/new', element: <NewLeadFromConversationPage /> },
      { path: 'conversations/:conversationId/orders/new', element: <ConversationOrderCreatePage /> },
      { path: 'conversations/:conversationId/appointments/new', element: <NewAppointmentFromConversationPage /> },
      { path: 'templates', element: <TemplatesPage /> },
      { path: 'templates/new', element: <NewTemplatePage /> },
      { path: 'prospects', element: <ProspectsPage /> },
      { path: 'prospects/new', element: <NewLeadPage /> },
      { path: 'prospects/:prospectId', element: <LeadDetailPage /> },
      { path: 'prospects/:prospectId/edit', element: <EditLeadPage /> },
      { path: 'agenda', element: <CompleteAgendaPage /> },
      { path: 'appointments', element: <AppointmentsPage /> },
      { path: 'appointments/new', element: <NewAppointmentPage /> },
      { path: 'appointments/:appointmentId', element: <AppointmentDetailPage /> },
      { path: 'appointments/:appointmentId/edit', element: <EditAppointmentPage /> },
      { path: 'appointments/:appointmentId/reschedule', element: <RescheduleAppointmentPage /> },
      { path: 'prospects/:prospectId/appointments/new', element: <NewAppointmentFromProspectPage /> },
      { path: 'prospects/:prospectId/orders/new', element: <ProspectOrderCreatePage /> },
      { path: 'orders', element: <OrdersPage /> },
      { path: 'orders/new', element: <OrderCreatePage /> },
      { path: 'orders/:orderId', element: <OrderDetailPage /> },
      { path: 'orders/:orderId/payments/new', element: <OrderPaymentPage /> },
      { path: 'catalog', element: <CatalogPage /> },
      { path: 'catalog/products/new', element: <CatalogFormPage /> },
      { path: 'catalog/services/new', element: <CatalogFormPage /> },
      { path: 'catalog/products/:productId/edit', element: <CatalogFormPage /> },
      { path: 'catalog/services/:serviceId/edit', element: <CatalogFormPage /> },
      { path: 'automation-rules', element: <AutomationRulesPage /> },
      { path: 'automation-rules/new', element: <AutomationRuleFormPage /> },
      { path: 'automation-rules/:ruleId/edit', element: <AutomationRuleFormPage /> },
      { path: 'automation-rules/:ruleId/test', element: <AutomationRuleRunPage /> },
      { path: 'business-ai', element: <BusinessAiPage /> },
      { path: 'reports', element: <ReportsPage /> },
      { path: 'configuration', element: <RequireRole allowedRoles={['OWNER', 'ADMIN', 'SUPERVISOR']}><ConfigurationPage /></RequireRole> },
      { path: 'admin', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><AdministrationPage /></RequireRole> },
      { path: 'admin/company', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><CompanySettingsPage /></RequireRole> },
      { path: 'admin/locations', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><AdminLocationsPage /></RequireRole> },
      { path: 'admin/multisite', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><MultisiteOperationsPage /></RequireRole> },
      { path: 'admin/users', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><AdminUsersPage /></RequireRole> },
      { path: 'admin/users/new', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><AdminUserFormPage /></RequireRole> },
      { path: 'admin/users/:userId/edit', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><AdminUserFormPage /></RequireRole> },
      { path: 'admin/whatsapp-web', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><WhatsAppWebConnectionPage /></RequireRole> },
      { path: 'admin/security', element: <RequireRole allowedRoles={['OWNER', 'ADMIN']}><AdminSecurityPage /></RequireRole> },
    ],
  },
  { path: '*', element: <Navigate replace to="/login" /> },
]

export const router = createBrowserRouter(appRoutes)

// eslint-disable-next-line react-refresh/only-export-components
function RequireRole({
  allowedRoles,
  children,
}: {
  allowedRoles: string[]
  children: ReactNode
}) {
  const { user } = useShellSession()

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate replace to="/dashboard" />
  }

  return children
}
