import { useMemo } from 'react'
import type { AestheticIntentLogResponse } from '../../../services/api/types'

type MetricCardData = {
  accent: 'green' | 'blue' | 'orange'
  description: string
  icon: 'spark' | 'chat' | 'shield' | 'human' | 'send'
  title: string
  value: string
}

function normalizeConfidence(value: number) {
  return value <= 1 ? value * 100 : value
}

function formatPercent(value: number) {
  return `${Math.round(value)}%`
}

export function useBusinessAiMetrics(active: boolean, logs: AestheticIntentLogResponse[]) {
  const metrics = useMemo<MetricCardData[]>(() => {
    const totalLogs = logs.length
    const humanHandoffs = logs.filter((log) => log.requiresHumanHandoff).length
    const autoResolved = totalLogs === 0 ? 0 : ((totalLogs - humanHandoffs) / totalLogs) * 100
    const averageConfidence =
      totalLogs === 0
        ? 0
        : logs.reduce((sum, log) => sum + normalizeConfidence(log.confidence), 0) / totalLogs
    const suggestedToday = logs.filter((log) => {
      const created = new Date(log.createdAt)
      const today = new Date()
      return created.toDateString() === today.toDateString()
    }).length

    return [
      {
        accent: active ? 'green' : 'orange',
        description: active
          ? 'Asistente inteligente en funcionamiento'
          : 'Asistente pausado desde configuración',
        icon: 'spark' as const,
        title: 'IA activa',
        value: active ? 'Sí' : 'No',
      },
      {
        accent: 'blue' as const,
        description: 'Estimado desde los últimos análisis de intención',
        icon: 'chat' as const,
        title: 'Conversaciones resueltas',
        value: totalLogs === 0 ? 'Sin datos' : formatPercent(autoResolved),
      },
      {
        accent: averageConfidence >= 70 ? 'green' as const : 'orange' as const,
        description: 'Confianza promedio registrada por el motor de intenciones',
        icon: 'shield' as const,
        title: 'Seguridad estimada de la respuesta',
        value: totalLogs === 0 ? 'Sin datos' : formatPercent(averageConfidence),
      },
      {
        accent: humanHandoffs > 0 ? 'orange' as const : 'green' as const,
        description: 'Casos marcados para escalamiento humano',
        icon: 'human' as const,
        title: 'Derivadas a persona',
        value: totalLogs === 0 ? '0%' : formatPercent((humanHandoffs / totalLogs) * 100),
      },
      {
        accent: 'green' as const,
        description: 'Sugerencias generadas desde pruebas y mensajes reales',
        icon: 'send' as const,
        title: 'Respuestas sugeridas hoy',
        value: String(suggestedToday),
      },
    ]
  }, [active, logs])

  return { metrics }
}
