package com.subtracker.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subtracker.model.ConexionBancaria;
import com.subtracker.model.Usuario;

@Repository
public interface ConexionBancariaRepository extends JpaRepository<ConexionBancaria, Long> {

	Optional<ConexionBancaria> findFirstByUsuarioIdOrderByExpiraEnDesc(Long usuarioId);

	Optional<ConexionBancaria> findByIdSesion(String idSesion);

	List<ConexionBancaria> findByExpiraEnBetween(LocalDateTime inicio, LocalDateTime fin);

	Optional<ConexionBancaria> findTopByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

	Optional<ConexionBancaria> findByUsuario(Usuario usuario);
}
