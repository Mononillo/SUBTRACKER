package com.subtracker.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.subtracker.model.Suscripcion;
import com.subtracker.model.Suscripcion.EstadoSuscripcion;
import com.subtracker.model.SuscripcionTransaccion;
import com.subtracker.model.Usuario;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.TransaccionService;
import com.subtracker.service.UsuarioService;

@Controller
public class SuscripcionesController {
	private final SuscripcionService suscripcionService;
	private final UsuarioService usuarioService;
	private final TransaccionService transaccionService;

	public SuscripcionesController(SuscripcionService suscripcionService, UsuarioService usuarioService,
			TransaccionService transaccionService) {
		this.suscripcionService = suscripcionService;
		this.usuarioService = usuarioService;
		this.transaccionService = transaccionService;
	}

	@GetMapping("/suscripciones/{id}")
	public String detalle(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails, Model model) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		Usuario usuario = new Usuario();
		if (usuarioOpt.isPresent()) {
			usuario = usuarioOpt.get();
		}
		Suscripcion suscripcion = suscripcionService.obtenerSuscripcionPorId(id);

		if (suscripcion == null || !suscripcion.getUsuario().getId().equals(usuario.getId())) {
			return "redirect:/suscripciones";
		}

		List<SuscripcionTransaccion> transacciones = transaccionService.buscarTransaccionesPorSuscripcion(id);

		int iconColor = Math.abs(suscripcion.getNombreServicio().hashCode()) % 10;

		model.addAttribute("suscripcion", suscripcion);
		model.addAttribute("transacciones", transacciones);
		model.addAttribute("iconColor", iconColor);
		model.addAttribute("usuario", usuario);
		return "layout/suscripcion-detalle";
	}

	@PostMapping("/suscripciones/{id}/cancelar")
	public String cancelar(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		Usuario usuario = new Usuario();
		if (usuarioOpt.isPresent()) {
			usuario = usuarioOpt.get();
		}
		suscripcionService.cancelarSuscripcion(id, usuario.getId());
		return "redirect:/suscripciones";
	}

	@GetMapping("/suscripciones")
	public String listar(@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(defaultValue = "todas") String filtro, @RequestParam(defaultValue = "") String busqueda,
			Model model) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		Usuario usuario = new Usuario();
		if (usuarioOpt.isPresent()) {
			usuario = usuarioOpt.get();
		}

		List<Suscripcion> todas = suscripcionService.obtenerListaSuscripciones(usuario.getId());

		List<Suscripcion> filtradas = todas.stream()
				.filter(s -> filtro.equals("todas") || s.getEstado().name().equalsIgnoreCase(filtro))
				.filter(s -> busqueda.isEmpty() || s.getNombreServicio().toLowerCase().contains(busqueda.toLowerCase()))
				.toList();

		model.addAttribute("suscripciones", filtradas);
		model.addAttribute("filtroActivo", filtro);
		model.addAttribute("busqueda", busqueda);
		model.addAttribute("totalTodas", todas.size());
		model.addAttribute("totalActivas",
				todas.stream().filter(s -> s.getEstado() == EstadoSuscripcion.ACTIVA).count());
		model.addAttribute("totalPotenciales",
				todas.stream().filter(s -> s.getEstado() == EstadoSuscripcion.POTENCIAL).count());
		model.addAttribute("totalCanceladas",
				todas.stream().filter(s -> s.getEstado() == EstadoSuscripcion.CANCELADA).count());
		model.addAttribute("usuario", usuario);
		return "layout/suscripciones";
	}
}
