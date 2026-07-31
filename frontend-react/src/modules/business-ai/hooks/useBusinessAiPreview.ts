import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useToast } from '../../../lib/toast'
import { previewAiRequest } from '../../../services/api/aestheticApi'
import {
  getConversationsRequest,
  sendConversationMessageRequest,
} from '../../../services/api/conversationsApi'
import type { AgentRoutingResult, AiPreviewResponse } from '../../../services/api/types'

export function useBusinessAiPreview(userPermissions: string[] = []) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const [scenario, setScenario] = useState(
    'Hola, quiero saber que tipo de depilacion ofrecen y agendar depilacion bozo para manana a las 14 horas.',
  )
  const [conversationSearch, setConversationSearch] = useState('')
  const [selectedConversationId, setSelectedConversationId] = useState('')
  const [previewResponse, setPreviewResponse] = useState<AiPreviewResponse | null>(null)
  const [previewEditable, setPreviewEditable] = useState(false)
  const [editableResponse, setEditableResponse] = useState('')
  const [showSendConfirm, setShowSendConfirm] = useState(false)

  const canSend = userPermissions.includes('ALL') || userPermissions.includes('BUSINESS_AI_SEND') || userPermissions.includes('CONVERSATIONS_REPLY')

  const conversationsQuery = useQuery({
    queryKey: ['business-ai', 'conversations', conversationSearch],
    queryFn: () =>
      getConversationsRequest({
        page: 0,
        size: 50,
        search: conversationSearch.trim() || undefined,
        status: 'OPEN',
      }),
    placeholderData: keepPreviousData,
  })

  const conversations = conversationsQuery.data?.items ?? []

  const selectedConversation = useMemo(
    () => conversations.find((c) => c.id === selectedConversationId) ?? null,
    [conversations, selectedConversationId],
  )

  const routingResult: AgentRoutingResult | null = previewResponse?.result ?? null

  const analyzeMutation = useMutation({
    mutationFn: (message: string) =>
      previewAiRequest({
        message,
        conversationId: selectedConversationId || null,
      }),
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo ejecutar la prueba.',
        title: 'No se pudo probar la IA',
        tone: 'error',
      })
    },
    onSuccess: (result) => {
      setPreviewResponse(result)
      setEditableResponse(result.result?.responseToCustomer ?? '')
      if (result.result) {
        showToast({
          description: `Intención: ${formatIntent(result.result.primaryIntent)}. ${result.result.requiresHuman ? 'Requiere derivación humana.' : 'Puede responder automáticamente.'}`,
          title: 'Prueba ejecutada',
          tone: 'success',
        })
      }
    },
  })

  const sendMutation = useMutation({
    mutationFn: ({ body, conversationId }: { body: string; conversationId: string }) =>
      sendConversationMessageRequest(conversationId, { body }),
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo enviar la respuesta.',
        title: 'Error al enviar',
        tone: 'error',
      })
    },
    onSuccess: () => {
      setShowSendConfirm(false)
      void queryClient.invalidateQueries({ queryKey: ['business-ai', 'conversations'] })
      void queryClient.invalidateQueries({ queryKey: ['conversations'] })
      showToast({
        description: 'La respuesta fue enviada y registrada en la conversación.',
        title: 'Respuesta enviada',
        tone: 'success',
      })
    },
  })

  const runScenario = (message: string) => {
    if (!message.trim()) {
      showToast({
        description: 'Escribe una consulta de cliente para probar el comportamiento.',
        title: 'Escenario requerido',
        tone: 'warning',
      })
      return
    }
    analyzeMutation.mutate(message.trim())
  }

  const confirmSend = () => {
    if (!selectedConversationId) {
      showToast({
        description: 'Selecciona una conversación para enviar la respuesta.',
        title: 'Conversación requerida',
        tone: 'warning',
      })
      return
    }
    if (!canSend) {
      showToast({
        description: 'No tienes permiso para enviar mensajes a conversaciones.',
        title: 'Permiso denegado',
        tone: 'error',
      })
      return
    }
    const body = previewEditable ? editableResponse : (routingResult?.responseToCustomer ?? '')
    if (!body.trim()) {
      showToast({
        description: 'No hay respuesta para enviar.',
        title: 'Respuesta vacía',
        tone: 'warning',
      })
      return
    }
    sendMutation.mutate({ body: body.trim(), conversationId: selectedConversationId })
  }

  return {
    scenario,
    setScenario,
    conversationSearch,
    setConversationSearch,
    selectedConversationId,
    setSelectedConversationId,
    selectedConversation,
    previewResponse,
    setPreviewResponse,
    routingResult,
    previewEditable,
    setPreviewEditable,
    editableResponse,
    setEditableResponse,
    conversations,
    runScenario,
    confirmSend,
    showSendConfirm,
    setShowSendConfirm,
    isAnalyzing: analyzeMutation.isPending,
    isSending: sendMutation.isPending,
    canSend,
  }
}

function formatIntent(intent: string): string {
  return intent
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}
