package com.subtracker.service;

import org.springframework.stereotype.Service;

import com.subtracker.model.CuentaBancaria;
import com.subtracker.repository.CuentaBancariaRepository;

@Service
public class CuentaBancariaService {

	private final CuentaBancariaRepository cuentaBancariaRepository;

	public CuentaBancariaService(CuentaBancariaRepository cuentaBancariaRepository) {
		this.cuentaBancariaRepository = cuentaBancariaRepository;
	}

	public CuentaBancaria obtenerCuentaPorUsuarioId(Long usuarioId) {

		return cuentaBancariaRepository.findByUsuarioId(usuarioId).get(0);

	}

}
