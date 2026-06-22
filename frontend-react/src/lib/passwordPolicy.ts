import { z } from 'zod'

export const PASSWORD_POLICY_HINT =
  'Minimo 8 caracteres, al menos una mayuscula y un numero.'

export function buildPasswordSchema(fieldLabel: string) {
  return z
    .string()
    .min(8, `${fieldLabel} debe tener al menos 8 caracteres.`)
    .max(72, `${fieldLabel} no puede superar 72 caracteres.`)
    .regex(/[A-Z]/, `${fieldLabel} debe incluir al menos una mayuscula.`)
    .regex(/\d/, `${fieldLabel} debe incluir al menos un numero.`)
}

