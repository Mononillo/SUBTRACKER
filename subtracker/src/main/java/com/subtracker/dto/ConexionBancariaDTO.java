package com.subtracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO para conexiones con Enable Banking. NO incluye tokens por seguridad, solo
 * metadatos.
 */
@Builder
public record ConexionBancariaDTO(Long id, Long usuarioId, String nombreUsuario, Long cuentaBancariaId, String iban,
		String idSesion,

		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime expiraEn) {
}