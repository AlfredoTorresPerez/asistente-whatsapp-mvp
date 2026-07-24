const legendItems = [
  { color: '#10b981', label: 'Confirmada' },
  { color: '#f59e0b', label: 'Reservada' },
  { color: '#38bdf8', label: 'Reprogramada' },
  { color: '#94a3b8', label: 'Completada' },
  { color: '#fb923c', label: 'No asistió' },
  { color: '#f87171', label: 'Cancelada' },
]

export function CalendarLegend() {
  return (
    <div className="flex flex-wrap items-center gap-4" role="list" aria-label="Leyenda de estados">
      {legendItems.map((item) => (
        <div className="flex items-center gap-1.5" key={item.label} role="listitem">
          <span
            aria-hidden="true"
            className="inline-block h-2.5 w-2.5 rounded-full"
            style={{ backgroundColor: item.color }}
          />
          <span className="text-xs font-medium text-slate-600">{item.label}</span>
        </div>
      ))}
    </div>
  )
}
