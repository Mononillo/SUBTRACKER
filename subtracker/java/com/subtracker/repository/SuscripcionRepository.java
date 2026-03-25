package com.subtracker.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.subtracker.model.Comercio;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Suscripcion.EstadoSuscripcion;
import com.subtracker.model.Usuario;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, String> {

	List<Suscripcion> findByUsuarioId(Long usuarioId);

	List<Suscripcion> findByUsuarioIdAndEstado(Long usuarioId, EstadoSuscripcion estado);

	List<Suscripcion> findByProximaRenovacionBetweenAndEstado(LocalDate inicio, LocalDate fin,
			EstadoSuscripcion estado);

	List<Suscripcion> findByUsuarioIdAndPatronComercioContainingIgnoreCase(Long usuarioId, String patron);

	Long countByUsuarioIdAndEstado(Long usuarioId, EstadoSuscripcion estado);

	@Query("SELECT s FROM Suscripcion s WHERE s.usuario.id = :usuarioId AND s.estado = 'POTENCIAL' AND s.confianza IN ('ALTA', 'MEDIA')")
	List<Suscripcion> findPotencialesByUsuarioId(@Param("usuarioId") Long usuarioId);

	@Query(value = "SELECT COALESCE(SUM(s.importe), 0) FROM t_suscripcion s " + "WHERE s.usuario_id = :usuarioId "
			+ "AND s.estado = 'ACTIVA' " + "AND YEAR(s.proxima_renovacion) = YEAR(CURDATE()) "
			+ "AND MONTH(s.proxima_renovacion) = MONTH(CURDATE())", nativeQuery = true)
	Double sumImporteMesActual(@Param("usuarioId") Long usuarioId);

	@Query(value = "SELECT COALESCE(SUM(s.importe), 0) FROM t_suscripcion s " + "WHERE s.usuario_id = :usuarioId "
			+ "AND s.estado = 'ACTIVA' " + "AND YEAR(s.proxima_renovacion) = YEAR(CURDATE())", nativeQuery = true)
	Double sumGastoAnual(@Param("usuarioId") Long usuarioId);

	@Query(value = """
			SELECT s.proxima_renovacion
			FROM t_suscripcion s
			WHERE s.usuario_id = :usuarioId
			  AND s.estado = 'ACTIVA'
			  AND s.proxima_renovacion >= CURRENT_DATE
			ORDER BY s.proxima_renovacion ASC
			LIMIT 1
			""", nativeQuery = true)
	Optional<Date> findProximoPago(@Param("usuarioId") Long usuarioId);

	Optional<Suscripcion> findByUsuarioAndComercio(Usuario usuario, Comercio comercio);

	@Query("SELECT s.nombreServicio FROM Suscripcion s WHERE LOWER(s.patronComercio) LIKE LOWER(CONCAT('%', :patron, '%')) LIMIT 1")
	Optional<String> findNombreServicioByPatronContaining(@Param("patron") String patron);
}