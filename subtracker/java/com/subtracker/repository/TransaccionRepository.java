package com.subtracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subtracker.model.Transaccion;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

	Optional<Transaccion> findByIdExterno(String idExterno);

	List<Transaccion> findByCuentaBancariaId(Long cuentaId);

	List<Transaccion> findByComercioContainingIgnoreCase(String comercio);

	boolean existsByIdExterno(String idExterno);

}