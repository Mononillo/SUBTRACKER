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
				String json = """
												{
						"transactions": [
						{
						  "entry_reference": "nflx-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "17.99" },
						  "creditor": { "name": "NETFLIX INTERNATIONAL BV" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-15",
						  "value_date": "2026-01-15",
						  "remittance_information": ["NETFLIX PREMIUM ENE 2026"]
						},
						{
						  "entry_reference": "nflx-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "17.99" },
						  "creditor": { "name": "NETFLIX INTERNATIONAL BV" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-15",
						  "value_date": "2026-02-15",
						  "remittance_information": ["NETFLIX PREMIUM FEB 2026"]
						},
						{
						  "entry_reference": "nflx-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "17.99" },
						  "creditor": { "name": "NETFLIX INTERNATIONAL BV" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-15",
						  "value_date": "2026-03-15",
						  "remittance_information": ["NETFLIX PREMIUM MAR 2026"]
						},
						{
						  "entry_reference": "spfy-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "9.99" },
						  "creditor": { "name": "SPOTIFY AB" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-05",
						  "value_date": "2026-01-05",
						  "remittance_information": ["SPOTIFY PREMIUM ENE"]
						},
						{
						  "entry_reference": "spfy-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "9.99" },
						  "creditor": { "name": "SPOTIFY AB" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-05",
						  "value_date": "2026-02-05",
						  "remittance_information": ["SPOTIFY PREMIUM FEB"]
						},
						{
						  "entry_reference": "spfy-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "9.99" },
						  "creditor": { "name": "SPOTIFY AB" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-05",
						  "value_date": "2026-03-05",
						  "remittance_information": ["SPOTIFY PREMIUM MAR"]
						},
						{
						  "entry_reference": "disn-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "8.99" },
						  "creditor": { "name": "DISNEY PLUS" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-10",
						  "value_date": "2026-01-10",
						  "remittance_information": ["DISNEY+ ENE"]
						},
						{
						  "entry_reference": "disn-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "8.99" },
						  "creditor": { "name": "DISNEY PLUS" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-10",
						  "value_date": "2026-02-10",
						  "remittance_information": ["DISNEY+ FEB"]
						},
						{
						  "entry_reference": "disn-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "8.99" },
						  "creditor": { "name": "DISNEY PLUS" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-10",
						  "value_date": "2026-03-10",
						  "remittance_information": ["DISNEY+ MAR"]
						},
						{
						  "entry_reference": "hbo-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "9.99" },
						  "creditor": { "name": "HBO MAX EMEA" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-18",
						  "value_date": "2026-01-18",
						  "remittance_information": ["HBO MAX ENE"]
						},
						{
						  "entry_reference": "hbo-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "9.99" },
						  "creditor": { "name": "HBO MAX EMEA" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-18",
						  "value_date": "2026-02-18",
						  "remittance_information": ["HBO MAX FEB"]
						},
						{
						  "entry_reference": "hbo-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "9.99" },
						  "creditor": { "name": "HBO MAX EMEA" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-18",
						  "value_date": "2026-03-18",
						  "remittance_information": ["HBO MAX MAR"]
						},
						{
						  "entry_reference": "dazn-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "29.99" },
						  "creditor": { "name": "DAZN LIMITED" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-22",
						  "value_date": "2026-01-22",
						  "remittance_information": ["DAZN DEPORTES ENE"]
						},
						{
						  "entry_reference": "dazn-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "29.99" },
						  "creditor": { "name": "DAZN LIMITED" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-22",
						  "value_date": "2026-02-22",
						  "remittance_information": ["DAZN DEPORTES FEB"]
						},
						{
						  "entry_reference": "dazn-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "29.99" },
						  "creditor": { "name": "DAZN LIMITED" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-22",
						  "value_date": "2026-03-22",
						  "remittance_information": ["DAZN DEPORTES MAR"]
						},
						{
						  "entry_reference": "amzn-2025",
						  "transaction_amount": { "currency": "EUR", "amount": "49.90" },
						  "creditor": { "name": "AMAZON PRIME VIDEO" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2025-03-10",
						  "value_date": "2025-03-10",
						  "remittance_information": ["AMAZON PRIME ANUAL 2025"]
						},
						{
						  "entry_reference": "amzn-2026",
						  "transaction_amount": { "currency": "EUR", "amount": "49.90" },
						  "creditor": { "name": "AMAZON PRIME VIDEO" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-10",
						  "value_date": "2026-03-10",
						  "remittance_information": ["AMAZON PRIME ANUAL 2026"]
						},
						{
						  "entry_reference": "msft-2025",
						  "transaction_amount": { "currency": "EUR", "amount": "69.00" },
						  "creditor": { "name": "MICROSOFT 365" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2025-01-25",
						  "value_date": "2025-01-25",
						  "remittance_information": ["MICROSOFT 365 ANUAL 2025"]
						},
						{
						  "entry_reference": "msft-2026",
						  "transaction_amount": { "currency": "EUR", "amount": "69.00" },
						  "creditor": { "name": "MICROSOFT 365" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-25",
						  "value_date": "2026-01-25",
						  "remittance_information": ["MICROSOFT 365 ANUAL 2026"]
						},
						{
						  "entry_reference": "adbe-2025",
						  "transaction_amount": { "currency": "EUR", "amount": "60.49" },
						  "creditor": { "name": "ADOBE CREATIVE CLOUD" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2025-02-20",
						  "value_date": "2025-02-20",
						  "remittance_information": ["ADOBE CC ANUAL 2025"]
						},
						{
						  "entry_reference": "adbe-2026",
						  "transaction_amount": { "currency": "EUR", "amount": "60.49" },
						  "creditor": { "name": "ADOBE CREATIVE CLOUD" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-20",
						  "value_date": "2026-02-20",
						  "remittance_information": ["ADOBE CC ANUAL 2026"]
						},
						{
						  "entry_reference": "drop-2025",
						  "transaction_amount": { "currency": "EUR", "amount": "119.88" },
						  "creditor": { "name": "DROPBOX" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2025-01-15",
						  "value_date": "2025-01-15",
						  "remittance_information": ["DROPBOX PLUS ANUAL 2025"]
						},
						{
						  "entry_reference": "drop-2026",
						  "transaction_amount": { "currency": "EUR", "amount": "119.88" },
						  "creditor": { "name": "DROPBOX" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-15",
						  "value_date": "2026-01-15",
						  "remittance_information": ["DROPBOX PLUS ANUAL 2026"]
						},
						{
						  "entry_reference": "fit-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "39.90" },
						  "creditor": { "name": "ALTAFIT" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-05",
						  "value_date": "2026-01-05",
						  "remittance_information": ["ALTAFIT TRIMESTRAL ENE"]
						},
						{
						  "entry_reference": "fit-2026-04",
						  "transaction_amount": { "currency": "EUR", "amount": "39.90" },
						  "creditor": { "name": "ALTAFIT" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-04-05",
						  "value_date": "2026-04-05",
						  "remittance_information": ["ALTAFIT TRIMESTRAL ABR"]
						},
						{
						  "entry_reference": "fit-2026-07",
						  "transaction_amount": { "currency": "EUR", "amount": "39.90" },
						  "creditor": { "name": "ALTAFIT" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-07-05",
						  "value_date": "2026-07-05",
						  "remittance_information": ["ALTAFIT TRIMESTRAL JUL"]
						},
						{
						  "entry_reference": "rev-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "12.50" },
						  "creditor": { "name": "REVISTA DIGITAL" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-01",
						  "value_date": "2026-02-01",
						  "remittance_information": ["REVISTA BIMESTRAL FEB"]
						},
						{
						  "entry_reference": "rev-2026-04",
						  "transaction_amount": { "currency": "EUR", "amount": "12.50" },
						  "creditor": { "name": "REVISTA DIGITAL SL" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-04-01",
						  "value_date": "2026-04-01",
						  "remittance_information": ["REVISTA BIMESTRAL ABR"]
						},
						{
						  "entry_reference": "rev-2026-06",
						  "transaction_amount": { "currency": "EUR", "amount": "12.50" },
						  "creditor": { "name": "REVISTA DIGITAL" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-06-01",
						  "value_date": "2026-06-01",
						  "remittance_information": ["REVISTA BIMESTRAL JUN"]
						},
						{
						  "entry_reference": "seg-2026-01",
						  "transaction_amount": { "currency": "EUR", "amount": "35.00" },
						  "creditor": { "name": "SEGUROS CASER" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-01-20",
						  "value_date": "2026-01-20",
						  "remittance_information": ["SEGURO MOVIL SEMESTRAL ENE"]
						},
						{
						  "entry_reference": "seg-2026-07",
						  "transaction_amount": { "currency": "EUR", "amount": "35.00" },
						  "creditor": { "name": "SEGUROS CASER" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-07-20",
						  "value_date": "2026-07-20",
						  "remittance_information": ["SEGURO MOVIL SEMESTRAL JUL"]
						},
						{
						  "entry_reference": "apptv-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "6.99" },
						  "creditor": { "name": "APPLE TV+" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-20",
						  "value_date": "2026-03-20",
						  "remittance_information": ["APPLE TV+ MAR 2026"]
						},
						{
						  "entry_reference": "amzn-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "10.99" },
						  "creditor": { "name": "AMAZON PRIME VIDEO" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-25",
						  "value_date": "2026-03-25",
						  "remittance_information": ["PRIME VIDEO MAR"]
						},
						{
						  "entry_reference": "zara-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "89.95" },
						  "creditor": { "name": "ZARA ESPAÑA SA" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-12",
						  "value_date": "2026-03-12",
						  "remittance_information": ["COMPRA ZARA GRAN VÍA"]
						},
						{
						  "entry_reference": "merc-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "87.32" },
						  "creditor": { "name": "MERCADONA" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-07",
						  "value_date": "2026-03-07",
						  "remittance_information": ["COMPRA SEMANAL MERCADONA"]
						},
						{
						  "entry_reference": "elc-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "65.30" },
						  "creditor": { "name": "EL CORTE INGLES" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-05",
						  "value_date": "2026-03-05",
						  "remittance_information": ["PERFUMERIA EL CORTE INGLÉS"]
						},
						{
						  "entry_reference": "fnac-2026-02",
						  "transaction_amount": { "currency": "EUR", "amount": "34.99" },
						  "creditor": { "name": "FNAC" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-02-20",
						  "value_date": "2026-02-20",
						  "remittance_information": ["LIBRO FNAC"]
						},
						{
						  "entry_reference": "medi-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "1299.00" },
						  "creditor": { "name": "MEDIA MARKT" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-08",
						  "value_date": "2026-03-08",
						  "remittance_information": ["PORTÁTIL ASUS"]
						},
						{
						  "entry_reference": "decath-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "156.75" },
						  "creditor": { "name": "DECATHLON" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-03",
						  "value_date": "2026-03-03",
						  "remittance_information": ["BICICLETA MONTAÑA"]
						},
						{
						  "entry_reference": "repsol-2026-03",
						  "transaction_amount": { "currency": "EUR", "amount": "65.40" },
						  "creditor": { "name": "REPSOL" },
						  "credit_debit_indicator": "DBIT",
						  "status": "BOOK",
						  "booking_date": "2026-03-04",
						  "value_date": "2026-03-04",
						  "remittance_information": ["GASOLINA REPSOL"]
						}
						]
						}
												""";
				List<Suscripcion> nuevas = suscripcionService.procesarSuscripcionesDesdeJson(json, usuario,
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