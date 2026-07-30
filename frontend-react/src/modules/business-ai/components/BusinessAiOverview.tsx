import { Card } from '../../../components/ui/Card'

type Props = {
  active: boolean
  lastModifiedAt?: string | null
  lastModifiedBy?: string | null
}

export function BusinessAiOverview({ active, lastModifiedAt, lastModifiedBy }: Props) {
  return (
    <Card className="p-4">
      <h2 className="text-lg font-semibold">Resumen del asistente</h2>
      <p className="mt-1 text-sm text-gray-600">
        {active
          ? 'El asistente está activo y procesando conversaciones automáticamente.'
          : 'El asistente está pausado. Los mensajes no serán procesados hasta que se active.'}
      </p>
      {lastModifiedAt && (
        <p className="mt-2 text-xs text-gray-400">
          Última modificación: {lastModifiedAt}
          {lastModifiedBy ? ` por ${lastModifiedBy}` : ''}
        </p>
      )}
    </Card>
  )
}
