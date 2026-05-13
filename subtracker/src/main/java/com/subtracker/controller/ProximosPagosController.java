package com.subtracker.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.subtracker.dto.ProximoPagoDTO;
import com.subtracker.dto.SuscripcionDTO;
import com.subtracker.model.Usuario;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.UsuarioService;

@Controller
public class ProximosPagosController {
	private final SuscripcionService suscripcionService;
	private final UsuarioService usuarioService;

	public ProximosPagosController(SuscripcionService suscripcionService, UsuarioService usuarioService) {
		this.suscripcionService = suscripcionService;
		this.usuarioService = usuarioService;
	}

	@GetMapping("/proximos-pagos")
	public String proximosPagos(@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(defaultValue = "30") String periodo, Model model) {

		Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());

		Usuario usuario = new Usuario();
		if (usuarioOpt.isPresent()) {
			usuario = usuarioOpt.get();
		}

		LocalDate hoy = LocalDate.now();
		LocalDate hasta = periodo.equals("todo") ? LocalDate.of(9999, 12, 31) : hoy.plusDays(Long.parseLong(periodo));

		List<SuscripcionDTO> todas = suscripcionService.obtenerSuscripcionesActivasDTO(usuario.getId());

		List<ProximoPagoDTO> pagos = todas.stream().filter(s -> s.proximaRenovacion() != null)
				.filter(s -> !s.proximaRenovacion().isBefore(hoy)).filter(s -> !s.proximaRenovacion().isAfter(hasta))
				.sorted(Comparator.comparing(SuscripcionDTO::proximaRenovacion)).map(s -> new ProximoPagoDTO(s, hoy))
				.toList();

		Map<LocalDate, List<ProximoPagoDTO>> pagosPorFecha = pagos.stream().collect(Collectors
				.groupingBy(dto -> dto.getSuscripcion().proximaRenovacion(), TreeMap::new, Collectors.toList()));

		Map<LocalDate, String> totalesPorFecha = pagosPorFecha.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> "€" + String.format("%.2f",
						e.getValue().stream().mapToDouble(dto -> dto.getSuscripcion().importe().doubleValue()).sum())));

		model.addAttribute("pagosPorFecha", pagosPorFecha);
		model.addAttribute("totalesPorFecha", totalesPorFecha);
		model.addAttribute("pagos", pagos);
		model.addAttribute("periodo", periodo);
		model.addAttribute("hoy", hoy);
		model.addAttribute("gastoTotal",
				pagos.stream().mapToDouble(dto -> dto.getSuscripcion().importe().doubleValue()).sum());
		model.addAttribute("gastoPendiente7", calcularGastoEnDias(todas, hoy, 7));
		model.addAttribute("gastoPendiente30", calcularGastoEnDias(todas, hoy, 30));
		model.addAttribute("usuario", usuario);
		return "layout/proximos-pagos";
	}

	private double calcularGastoEnDias(List<SuscripcionDTO> suscripciones, LocalDate hoy, int dias) {
		LocalDate hasta = hoy.plusDays(dias);
		return suscripciones.stream().filter(s -> s.proximaRenovacion() != null)
				.filter(s -> !s.proximaRenovacion().isBefore(hoy)).filter(s -> !s.proximaRenovacion().isAfter(hasta))
				.mapToDouble(s -> s.importe().doubleValue()).sum();
	}
}
