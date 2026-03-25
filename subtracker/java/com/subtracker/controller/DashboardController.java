package com.subtracker.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.subtracker.model.Usuario;
import com.subtracker.service.ConexionBancariaService;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

	private final UsuarioService usuarioService;
	private final SuscripcionService suscripcionService;
	private final ConexionBancariaService conexionBancariaService;

	public DashboardController(SuscripcionService suscripcionService, UsuarioService usuarioService,
			ConexionBancariaService conexionBancariaService) {
		this.suscripcionService = suscripcionService;
		this.usuarioService = usuarioService;
		this.conexionBancariaService = conexionBancariaService;
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes attributes, HttpSession session) {

		if (userDetails == null || userDetails.getUsername() == null) {
			return "redirect:/auth/login";
		}

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		if (usuarioOpt.isEmpty()) {

			log.warn("Usuario autenticado no encontrado en BD: {}", userDetails.getUsername());
			return "redirect:/auth/login";
		}

		Usuario usuario = usuarioOpt.get();

		if (!conexionBancariaService.tieneConexionActiva(usuario)) {
			attributes.addFlashAttribute("usuarioId", usuario.getId());
			attributes.addFlashAttribute("usuarioNombre", usuario.getNombre());
			attributes.addFlashAttribute("mensaje", "Para usar todas las funciones, conecta tu cuenta bancaria");
			return "redirect:/auth/banking-auth";
		}

		Long usuarioId = usuario.getId();

		Double gastoMensual = suscripcionService.obtenerGastoMensual(usuarioId);
		Double gastoAnual = suscripcionService.obtenerGastoAnual(usuarioId);

		Map<String, Object> stats = new HashMap<>();
		stats.put("activas", suscripcionService.obtenerNumeroSuscripcionesActivas(usuarioId));
		stats.put("gastoMensual", String.format("%.2f", gastoMensual));
		stats.put("anual", String.format("%.2f", gastoAnual));
		stats.put("proximoPago", suscripcionService.obtenerProximoPago(usuarioId) + " días");

		model.addAttribute("stats", stats);
		model.addAttribute("suscripciones", suscripcionService.obtenerListaSuscripciones(usuarioId));
		model.addAttribute("titulo", "Dashboard");
		model.addAttribute("subtitulo", "Resumen de tus suscripciones activas");
		model.addAttribute("usuario", usuario);
		model.addAttribute("suscripciones", suscripcionService.obtenerListaSuscripciones(usuarioId));

		return "layout/dashboard";
	}
}