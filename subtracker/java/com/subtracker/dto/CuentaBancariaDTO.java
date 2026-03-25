package com.subtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

/**
 * DTO para cuentas bancarias. Incluye validación básica de IBAN (formato
 * español simplificado).
 */
@Builder
public record CuentaBancariaDTO(Long id, Long usuarioId, String nombreUsuario, String nombreBanco,

		@NotBlank(message = "El IBAN es obligatorio") @Pattern(regexp = "^ES\\d{22}$", message = "El IBAN debe tener formato español válido (ES + 22 dígitos)") String iban) {
}
