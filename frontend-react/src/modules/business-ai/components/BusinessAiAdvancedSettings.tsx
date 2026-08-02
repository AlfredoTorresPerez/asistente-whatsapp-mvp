import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import type { AgentRoutingResult, PromptTemplateResponse } from '../../../services/api/types'
import { PromptVersionHistory } from './PromptVersionHistory'

type Props = {
  assistantPrompt: string
  onAssistantPromptChange: (v: string) => void
  activePrompt: PromptTemplateResponse | null
  prompts: PromptTemplateResponse[]
  onSavePrompt: () => void
  isSavingPrompt: boolean
  hasChanges: boolean
  routingResult: AgentRoutingResult | null
}

const AGENT_DISPLAY: Record<string, string> = {
  RECEPTION: 'Recepción',
  SALES: 'Ventas',
  BOOKING: 'Agenda',
  SUPPORT: 'Soporte',
  PAYMENTS: 'Pagos',
  FOLLOW_UP: 'Seguimiento',
  KNOWLEDGE: 'Conocimiento',
  HUMAN_HANDOFF: 'Derivación Humana',
}

function formatIntent(intent: string): string {
  return intent
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

export function BusinessAiAdvancedSettings({
  assistantPrompt,
  onAssistantPromptChange,
  activePrompt,
  prompts,
  onSavePrompt,
  isSavingPrompt,
  hasChanges,
  routingResult,
}: Props) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <Card className="p-4">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex w-full items-center justify-between text-left"
      >
        <div>
          <h2 className="text-lg font-semibold">Configuración avanzada</h2>
          <p className="mt-1 text-xs text-gray-500">
            Instrucciones internas, versión del motor y parámetros técnicos.
          </p>
        </div>
        <span className="text-xl text-gray-400 transition-transform" style={{ transform: isOpen ? 'rotate(180deg)' : 'none' }}>
          ▾
        </span>
      </button>

      {isOpen && (
        <div className="mt-4 space-y-4">
          {routingResult && (
            <div className="space-y-3 rounded-lg border border-gray-200 bg-gray-50 p-3">
              <h3 className="text-sm font-semibold text-gray-700">Traza del ruteo</h3>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div>
                  <span className="font-medium text-gray-600">Agente seleccionado:</span>{' '}
                  <span className="text-gray-800">{AGENT_DISPLAY[routingResult.agentType] ?? routingResult.agentType}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-600">Intención primaria:</span>{' '}
                  <span className="text-gray-800">{formatIntent(routingResult.primaryIntent)}</span>
                </div>
                {routingResult.secondaryIntent && (
                  <div>
                    <span className="font-medium text-gray-600">Intención secundaria:</span>{' '}
                    <span className="text-gray-800">{formatIntent(routingResult.secondaryIntent)}</span>
                  </div>
                )}
                <div>
                  <span className="font-medium text-gray-600">Confianza:</span>{' '}
                  <span className="text-gray-800">{Math.round(routingResult.confidence * 100)}%</span>
                </div>
                <div>
                  <span className="font-medium text-gray-600">Urgencia:</span>{' '}
                  <span className="text-gray-800">{routingResult.urgency}</span>
                </div>
                <div>
                  <span className="font-medium text-gray-600">Derivación humana:</span>{' '}
                  <StatusBadge label={routingResult.requiresHuman ? 'Activo' : 'Inactivo'} tone={routingResult.requiresHuman ? 'success' : 'neutral'} />
                </div>
              </div>

              {routingResult.handoffReason && (
                <div className="text-xs">
                  <span className="font-medium text-gray-600">Motivo de derivación:</span>{' '}
                  <span className="text-gray-800">{routingResult.handoffReason}</span>
                </div>
              )}

              {Object.keys(routingResult.extractedData).length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-gray-600">Datos extraídos:</p>
                  <div className="max-h-40 space-y-0.5 overflow-y-auto rounded bg-white p-2 text-xs font-mono">
                    {Object.entries(routingResult.extractedData).map(([key, value]) => (
                      <div key={key} className="flex gap-2">
                        <span className="shrink-0 text-gray-500">{key}:</span>
                        <span className="text-gray-800 break-all">{value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {routingResult.missingData.length > 0 && (
                <div className="text-xs">
                  <span className="font-medium text-gray-600">Datos faltantes:</span>{' '}
                  <span className="text-gray-800">{routingResult.missingData.map(formatIntent).join(', ')}</span>
                </div>
              )}
            </div>
          )}

          <div>
            <label className="text-sm font-medium">Instrucciones internas del asistente</label>
            <p className="text-xs text-gray-500">
              Define el comportamiento, reglas y límites del asistente. Es el prompt base que el motor de IA utiliza para responder.
            </p>
            <Textarea
              value={assistantPrompt}
              onChange={(e) => onAssistantPromptChange(e.target.value)}
              rows={12}
              className="mt-1 w-full font-mono text-xs"
            />
          </div>

          <div className="flex items-center justify-between text-xs text-gray-400">
            <span>
              Versión activa: {activePrompt ? `v${activePrompt.version}` : 'Sin versión activa'}
              {activePrompt?.nombre ? ` - ${activePrompt.nombre}` : ''}
            </span>
            {activePrompt?.updatedAt && (
              <span>Última modificación: {activePrompt.updatedAt}</span>
            )}
          </div>

          {hasChanges && (
            <Button onClick={onSavePrompt} loading={isSavingPrompt} className="w-full">
              Guardar instrucciones
            </Button>
          )}

          <PromptVersionHistory prompts={prompts} activeVersion={activePrompt?.version ?? null} />
        </div>
      )}
    </Card>
  )
}
