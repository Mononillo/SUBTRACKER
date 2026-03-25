package com.subtracker.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.subtracker.dto.RegistroDTO;
import com.subtracker.model.Usuario;
import com.subtracker.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

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

}