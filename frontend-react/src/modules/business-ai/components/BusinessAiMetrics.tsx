import { Card } from '../../../components/ui/Card'
import { StatusBadge } from '../../../components/ui/StatusBadge'

type MetricCardData = {
  accent: 'green' | 'blue' | 'orange'
  description: string
  icon: 'spark' | 'chat' | 'shield' | 'human' | 'send'
  title: string
  value: string
}

const icons: Record<string, string> = {
  spark: '✦',
  chat: '💬',
  shield: '🛡',
  human: '👤',
  send: '📨',
}

export function MetricCard({ metric }: { metric: MetricCardData }) {
  return (
    <Card className="flex flex-col gap-1 p-4">
      <div className="flex items-center justify-between">
        <span className="text-lg">{icons[metric.icon] ?? '📊'}</span>
        <StatusBadge status={metric.accent === 'green' ? 'active' : metric.accent === 'orange' ? 'warning' : 'info'} />
      </div>
      <p className="text-2xl font-bold">{metric.value}</p>
      <p className="text-sm font-medium text-gray-700">{metric.title}</p>
      <p className="text-xs text-gray-500">{metric.description}</p>
    </Card>
  )
}

export function BusinessAiMetrics({ metrics }: { metrics: MetricCardData[] }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      {metrics.map((metric) => (
        <MetricCard key={metric.title} metric={metric} />
      ))}
    </div>
  )
}
