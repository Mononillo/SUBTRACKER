package com.subtracker.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.subtracker.repository.SuscripcionRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class IaNormalizacionService {

	private static final Logger log = LoggerFactory.getLogger(IaNormalizacionService.class);

	private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
	private static final String MODEL_NAME = "llama-3.1-8b-instant";
	@Value("${groq.api-key}")
	private String API_KEY;

	private final SuscripcionRepository suscripcionRepository;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final Map<String, String> cache = new ConcurrentHashMap<>();

	public IaNormalizacionService(SuscripcionRepository suscripcionRepository) {
		this.suscripcionRepository = suscripcionRepository;
		this.httpClient = HttpClient.newHttpClient();
		this.objectMapper = new ObjectMapper();
	}

	// ===== NORMALIZAR COMERCIO =====

	public String normalizarComercio(String descripcionOriginal) {
		if (descripcionOriginal == null || descripcionOriginal.trim().isEmpty()) {
			return descripcionOriginal;
		}

		String clave = descripcionOriginal.trim();

		if (cache.containsKey(clave)) {
			return cache.get(clave);
		}

		Optional<String> nombreEnBd = suscripcionRepository.findNombreServicioByPatronContaining(clave);
		if (nombreEnBd.isPresent()) {
			cache.put(clave, nombreEnBd.get());
			return nombreEnBd.get();
		}

		try {
			String nombreNormalizado = llamarIa(promptNormalizacion(clave));
			nombreNormalizado = nombreNormalizado.split("\n")[0].replaceAll("[\"'.]", "").trim();
			cache.put(clave, nombreNormalizado);
			return nombreNormalizado;
		} catch (Exception e) {
			log.warn("No se pudo normalizar '{}', se usa el original. Causa: {}", descripcionOriginal, e.getMessage());
			return descripcionOriginal;
		}
	}

	// ===== ES SUSCRIPCION =====

	public boolean esSuscripcion(JsonNode tx) {
		try {
			String system = "Eres un clasificador de transacciones bancarias. " + "Respondes ÚNICAMENTE con SI o NO. "
					+ "SI = suscripción recurrente (streaming, software, gimnasio, gaming). "
					+ "NO = supermercado, tienda física, restaurante, combustible, transporte, persona. "
					+ "En caso de duda responde SI.";

			String respuesta = llamarIa(system, promptEsSuscripcion(tx));
			return "SI".equalsIgnoreCase(respuesta.trim());
		} catch (Exception e) {
			log.warn("No se pudo determinar si es suscripcion. Causa: {}", e.getMessage());
			return false;
		}
	}

	// ===== LLAMADA A GROQ =====

	private String llamarIa(String userPrompt) throws Exception {
		return llamarIa(null, userPrompt);
	}

	private String llamarIa(String systemPrompt, String userPrompt) throws Exception {
		List<Map<String, String>> messages = new ArrayList<>();

		if (systemPrompt != null) {
			messages.add(Map.of("role", "system", "content", systemPrompt));
		}
		messages.add(Map.of("role", "user", "content", userPrompt));

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", MODEL_NAME);
		requestBody.put("messages", messages);
		requestBody.put("temperature", 0.0);
		requestBody.put("max_tokens", 25);
		requestBody.put("stop", List.of("\n"));

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GROQ_URL))
				.header("Content-Type", "application/json").header("Authorization", "Bearer " + API_KEY)
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody))).build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			log.error("Error Groq. Status: {}, Body: {}", response.statusCode(), response.body());
			throw new RuntimeException("Error Groq: " + response.body());
		}

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		return jsonResponse.get("choices").get(0).get("message").get("content").asString().trim();
	}

	// ===== PROMPTS =====

	private String promptNormalizacion(String descripcion) {
		return "Descripción bancaria: \"" + descripcion + "\"\n\n"
				+ "Extrae el nombre comercial eliminando sufijos legales, países e identificadores técnicos. "
				+ "Solo responde con el nombre comercial, sin explicaciones. "
				+ "Si es una empresa conocida usa su nombre popular. "
				+ "Si no la conoces, limpia la descripción dejando solo el nombre principal.\n\n"
				+ "NETFLIX INTERNATIONAL BV  Netflix\n" + "SPOTIFY AB STOCKHOLM  Spotify\n"
				+ "AMAZON MARKETPLACE EU  Amazon\n" + "CLINICA DENTAL LOPEZ SL  Clinica Dental Lopez\n"
				+ "GIMNASIO SANCHEZ CORP  Gimnasio Sanchez\n" + "ACADEMIA_IDIOMAS_MADRID  Academia Idiomas Madrid\n"
				+ "basic_fit_spain  Basic-Fit\n" + "microsoft_ireland_ops  Microsoft\n\n" + "Nombre comercial:";
	}

	private String promptEsSuscripcion(JsonNode tx) {
		String comercio = tx.has("creditor") && tx.get("creditor").has("name")
				? tx.get("creditor").get("name").asString()
				: "DESCONOCIDO";
		String importe = tx.has("transaction_amount") ? tx.get("transaction_amount").get("amount").asString() : "0";
		String descripcion = tx.has("remittance_information") && tx.get("remittance_information").isArray()
				&& tx.get("remittance_information").size() > 0 ? tx.get("remittance_information").get(0).asString()
						: "";

		return "Comercio: " + comercio + "\n" + "Importe: " + importe + " €\n" + "Descripción: " + descripcion;
	}
}