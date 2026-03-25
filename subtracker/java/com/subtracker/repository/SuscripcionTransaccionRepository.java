package com.subtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subtracker.model.SuscripcionTransaccion;
import com.subtracker.model.SuscripcionTransaccion.SuscripcionTransaccionId;

public interface SuscripcionTransaccionRepository
		extends JpaRepository<SuscripcionTransaccion, SuscripcionTransaccionId> {

	void deleteBySuscripcionId(Long suscripcionId);

	List<SuscripcionTransaccion> findBySuscripcionId(String id);

	List<SuscripcionTransaccion> findByTransaccionId(Long transaccionId);
}