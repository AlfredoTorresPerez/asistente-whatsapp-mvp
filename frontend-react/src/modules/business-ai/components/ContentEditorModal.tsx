import { Button } from '../../../components/ui/Button'
import { Input } from '../../../components/ui/Input'
import { Modal } from '../../../components/overlay/Modal'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'

type EditorState = {
  open: boolean
  mode: 'create' | 'edit'
  type: 'service' | 'product' | 'rule'
  id?: string
  title: string
  description: string
  categoryCode: string
  ruleType: string
  price: string
  durationMinutes: string
  stock: string
  priority: string
  active: boolean
}

type Props = {
  editor: EditorState
  onClose: () => void
  onChange: (editor: EditorState) => void
  onSave: () => void
  isSaving: boolean
  serviceCategories: { code: string; name: string }[]
  productCategories: { code: string; name: string }[]
}

export function ContentEditorModal({
  editor,
  onClose,
  onChange,
  onSave,
  isSaving,
  serviceCategories,
  productCategories,
}: Props) {
  if (!editor.open) return null

  const title = editor.mode === 'create' ? 'Agregar contenido' : 'Editar contenido'

  return (
    <Modal maxWidthClassName="max-w-[720px]" onClose={onClose} open={true}>
      <div className="p-6">
        <h2 className="text-lg font-semibold">{title}</h2>

        <div className="mt-4 space-y-4">
          <Input
            label="Nombre"
            value={editor.title}
            onChange={(e) => onChange({ ...editor, title: e.target.value })}
          />

          <Textarea
            label="Descripción"
            value={editor.description}
            onChange={(e) => onChange({ ...editor, description: e.target.value })}
            rows={4}
          />

          {editor.type === 'service' && (
            <>
              <Select
                label="Categoría"
                value={editor.categoryCode}
                onChange={(e) => onChange({ ...editor, categoryCode: e.target.value })}
                options={[
                  ...serviceCategories.map((c) => ({ label: c.name, value: c.code })),
                ]}
              />
              <Input
                label="Precio"
                type="number"
                value={editor.price}
                onChange={(e) => onChange({ ...editor, price: e.target.value })}
              />
              <Input
                label="Duración (minutos)"
                type="number"
                value={editor.durationMinutes}
                onChange={(e) => onChange({ ...editor, durationMinutes: e.target.value })}
              />
            </>
          )}

          {editor.type === 'product' && (
            <>
              <Select
                label="Categoría"
                value={editor.categoryCode}
                onChange={(e) => onChange({ ...editor, categoryCode: e.target.value })}
                options={[
                  ...productCategories.map((c) => ({ label: c.name, value: c.code })),
                ]}
              />
              <Input
                label="Precio"
                type="number"
                value={editor.price}
                onChange={(e) => onChange({ ...editor, price: e.target.value })}
              />
              <Input
                label="Stock"
                type="number"
                value={editor.stock}
                onChange={(e) => onChange({ ...editor, stock: e.target.value })}
              />
            </>
          )}

          {editor.type === 'rule' && (
            <>
              <Select
                label="Tipo de regla"
                value={editor.ruleType}
                onChange={(e) => onChange({ ...editor, ruleType: e.target.value })}
                options={[
                  { label: 'Comercial', value: 'COMMERCIAL' },
                  { label: 'Seguridad', value: 'SAFETY' },
                  { label: 'Disponibilidad', value: 'AVAILABILITY' },
                  { label: 'Pago', value: 'PAYMENT' },
                ]}
              />
              <Input
                label="Prioridad"
                type="number"
                value={editor.priority}
                onChange={(e) => onChange({ ...editor, priority: e.target.value })}
              />
            </>
          )}

          <label className="flex items-center gap-3">
            <input
              type="checkbox"
              checked={editor.active}
              onChange={(e) => onChange({ ...editor, active: e.target.checked })}
              className="h-4 w-4 rounded border-gray-300"
            />
            <span className="text-sm font-medium">Activo</span>
          </label>
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={onSave} loading={isSaving}>
            Guardar
          </Button>
        </div>
      </div>
    </Modal>
  )
}
