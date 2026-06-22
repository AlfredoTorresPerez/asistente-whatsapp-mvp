package com.asistentewhatsapp.administration.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 80, message = "El apellido no puede superar 80 caracteres.")
        String lastName,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo debe tener un formato valido.")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres.")
        String email,

        @Size(max = 30, message = "El telefono no puede superar 30 caracteres.")
        String phone,

        @NotBlank(message = "El rol es obligatorio.")
        String role,

        @NotBlank(message = "El estado es obligatorio.")
        @Pattern(regexp = "ACTIVE|INACTIVE|LOCKED", message = "El estado debe ser ACTIVE, INACTIVE o LOCKED.")
        String status,

        @Size(max = 60, message = "La zona horaria no puede superar 60 caracteres.")
        String timezone,

        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.")
        String temporaryPassword) {
}
