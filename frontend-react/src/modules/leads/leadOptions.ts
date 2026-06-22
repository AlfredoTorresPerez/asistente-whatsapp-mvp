export const leadStageOptions = [
  { label: 'Nueva', value: 'NEW' },
  { label: 'Contactada', value: 'CONTACTED' },
  { label: 'Interesada', value: 'INTERESTED' },
  { label: 'Agendada', value: 'SCHEDULED' },
  { label: 'Ganada', value: 'WON' },
  { label: 'Perdida', value: 'LOST' },
] as const

export const leadOriginOptions = [
  { label: 'Todos', value: '' },
  { label: 'Manual', value: 'MANUAL' },
  { label: 'Conversacion', value: 'CONVERSATION' },
] as const

export function getLeadStageLabel(stage: string) {
  return leadStageOptions.find((option) => option.value === stage)?.label ?? stage
}

export function getLeadStageTone(stage: string) {
  switch (stage) {
    case 'WON':
      return 'success'
    case 'LOST':
      return 'danger'
    case 'CONTACTED':
    case 'SCHEDULED':
      return 'warning'
    case 'NEW':
    case 'INTERESTED':
      return 'info'
    default:
      return 'neutral'
  }
}

export function getLeadOriginLabel(sourceType: string) {
  switch (sourceType) {
    case 'MANUAL':
      return 'Manual'
    case 'CONVERSATION':
      return 'Conversacion'
    default:
      return sourceType
  }
}
