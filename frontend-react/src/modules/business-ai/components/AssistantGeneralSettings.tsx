import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Select } from '../../../components/ui/Select'

type Props = {
  active: boolean
  mode: string
  tone: string
  language: string
  escalationThreshold: string
  onActiveChange: (v: boolean) => void
  onModeChange: (v: 'suggest' | 'auto') => void
  onToneChange: (v: string) => void
  onLanguageChange: (v: string) => void
  onEscalationThresholdChange: (v: string) => void
  onSave: () => void
  isSaving: boolean
  hasChanges: boolean
}

export function AssistantGeneralSettings({
  active,
  mode,
  tone,
  language,
  escalationThreshold,
  onActiveChange,
  onModeChange,
  onToneChange,
  onLanguageChange,
  onEscalationThresholdChange,
  onSave,
  isSaving,
  hasChanges,
}: Props) {
  return (
    <Card className="p-4">
      <h2 className="text-lg font-semibold">Configuración general</h2>
      <p className="mt-1 text-xs text-gray-500">
        Controla el comportamiento básico del asistente: cuándo responde, cómo se comunica y en qué idioma.
      </p>

      <div className="mt-4 space-y-4">
        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={active}
            onChange={(e) => onActiveChange(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <div>
            <span className="text-sm font-medium">Asistente activo</span>
            <p className="text-xs text-gray-500">Activa o pausa la atención automática de conversaciones.</p>
          </div>
        </label>

        <div>
          <label className="text-sm font-medium">Modo de respuesta</label>
          <p className="text-xs text-gray-500">Define si el asistente responde automáticamente o solo sugiere respuestas.</p>
          <div className="mt-1 flex gap-2">
            <Button
              variant={mode === 'auto' ? 'primary' : 'secondary'}
              onClick={() => onModeChange('auto')}
              size="sm"
            >
              Automático
            </Button>
            <Button
              variant={mode === 'suggest' ? 'primary' : 'secondary'}
              onClick={() => onModeChange('suggest')}
              size="sm"
            >
              Solo sugerir
            </Button>
          </div>
        </div>

        <div>
          <label className="text-sm font-medium">Tono de comunicación</label>
          <p className="text-xs text-gray-500">Estilo de redacción que usará el asistente al hablar con los clientes.</p>
          <div className="mt-1 flex gap-2">
            {['Cercano', 'Profesional', 'Comercial'].map((t) => (
              <Button
                key={t}
                variant={tone === t ? 'primary' : 'secondary'}
                onClick={() => onToneChange(t)}
                size="sm"
              >
                {t}
              </Button>
            ))}
          </div>
        </div>

        <div>
          <label className="text-sm font-medium">Idioma</label>
          <p className="text-xs text-gray-500">Idioma principal para las respuestas del asistente.</p>
          <Select
            value={language}
            onChange={(e) => onLanguageChange(e.target.value)}
            options={[
              { label: 'Español', value: 'es' },
              { label: 'English', value: 'en' },
              { label: 'Português', value: 'pt' },
            ]}
            className="mt-1"
          />
        </div>

        <div>
          <label className="text-sm font-medium">Cuándo derivar a una persona</label>
          <p className="text-xs text-gray-500">
            Umbral mínimo de seguridad estimada para que el asistente responda automaticamente. Si la confianza está por debajo, deriva a atención humana.
          </p>
          <Select
            value={escalationThreshold}
            onChange={(e) => onEscalationThresholdChange(e.target.value)}
            options={[
              { label: '50% - Derivar con frecuencia', value: '50' },
              { label: '70% - Balanceado', value: '70' },
              { label: '90% - Solo respuestas seguras', value: '90' },
            ]}
            className="mt-1"
          />
        </div>

        {hasChanges && (
          <Button onClick={onSave} loading={isSaving} className="w-full">
            Guardar configuración
          </Button>
        )}
      </div>
    </Card>
  )
}
