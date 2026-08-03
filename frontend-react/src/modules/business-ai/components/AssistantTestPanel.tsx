import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import type { AgentRoutingResult } from '../../../services/api/types'

type Props = {
  scenario: string
  onScenarioChange: (v: string) => void
  onRun: () => void
  isAnalyzing: boolean
  routingResult: AgentRoutingResult | null
  previewEditable: boolean
  editableResponse: string
  onEditableResponseChange: (v: string) => void
  onToggleEdit: () => void
  conversations: { id: string; customerName?: string; customerPhone?: string; lastMessage?: string }[]
  conversationSearch: string
  onConversationSearchChange: (v: string) => void
  selectedConversationId: string
  onSelectedConversationChange: (v: string) => void
  selectedConversation: { customerName?: string; customerPhone?: string } | null
  onSend: () => void
  isSending: boolean
  canSend: boolean
  showSendConfirm: boolean
  onShowSendConfirm: (v: boolean) => void
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

function formatIntent(intent: string): string {
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

function maskPhone(value?: string) {
  if (!value) return ''
  const digits = value.replace(/\D/g, '')
  if (digits.length <= 4) return '****'
  return `***${digits.slice(-4)}`
}

function extractedValue(data: Record<string, string>, keys: string[]) {
  for (const key of keys) {
    const value = data[key]
    if (value) return value
  }
  return 'No detectado'
}

export function AssistantTestPanel({
  scenario,
  onScenarioChange,
  onRun,
  isAnalyzing,
  routingResult,
  previewEditable,
  editableResponse,
  onEditableResponseChange,
  onToggleEdit,
  conversations,
  conversationSearch,
  onConversationSearchChange,
  selectedConversationId,
  onSelectedConversationChange,
  selectedConversation,
  onSend,
  isSending,
  canSend,
  showSendConfirm,
  onShowSendConfirm,
}: Props) {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="p-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Probar asistente</h2>
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wider text-amber-700 ring-1 ring-amber-200">
            Modo de prueba
          </span>
        </div>
        <p className="mt-1 text-xs text-gray-500">
          No se enviarán mensajes ni se modificarán reservas.
        </p>

        <div className="mt-3 space-y-3">
          <div>
            <label className="text-sm font-medium">Mensaje del cliente</label>
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

          {routingResult && (
            <>
              <div className="space-y-2 rounded-lg border border-gray-200 bg-gray-50 p-3">
                <ChatBubble align="left">{scenario}</ChatBubble>
                {previewEditable ? (
                  <Textarea
                    value={editableResponse}
                    onChange={(e) => onEditableResponseChange(e.target.value)}
                    rows={4}
                    className="w-full"
                  />
                ) : (
                  <ChatBubble align="right">
                    {routingResult.responseToCustomer || 'Sin respuesta'}
                  </ChatBubble>
                )}
              </div>

              <div className="space-y-2 rounded-lg border border-gray-200 p-3">
                <div className="grid gap-2 text-sm text-gray-700 sm:grid-cols-2">
                  <InfoItem label="Mensaje recibido" value={scenario || 'Sin mensaje'} />
                  <InfoItem label="Intención detectada" value={formatIntent(routingResult.primaryIntent)} />
                  <InfoItem
                    label="Servicio"
                    value={extractedValue(routingResult.extractedData, ['servicio', 'service', 'serviceName'])}
                  />
                  <InfoItem
                    label="Sucursal"
                    value={extractedValue(routingResult.extractedData, ['sucursal', 'location', 'locationName'])}
                  />
                  <InfoItem
                    label="Fecha"
                    value={extractedValue(routingResult.extractedData, ['fecha', 'date', 'bookingDate'])}
                  />
                  <InfoItem
                    label="Hora"
                    value={extractedValue(routingResult.extractedData, ['hora', 'time', 'startTime'])}
                  />
                  <InfoItem
                    label="Profesional"
                    value={extractedValue(routingResult.extractedData, ['profesional', 'professional', 'professionalName'])}
                  />
                  <InfoItem
                    label="Confianza"
                    value={`${Math.round(routingResult.confidence * 100)}%`}
                  />
                  <InfoItem
                    label="Reglas aplicadas"
                    value={extractedValue(routingResult.extractedData, ['regla', 'rule', 'rulesApplied'])}
                  />
                  <InfoItem
                    label="Disponibilidad consultada"
                    value={
                      ['AVAILABILITY_QUERY', 'BOOKING_REQUEST', 'BOOKING_CHANGE'].includes(
                        routingResult.primaryIntent,
                      )
                        ? 'Considerada por el flujo de agenda'
                        : 'No requerida'
                    }
                  />
                </div>

                <div className="flex items-center gap-2">
                  <StatusBadge
                    label={
                      routingResult.confidence >= 0.7
                        ? 'Activo'
                        : routingResult.confidence >= 0.3
                          ? 'Atención'
                          : 'Inactivo'
                    }
                    tone={
                      routingResult.confidence >= 0.7
                        ? 'success'
                        : routingResult.confidence >= 0.3
                          ? 'warning'
                          : 'neutral'
                    }
                  />
                  <span className="text-sm text-gray-600">
                    Seguridad: {Math.round(routingResult.confidence * 100)}%
                  </span>
                </div>

                <p className="text-sm text-gray-700">
                  <span className="font-medium">Motivo de consulta:</span>{' '}
                  {formatIntent(routingResult.primaryIntent)}
                </p>

                {routingResult.secondaryIntent && (
                  <p className="text-sm text-gray-700">
                    <span className="font-medium">Secundario:</span>{' '}
                    {formatIntent(routingResult.secondaryIntent)}
                  </p>
                )}

                {routingResult.missingData.length > 0 && (
                  <div>
                    <p className="text-sm font-medium text-gray-700">
                      Información faltante:
                    </p>
                    <ul className="mt-1 list-inside list-disc text-sm text-gray-600">
                      {routingResult.missingData.map((d) => (
                        <li key={d}>{formatIntent(d)}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div>
                  <p className="text-sm font-medium text-gray-700">Advertencias:</p>
                  <p className="mt-1 text-sm text-gray-600">
                    {routingResult.missingData.length > 0
                      ? 'Falta información para completar la acción.'
                      : routingResult.requiresHuman
                        ? 'La respuesta debe revisarla una persona antes de continuar.'
                        : 'Sin advertencias.'}
                  </p>
                </div>

                <div>
                  <p className="text-sm font-medium text-gray-700">Respuesta propuesta:</p>
                  <p className="mt-1 whitespace-pre-wrap text-sm text-gray-600">
                    {routingResult.responseToCustomer || 'Sin respuesta propuesta.'}
                  </p>
                </div>

                <div
                  className={`rounded-md px-3 py-2 text-sm font-medium ${
                    routingResult.requiresHuman
                      ? 'bg-red-50 text-red-700'
                      : 'bg-green-50 text-green-700'
                  }`}
                >
                  {routingResult.requiresHuman
                    ? `Debe derivarse a una persona${routingResult.handoffReason ? `: ${routingResult.handoffReason}` : ''}`
                    : 'Puede responder automáticamente'}
                </div>
              </div>

              <Button variant="secondary" onClick={onToggleEdit} size="sm" className="w-full">
                {previewEditable ? 'Terminar edición' : 'Editar respuesta'}
              </Button>
            </>
          )}
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="text-lg font-semibold">Enviar a conversación real</h2>
        <p className="mt-1 text-xs text-gray-500">
          Selecciona una conversación activa para enviar la respuesta aprobada.
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
                  {c.customerName ?? 'Sin nombre'} {c.customerPhone ? `- ${maskPhone(c.customerPhone)}` : ''}
                </option>
              ))}
            </select>
          )}

          {selectedConversation && (
            <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 text-sm">
              <p><span className="font-medium">Cliente:</span> {selectedConversation.customerName ?? 'Sin nombre'}</p>
              <p><span className="font-medium">Teléfono:</span> {maskPhone(selectedConversation.customerPhone) || 'No disponible'}</p>
            </div>
          )}

          <Button
            onClick={() => onShowSendConfirm(true)}
            disabled={!selectedConversationId || !routingResult || !canSend}
            className="w-full"
          >
            {!canSend
              ? 'Sin permiso para enviar'
              : !selectedConversationId
                ? 'Selecciona una conversación'
                : !routingResult
                  ? 'Ejecuta una prueba primero'
                  : 'Enviar a conversación'}
          </Button>
        </div>
      </Card>

      {showSendConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold">Confirmar envío</h3>
            <p className="mt-2 text-sm text-gray-600">
              Se enviará la respuesta a{' '}
              <strong>{selectedConversation?.customerName ?? 'el cliente'}</strong>
              {selectedConversation?.customerPhone
                ? ` (${maskPhone(selectedConversation.customerPhone)})`
                : ''}
              . Esta acción no se puede deshacer.
            </p>
            <div className="mt-4 flex justify-end gap-2">
              <Button variant="secondary" onClick={() => onShowSendConfirm(false)}>
                Cancelar
              </Button>
              <Button onClick={onSend} loading={isSending}>
                Confirmar envío
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-white p-2">
      <p className="text-xs font-medium text-gray-500">{label}</p>
      <p className="mt-0.5 break-words text-sm font-medium text-gray-800">{value}</p>
    </div>
  )
}
