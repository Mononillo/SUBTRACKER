package com.subtracker.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.subtracker.model.Usuario;
import com.subtracker.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findByCorreo(email)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

		return User.builder().username(usuario.getCorreo()).password(usuario.getHashContrasena()).roles("USER").build();
	}
}