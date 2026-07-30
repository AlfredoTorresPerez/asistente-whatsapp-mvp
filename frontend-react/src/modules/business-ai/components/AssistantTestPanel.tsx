import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import type { IntentAnalysisResponse } from '../../../services/api/types'

type Props = {
  scenario: string
  onScenarioChange: (v: string) => void
  onRun: () => void
  isAnalyzing: boolean
  analysisResult: IntentAnalysisResponse | null
  previewEditable: boolean
  previewResponse: string
  onPreviewResponseChange: (v: string) => void
  onToggleEdit: () => void
  conversations: { id: string; customerName?: string; lastMessage?: string }[]
  conversationSearch: string
  onConversationSearchChange: (v: string) => void
  selectedConversationId: string
  onSelectedConversationChange: (v: string) => void
  onApprove: () => void
  isSending: boolean
}

function ChatBubble({ align, children }: { align: 'left' | 'right'; children: string }) {
  return (
    <div className={`flex ${align === 'right' ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] rounded-lg px-3 py-2 text-sm whitespace-pre-wrap ${
          align === 'right'
            ? 'bg-green-500 text-white'
            : 'bg-gray-100 text-gray-800'
        }`}
      >
        {children}
      </div>
    </div>
  )
}

export function AssistantTestPanel({
  scenario,
  onScenarioChange,
  onRun,
  isAnalyzing,
  analysisResult,
  previewEditable,
  previewResponse,
  onPreviewResponseChange,
  onToggleEdit,
  conversations,
  conversationSearch,
  onConversationSearchChange,
  selectedConversationId,
  onSelectedConversationChange,
  onApprove,
  isSending,
}: Props) {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="p-4">
        <h2 className="text-lg font-semibold">Probar asistente</h2>
        <p className="mt-1 text-xs text-gray-500">
          Escribe un mensaje simulado para ver cómo respondería el asistente.
        </p>

        <div className="mt-3 space-y-3">
          <div>
            <label className="text-sm font-medium">Mensaje de prueba</label>
            <Textarea
              value={scenario}
              onChange={(e) => onScenarioChange(e.target.value)}
              rows={4}
              className="mt-1 w-full"
              placeholder="Escribe el mensaje del cliente..."
            />
          </div>

          <Button onClick={onRun} loading={isAnalyzing} className="w-full">
            Probar
          </Button>

          {analysisResult && (
            <div>
              <div className="mb-2 flex items-center gap-2">
                <StatusBadge
                  status={
                    analysisResult.confianza >= 0.7
                      ? 'active'
                      : analysisResult.confianza >= 0.3
                        ? 'warning'
                        : 'inactive'
                  }
                />
                <span className="text-sm text-gray-600">
                  Seguridad estimada: {Math.round(analysisResult.confianza * 100)}%
                </span>
              </div>
              <p className="mb-1 text-xs text-gray-500">
                Motivo de la consulta: {analysisResult.intencion}
              </p>
            </div>
          )}
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="text-lg font-semibold">Vista previa de conversación</h2>
        <p className="mt-1 text-xs text-gray-500">
          Revisa y edita la respuesta antes de enviarla a un cliente real.
        </p>

        <div className="mt-3 space-y-3">
          <div>
            <label className="text-sm font-medium">Buscar conversación</label>
            <input
              type="text"
              value={conversationSearch}
              onChange={(e) => onConversationSearchChange(e.target.value)}
              className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              placeholder="Buscar por nombre o teléfono..."
            />
          </div>

          {conversations.length > 0 && (
            <select
              value={selectedConversationId}
              onChange={(e) => onSelectedConversationChange(e.target.value)}
              className="w-full rounded border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">Seleccionar conversación</option>
              {conversations.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.customerName ?? 'Sin nombre'} - {c.lastMessage?.slice(0, 40) ?? ''}
                </option>
              ))}
            </select>
          )}

          <div className="space-y-2 rounded-lg border border-gray-200 bg-gray-50 p-3">
            <ChatBubble align="left">{scenario || 'Mensaje del cliente...'}</ChatBubble>

            {previewEditable ? (
              <Textarea
                value={previewResponse}
                onChange={(e) => onPreviewResponseChange(e.target.value)}
                rows={4}
                className="w-full"
              />
            ) : (
              <ChatBubble align="right">
                {previewResponse || 'La respuesta del asistente aparecerá aquí...'}
              </ChatBubble>
            )}
          </div>

          <div className="flex gap-2">
            {analysisResult && (
              <>
                <Button variant="secondary" onClick={onToggleEdit} size="sm">
                  {previewEditable ? 'Terminar edición' : 'Editar'}
                </Button>
                <Button
                  onClick={onApprove}
                  loading={isSending}
                  size="sm"
                  disabled={!selectedConversationId}
                >
                  Aprobar y enviar
                </Button>
              </>
            )}
          </div>
        </div>
      </Card>
    </div>
  )
}
