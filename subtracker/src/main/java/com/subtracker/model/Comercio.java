package com.subtracker.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "T_COMERCIO")
public class Comercio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, unique = true, length = 150)
	private String nombre;

	@Column(name = "patron", length = 255)
	private String patron;

	@Column(name = "fecha_creacion", updatable = false)
	private LocalDateTime fechaCreacion;

	@Builder.Default
	@OneToMany(mappedBy = "comercio")
	private List<Suscripcion> suscripciones = new ArrayList<>();
}
