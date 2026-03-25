package com.subtracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la relación entre suscripciones y transacciones.
 */
@Builder
public record SuscripcionTransaccionDTO(Long suscripcionId, Long transaccionId, String nombreServicio,
		BigDecimal importeTransaccion, String comercio,

		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime fechaAsociacion,

		Double confianzaAsociacion) {
}
