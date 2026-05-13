package com.subtracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * DTO para comercios normalizados.
 */
@Builder
public record ComercioDTO(Long id,

		@NotBlank(message = "El nombre del comercio es obligatorio") String nombre,

		String patron) {
}