import { Card } from '../../../components/ui/Card'

type Props = {
  allowedTopics: Record<string, boolean>
  blockedTopics: Record<string, boolean>
  allowPrices: boolean
  allowBooking: boolean
  allowPromotions: boolean
  requireAvailabilityCheck: boolean
  onAllowedTopicsChange: (topics: Record<string, boolean>) => void
  onBlockedTopicsChange: (topics: Record<string, boolean>) => void
  onAllowPricesChange: (v: boolean) => void
  onAllowBookingChange: (v: boolean) => void
  onAllowPromotionsChange: (v: boolean) => void
  onRequireAvailabilityCheckChange: (v: boolean) => void
}

function TopicChecklist({
  title,
  topics,
  onChange,
}: {
  title: string
  topics: Record<string, boolean>
  onChange: (topics: Record<string, boolean>) => void
}) {
  const entries = Object.entries(topics)
  if (entries.length === 0) return null
  return (
    <div>
      <p className="mb-1 text-sm font-medium">{title}</p>
      <div className="grid grid-cols-2 gap-1">
        {entries.map(([topic, enabled]) => (
          <label key={topic} className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={enabled}
              onChange={() => onChange({ ...topics, [topic]: !enabled })}
              className="h-3.5 w-3.5 rounded border-gray-300"
            />
            {topic}
          </label>
        ))}
      </div>
    </div>
  )
}

export function AssistantCapabilities(props: Props) {
  return (
    <Card className="p-4">
      <h2 className="text-lg font-semibold">Capacidades del asistente</h2>
      <p className="mt-1 text-xs text-gray-500">
        Define qué temas puede tratar el asistente y qué acciones puede realizar automáticamente.
      </p>

      <div className="mt-4 space-y-4">
        <TopicChecklist
          title="Temas permitidos"
          topics={props.allowedTopics}
          onChange={props.onAllowedTopicsChange}
        />
        <TopicChecklist
          title="Temas bloqueados"
          topics={props.blockedTopics}
          onChange={props.onBlockedTopicsChange}
        />

        <hr className="border-gray-200" />

        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={props.allowPrices}
            onChange={(e) => props.onAllowPricesChange(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <div>
            <span className="text-sm font-medium">Precios</span>
            <p className="text-xs text-gray-500">Puede informar precios de servicios y productos.</p>
          </div>
        </label>

        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={props.allowBooking}
            onChange={(e) => props.onAllowBookingChange(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <div>
            <span className="text-sm font-medium">Agendar citas</span>
            <p className="text-xs text-gray-500">Puede agendar, reprogramar y cancelar reservas.</p>
          </div>
        </label>

        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={props.allowPromotions}
            onChange={(e) => props.onAllowPromotionsChange(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <div>
            <span className="text-sm font-medium">Promociones</span>
            <p className="text-xs text-gray-500">Puede ofrecer promociones y descuentos activos.</p>
          </div>
        </label>

        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={props.requireAvailabilityCheck}
            onChange={(e) => props.onRequireAvailabilityCheckChange(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <div>
            <span className="text-sm font-medium">Validar disponibilidad real</span>
            <p className="text-xs text-gray-500">Verifica la agenda digital antes de confirmar una cita.</p>
          </div>
        </label>
      </div>
    </Card>
  )
}
