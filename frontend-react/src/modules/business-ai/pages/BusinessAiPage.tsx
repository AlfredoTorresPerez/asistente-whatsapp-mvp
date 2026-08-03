import { useCallback, useMemo, useState } from 'react'
import { useBeforeUnload } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { usePermissions } from '../../../hooks/usePermissions'
import type { AgentIntent } from '../../../services/api/types'
import { useShellSession } from '../../../lib/shellSession'
import { useBusinessAiSettings } from '../hooks/useBusinessAiSettings'
import { useBusinessAiPreview } from '../hooks/useBusinessAiPreview'
import { useBusinessAiMetrics } from '../hooks/useBusinessAiMetrics'
import { useBusinessAiAudit } from '../hooks/useBusinessAiAudit'
import { useBusinessKnowledgeHealth } from '../hooks/useBusinessKnowledgeHealth'
import { useBusinessReadiness } from '../hooks/useBusinessReadiness'
import { BusinessAiMetrics } from '../components/BusinessAiMetrics'
import { BusinessAiOverview } from '../components/BusinessAiOverview'
import { AssistantGeneralSettings } from '../components/AssistantGeneralSettings'
import { AssistantCapabilities } from '../components/AssistantCapabilities'
import { AssistantTestPanel } from '../components/AssistantTestPanel'
import { BusinessInformationSummary } from '../components/BusinessInformationSummary'
import { UnresolvedQueriesPanel } from '../components/UnresolvedQueriesPanel'
import { BusinessAiAdvancedSettings } from '../components/BusinessAiAdvancedSettings'
import { KnowledgeBaseModal } from '../components/KnowledgeBaseModal'

type Section = 'general' | 'test' | 'info' | 'audit' | 'advanced'

function formatAiIntent(intent: string) {
  const labels: Record<string, string> = {
    AVAILABILITY_QUERY: 'Consulta de disponibilidad',
    BOOKING_CANCEL: 'Cancelar cita',
    BOOKING_CHANGE: 'Reprogramar cita',
    BOOKING_REQUEST: 'Crear cita',
    BOOKING_STATUS: 'Estado de cita',
    BUSINESS_HOURS_QUERY: 'Horario de atención',
    COMMERCIAL_AND_BOOKING: 'Consulta comercial y cita',
    COMMERCIAL_INQUIRY: 'Consulta comercial',
    COMPLAINT: 'Reclamo',
    FOLLOW_UP: 'Seguimiento',
    GREETING: 'Saludo',
    HUMAN_REQUEST: 'Solicita atención humana',
    KNOWLEDGE_QUERY: 'Consulta de información',
    LOCATION_QUERY: 'Consulta de sucursal',
    PAYMENT_INQUIRY: 'Consulta de pago',
    PAYMENT_PROBLEM: 'Problema de pago',
    PRICE_REQUEST: 'Consulta de precio',
    PROFESSIONAL_QUERY: 'Consulta de profesional',
    QUOTE_REQUEST: 'Solicitud de cotización',
    SERVICE_INFORMATION: 'Información de servicio',
    SERVICE_RECOMMENDATION: 'Recomendación de servicio',
    SUPPORT_GENERAL: 'Soporte',
    TECHNICAL_MESSAGE: 'Mensaje no comercial',
    THANKS_OR_FAREWELL: 'Cierre de conversación',
    WAITLIST_QUERY: 'Lista de espera',
  }
  return labels[intent] ?? intent.replace(/_/g, ' ').toLowerCase()
}

export function BusinessAiPage() {
  const session = useShellSession()
  const { hasPermission } = usePermissions()
  const userPerms = session.user?.permissions ?? []
  const canManage = hasPermission('BUSINESS_AI_MANAGE')
  const canTest = hasPermission('BUSINESS_AI_TEST')
  const canViewAudit = hasPermission('BUSINESS_AI_AUDIT_VIEW')
  const canViewInfo = hasPermission('BUSINESS_AI_VIEW')

  const settings = useBusinessAiSettings()
  const preview = useBusinessAiPreview(userPerms)
  const audit = useBusinessAiAudit(canViewAudit)
  const knowledge = useBusinessKnowledgeHealth(audit.logs, userPerms)
  const readiness = useBusinessReadiness(userPerms)
  const { metrics } = useBusinessAiMetrics(settings.config.active, audit.logs)

  const hasUnsaved = settings.hasUnsavedSettings || settings.hasUnsavedPrompt
  useBeforeUnload(
    useCallback((e: BeforeUnloadEvent) => {
      if (hasUnsaved) e.preventDefault()
    }, [hasUnsaved]),
  )

  const [activeSection, setActiveSection] = useState<Section>('general')

  const sections = useMemo(() => {
    const items: { label: string; value: Section }[] = [
      { label: 'Configuración general', value: 'general' },
    ]
    if (canTest) items.push({ label: 'Probar asistente', value: 'test' })
    if (canViewInfo) items.push({ label: 'Información del negocio', value: 'info' })
    if (canViewAudit) items.push({ label: 'Consultas por revisar', value: 'audit' })
    if (canManage) items.push({ label: 'Configuración avanzada', value: 'advanced' })
    return items
  }, [canManage, canTest, canViewAudit, canViewInfo])

  if (settings.isLoading) {
    return (
      <section className="mx-auto max-w-[1440px] px-4 py-6">
        <PageHeader title="IA del Negocio" description="Cargando configuración..." />
        <div className="mt-8 flex justify-center text-gray-400">Cargando...</div>
      </section>
    )
  }

  if (settings.settingsQuery.isError) {
    return (
      <section className="mx-auto max-w-[1440px] px-4 py-6">
        <PageHeader title="IA del Negocio" description="Error al cargar la configuración" />
        <Card className="mt-4 border-red-200 bg-red-50 p-4 text-red-700">
          No se pudo cargar la configuración del asistente. Verifica la conexión e intenta nuevamente.
        </Card>
      </section>
    )
  }

  return (
    <section className="mx-auto max-w-[1440px] px-4 py-6">
      <PageHeader
        eyebrow="Asistente inteligente"
        title="IA del Negocio"
        description="Configura el asistente inteligente para la atención de tus clientes por WhatsApp."
      />

      <div className="mt-6">
        <BusinessAiMetrics metrics={metrics} />
      </div>

      <BusinessAiOverview
        active={settings.config.active}
        lastModifiedAt={
          settings.activePrompt?.fechaActualizacion ?? settings.settingsQuery.data?.updatedAt
        }
      />

      <div className="mt-6 flex gap-1 border-b border-gray-200">
        {sections.map((s) => (
          <button
            key={s.value}
            onClick={() => setActiveSection(s.value)}
            className={`px-4 py-2 text-sm font-medium transition-colors ${
              activeSection === s.value
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      <div className="mt-4">
        {activeSection === 'general' && (
          <div className="grid gap-4 lg:grid-cols-2">
            <AssistantGeneralSettings
              active={settings.config.active}
              mode={settings.config.mode}
              tone={settings.config.tone}
              language={settings.config.language}
              escalationThreshold={settings.config.escalationThreshold}
              onActiveChange={(v) => settings.setConfig({ ...settings.config, active: v })}
              onModeChange={(v) => settings.setConfig({ ...settings.config, mode: v })}
              onToneChange={(v) => settings.setConfig({ ...settings.config, tone: v as 'Cercano' | 'Profesional' | 'Comercial' })}
              onLanguageChange={(v) => settings.setConfig({ ...settings.config, language: v })}
              onEscalationThresholdChange={(v) =>
                settings.setConfig({ ...settings.config, escalationThreshold: v })
              }
              onSave={() => settings.saveSettingsMutation.mutate()}
              isSaving={settings.saveSettingsMutation.isPending}
              hasChanges={settings.hasUnsavedSettings}
              lastModifiedAt={settings.settingsQuery.data?.updatedAt}
              updatedBy={settings.settingsQuery.data?.updatedBy}
            />
            <AssistantCapabilities
              allowedTopics={settings.allowedTopics}
              blockedTopics={settings.blockedTopics}
              allowPrices={settings.config.allowPrices}
              allowBooking={settings.config.allowBooking}
              allowPromotions={settings.config.allowPromotions}
              requireAvailabilityCheck={settings.config.requireAvailabilityCheck}
              onAllowedTopicsChange={settings.setAllowedTopics}
              onBlockedTopicsChange={settings.setBlockedTopics}
              onAllowPricesChange={(v) => settings.setConfig({ ...settings.config, allowPrices: v })}
              onAllowBookingChange={(v) => settings.setConfig({ ...settings.config, allowBooking: v })}
              onAllowPromotionsChange={(v) =>
                settings.setConfig({ ...settings.config, allowPromotions: v })
              }
              onRequireAvailabilityCheckChange={(v) =>
                settings.setConfig({ ...settings.config, requireAvailabilityCheck: v })
              }
            />
          </div>
        )}

        {activeSection === 'test' && (
          <AssistantTestPanel
            scenario={preview.scenario}
            onScenarioChange={preview.setScenario}
            onRun={() => preview.runScenario(preview.scenario)}
            isAnalyzing={preview.isAnalyzing}
            routingResult={preview.routingResult}
            previewEditable={preview.previewEditable}
            editableResponse={preview.editableResponse}
            onEditableResponseChange={preview.setEditableResponse}
            onToggleEdit={() => preview.setPreviewEditable(!preview.previewEditable)}
            conversations={preview.conversations}
            conversationSearch={preview.conversationSearch}
            onConversationSearchChange={preview.setConversationSearch}
            selectedConversationId={preview.selectedConversationId}
            onSelectedConversationChange={preview.setSelectedConversationId}
            selectedConversation={preview.selectedConversation}
            onSend={preview.confirmSend}
            isSending={preview.isSending}
            canSend={preview.canSend}
            showSendConfirm={preview.showSendConfirm}
            onShowSendConfirm={preview.setShowSendConfirm}
          />
        )}

        {activeSection === 'info' && (
          <BusinessInformationSummary
            summaryCards={readiness.summaryCards}
            readinessChecks={readiness.readinessChecks}
            passedCount={readiness.passedCount}
            totalChecks={readiness.totalChecks}
            isLoading={readiness.isLoading}
          />
        )}

        {activeSection === 'audit' && (
          <UnresolvedQueriesPanel
            entries={audit.paginatedLogs.map((log) => ({
              id: log.id,
              title: formatAiIntent(log.intent),
              category: formatAiIntent(log.intent),
              status: log.requiresHumanHandoff ? 'pending' : 'resolved',
              updatedAt: log.createdAt,
              description: log.sourceMessage,
              type: 'audit',
              log,
            }))}
            page={audit.auditPage}
            totalPages={audit.totalPages}
            totalLogs={audit.totalLogs}
            onPageChange={audit.setAuditPage}
            onSelectEntry={(entry) => {
              preview.setScenario(entry.title)
              preview.setPreviewResponse({
                result: {
                  businessId: '',
                  conversationId: '',
                  customerId: '',
                  primaryIntent: (entry.category ?? 'HUMAN_REQUEST') as AgentIntent,
                  secondaryIntent: null,
                  agentType: 'HUMAN_HANDOFF',
                  extractedData: {},
                  missingData: [],
                  urgency: 'normal',
                  requiresHuman: entry.log?.requiresHumanHandoff ?? false,
                  handoffReason: entry.log?.handoffReason ?? null,
                  responseToCustomer: entry.description,
                  confidence: entry.log?.confidence ?? 0,
                  summaryForHuman: null,
                },
                status: 'OK',
                message: 'Cargado desde historial',
              })
              setActiveSection('test')
            }}
            isLoading={audit.isLoading}
          />
        )}

        {activeSection === 'advanced' && (
          <BusinessAiAdvancedSettings
            assistantPrompt={settings.assistantPrompt}
            onAssistantPromptChange={settings.setAssistantPrompt}
            activePrompt={settings.activePrompt ?? null}
            prompts={settings.promptsQuery.data ?? []}
            onSavePrompt={() => settings.savePromptMutation.mutate()}
            isSavingPrompt={settings.savePromptMutation.isPending}
            hasChanges={settings.hasUnsavedPrompt}
            routingResult={preview.routingResult}
          />
        )}
      </div>

      <KnowledgeBaseModal
        open={knowledge.showBaseModal}
        onClose={() => knowledge.setShowBaseModal(false)}
        services={knowledge.services}
        rules={knowledge.rules}
      />
    </section>
  )
}
