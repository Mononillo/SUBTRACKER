package com.subtracker.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO principal para suscripciones. Incluye toda la información necesaria para
 * la vista.
 */
@Builder
public record SuscripcionDTO(String id, Long usuarioId, String nombreUsuario,

		@Size(min = 2, max = 100, message = "El nombre del servicio debe tener entre 2 y 100 caracteres") String nombreServicio,

		Long comercioId, String nombreComercio, String patronComercio,

		@NotNull(message = "El importe es obligatorio") @Positive(message = "El importe debe ser positivo") Double importe,

		String moneda, String frecuencia, // "MENSUAL", "ANUAL", "DESCONOCIDA"
		String estado, // "POTENCIAL", "ACTIVA", "CANCELADA"

		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaInicio,

		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate proximaRenovacion,

		String confianza // "BAJA", "MEDIA", "ALTA"
) {
}