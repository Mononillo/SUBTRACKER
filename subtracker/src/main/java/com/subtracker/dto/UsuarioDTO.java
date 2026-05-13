package com.subtracker.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO para transferir datos de usuario. No incluye información sensible como la
 * contraseña.
 */
@Builder
public record UsuarioDTO(Long id,

		@NotBlank(message = "El correo es obligatorio") @Email(message = "Formato de correo inválido") String correo,

		@NotBlank(message = "El nombre es obligatorio") @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres") String nombre,

		LocalDateTime fechaRegistro) {
}