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
  WhatsAppSimulatorPage,
  WhatsAppWebConnectionPage,
} from '../modules/administration'
import {
  AppointmentDetailPage,
  AppointmentsPage,
  BookingCancellationPage,
  BookingConfirmationPage,
  BookingReschedulePage,
  CreatePublicBookingPage,
  CustomerBookingsPage,
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
import {
  AutomationRuleFormPage,
  AutomationRuleRunPage,
  AutomationRulesPage,
} from '../modules/rules'
import { ReportsPage } from '../modules/reports'
import { LandingPage } from '../modules/landing/pages/LandingPage'
import { CenterPublicPage } from '../modules/centros/pages/CenterPublicPage'
import { CenterWhatsAppRedirect } from '../modules/centros/pages/CenterWhatsAppRedirect'

import { PrivateRouteShell, PublicRouteShell } from './RouteShells'
import { useShellSession } from '../lib/shellSession'

export const appRoutes: RouteObject[] = [
  { path: '/reservas/confirmar/:token', element: <BookingConfirmationPage /> },
  { path: '/reservas/reprogramar/:token', element: <BookingReschedulePage /> },
  { path: '/reservas/cancelar/:token', element: <BookingCancellationPage /> },

  { path: '/reservar', element: <CreatePublicBookingPage /> },
  { path: '/reservas/mis-reservas/:token', element: <CustomerBookingsPage /> },

  { path: '/centros/:slug', element: <CenterPublicPage /> },
  { path: '/centros/:slug/whatsapp', element: <CenterWhatsAppRedirect /> },

  { path: '/', element: <LandingPage /> },

  {
    element: <PublicRouteShell />,
    children: [
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
      {
        path: 'conversations',
        element: (
          <RequirePermission permission="CONVERSATIONS_VIEW">
            <ConversationsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'conversations/new',
        element: (
          <RequirePermission permission="CONVERSATIONS_REPLY">
            <NewConversationPage />
          </RequirePermission>
        ),
      },
      {
        path: 'conversations/:conversationId',
        element: (
          <RequirePermission permission="CONVERSATIONS_VIEW">
            <ConversationsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'conversations/:conversationId/prospects/new',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <NewLeadFromConversationPage />
          </RequirePermission>
        ),
      },

      {
        path: 'conversations/:conversationId/appointments/new',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <NewAppointmentFromConversationPage />
          </RequirePermission>
        ),
      },
      {
        path: 'templates',
        element: (
          <RequirePermission permission="TEMPLATE_MANAGE">
            <TemplatesPage />
          </RequirePermission>
        ),
      },
      {
        path: 'templates/new',
        element: (
          <RequirePermission permission="TEMPLATE_MANAGE">
            <NewTemplatePage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <ProspectsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/new',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <NewLeadPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/:prospectId',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <LeadDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/:prospectId/edit',
        element: (
          <RequirePermission permission="LEAD_MANAGE">
            <EditLeadPage />
          </RequirePermission>
        ),
      },
      {
        path: 'agenda',
        element: (
          <RequirePermission permission="AGENDA_VIEW">
            <CompleteAgendaPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <AppointmentsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/new',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <NewAppointmentPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/:appointmentId',
        element: (
          <RequirePermission permission="BOOKINGS_UPDATE">
            <AppointmentDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/:appointmentId/edit',
        element: (
          <RequirePermission permission="BOOKINGS_UPDATE">
            <EditAppointmentPage />
          </RequirePermission>
        ),
      },
      {
        path: 'appointments/:appointmentId/reschedule',
        element: (
          <RequirePermission permission="BOOKINGS_RESCHEDULE">
            <RescheduleAppointmentPage />
          </RequirePermission>
        ),
      },
      {
        path: 'prospects/:prospectId/appointments/new',
        element: (
          <RequirePermission permission="BOOKINGS_CREATE">
            <NewAppointmentFromProspectPage />
          </RequirePermission>
        ),
      },

      {
        path: 'catalog',
        element: (
          <RequirePermission permission="CATALOG_VIEW">
            <CatalogPage />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/products/new',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <CatalogFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/services/new',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <CatalogFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/products/:productId/edit',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <CatalogFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'catalog/services/:serviceId/edit',
        element: (
          <RequirePermission permission="CATALOG_MANAGE">
            <CatalogFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRulesPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules/new',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRuleFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules/:ruleId/edit',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRuleFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'automation-rules/:ruleId/test',
        element: (
          <RequirePermission permission="AUTOMATION_MANAGE">
            <AutomationRuleRunPage />
          </RequirePermission>
        ),
      },
      {
        path: 'business-ai',
        element: (
          <RequirePermission permission="REPORTS_VIEW">
            <BusinessAiPage />
          </RequirePermission>
        ),
      },
      {
        path: 'reports',
        element: (
          <RequirePermission permission="REPORTS_VIEW">
            <ReportsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'configuration',
        element: (
          <RequireAnyPermission permissions={['WHATSAPP_CONFIG_VIEW', 'CALENDAR_CONFIG_VIEW']}>
            <ConfigurationPage />
          </RequireAnyPermission>
        ),
      },
      {
        path: 'admin',
        element: (
          <RequirePermission permission="ADMIN_MANAGE">
            <AdministrationPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/company',
        element: (
          <RequirePermission permission="LOCATIONS_MANAGE">
            <CompanySettingsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/locations',
        element: (
          <RequirePermission permission="LOCATIONS_MANAGE">
            <AdminLocationsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/multisite',
        element: (
          <RequirePermission permission="ADMIN_MANAGE">
            <MultisiteOperationsPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/users',
        element: (
          <RequirePermission permission="USERS_MANAGE">
            <AdminUsersPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/users/new',
        element: (
          <RequirePermission permission="USERS_MANAGE">
            <AdminUserFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/users/:userId/edit',
        element: (
          <RequirePermission permission="USERS_MANAGE">
            <AdminUserFormPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/whatsapp-web',
        element: (
          <RequirePermission permission="WHATSAPP_CONFIG_MANAGE">
            <WhatsAppWebConnectionPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/whatsapp-simulator',
        element: (
          <RequirePermission permission="WHATSAPP_CONFIG_MANAGE">
            <WhatsAppSimulatorPage />
          </RequirePermission>
        ),
      },
      {
        path: 'admin/security',
        element: (
          <RequirePermission permission="SECURITY_AUDIT_VIEW">
            <AdminSecurityPage />
          </RequirePermission>
        ),
      },
    ],
  },
  { path: '*', element: <Navigate replace to="/login" /> },
]

export const router = createBrowserRouter(appRoutes)

// Componente para verificar permisos granulares
// eslint-disable-next-line react-refresh/only-export-components
function RequirePermission({ permission, children }: { permission: string; children: ReactNode }) {
  const { user } = useShellSession()

  if (!user || !user.permissions?.includes(permission)) {
    return <Navigate replace to="/dashboard" />
  }

  return children
}

// Componente para verificar que el usuario tenga al menos uno de los permisos
// eslint-disable-next-line react-refresh/only-export-components
function RequireAnyPermission({
  permissions,
  children,
}: {
  permissions: string[]
  children: ReactNode
}) {
  const { user } = useShellSession()

  if (!user || !permissions.some((p) => user.permissions?.includes(p))) {
    return <Navigate replace to="/dashboard" />
  }

  return children
}
