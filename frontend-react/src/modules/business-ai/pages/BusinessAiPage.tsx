import { useCallback, useMemo, useState } from 'react'
import { useBeforeUnload } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { useBusinessAiSettings } from '../hooks/useBusinessAiSettings'
import { useBusinessAiPreview } from '../hooks/useBusinessAiPreview'
import { useBusinessAiMetrics } from '../hooks/useBusinessAiMetrics'
import { useBusinessAiAudit } from '../hooks/useBusinessAiAudit'
import { useBusinessKnowledgeHealth } from '../hooks/useBusinessKnowledgeHealth'
import { BusinessAiMetrics } from '../components/BusinessAiMetrics'
import { BusinessAiOverview } from '../components/BusinessAiOverview'
import { AssistantGeneralSettings } from '../components/AssistantGeneralSettings'
import { AssistantCapabilities } from '../components/AssistantCapabilities'
import { AssistantTestPanel } from '../components/AssistantTestPanel'
import { BusinessInformationSummary } from '../components/BusinessInformationSummary'
import { UnresolvedQueriesPanel } from '../components/UnresolvedQueriesPanel'
import { BusinessAiAdvancedSettings } from '../components/BusinessAiAdvancedSettings'
import { ContentEditorModal } from '../components/ContentEditorModal'
import { KnowledgeBaseModal } from '../components/KnowledgeBaseModal'

type Section = 'general' | 'test' | 'info' | 'audit' | 'advanced'

export function BusinessAiPage() {
  const settings = useBusinessAiSettings()
  const preview = useBusinessAiPreview()
  const audit = useBusinessAiAudit()
  const knowledge = useBusinessKnowledgeHealth(audit.logs)
  const { metrics } = useBusinessAiMetrics(settings.config.active, audit.logs)

  const hasUnsaved = settings.hasUnsavedSettings || settings.hasUnsavedPrompt
  useBeforeUnload(
    useCallback((e: BeforeUnloadEvent) => {
      if (hasUnsaved) e.preventDefault()
    }, [hasUnsaved]),
  )

  const [activeSection, setActiveSection] = useState<Section>('general')
  const [rowToToggle, setRowToToggle] = useState<{ row: any; active: boolean } | null>(null)

  const handleEditRow = useCallback((row: any) => {
    if (row.type === 'audit') {
      preview.setPreviewResponse(row.description)
      preview.setAnalysisResult({
        intencion: row.category,
        confianza: row.log?.confidence ?? 0,
        respuestaSugerida: row.description,
        mensajeUsuario: row.title,
      } as any)
      setActiveSection('test')
      return
    }
    knowledge.setEditor({
      open: true,
      mode: 'edit',
      type: row.type,
      id: row.id,
      title: row.title,
      description: row.description,
      categoryCode: row.category,
      ruleType: row.ruleType ?? '',
      price: row.service?.priceBase?.toString() ?? row.product?.price?.toString() ?? '',
      durationMinutes: row.service?.durationMinutes?.toString() ?? '',
      stock: row.product?.stock?.toString() ?? '',
      priority: row.rule?.priority?.toString() ?? '50',
      active: row.status === 'active',
      source: row,
    })
  }, [preview, knowledge])

  const sections = useMemo(() => [
    { label: 'Configuración general', value: 'general' as const },
    { label: 'Probar asistente', value: 'test' as const },
    { label: 'Información del negocio', value: 'info' as const },
    { label: 'Consultas por revisar', value: 'audit' as const },
    { label: 'Configuración avanzada', value: 'advanced' as const },
  ], [])

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
        title="IA del Negocio"
        description="Configura el asistente inteligente para la atención de tus clientes por WhatsApp."
      />

      <div className="mt-6">
        <BusinessAiMetrics metrics={metrics} />
      </div>

      <BusinessAiOverview
        active={settings.config.active}
        lastModifiedAt={
          settings.activePrompt?.updatedAt ?? settings.settingsQuery.data?.updatedAt
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
              onToneChange={(v) => settings.setConfig({ ...settings.config, tone: v })}
              onLanguageChange={(v) => settings.setConfig({ ...settings.config, language: v })}
              onEscalationThresholdChange={(v) =>
                settings.setConfig({ ...settings.config, escalationThreshold: v })
              }
              onSave={() => settings.saveSettingsMutation.mutate()}
              isSaving={settings.saveSettingsMutation.isPending}
              hasChanges={settings.hasUnsavedSettings}
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
            analysisResult={preview.analysisResult}
            previewEditable={preview.previewEditable}
            previewResponse={preview.previewResponse}
            onPreviewResponseChange={preview.setPreviewResponse}
            onToggleEdit={() => preview.setPreviewEditable(!preview.previewEditable)}
            conversations={preview.conversations}
            conversationSearch={preview.conversationSearch}
            onConversationSearchChange={preview.setConversationSearch}
            selectedConversationId={preview.selectedConversationId}
            onSelectedConversationChange={preview.setSelectedConversationId}
            onApprove={preview.approveAndSend}
            isSending={preview.isSending}
          />
        )}

        {activeSection === 'info' && (
          <BusinessInformationSummary
            activeTab={knowledge.activeTab}
            onTabChange={(tab) => {
              knowledge.setActiveTab(tab as any)
              knowledge.setKnowledgePage(0)
            }}
            tabs={knowledge.tabs}
            rows={knowledge.rows}
            paginatedRows={knowledge.paginatedRows}
            page={knowledge.knowledgePage}
            totalPages={knowledge.totalPages}
            onPageChange={knowledge.setKnowledgePage}
            search={knowledge.search}
            onSearchChange={knowledge.setSearch}
            statusFilter={knowledge.statusFilter}
            onStatusFilterChange={knowledge.setStatusFilter}
            onAdd={() => {
              const type =
                knowledge.activeTab === 'products' ? 'product'
                  : knowledge.activeTab === 'rules' || knowledge.activeTab === 'policies' ? 'rule'
                    : 'service'
              knowledge.setEditor({
                open: true, mode: 'create', type, title: '', description: '',
                categoryCode: '', ruleType: '', price: '', durationMinutes: '',
                stock: '', priority: '50', active: true,
              })
            }}
            onEdit={handleEditRow}
            onToggleStatus={(row) => setRowToToggle({ row, active: row.status !== 'active' })}
            isLoading={knowledge.isKnowledgeLoading}
            onOpenFullBase={() => knowledge.setShowBaseModal(true)}
          />
        )}

        {activeSection === 'audit' && (
          <UnresolvedQueriesPanel
            entries={audit.paginatedLogs.map((log) => ({
              id: log.id,
              title: log.title ?? log.description,
              category: log.category,
              status: log.status,
              updatedAt: log.updatedAt,
              description: log.description,
              type: 'audit',
              log,
            }))}
            page={audit.auditPage}
            totalPages={audit.totalPages}
            totalLogs={audit.totalLogs}
            onPageChange={audit.setAuditPage}
            onSelectEntry={(entry) => {
              preview.setPreviewResponse(entry.description)
              preview.setAnalysisResult({
                intencion: entry.category,
                confianza: (entry as any).log?.confidence ?? 0,
                respuestaSugerida: entry.description,
                mensajeUsuario: entry.title,
              } as any)
              setActiveSection('test')
            }}
            isLoading={audit.isLoading}
          />
        )}

        {activeSection === 'advanced' && (
          <BusinessAiAdvancedSettings
            assistantPrompt={settings.assistantPrompt}
            onAssistantPromptChange={settings.setAssistantPrompt}
            activePrompt={settings.activePrompt}
            prompts={settings.promptsQuery.data ?? []}
            onSavePrompt={() => settings.savePromptMutation.mutate()}
            isSavingPrompt={settings.savePromptMutation.isPending}
            hasChanges={settings.hasUnsavedPrompt}
          />
        )}
      </div>

      <ContentEditorModal
        editor={knowledge.editor}
        onClose={() => knowledge.setEditor({ ...knowledge.editor, open: false })}
        onChange={knowledge.setEditor}
        onSave={() => knowledge.saveEditorMutation.mutate(knowledge.editor)}
        isSaving={knowledge.saveEditorMutation.isPending}
        serviceCategories={knowledge.serviceCategories}
        productCategories={knowledge.productCategories}
      />

      <KnowledgeBaseModal
        open={knowledge.showBaseModal}
        onClose={() => knowledge.setShowBaseModal(false)}
        services={knowledge.services}
        products={knowledge.products}
        rules={knowledge.rules}
      />
    </section>
  )
}
