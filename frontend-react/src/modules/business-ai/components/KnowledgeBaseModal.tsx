import { Modal } from '../../../components/overlay/Modal'

type Props = {
  open: boolean
  onClose: () => void
  services: { name: string }[]
  rules: { name: string }[]
}

function KnowledgeColumn({ title, items }: { title: string; items: string[] }) {
  return (
    <div>
      <p className="mb-2 text-sm font-semibold text-gray-700">{title}</p>
      <div className="flex max-h-[400px] flex-col gap-1 overflow-y-auto">
        {items.length === 0 ? (
          <p className="text-sm text-gray-400">Sin elementos</p>
        ) : (
          items.map((item) => (
            <span
              key={item}
              className="truncate rounded bg-gray-100 px-2 py-1 text-sm text-gray-700"
            >
              {item}
            </span>
          ))
        )}
      </div>
    </div>
  )
}

export function KnowledgeBaseModal({ open, onClose, services, rules }: Props) {
  return (
    <Modal maxWidthClassName="max-w-[980px]" onClose={onClose} open={open}>
      <div className="p-6">
        <h2 className="mb-4 text-lg font-semibold">Base de conocimiento completa</h2>
        <div className="grid gap-6 md:grid-cols-2">
          <KnowledgeColumn
            title="Servicios"
            items={services.map((s) => s.name)}
          />
          <KnowledgeColumn
            title="Reglas"
            items={rules.map((r) => r.name)}
          />
        </div>
      </div>
    </Modal>
  )
}
