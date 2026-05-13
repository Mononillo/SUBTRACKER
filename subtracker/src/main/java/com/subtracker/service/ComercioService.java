package com.subtracker.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.subtracker.model.Comercio;
import com.subtracker.repository.ComercioRepository;

@Service
public class ComercioService {

	private final ComercioRepository comercioRepository;

	ComercioService(ComercioRepository comercioRepository) {
		this.comercioRepository = comercioRepository;
	}

	public Optional<Comercio> buscarComercioPorNombre(String nombre) {
		try {
			return comercioRepository.findByNombreContainingIgnoreCase(nombre).get(0);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Comercio guardar(Comercio comercio) {
		return comercioRepository.save(comercio);
	}

	public List<Comercio> obtenerListaComerciosPorUsuario(Long idUsuario) {

		return comercioRepository.findComerciosPorIdUsuario(idUsuario);

	}
}
