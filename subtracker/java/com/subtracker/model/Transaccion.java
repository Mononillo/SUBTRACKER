package com.subtracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "T_TRANSACCION")
public class Transaccion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "cuenta_bancaria_id", nullable = false)
	private CuentaBancaria cuentaBancaria;

	@Column(name = "id_externo", length = 100, unique = true)
	private String idExterno;

	@Column(name = "fecha_transaccion", nullable = false)
	private LocalDate fechaTransaccion;

	@Column(name = "importe", nullable = false, precision = 10, scale = 2)
	private BigDecimal importe;

	@Column(name = "moneda", length = 3, nullable = false)
	private String moneda;

	@Column(name = "descripcion", length = 255)
	private String descripcion;

	@Column(name = "comercio", length = 150)
	private String comercio;

	@ManyToOne
	@JoinColumn(name = "comercio_id")
	private Comercio comercioRelacionado;

	@Column(name = "fecha_registro", updatable = false)
	private LocalDateTime fechaRegistro;

	@OneToMany(mappedBy = "transaccion")
	private List<SuscripcionTransaccion> suscripciones;

	@PrePersist
	protected void onCreate() {
		fechaRegistro = LocalDateTime.now();
	}
}
