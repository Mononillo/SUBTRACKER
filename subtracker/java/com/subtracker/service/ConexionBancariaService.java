package com.subtracker.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.subtracker.model.ConexionBancaria;
import com.subtracker.model.CuentaBancaria;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Usuario;
import com.subtracker.repository.ConexionBancariaRepository;
import com.subtracker.repository.CuentaBancariaRepository;
import com.subtracker.service.banking.EnableBankingRestClient;

import jakarta.transaction.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class ConexionBancariaService {

	private static final Logger log = LoggerFactory.getLogger(ConexionBancariaService.class);

	private final SuscripcionService suscripcionService;
	private final ConexionBancariaRepository conexionRepository;
	private final EnableBankingRestClient bankingClient;
	private final CuentaBancariaRepository cuentaBancariaRepository;

	private final ExecutorService executor = Executors.newCachedThreadPool();

	public ConexionBancariaService(ConexionBancariaRepository conexionRepository, EnableBankingRestClient bankingClient,
			CuentaBancariaRepository cuentaBancariaRepository, SuscripcionService suscripcionService) {
		this.conexionRepository = conexionRepository;
		this.bankingClient = bankingClient;
		this.cuentaBancariaRepository = cuentaBancariaRepository;
		this.suscripcionService = suscripcionService;
	}

	// ===== INICIAR CONEXIÓN =====

	@Transactional
	public String iniciarConexion(Usuario usuario, String redirectUri) throws Exception {
		ConexionBancaria conexion = conexionRepository.findByUsuario(usuario).map(existing -> {
			log.info("Conexión existente encontrada para usuario: {}", usuario.getCorreo());
			return existing;
		}).orElseGet(() -> {
			log.info("Creando nueva conexión para usuario: {}", usuario.getCorreo());
			ConexionBancaria nueva = new ConexionBancaria();
			nueva.setUsuario(usuario);
			return nueva;
		});

		String state = UUID.randomUUID().toString();
		conexion.setIdSesion(state);
		conexion.setFechaCreacion(LocalDateTime.now());
		conexion = conexionRepository.saveAndFlush(conexion);

		log.info("Conexión guardada con ID: {}, State: {}", conexion.getId(), state);

		JsonNode authResponse = bankingClient.iniciarAutenticacionMock(redirectUri, state);
		return authResponse.get("url").asString();
	}

	// ===== PROCESAR CALLBACK =====

	@Transactional
	public void procesarCallback(String code, String state) throws Exception {
		ConexionBancaria conexion = conexionRepository.findByIdSesion(state)
				.orElseThrow(() -> new IllegalArgumentException("No se encontró conexión con state: " + state));

		JsonNode tokenResponse = bankingClient.obtenerToken(code);

		if (!tokenResponse.has("session_id")) {
			log.error("No se recibió session_id en la respuesta del token");
			throw new IllegalStateException("La respuesta del banco no contiene session_id");
		}

		String sessionId = tokenResponse.get("session_id").asString();
		log.debug("Session ID recibido: {}", sessionId);
		conexion.setTokenAcceso(sessionId);
		conexion.setTokenRefresco(sessionId);

		// ===== PROCESAR CUENTA BANCARIA =====
		CuentaBancaria cuenta = procesarCuentaBancaria(tokenResponse, conexion);
		conexion.setCuentaBancaria(cuenta);

		conexion.setExpiraEn(LocalDateTime.now().plusDays(179));
		conexion.setFechaActualizacion(LocalDateTime.now());
		conexionRepository.save(conexion);

		// ===== LANZAR PROCESAMIENTO DE SUSCRIPCIONES EN SEGUNDO PLANO =====
		if (cuenta != null) {
			final String accountUid = cuenta.getUid();
			final CuentaBancaria cuentaFinal = cuenta;
			final Usuario usuario = conexion.getUsuario();

			executor.submit(() -> {
				try {
					log.info("[ASYNC] Iniciando procesamiento de suscripciones para cuenta: {}", accountUid);
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
					log.error("[ASYNC] Error procesando suscripciones para cuenta {}: {}", accountUid, e.getMessage(),
							e);
				}
			});
		} else {
			log.warn("No se inició el procesamiento de suscripciones: no se encontró cuenta bancaria");
		}
	}

	// ===== PROCESAR CUENTA BANCARIA =====

	private CuentaBancaria procesarCuentaBancaria(JsonNode tokenResponse, ConexionBancaria conexion) {
		if (!tokenResponse.has("accounts") || !tokenResponse.get("accounts").isArray()
				|| tokenResponse.get("accounts").size() == 0) {
			log.warn("No se encontraron cuentas en la respuesta del token");
			return null;
		}

		JsonNode cuentaNode = tokenResponse.get("accounts").get(0);
		String uid = cuentaNode.get("uid").asString();
		String nombreBanco = tokenResponse.has("aspsp") && tokenResponse.get("aspsp").has("name")
				? tokenResponse.get("aspsp").get("name").asString()
				: "Mock ASPSP";

		Optional<CuentaBancaria> cuentaExistente = cuentaBancariaRepository.findByUid(uid);

		if (cuentaExistente.isPresent()) {
			CuentaBancaria cuenta = cuentaExistente.get();
			log.info("Cuenta bancaria existente encontrada con UID: {}", uid);
			cuenta.setNombreBanco(nombreBanco);
			return cuentaBancariaRepository.save(cuenta);
		}

		CuentaBancaria nueva = new CuentaBancaria();
		nueva.setUsuario(conexion.getUsuario());
		nueva.setUid(uid);
		nueva.setNombreBanco(nombreBanco);
		nueva.setFechaCreacion(LocalDateTime.now());
		nueva = cuentaBancariaRepository.save(nueva);
		log.info("Nueva cuenta bancaria guardada con ID: {}, UID: {}", nueva.getId(), uid);
		return nueva;
	}

	// ===== CONSULTAS =====

	public Optional<ConexionBancaria> obtenerConexionPorUsuario(Usuario usuario) {
		return conexionRepository.findTopByUsuarioOrderByFechaCreacionDesc(usuario);
	}

	public boolean tieneConexionActiva(Usuario usuario) {
		return conexionRepository.findTopByUsuarioOrderByFechaCreacionDesc(usuario).map(c -> c.getTokenAcceso() != null)
				.orElse(false);
	}
}