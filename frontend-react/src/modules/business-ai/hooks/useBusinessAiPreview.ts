import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { keepPreviousData, useState } from 'react'
import { useToast } from '../../../lib/toast'
import {
  analyzeAestheticIntent,
} from '../../../services/api/aestheticApi'
import {
  getConversationsRequest,
  sendConversationMessageRequest,
} from '../../../services/api/conversationsApi'
import type { IntentAnalysisResponse } from '../../../services/api/types'

export function useBusinessAiPreview() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const [scenario, setScenario] = useState(
    'Hola, quiero saber que tipo de depilacion ofrecen y agendar depilacion bozo para manana a las 14 horas.',
  )
  const [conversationSearch, setConversationSearch] = useState('')
  const [selectedConversationId, setSelectedConversationId] = useState('')
  const [analysisResult, setAnalysisResult] = useState<IntentAnalysisResponse | null>(null)
  const [previewEditable, setPreviewEditable] = useState(false)
  const [previewResponse, setPreviewResponse] = useState('')

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

  const analyzeMutation = useMutation({
    mutationFn: analyzeAestheticIntent,
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo ejecutar la prueba.',
        title: 'No se pudo probar la IA',
        tone: 'error',
      })
    },
    onSuccess: (result) => {
      setAnalysisResult(result)
      setPreviewResponse(result.respuestaSugerida)
      void queryClient.invalidateQueries({ queryKey: ['business-ai', 'intent-logs'] })
      showToast({
        description: `Intención detectada: ${result.intencion}.`,
        title: 'Prueba ejecutada',
        tone: 'success',
      })
    },
  })

  const sendApprovedMutation = useMutation({
    mutationFn: ({ body, conversationId }: { body: string; conversationId: string }) =>
      sendConversationMessageRequest(conversationId, { body }),
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo enviar la respuesta aprobada al cliente.',
        title: 'No se pudo enviar',
        tone: 'error',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['business-ai', 'conversations'] })
      void queryClient.invalidateQueries({ queryKey: ['conversations'] })
      showToast({
        description: 'La respuesta aprobada fue enviada y registrada en la conversación seleccionada.',
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
    analyzeMutation.mutate({ message: message.trim() })
  }

  const approveAndSend = () => {
    if (!selectedConversationId) {
      showToast({
        description: 'Selecciona una conversación para enviar la respuesta.',
        title: 'Conversación requerida',
        tone: 'warning',
      })
      return
    }
    if (!previewResponse.trim()) {
      showToast({
        description: 'No hay respuesta para enviar.',
        title: 'Respuesta vacía',
        tone: 'warning',
      })
      return
    }
    sendApprovedMutation.mutate({
      body: previewResponse,
      conversationId: selectedConversationId,
    })
  }

  return {
    scenario,
    setScenario,
    conversationSearch,
    setConversationSearch,
    selectedConversationId,
    setSelectedConversationId,
    analysisResult,
    setAnalysisResult,
    previewEditable,
    setPreviewEditable,
    previewResponse,
    setPreviewResponse,
    conversations,
    analyzeMutation,
    sendApprovedMutation,
    runScenario,
    approveAndSend,
    isAnalyzing: analyzeMutation.isPending,
    isSending: sendApprovedMutation.isPending,
  }
}
