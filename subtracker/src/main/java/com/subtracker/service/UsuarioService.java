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
				.hashContrasena(passwordEncoder.encode(dto.getPassword())).nombre(dto.getNombre()).build();

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

	@Transactional
	public void eliminarUsuario(Long usuarioId) {

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		// 1. PRIMERO: Eliminar relaciones suscripción-transacción
		suscripcionTransaccionRepository.deleteBySuscripcionUsuarioId(usuarioId);

		// 2. SEGUNDO: Eliminar transacciones
		transaccionRepository.deleteByCuentaBancariaUsuarioId(usuarioId);

		// 3. TERCERO: Eliminar cuentas bancarias
		cuentaBancariaRepository.deleteByUsuarioId(usuarioId);

		// 4. CUARTO: Eliminar suscripciones
		suscripcionRepository.deleteByUsuarioId(usuarioId);

		// 5. QUINTO: Finalmente eliminar el usuario
		usuarioRepository.delete(usuario);
	}

	public void guardar(Usuario usuario) {
		usuarioRepository.save(usuario);
	}

}