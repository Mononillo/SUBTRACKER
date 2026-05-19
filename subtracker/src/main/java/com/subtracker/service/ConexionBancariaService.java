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
					List<Suscripcion> nuevas = suscripcionService.procesarSuscripcionesDesdeApi(accountUid, usuario,
							cuentaFinal);
					suscripcionService.notificarActualizacion(usuario.getId());
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

		// Si la respuesta no contiene cuentas no se puede continuar
		if (!tokenResponse.has("accounts") || !tokenResponse.get("accounts").isArray()
				|| tokenResponse.get("accounts").size() == 0) {
			return null;
		}

		// Se toma solo la primera cuenta de la lista
		JsonNode cuentaNode = tokenResponse.get("accounts").get(0);
		String uid = cuentaNode.get("uid").asString();

		// Extrae el nombre del banco o usa un valor por defecto si no viene en la
		// respuesta
		String nombreBanco = tokenResponse.has("aspsp") && tokenResponse.get("aspsp").has("name")
				? tokenResponse.get("aspsp").get("name").asString()
				: "Mock ASPSP";

		Optional<CuentaBancaria> cuentaExistente = cuentaBancariaRepository.findByUid(uid);

		// Si la cuenta ya existe en BD solo actualiza el nombre del banco
		if (cuentaExistente.isPresent()) {
			CuentaBancaria cuenta = cuentaExistente.get();
			cuenta.setNombreBanco(nombreBanco);
			return cuentaBancariaRepository.save(cuenta);
		}

		// Si no existe la crea y la asocia al usuario de la conexion
		CuentaBancaria nueva = new CuentaBancaria();
		nueva.setUsuario(conexion.getUsuario());
		nueva.setUid(uid);
		nueva.setNombreBanco(nombreBanco);
		nueva.setFechaCreacion(LocalDateTime.now());
		return cuentaBancariaRepository.save(nueva);
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