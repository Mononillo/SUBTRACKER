package com.subtracker.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.subtracker.model.SuscripcionTransaccion;
import com.subtracker.repository.SuscripcionTransaccionRepository;

@Service
public class TransaccionService {

	private final SuscripcionTransaccionRepository suscripcionTransaccionRepository;

	public TransaccionService(SuscripcionTransaccionRepository suscripcionTransaccionRepository) {
		this.suscripcionTransaccionRepository = suscripcionTransaccionRepository;
	}

	public List<SuscripcionTransaccion> buscarTransaccionesPorSuscripcion(String id) {

		return suscripcionTransaccionRepository.findBySuscripcionId(id);

	}

}
