package com.subtracker.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.subtracker.config.BankingProperties;
import com.subtracker.dto.SuscripcionDTO;
import com.subtracker.model.Comercio;
import com.subtracker.model.CuentaBancaria;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Suscripcion.Confianza;
import com.subtracker.model.Suscripcion.EstadoSuscripcion;
import com.subtracker.model.Suscripcion.Frecuencia;
import com.subtracker.model.SuscripcionTransaccion;
import com.subtracker.model.Transaccion;
import com.subtracker.model.Usuario;
import com.subtracker.repository.SuscripcionRepository;
import com.subtracker.repository.SuscripcionTransaccionRepository;
import com.subtracker.repository.TransaccionRepository;
import com.subtracker.service.banking.JwtGenerator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class SuscripcionService {

	private static final Logger log = LoggerFactory.getLogger(SuscripcionService.class);

	private final IaNormalizacionService iaNormalizacionService;
	private final ComercioService comercioService;
	private final BankingProperties bankingProperties;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final JwtGenerator jwtGenerator;
	private final SuscripcionRepository suscripcionRepository;
	private final TransaccionRepository transaccionRepository;
	private final SuscripcionTransaccionRepository suscripcionTransaccionRepository;

	public SuscripcionService(BankingProperties bankingProperties, ObjectMapper objectMapper,
			IaNormalizacionService iaNormalizacionService, ComercioService comercioService, JwtGenerator jwtGenerator,
			SuscripcionRepository suscripcionRepository, TransaccionRepository transaccionRepository,
			SuscripcionTransaccionRepository suscripcionTransaccionRepository) {
		this.bankingProperties = bankingProperties;
		this.httpClient = HttpClient.newHttpClient();
		this.objectMapper = objectMapper;
		this.comercioService = comercioService;
		this.iaNormalizacionService = iaNormalizacionService;
		this.jwtGenerator = jwtGenerator;
		this.suscripcionRepository = suscripcionRepository;
		this.suscripcionTransaccionRepository = suscripcionTransaccionRepository;
		this.transaccionRepository = transaccionRepository;
	}

	// ===== CONSULTAS BÁSICAS =====

	public Long obtenerNumeroSuscripcionesActivas(Long usuarioId) {
		Objects.requireNonNull(usuarioId, "El id de usuario no puede ser nulo");
		return suscripcionRepository.countByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
	}

	public List<Suscripcion> obtenerListaSuscripciones(Long usuarioId) {
		Objects.requireNonNull(usuarioId, "El id de usuario no puede ser nulo");
		return suscripcionRepository.findByUsuarioId(usuarioId);
	}

	public Double obtenerGastoMensual(Long usuarioId) {
		Objects.requireNonNull(usuarioId, "El id de usuario no puede ser nulo");
		return suscripcionRepository.sumImporteMesActual(usuarioId);
	}

	public Double obtenerGastoAnual(Long usuarioId) {
		Objects.requireNonNull(usuarioId, "El id de usuario no puede ser nulo");
		return suscripcionRepository.sumGastoAnual(usuarioId);
	}

	public Long obtenerProximoPago(Long usuarioId) {
		return suscripcionRepository.findProximoPago(usuarioId).map(fechaPago -> {
			LocalDate fechaPagoLocal = fechaPago.toLocalDate();
			LocalDate hoy = LocalDate.now();
			return ChronoUnit.DAYS.between(hoy, fechaPagoLocal);
		}).orElse(0L);
	}

	// ===== MÉTODOS PARA OBTENER TRANSACCIONES =====

	public List<JsonNode> obtenerTransaccionesDesdeApi(String accountUid) throws Exception {
		List<JsonNode> todasLasTransacciones = new ArrayList<>();
		String continuationKey = null;
		int pagina = 1;

		do {
			String url = bankingProperties.getBaseUrl() + "/accounts/" + accountUid + "/transactions";
			if (continuationKey != null) {
				url += "?continuation_key=" + continuationKey;
			}

			log.debug("Solicitando página {}...", pagina);
			JsonNode respuesta = obtenerPaginaTransacciones(url);

			JsonNode transaccionesPagina = respuesta.get("transactions");
			if (transaccionesPagina != null && transaccionesPagina.isArray()) {
				StreamSupport.stream(transaccionesPagina.spliterator(), false).forEach(todasLasTransacciones::add);
				log.debug("Página {}: {} transacciones", pagina, transaccionesPagina.size());
			}

			continuationKey = respuesta.has("continuation_key") ? respuesta.get("continuation_key").asString() : null;
			pagina++;

		} while (continuationKey != null && !continuationKey.isEmpty());

		log.info("TOTAL: {} transacciones recuperadas desde API", todasLasTransacciones.size());
		return todasLasTransacciones;
	}

	public List<JsonNode> obtenerTransaccionesDesdeJson(String jsonString) throws Exception {
		List<JsonNode> transacciones = new ArrayList<>();

		JsonNode rootNode = objectMapper.readTree(jsonString);
		JsonNode transaccionesNode = rootNode.get("transactions");

		if (transaccionesNode == null || !transaccionesNode.isArray()) {
			throw new IllegalArgumentException("El JSON debe contener un array 'transactions'");
		}

		transaccionesNode.forEach(transacciones::add);
		log.info("Cargadas {} transacciones desde JSON", transacciones.size());

		return transacciones;
	}

	private JsonNode obtenerPaginaTransacciones(String url) throws Exception {
		String jwt = jwtGenerator.generarJWT();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bearer " + jwt)
				.header("Accept", "application/json").GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() >= 400) {
			throw new RuntimeException("Error HTTP " + response.statusCode() + ": " + response.body());
		}

		return objectMapper.readTree(response.body());
	}

	// ===== MÉTODOS PRINCIPALES PARA PROCESAR SUSCRIPCIONES =====

	public synchronized List<Suscripcion> procesarSuscripcionesDesdeApi(String accountId, Usuario usuario,
			CuentaBancaria cuenta) {
		try {
			List<JsonNode> transacciones = obtenerTransaccionesDesdeApi(accountId);
			return procesarTransacciones(transacciones, usuario, cuenta);
		} catch (Exception e) {
			log.error("Error al procesar suscripciones desde API para cuenta {}: {}", accountId, e.getMessage(), e);
			throw new RuntimeException("Error al procesar suscripciones desde API: " + accountId, e);
		}
	}

	public synchronized List<Suscripcion> procesarSuscripcionesDesdeJson(String jsonString, Usuario usuario,
			CuentaBancaria cuenta) {
		try {
			List<JsonNode> transacciones = obtenerTransaccionesDesdeJson(jsonString);
			return procesarTransacciones(transacciones, usuario, cuenta);
		} catch (Exception e) {
			log.error("Error al procesar suscripciones desde JSON: {}", e.getMessage(), e);
			throw new RuntimeException("Error al procesar suscripciones desde JSON", e);
		}
	}

	private List<Suscripcion> procesarTransacciones(List<JsonNode> transacciones, Usuario usuario,
			CuentaBancaria cuenta) throws Exception {
		List<Suscripcion> nuevasSuscripciones = new ArrayList<>();

		Map<String, JsonNode> transaccionesUnicas = new HashMap<>();
		for (JsonNode tx : transacciones) {
			if (tx.has("entry_reference")) {
				transaccionesUnicas.putIfAbsent(tx.get("entry_reference").asString(), tx);
			} else {
				log.warn("Transacción sin entry_reference, se ignorará: {}", tx);
			}
		}

		if (transaccionesUnicas.size() < transacciones.size()) {
			log.info("Se eliminaron {} transacciones duplicadas", transacciones.size() - transaccionesUnicas.size());
		}

		Map<String, List<JsonNode>> transaccionesPorComercio = agruparTransaccionesPorComercio(
				new ArrayList<>(transaccionesUnicas.values()));

		for (Map.Entry<String, List<JsonNode>> entry : transaccionesPorComercio.entrySet()) {
			String nombreComercio = entry.getKey();
			List<JsonNode> transaccionesComercio = entry.getValue();

			transaccionesComercio.sort(Comparator.comparing(tx -> tx.get("booking_date").asString()));

			Suscripcion nuevaSuscripcion = analizarComercio(transaccionesComercio, nombreComercio, usuario, cuenta);

			if (nuevaSuscripcion != null) {
				nuevasSuscripciones.add(nuevaSuscripcion);
			}
		}

		log.info("Proceso completado. {} nuevas suscripciones insertadas", nuevasSuscripciones.size());
		return nuevasSuscripciones;
	}

	private Map<String, List<JsonNode>> agruparTransaccionesPorComercio(List<JsonNode> transacciones) {
		Map<String, List<JsonNode>> agrupadas = new HashMap<>();
		for (JsonNode tx : transacciones) {
			if (!tx.has("credit_debit_indicator") || !tx.get("credit_debit_indicator").asString().equals("DBIT")) {
				continue;
			}
			if (!tx.has("creditor") || !tx.get("creditor").has("name")) {
				continue;
			}
			String nombreOriginal = tx.get("creditor").get("name").asString();
			String nombreNormalizado = iaNormalizacionService.normalizarComercio(nombreOriginal);
			agrupadas.computeIfAbsent(nombreNormalizado, k -> new ArrayList<>()).add(tx);
		}
		return agrupadas;
	}

	// ===== ANALIZAR COMERCIO =====

	private Suscripcion analizarComercio(List<JsonNode> transacciones, String nombreComercio, Usuario usuario,
			CuentaBancaria cuenta) throws Exception {

		if (!esSuscripcion(transacciones)) {
			return null;
		}

		Confianza nivelConfianza = calcularNivelConfianza(transacciones);

		Optional<Comercio> comercioOpt = comercioService.buscarComercioPorNombre(nombreComercio);
		Comercio comercio;

		if (comercioOpt.isPresent()) {
			comercio = comercioOpt.get();
			nivelConfianza = aumentarConfianzaPorComercioExistente(nivelConfianza);
		} else {
			comercio = Comercio.builder().nombre(nombreComercio).patron(nombreComercio)
					.fechaCreacion(LocalDateTime.now()).build();
			comercio = comercioService.guardar(comercio);
		}

		Optional<Suscripcion> suscripcionExistente = suscripcionRepository.findByUsuarioAndComercio(usuario, comercio);

		if (suscripcionExistente.isPresent()) {
			actualizarSuscripcion(suscripcionExistente.get(), transacciones);
			return null;
		}

		return crearSuscripcion(comercio, transacciones, usuario, cuenta, nivelConfianza);
	}

	// ===== DETECCIÓN DE SUSCRIPCIÓN =====

	private boolean esSuscripcion(List<JsonNode> transacciones) throws Exception {
		if (transacciones.isEmpty()) {
			return false;
		}
		if (transacciones.size() < 2) {
			return iaNormalizacionService.esSuscripcion(transacciones.get(0));
		}

		double primerMonto = transacciones.get(0).get("transaction_amount").get("amount").asDouble();
		if (primerMonto == 0.0) {
			return false;
		}

		for (JsonNode tx : transacciones) {
			double monto = tx.get("transaction_amount").get("amount").asDouble();
			double variacion = Math.abs(monto - primerMonto) / primerMonto * 100;
			if (variacion > 5.0) {
				return false;
			}
		}
		return true;
	}

	// ===== CALCULAR NIVEL DE CONFIANZA =====

	private Confianza calcularNivelConfianza(List<JsonNode> transacciones) {
		if (transacciones.size() < 2) {
			return Confianza.MUY_BAJA;
		}

		double puntuacion = 0.0;

		if (transacciones.size() >= 6)
			puntuacion += 20;
		else if (transacciones.size() >= 4)
			puntuacion += 16;
		else
			puntuacion += 10;

		double primerMonto = transacciones.get(0).get("transaction_amount").get("amount").asDouble();
		if (primerMonto != 0.0) {
			double variacionMaxima = transacciones.stream()
					.mapToDouble(tx -> tx.get("transaction_amount").get("amount").asDouble())
					.map(monto -> Math.abs(monto - primerMonto) / primerMonto * 100).max().orElse(0.0);

			if (variacionMaxima < 1.0)
				puntuacion += 25;
			else if (variacionMaxima < 3.0)
				puntuacion += 20;
			else if (variacionMaxima < 5.0)
				puntuacion += 12;
		}

		long diasPromedio = calcularDiasPromedio(transacciones);
		puntuacion += calcularPuntuacionPorPeriodicidad(diasPromedio);

		return convertirPuntuacionAConfianza(puntuacion);
	}

	// ===== FRECUENCIA =====

	private Frecuencia detectarFrecuencia(long diasPromedio) {
		if (diasPromedio >= 25 && diasPromedio <= 35)
			return Frecuencia.MENSUAL;
		if (diasPromedio >= 55 && diasPromedio <= 65)
			return Frecuencia.BIMESTRAL;
		if (diasPromedio >= 85 && diasPromedio <= 95)
			return Frecuencia.TRIMESTRAL;
		if (diasPromedio >= 115 && diasPromedio <= 125)
			return Frecuencia.CUATRIMESTRAL;
		if (diasPromedio >= 175 && diasPromedio <= 185)
			return Frecuencia.SEMESTRAL;
		if (diasPromedio >= 355 && diasPromedio <= 375)
			return Frecuencia.ANUAL;
		return Frecuencia.DESCONOCIDA;
	}

	private double calcularPuntuacionPorPeriodicidad(long diasPromedio) {
		Frecuencia f = detectarFrecuencia(diasPromedio);
		if (f == Frecuencia.DESCONOCIDA)
			return 0;

		long[] centros = { 30, 60, 90, 120, 180, 365 };
		for (long centro : centros) {
			if (Math.abs(diasPromedio - centro) <= 3)
				return 35;
		}
		return 28;
	}

	private Confianza convertirPuntuacionAConfianza(double puntuacion) {
		System.out.println(puntuacion);
		if (puntuacion >= 90)
			return Confianza.MUY_ALTA;
		if (puntuacion >= 75)
			return Confianza.ALTA;
		if (puntuacion >= 50)
			return Confianza.MEDIA;
		if (puntuacion >= 25)
			return Confianza.BAJA;
		return Confianza.MUY_BAJA;
	}

	private Confianza aumentarConfianzaPorComercioExistente(Confianza confianzaActual) {
		switch (confianzaActual) {
		case MUY_BAJA:
			return Confianza.BAJA;
		case BAJA:
			return Confianza.MEDIA;
		case MEDIA:
			return Confianza.ALTA;
		case ALTA:
		case MUY_ALTA:
		default:
			return Confianza.MUY_ALTA;
		}
	}

	// ===== CALCULAR DÍAS PROMEDIO =====

	private long calcularDiasPromedio(List<JsonNode> transacciones) {
		if (transacciones.size() < 2)
			return 0;

		long totalDias = 0;
		for (int i = 1; i < transacciones.size(); i++) {
			LocalDate fecha1 = LocalDate.parse(transacciones.get(i - 1).get("booking_date").asString());
			LocalDate fecha2 = LocalDate.parse(transacciones.get(i).get("booking_date").asString());
			totalDias += ChronoUnit.DAYS.between(fecha1, fecha2);
		}
		return Math.round((double) totalDias / (transacciones.size() - 1));
	}

	// ===== CREAR SUSCRIPCIÓN =====

	private Suscripcion crearSuscripcion(Comercio comercio, List<JsonNode> transaccionesJson, Usuario usuario,
			CuentaBancaria cuenta, Confianza nivelConfianza) {

		JsonNode primeraTx = transaccionesJson.get(0);
		JsonNode ultimaTx = transaccionesJson.get(transaccionesJson.size() - 1);

		Frecuencia frecuencia;
		LocalDate proximaRenovacion;

		if (transaccionesJson.size() > 1) {
			long diasPromedio = calcularDiasPromedio(transaccionesJson);
			frecuencia = detectarFrecuencia(diasPromedio);
			proximaRenovacion = calcularProximaRenovacion(ultimaTx, frecuencia, transaccionesJson);
		} else {
			frecuencia = Frecuencia.DESCONOCIDA;
			proximaRenovacion = LocalDate.parse(ultimaTx.get("booking_date").asString()).plusMonths(1);
		}

		double importe = ultimaTx.get("transaction_amount").get("amount").asDouble();
		String moneda = ultimaTx.get("transaction_amount").get("currency").asString();
		String descripcion = ultimaTx.get("creditor").get("name").asString();
		LocalDate fechaInicio = LocalDate.parse(primeraTx.get("booking_date").asString());

		String id = descripcion + frecuencia + fechaInicio;

		EstadoSuscripcion estado;
		if (transaccionesJson.size() == 1) {
			estado = EstadoSuscripcion.POTENCIAL;
		} else if (proximaRenovacion != null && proximaRenovacion.isBefore(LocalDate.now())) {
			estado = EstadoSuscripcion.CANCELADA;
		} else {
			estado = EstadoSuscripcion.ACTIVA;
		}

		Suscripcion suscripcion = Suscripcion.builder().id(id).comercio(comercio).usuario(usuario).importe(importe)
				.fechaInicio(fechaInicio).proximaRenovacion(proximaRenovacion).nombreServicio(comercio.getNombre())
				.frecuencia(frecuencia).confianza(nivelConfianza).estado(estado).patronComercio(descripcion)
				.moneda(moneda).fechaCreacion(LocalDateTime.now()).build();

		suscripcion = suscripcionRepository.save(suscripcion);

		List<SuscripcionTransaccion> relaciones = new ArrayList<>();

		for (JsonNode txJson : transaccionesJson) {
			Transaccion transaccion = crearTransaccionDesdeJson(txJson, cuenta, comercio);

			Optional<Transaccion> existente = transaccionRepository.findByIdExterno(transaccion.getIdExterno());
			Transaccion transaccionGuardada;

			if (existente.isPresent()) {
				transaccionGuardada = existente.get();
				log.debug("Transacción ya existente: {}", transaccion.getIdExterno());
			} else {
				transaccionGuardada = transaccionRepository.save(transaccion);
				log.debug("Nueva transacción guardada: {}", transaccion.getIdExterno());
			}

			SuscripcionTransaccion relacion = new SuscripcionTransaccion();
			relacion.setSuscripcion(suscripcion);
			relacion.setTransaccion(transaccionGuardada);
			relacion.setId(new SuscripcionTransaccion.SuscripcionTransaccionId(suscripcion.getId(),
					transaccionGuardada.getId()));

			relaciones.add(relacion);
		}

		suscripcionTransaccionRepository.saveAll(relaciones);

		log.debug("Suscripción creada con ID: {}, asociada a {} transacciones", suscripcion.getId(), relaciones.size());

		return suscripcion;
	}

	private Transaccion crearTransaccionDesdeJson(JsonNode txJson, CuentaBancaria cuenta, Comercio comercio) {
		Objects.requireNonNull(txJson, "El nodo JSON no puede ser nulo");
		Objects.requireNonNull(cuenta, "La cuenta bancaria no puede ser nula");

		Transaccion transaccion = new Transaccion();

		String idExterno = txJson.has("entry_reference") ? txJson.get("entry_reference").asString()
				: UUID.randomUUID().toString();
		transaccion.setIdExterno(idExterno);

		transaccion.setCuentaBancaria(cuenta);

		if (txJson.has("booking_date")) {
			transaccion.setFechaTransaccion(LocalDate.parse(txJson.get("booking_date").asString()));
		} else {
			log.warn("Transacción sin booking_date, usando fecha actual");
			transaccion.setFechaTransaccion(LocalDate.now());
		}

		if (txJson.has("transaction_amount")) {
			JsonNode amountNode = txJson.get("transaction_amount");
			transaccion.setImporte(BigDecimal.valueOf(amountNode.get("amount").asDouble()));
			transaccion.setMoneda(amountNode.has("currency") ? amountNode.get("currency").asString() : "EUR");
		} else {
			log.error("Transacción sin transaction_amount: {}", txJson);
			throw new IllegalArgumentException("La transacción no tiene campo transaction_amount");
		}

		if (txJson.has("remittance_information") && txJson.get("remittance_information").isArray()
				&& txJson.get("remittance_information").size() > 0) {
			transaccion.setDescripcion(txJson.get("remittance_information").get(0).asString());
		} else {
			transaccion.setDescripcion("");
		}

		if (comercio != null) {
			transaccion.setComercio(comercio.getNombre());
			transaccion.setComercioRelacionado(comercio);
		} else {
			String nombreCreditor = txJson.has("creditor") && txJson.get("creditor").has("name")
					? txJson.get("creditor").get("name").asString()
					: "DESCONOCIDO";
			transaccion.setComercio(nombreCreditor);
			transaccion.setComercioRelacionado(null);
		}

		transaccion.setFechaRegistro(LocalDateTime.now());

		log.debug("Transacción creada: idExterno={}, fecha={}, importe={} {}, comercio={}", transaccion.getIdExterno(),
				transaccion.getFechaTransaccion(), transaccion.getImporte(), transaccion.getMoneda(),
				transaccion.getComercio());

		return transaccion;
	}

	// ===== ACTUALIZAR SUSCRIPCIÓN =====

	private void actualizarSuscripcion(Suscripcion suscripcion, List<JsonNode> nuevasTransacciones) {
		JsonNode ultimaTx = nuevasTransacciones.get(nuevasTransacciones.size() - 1);
		double importe = ultimaTx.get("transaction_amount").get("amount").asDouble();

		suscripcion.setImporte(importe);
		suscripcion.setProximaRenovacion(
				calcularProximaRenovacion(ultimaTx, suscripcion.getFrecuencia(), nuevasTransacciones));

		if (suscripcion.getEstado().equals(EstadoSuscripcion.POTENCIAL) && nuevasTransacciones.size() > 1) {
			suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
			log.info("Suscripción '{}' promovida de POTENCIAL a ACTIVA tras detectar {} pagos", suscripcion.getId(),
					nuevasTransacciones.size());
		}

		Confianza nuevaConfianza = calcularNivelConfianza(nuevasTransacciones);
		suscripcion.setConfianza(aumentarConfianzaPorComercioExistente(nuevaConfianza));

		suscripcionRepository.save(suscripcion);
	}

	// ===== CALCULAR PRÓXIMA RENOVACIÓN =====

	private LocalDate calcularProximaRenovacion(JsonNode ultimaTx, Frecuencia frecuencia,
			List<JsonNode> transacciones) {
		LocalDate ultimaFecha = LocalDate.parse(ultimaTx.get("booking_date").asString());

		switch (frecuencia) {
		case MENSUAL:
			return ultimaFecha.plusMonths(1);
		case BIMESTRAL:
			return ultimaFecha.plusMonths(2);
		case TRIMESTRAL:
			return ultimaFecha.plusMonths(3);
		case CUATRIMESTRAL:
			return ultimaFecha.plusMonths(4);
		case SEMESTRAL:
			return ultimaFecha.plusMonths(6);
		case ANUAL:
			return ultimaFecha.plusYears(1);
		default:
			long diasPromedio = calcularDiasPromedio(transacciones);
			return diasPromedio > 0 ? ultimaFecha.plusDays(diasPromedio) : ultimaFecha.plusMonths(1);
		}
	}

	public List<SuscripcionDTO> obtenerSuscripcionesActivasDTO(Long usuarioId) {
		return suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA).stream()
				.map(this::toDTO).toList();
	}

	private SuscripcionDTO toDTO(Suscripcion s) {
		return SuscripcionDTO.builder().id(s.getId()).usuarioId(s.getUsuario().getId())
				.nombreServicio(s.getNombreServicio())
				.comercioId(s.getComercio() != null ? s.getComercio().getId() : null)
				.patronComercio(s.getPatronComercio()).importe(s.getImporte()).moneda(s.getMoneda())
				.frecuencia(s.getFrecuencia().name()).estado(s.getEstado().name()).fechaInicio(s.getFechaInicio())
				.proximaRenovacion(s.getProximaRenovacion()).confianza(s.getConfianza().name()).build();
	}

	public Suscripcion obtenerSuscripcionPorId(String id) {

		return suscripcionRepository.findById(id).get();
	}

	public void cancelarSuscripcion(String id, Long id2) {

		for (Suscripcion s : suscripcionRepository.findByUsuarioId(id2)) {
			if (s.getId().equals(id)) {
				s.setEstado(EstadoSuscripcion.CANCELADA);
				suscripcionRepository.save(s);
			}
		}

	}
}
