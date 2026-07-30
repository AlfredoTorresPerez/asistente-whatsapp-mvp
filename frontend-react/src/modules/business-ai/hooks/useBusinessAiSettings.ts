import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useReducer, useState } from 'react'
import { useToast } from '../../../lib/toast'
import {
  activateBusinessAiPrompt,
  createBusinessAiPrompt,
  getBusinessAiPrompts,
  getBusinessAiSettings,
  saveBusinessAiSettings,
} from '../../../services/api/aestheticApi'
import type {
  BusinessAiSettingsResponse,
  PromptTemplateResponse,
  UpsertBusinessAiSettingsRequest,
} from '../../../services/api/types'
import { allowedTopicDefaults, blockedTopicDefaults, defaultPrompt } from '../lib/constants'
import { buildPrompt } from '../lib/businessAiHelpers'

type AssistantMode = 'suggest' | 'auto'
type AssistantTone = 'Cercano' | 'Profesional' | 'Comercial'

type AssistantConfigState = {
  active: boolean
  mode: AssistantMode
  tone: AssistantTone
  language: string
  escalationThreshold: string
  allowPrices: boolean
  allowBooking: boolean
  allowPromotions: boolean
  requireAvailabilityCheck: boolean
}

export function useBusinessAiSettings() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const [config, setConfig] = useState<AssistantConfigState>({
    active: true,
    allowBooking: true,
    allowPrices: true,
    allowPromotions: true,
    escalationThreshold: '70',
    language: 'es',
    mode: 'auto',
    requireAvailabilityCheck: true,
    tone: 'Cercano',
  })
  const [allowedTopics, setAllowedTopics] = useState(
    () => Object.fromEntries(allowedTopicDefaults.map((topic) => [topic, true])) as Record<string, boolean>,
  )
  const [blockedTopics, setBlockedTopics] = useState(
    () => Object.fromEntries(blockedTopicDefaults.map((topic) => [topic, true])) as Record<string, boolean>,
  )
  const [assistantPrompt, setAssistantPrompt] = useState(defaultPrompt)

  const settingsQuery = useQuery({
    queryKey: ['business-ai-settings'],
    queryFn: getBusinessAiSettings,
  })

  const promptsQuery = useQuery({
    queryKey: ['business-ai-prompts'],
    queryFn: getBusinessAiPrompts,
    placeholderData: keepPreviousData,
  })

  const activePrompt = useMemo(() => {
    const prompts = promptsQuery.data ?? []
    const settings = settingsQuery.data
    if (settings?.activePromptVersion != null) {
      return prompts.find((p) => p.version === settings.activePromptVersion)
    }
    return prompts.length > 0 ? prompts[0] : null
  }, [promptsQuery.data, settingsQuery.data])

  useEffect(() => {
    if (settingsQuery.data) {
      setConfig({
        active: settingsQuery.data.active,
        allowBooking: settingsQuery.data.allowBooking,
        allowPrices: settingsQuery.data.allowPrices,
        allowPromotions: settingsQuery.data.allowPromotions,
        escalationThreshold: String(Math.round(settingsQuery.data.escalationThreshold * 100)),
        language: settingsQuery.data.language,
        mode: settingsQuery.data.mode,
        requireAvailabilityCheck: settingsQuery.data.requireAvailabilityCheck,
        tone: settingsQuery.data.tone,
      })
      setAllowedTopics(
        Object.fromEntries(
          (settingsQuery.data.allowedTopics.length > 0
            ? settingsQuery.data.allowedTopics
            : allowedTopicDefaults
          ).map((topic) => [topic, true]),
        ),
      )
      setBlockedTopics(
        Object.fromEntries(
          (settingsQuery.data.blockedTopics.length > 0
            ? settingsQuery.data.blockedTopics
            : blockedTopicDefaults
          ).map((topic) => [topic, true]),
        ),
      )
    }
  }, [settingsQuery.data])

  useEffect(() => {
    if (activePrompt) {
      setAssistantPrompt(activePrompt.contenido)
    }
  }, [activePrompt])

  const saveSettingsMutation = useMutation({
    mutationFn: async () => {
      const request: UpsertBusinessAiSettingsRequest = {
        active: config.active,
        mode: config.mode,
        tone: config.tone,
        language: config.language,
        escalationThreshold: Number(config.escalationThreshold) / 100,
        allowPrices: config.allowPrices,
        allowBooking: config.allowBooking,
        allowPromotions: config.allowPromotions,
        requireAvailabilityCheck: config.requireAvailabilityCheck,
        allowedTopics: Object.entries(allowedTopics).filter(([, v]) => v).map(([k]) => k),
        blockedTopics: Object.entries(blockedTopics).filter(([, v]) => v).map(([k]) => k),
      }
      return saveBusinessAiSettings(request)
    },
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo guardar la configuración.',
        title: 'Error al guardar configuración',
        tone: 'error',
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['business-ai-settings'] })
      showToast({
        description: 'La configuración del asistente se actualizó correctamente.',
        title: 'Configuración guardada',
        tone: 'success',
      })
    },
  })

  const savePromptMutation = useMutation({
    mutationFn: async () => {
      return createBusinessAiPrompt({
        codigo: 'PROMPT_OPERATIVO_IA_NEGOCIO',
        nombre: 'Instrucciones internas del asistente',
        descripcion: 'Instrucción principal que define el comportamiento del asistente IA',
        modulo: 'AI_AGENT',
        tipo: 'SYSTEM_PROMPT',
        contenido: buildPrompt(config, assistantPrompt),
        prioridad: 1,
      })
    },
    onError: (error) => {
      showToast({
        description: error instanceof Error ? error.message : 'No se pudo guardar las instrucciones.',
        title: 'Error al guardar instrucciones',
        tone: 'error',
      })
    },
    onSuccess: (newPrompt) => {
      void queryClient.invalidateQueries({ queryKey: ['business-ai-prompts'] })
      activateBusinessAiPrompt({ promptId: newPrompt.id })
      void queryClient.invalidateQueries({ queryKey: ['business-ai-settings'] })
      showToast({
        description: 'Las instrucciones internas se guardaron y activaron correctamente.',
        title: 'Instrucciones guardadas',
        tone: 'success',
      })
    },
  })

  const hasUnsavedSettings = useMemo(() => {
    if (!settingsQuery.data) return false
    const s = settingsQuery.data
    return (
      config.active !== s.active ||
      config.mode !== s.mode ||
      config.tone !== s.tone ||
      config.language !== s.language ||
      Number(config.escalationThreshold) / 100 !== s.escalationThreshold ||
      config.allowPrices !== s.allowPrices ||
      config.allowBooking !== s.allowBooking ||
      config.allowPromotions !== s.allowPromotions ||
      config.requireAvailabilityCheck !== s.requireAvailabilityCheck
    )
  }, [config, settingsQuery.data])

  const hasUnsavedPrompt = assistantPrompt !== (activePrompt?.contenido ?? defaultPrompt)

  return {
    config,
    setConfig,
    allowedTopics,
    setAllowedTopics,
    blockedTopics,
    setBlockedTopics,
    assistantPrompt,
    setAssistantPrompt,
    activePrompt,
    settingsQuery,
    promptsQuery,
    saveSettingsMutation,
    savePromptMutation,
    hasUnsavedSettings,
    hasUnsavedPrompt,
    isLoading: settingsQuery.isLoading || promptsQuery.isLoading,
  }
}
