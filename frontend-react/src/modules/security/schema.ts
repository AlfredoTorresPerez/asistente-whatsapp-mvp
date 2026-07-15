import { z } from 'zod'
import { buildPasswordSchema } from '../../lib/passwordPolicy'

export const profileSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, 'El nombre es obligatorio.')
    .max(80, 'El nombre no puede superar 80 caracteres.'),
  lastName: z
    .string()
    .trim()
    .min(1, 'El apellido es obligatorio.')
    .max(80, 'El apellido no puede superar 80 caracteres.'),
  phone: z
    .string()
    .trim()
    .regex(/^$|^\+[1-9]\d{7,14}$/, 'Ingresa un telefono valido en formato internacional.'),
  timezone: z
    .string()
    .trim()
    .min(1, 'La zona horaria es obligatoria.')
    .max(60, 'La zona horaria no puede superar 60 caracteres.'),
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'La contrasena actual es obligatoria.'),
    newPassword: buildPasswordSchema('La nueva contrasena'),
    confirmPassword: z.string().min(1, 'Debes confirmar la nueva contrasena.'),
  })
  .refine((value) => value.newPassword === value.confirmPassword, {
    message: 'La confirmacion debe coincidir con la nueva contrasena.',
    path: ['confirmPassword'],
  })

export type ProfileFormValues = z.infer<typeof profileSchema>
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>
