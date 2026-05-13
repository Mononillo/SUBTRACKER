package com.subtracker.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.subtracker.model.Suscripcion;
import com.subtracker.model.Suscripcion.Confianza;
import com.subtracker.model.Suscripcion.EstadoSuscripcion;
import com.subtracker.model.Suscripcion.Frecuencia;
import com.subtracker.model.SuscripcionTransaccion;
import com.subtracker.model.Usuario;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.TransaccionService;
import com.subtracker.service.UsuarioService;

@Controller
@RequestMapping("/suscripciones")
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

	// ========================================
	// LISTAR SUSCRIPCIONES
	// ========================================
	@GetMapping
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

	// ========================================
	// DETALLE DE SUSCRIPCIÓN
	// ========================================
	@GetMapping("/{id}")
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

	// ========================================
	// CONFIRMAR SUSCRIPCIÓN (POTENCIAL → ACTIVA)
	// ========================================
	@PostMapping("/{id}/confirmar")
	public String confirmarSuscripcion(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			Suscripcion suscripcion = suscripcionService.obtenerSuscripcionPorId(id);

			if (suscripcion == null) {
				redirectAttributes.addFlashAttribute("error", "Suscripción no encontrada");
				return "redirect:/suscripciones";
			}

			suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
			suscripcion.setConfianza(Confianza.MUY_ALTA);
			suscripcionService.guardar(suscripcion);

			redirectAttributes.addFlashAttribute("success", "Suscripción confirmada como activa");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al confirmar: " + e.getMessage());
		}
		return "redirect:/suscripciones/" + id;
	}

	// ========================================
	// CANCELAR SUSCRIPCIÓN
	// ========================================
	@PostMapping("/{id}/cancelar")
	public String cancelar(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		Usuario usuario = new Usuario();
		if (usuarioOpt.isPresent()) {
			usuario = usuarioOpt.get();
		}

		suscripcionService.cancelarSuscripcion(id, usuario.getId());
		return "redirect:/suscripciones";
	}

	// ========================================
	// REACTIVAR SUSCRIPCIÓN (CANCELADA → ACTIVA)
	// ========================================
	@PostMapping("/{id}/reactivar")
	public String reactivarSuscripcion(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			Suscripcion suscripcion = suscripcionService.obtenerSuscripcionPorId(id);

			if (suscripcion == null) {
				redirectAttributes.addFlashAttribute("error", "Suscripción no encontrada");
				return "redirect:/suscripciones";
			}

			suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
			suscripcionService.guardar(suscripcion);

			redirectAttributes.addFlashAttribute("success", "Suscripción reactivada correctamente");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al reactivar: " + e.getMessage());
		}
		return "redirect:/suscripciones/" + id;
	}

	// ========================================
	// MOSTRAR FORMULARIO DE EDICIÓN
	// ========================================
	@GetMapping("/{id}/editar")
	public String mostrarFormularioEditar(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		if (usuarioOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		Usuario usuario = usuarioOpt.get();

		Suscripcion suscripcion = suscripcionService.obtenerSuscripcionPorId(id);

		if (suscripcion == null || !suscripcion.getUsuario().getId().equals(usuario.getId())) {
			return "redirect:/suscripciones";
		}

		model.addAttribute("usuario", usuario);
		model.addAttribute("suscripcion", suscripcion);
		return "layout/editar-suscripcion";
	}

	// ========================================
	// PROCESAR FORMULARIO DE EDICIÓN
	// ========================================
	@PostMapping("/{id}/editar")
	public String actualizarSuscripcion(@PathVariable String id, @RequestParam String nombreServicio,
			@RequestParam Double importe, @RequestParam String moneda, @RequestParam String frecuencia,
			@RequestParam(required = false) String patronComercio,
			@RequestParam(required = false) String proximaRenovacion,
			@RequestParam(required = false) String comercioNombre, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
		if (usuarioOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		Usuario usuario = usuarioOpt.get();

		try {
			Suscripcion suscripcion = suscripcionService.obtenerSuscripcionPorId(id);

			if (suscripcion == null || !suscripcion.getUsuario().getId().equals(usuario.getId())) {
				redirectAttributes.addFlashAttribute("error", "No tienes permiso para editar esta suscripción");
				return "redirect:/suscripciones";
			}

			// Validar datos obligatorios
			if (nombreServicio == null || nombreServicio.trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "El nombre del servicio es obligatorio");
				return "redirect:/suscripciones/" + id + "/editar";
			}

			if (importe == null || importe <= 0) {
				redirectAttributes.addFlashAttribute("error", "El importe debe ser mayor que 0");
				return "redirect:/suscripciones/" + id + "/editar";
			}

			// Actualizar campos
			suscripcion.setNombreServicio(nombreServicio.trim());
			suscripcion.setImporte(importe);
			suscripcion.setMoneda(moneda != null ? moneda : "EUR");
			suscripcion.setFrecuencia(Frecuencia.valueOf(frecuencia));
			suscripcion.setPatronComercio(patronComercio != null ? patronComercio.trim() : null);

			// Actualizar próxima renovación si se proporcionó
			if (proximaRenovacion != null && !proximaRenovacion.isEmpty()) {
				suscripcion.setProximaRenovacion(LocalDate.parse(proximaRenovacion));
			}

			suscripcionService.guardar(suscripcion);

			redirectAttributes.addFlashAttribute("success", "Suscripción actualizada correctamente");
			return "redirect:/suscripciones/" + id;

		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", "Frecuencia no válida");
			return "redirect:/suscripciones/" + id + "/editar";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
			return "redirect:/suscripciones/" + id + "/editar";
		}
	}
}