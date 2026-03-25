package com.subtracker.controller;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.subtracker.model.Usuario;
import com.subtracker.service.ConexionBancariaService;
import com.subtracker.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api/banking")
public class BankingController {

	private final UsuarioService usuarioService;
	private final ConexionBancariaService conexionService;

	public BankingController(UsuarioService usuarioService, ConexionBancariaService conexionService) {
		this.usuarioService = usuarioService;
		this.conexionService = conexionService;
	}

	@GetMapping("/conectar")
	public String conectarBanco(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes attributes) {
		try {
			if (userDetails == null || userDetails.getUsername() == null) {
				throw new Exception("Usuario no autenticado");
			}

			Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());

			if (usuarioOpt.isEmpty()) {
				throw new Exception("No se encontró el usuario en la base de datos");
			}

			Usuario usuario = usuarioOpt.get();

			String callbackUrl = "http://localhost:8080/api/banking/callback";
			String authUrl = conexionService.iniciarConexion(usuario, callbackUrl);

			Thread.sleep(100);

			return "redirect:" + authUrl;

		} catch (Exception e) {
			e.printStackTrace();
			attributes.addFlashAttribute("error", "Error al conectar: " + e.getMessage());
			return "redirect:/auth/banking-auth";
		}
	}

	@GetMapping("/callback")
	public String callback(@RequestParam(required = false) String code, @RequestParam(required = false) String state,
			@RequestParam(required = false) String error, RedirectAttributes attributes, HttpServletRequest request) {

		if (error != null) {
			attributes.addFlashAttribute("error", "Autorización cancelada o fallida");
			return "redirect:/auth/banking-auth";
		}

		if (code == null || state == null) {
			attributes.addFlashAttribute("error", "Faltan parámetros requeridos");
			return "redirect:/auth/banking-auth";
		}

		try {
			conexionService.procesarCallback(code, state);
			attributes.addFlashAttribute("conexion", "exitosa");
			return "redirect:/dashboard";

		} catch (Exception e) {
			e.printStackTrace();
			attributes.addFlashAttribute("error", "Error al procesar callback: " + e.getMessage());
			return "redirect:/auth/banking-auth";
		}
	}
}