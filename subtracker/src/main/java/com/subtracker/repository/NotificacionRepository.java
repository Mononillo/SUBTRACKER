package com.subtracker.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.subtracker.model.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

	List<Notificacion> findByUsuarioIdAndEnviadaFalse(Long usuarioId);

	List<Notificacion> findByFechaNotificacionAndEnviadaFalse(LocalDate fecha);

	List<Notificacion> findBySuscripcionIdOrderByFechaNotificacionDesc(Long suscripcionId);
}