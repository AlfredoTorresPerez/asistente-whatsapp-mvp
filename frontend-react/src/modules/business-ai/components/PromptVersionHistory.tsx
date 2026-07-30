import type { PromptTemplateResponse } from '../../../services/api/types'
import { StatusBadge } from '../../../components/ui/StatusBadge'

type Props = {
  prompts: PromptTemplateResponse[]
  activeVersion: number | null
}

export function PromptVersionHistory({ prompts, activeVersion }: Props) {
  if (prompts.length === 0) {
    return (
      <div className="rounded-lg border border-gray-100 p-3 text-sm text-gray-400">
        No hay versiones guardadas.
      </div>
    )
  }

  return (
    <div>
      <p className="mb-2 text-sm font-medium">Historial de versiones</p>
      <div className="space-y-1">
        {[...prompts]
          .sort((a, b) => b.version - a.version)
          .slice(0, 10)
          .map((prompt) => (
            <div
              key={prompt.id}
              className="flex items-center justify-between rounded border border-gray-100 px-3 py-2 text-sm"
            >
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs text-gray-500">v{prompt.version}</span>
                <span className="truncate text-gray-700">{prompt.nombre ?? 'Sin nombre'}</span>
              </div>
              <div className="flex items-center gap-2">
                {prompt.version === activeVersion && (
                  <StatusBadge status="active" />
                )}
                {prompt.updatedAt && (
                  <span className="text-xs text-gray-400">{prompt.updatedAt}</span>
                )}
              </div>
            </div>
          ))}
      </div>
    </div>
  )
}
