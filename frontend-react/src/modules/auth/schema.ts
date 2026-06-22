import { z } from 'zod'
import { buildPasswordSchema } from '../../lib/passwordPolicy'

export const loginSchema = z.object({
  email: z.email('Ingresa un correo valido.'),
  password: z
    .string()
    .min(8, 'La contrasena debe tener al menos 8 caracteres.')
    .max(72, 'La contrasena no puede superar 72 caracteres.'),
})

export const forgotPasswordSchema = z.object({
  email: z.email('Ingresa un correo valido.'),
})

export const resetPasswordSchema = z
  .object({
    newPassword: buildPasswordSchema('La nueva contrasena'),
    confirmPassword: z.string(),
  })
  .refine((value) => value.newPassword === value.confirmPassword, {
    message: 'La confirmacion debe coincidir con la nueva contrasena.',
    path: ['confirmPassword'],
  })

export type LoginFormValues = z.infer<typeof loginSchema>
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>
