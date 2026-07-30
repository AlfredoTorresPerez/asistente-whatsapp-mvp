import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import type { PromptTemplateResponse } from '../../../services/api/types'
import { PromptVersionHistory } from './PromptVersionHistory'

type Props = {
  assistantPrompt: string
  onAssistantPromptChange: (v: string) => void
  activePrompt: PromptTemplateResponse | null
  prompts: PromptTemplateResponse[]
  onSavePrompt: () => void
  isSavingPrompt: boolean
  hasChanges: boolean
}

export function BusinessAiAdvancedSettings({
  assistantPrompt,
  onAssistantPromptChange,
  activePrompt,
  prompts,
  onSavePrompt,
  isSavingPrompt,
  hasChanges,
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
