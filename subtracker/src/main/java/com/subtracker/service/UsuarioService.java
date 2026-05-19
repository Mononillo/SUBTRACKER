package com.subtracker.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.subtracker.dto.RegistroDTO;
import com.subtracker.model.Usuario;
import com.subtracker.repository.CuentaBancariaRepository;
import com.subtracker.repository.SuscripcionRepository;
import com.subtracker.repository.SuscripcionTransaccionRepository;
import com.subtracker.repository.TransaccionRepository;
import com.subtracker.repository.UsuarioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final CuentaBancariaRepository cuentaBancariaRepository;
	private final TransaccionRepository transaccionRepository;
	private final SuscripcionRepository suscripcionRepository;
	private final SuscripcionTransaccionRepository suscripcionTransaccionRepository;

	public Usuario registrarUsuario(RegistroDTO dto) {
		if (usuarioRepository.findByCorreo(dto.getCorreo()).isPresent()) {
			throw new RuntimeException("El correo ya está registrado");
		}
		Usuario usuario = Usuario.builder().correo(dto.getCorreo())
				.hashContrasena(passwordEncoder.encode(dto.getPassword())) // Hashea la contraseña con BCrypt
				.nombre(dto.getNombre()).build();

		return usuarioRepository.save(usuario);
	}

	public Long buscarUsuarioIdPorCorreo(String correo) {
		if (correo == null) {
			throw new RuntimeException("El correo es nulo");
		}
		return usuarioRepository.findByCorreo(correo).get().getId();
	}

	public Optional<Usuario> buscarUsuarioPorCorreo(String correo) {
		return usuarioRepository.findByCorreo(correo);
	}

	@Transactional // Si falla cualquier paso, se revierte todo
	public void eliminarUsuario(Long usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		// El orden respeta las claves foraneas: de hijo a padre
		suscripcionTransaccionRepository.deleteBySuscripcionUsuarioId(usuarioId);
		transaccionRepository.deleteByCuentaBancariaUsuarioId(usuarioId);
		cuentaBancariaRepository.deleteByUsuarioId(usuarioId);
		suscripcionRepository.deleteByUsuarioId(usuarioId);
		usuarioRepository.delete(usuario);
	}

	public void guardar(Usuario usuario) {
		usuarioRepository.save(usuario);
	}
}