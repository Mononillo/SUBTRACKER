package com.subtracker.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ProximoPagoDTO {

	private final SuscripcionDTO suscripcion;
	private final int diasRestantes;

	public ProximoPagoDTO(SuscripcionDTO suscripcion, LocalDate hoy) {
		this.suscripcion = suscripcion;
		this.diasRestantes = (int) ChronoUnit.DAYS.between(hoy, suscripcion.proximaRenovacion());
	}

	public SuscripcionDTO getSuscripcion() {
		return suscripcion;
	}

	public int getDiasRestantes() {
		return diasRestantes;
	}
}