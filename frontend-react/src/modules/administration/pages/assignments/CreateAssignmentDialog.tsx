import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Button } from '../../../../components/ui/Button'
import { Modal } from '../../../../components/overlay/Modal'
import { Select } from '../../../../components/ui/Select'
import { useToast } from '../../../../lib/toast'
import { listAestheticServices } from '../../../../services/api/aestheticApi'
import {
  assignProfessionalToServiceRequest,
  assignRoomToServiceRequest,
} from '../../../../services/api/assignmentsApi'
import { ApiClientError } from '../../../../services/api/httpClient'
import { listProfessionalsRequest } from '../../../../services/api/professionalsApi'
import { listRoomsRequest } from '../../../../services/api/roomsApi'

type CreateAssignmentDialogProps = {
  defaultServiceId?: string
  onClose: () => void
  open: boolean
}

type AssignmentTarget = 'professional' | 'room'

export function CreateAssignmentDialog({
  defaultServiceId,
  onClose,
  open,
}: CreateAssignmentDialogProps) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [target, setTarget] = useState<AssignmentTarget>('professional')
  const [serviceId, setServiceId] = useState(defaultServiceId ?? '')
  const [entityId, setEntityId] = useState('')

  const servicesQuery = useQuery({
    queryKey: ['administration', 'services', 'all'],
    queryFn: () => listAestheticServices({ size: 200 }),
    enabled: open,
  })
  const professionalsQuery = useQuery({
    queryKey: ['administration', 'professionals', 'all'],
    queryFn: () => listProfessionalsRequest({ size: 200, active: true }),
    enabled: open,
  })
  const roomsQuery = useQuery({
    queryKey: ['administration', 'rooms', 'all'],
    queryFn: () => listRoomsRequest({ size: 200, active: true }),
    enabled: open,
  })

  const mutation = useMutation({
    mutationFn: () => {
      const payload = { serviceId }
      if (target === 'professional') {
        return assignProfessionalToServiceRequest({ ...payload, professionalId: entityId })
      }
      return assignRoomToServiceRequest({ ...payload, roomId: entityId })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'assignments'] })
      showToast({
        title: 'Asignacion creada',
        description:
          target === 'professional'
            ? 'Profesional asignado al servicio.'
            : 'Cabina asignada al servicio.',
        tone: 'success',
      })
      onClose()
    },
    onError: (error) => {
      showToast({
        title: 'Error',
        description: error instanceof ApiClientError ? error.message : 'No se pudo crear la asignacion.',
        tone: 'error',
      })
    },
  })

  const serviceOptions = [
    { label: 'Selecciona servicio', value: '' },
    ...(servicesQuery.data?.items ?? []).map((s) => ({ label: s.name, value: s.id })),
  ]
  const professionalOptions = [
    { label: 'Selecciona profesional', value: '' },
    ...(professionalsQuery.data?.items ?? []).map((p) => ({
      label: p.displayName ?? p.fullName,
      value: p.id,
    })),
  ]
  const roomOptions = [
    { label: 'Selecciona cabina', value: '' },
    ...(roomsQuery.data?.items ?? []).map((r) => ({
      label: `${r.name} (${r.code})`,
      value: r.id,
    })),
  ]

  return (
    <Modal maxWidthClassName="max-w-[520px]" onClose={onClose} open={open}>
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
          Administracion
        </p>
        <h2 className="mt-3 text-[28px] font-semibold text-[var(--color-text)]">
          Asignar profesional o cabina
        </h2>
        <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
          Asocia un profesional o una cabina a un servicio del catalogo para habilitar su agenda.
        </p>

        <div className="mt-6 space-y-4">
          <Select
            label="Tipo de asignacion"
            onChange={(event) => {
              setTarget(event.target.value as AssignmentTarget)
              setEntityId('')
            }}
            options={[
              { label: 'Profesional a servicio', value: 'professional' },
              { label: 'Cabina a servicio', value: 'room' },
            ]}
            value={target}
          />
          <Select
            label="Servicio"
            onChange={(event) => setServiceId(event.target.value)}
            options={serviceOptions}
            value={serviceId}
          />
          <Select
            label={target === 'professional' ? 'Profesional' : 'Cabina'}
            onChange={(event) => setEntityId(event.target.value)}
            options={target === 'professional' ? professionalOptions : roomOptions}
            value={entityId}
          />
        </div>

        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button onClick={onClose} variant="secondary">
            Cancelar
          </Button>
          <Button
            disabled={!serviceId || !entityId}
            loading={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            Asignar
          </Button>
        </div>
      </div>
    </Modal>
  )
}
