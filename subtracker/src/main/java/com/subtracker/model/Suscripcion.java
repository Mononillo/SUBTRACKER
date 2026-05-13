package com.subtracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "T_SUSCRIPCION")
public class Suscripcion {

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "nombre_servicio", length = 100)
	private String nombreServicio;

	@ManyToOne
	@JoinColumn(name = "comercio_id")
	private Comercio comercio;

	@Column(name = "patron_comercio", length = 150)
	private String patronComercio;

	@Column(name = "importe", precision = 10)
	private Double importe;

	@Column(name = "moneda", length = 3)
	private String moneda;

	@Enumerated(EnumType.STRING)
	@Column(name = "frecuencia", columnDefinition = "ENUM('MENSUAL','BIMESTRAL','TRIMESTRAL','CUATRIMESTRAL','SEMESTRAL','ANUAL','DESCONOCIDA')")
	private Frecuencia frecuencia;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", columnDefinition = "ENUM('POTENCIAL','ACTIVA','CANCELADA')")
	private EstadoSuscripcion estado;

	@Column(name = "fecha_inicio")
	private LocalDate fechaInicio;

	@Column(name = "proxima_renovacion")
	private LocalDate proximaRenovacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "confianza", columnDefinition = "ENUM('MUY_BAJA','BAJA','MEDIA','ALTA','MUY_ALTA')")
	private Confianza confianza;

	@Column(name = "fecha_creacion", updatable = false)
	private LocalDateTime fechaCreacion;

	public enum Frecuencia {
		MENSUAL, BIMESTRAL, TRIMESTRAL, CUATRIMESTRAL, SEMESTRAL, ANUAL, DESCONOCIDA
	}

	public enum EstadoSuscripcion {
		POTENCIAL, ACTIVA, CANCELADA
	}

	public enum Confianza {
		BAJA, MEDIA, ALTA, MUY_BAJA, MUY_ALTA
	}
}