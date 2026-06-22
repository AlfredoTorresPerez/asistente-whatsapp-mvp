import type { UseFormRegisterReturn } from 'react-hook-form'
import { Select } from '../../components/ui/Select'
import type { BusinessLocationResponse } from '../../services/api/types'

type BusinessLocationSelectProps = {
  error?: string
  locations?: BusinessLocationResponse[]
  registration: UseFormRegisterReturn<'locationId'>
}

export function BusinessLocationSelect({ error, locations = [], registration }: BusinessLocationSelectProps) {
  const options = [
    {
      label: locations.length > 1 ? 'Selecciona sucursal' : 'Asignar sucursal automaticamente',
      value: '',
    },
    ...locations.map((location) => ({
      label: location.commune ? `${location.name} · ${location.commune}` : location.name,
      value: location.id,
    })),
  ]

  const hint = locations.length > 1
    ? 'Obligatorio cuando el negocio tiene mas de una sucursal activa.'
    : 'Si hay una sola sucursal activa, se asigna automaticamente.'

  return (
    <Select
      error={error}
      hint={hint}
      label="Sucursal"
      options={options}
      {...registration}
    />
  )
}
