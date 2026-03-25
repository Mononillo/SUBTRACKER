package com.subtracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.subtracker.model.Comercio;

@Repository
public interface ComercioRepository extends JpaRepository<Comercio, Long> {

	List<Optional<Comercio>> findByNombreContainingIgnoreCase(String nombre);

	@Query(value = "SELECT DISTINCT tc.* FROM t_comercio tc JOIN t_suscripcion ts ON tc.id = ts.comercio_id WHERE ts.usuario_id = :usuarioId", nativeQuery = true)
	List<Comercio> findComerciosPorIdUsuario(@Param("usuarioId") Long idUsuario);
}