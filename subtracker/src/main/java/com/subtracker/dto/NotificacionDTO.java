package com.subtracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

/**
 * DTO para notificaciones de renovación.
 */
@Builder
public record NotificacionDTO(String id, Long usuarioId, String nombreUsuario, Long suscripcionId, String nombreServicio,

		@NotBlank(message = "El mensaje no puede estar vacío") String mensaje,

		@NotNull(message = "La fecha de notificación es obligatoria") @FutureOrPresent(message = "La fecha de notificación debe ser hoy o futura") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaNotificacion,

		Boolean enviada) {
}
