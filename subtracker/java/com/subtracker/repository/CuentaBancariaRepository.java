package com.subtracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subtracker.model.CuentaBancaria;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long> {

	List<CuentaBancaria> findByUsuarioId(Long usuarioId);

	Optional<CuentaBancaria> findByIban(String iban);

	Optional<CuentaBancaria> findByUid(String uid);
}
