package com.subtracker.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "T_CONEXION_BANCARIA")
public class ConexionBancaria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne
	@JoinColumn(name = "cuenta_bancaria_id")
	private CuentaBancaria cuentaBancaria;

	@Column(name = "id_sesion", length = 255)
	private String idSesion;

	@Column(name = "token_acceso", columnDefinition = "TEXT")
	private String tokenAcceso;

	@Column(name = "token_refresco", columnDefinition = "TEXT")
	private String tokenRefresco;

	@Column(name = "expira_en")
	private LocalDateTime expiraEn;

	@Column(name = "fecha_creacion", updatable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion")
	private LocalDateTime fechaActualizacion;

}