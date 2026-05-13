package com.subtracker.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.subtracker.dto.RegistroDTO;
import com.subtracker.model.ConexionBancaria;
import com.subtracker.model.CuentaBancaria;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Usuario;
import com.subtracker.service.ConexionBancariaService;
import com.subtracker.service.CuentaBancariaService;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final UsuarioService usuarioService;
	private final ConexionBancariaService conexionBancariaService;
	private final AuthenticationManager authenticationManager;
	private final SuscripcionService suscripcionService;
	private final CuentaBancariaService cuentaBancariaService;

	private final ExecutorService executor = Executors.newCachedThreadPool();

	// ===== LOGIN =====

	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}

	// ===== REGISTRO =====

	@GetMapping("/register")
	public String mostrarFormularioRegistro(Model model) {
		model.addAttribute("usuario", new RegistroDTO());
		return "auth/register";
	}

	@PostMapping("/register")
	public String registrar(@Valid @ModelAttribute("usuario") RegistroDTO registroDTO, BindingResult result,
			Model model, HttpServletRequest request) {

		if (result.hasErrors()) {
			return "auth/register";
		}

		try {
			usuarioService.registrarUsuario(registroDTO);

			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					registroDTO.getCorreo(), registroDTO.getPassword());

			Authentication authentication = authenticationManager.authenticate(authToken);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			HttpSession session = request.getSession(true);
			session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

			return "redirect:/auth/banking-auth";

		} catch (RuntimeException e) {
			log.warn("Error en registro para correo {}: {}", registroDTO.getCorreo(), e.getMessage());
			model.addAttribute("error", e.getMessage());
			return "auth/register";
		}
	}

	// ===== BANKING AUTH =====

	@GetMapping("/banking-auth")
	public String mostrarAutorizacionBancaria(Model model, @AuthenticationPrincipal UserDetails userDetails,
			HttpSession session) {

		if (userDetails == null || userDetails.getUsername() == null) {
			return "redirect:/auth/login";
		}

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		if (usuarioOpt.isEmpty()) {
			return "redirect:/auth/login";
		}

		Usuario usuario = usuarioOpt.get();

		if (!conexionBancariaService.tieneConexionActiva(usuario)) {
			return "auth/banking-auth";
		}

		Optional<ConexionBancaria> conexionOpt = conexionBancariaService.obtenerConexionPorUsuario(usuario);
		if (conexionOpt.isEmpty()) {
			log.warn("tieneConexionActiva=true pero no se encontró conexión para usuario: {}", usuario.getCorreo());
			return "auth/banking-auth";
		}

		CuentaBancaria cuenta = cuentaBancariaService.obtenerCuentaPorUsuarioId(usuario.getId());
		if (cuenta == null) {
			log.warn("No se encontró cuenta bancaria para usuario: {}", usuario.getCorreo());
			return "auth/banking-auth";
		}

		final String accountUid = cuenta.getUid();
		final CuentaBancaria cuentaFinal = cuenta;

		executor.submit(() -> {
			try {
				log.info("[ASYNC] Procesando suscripciones desde API para usuario: {}", usuario.getCorreo());
				List<Suscripcion> nuevas = suscripcionService.procesarSuscripcionesDesdeApi(accountUid, usuario,
						cuentaFinal);

				log.info("[ASYNC] Procesadas {} nuevas suscripciones", nuevas.size());
			} catch (Exception e) {
				log.error("[ASYNC] Error procesando suscripciones para usuario {}: {}", usuario.getCorreo(),
						e.getMessage(), e);
			}
		});

		return "redirect:/dashboard";
	}
}