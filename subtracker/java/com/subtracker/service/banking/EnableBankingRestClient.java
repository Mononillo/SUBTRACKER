package com.subtracker.service.banking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.subtracker.config.BankingProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class EnableBankingRestClient {

	private final JwtGenerator jwtGenerator;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final BankingProperties bankingProperties;

	public EnableBankingRestClient(JwtGenerator jwtGenerator, BankingProperties bankingProperties) {
		this.jwtGenerator = jwtGenerator;
		this.bankingProperties = bankingProperties;
		this.objectMapper = new ObjectMapper();
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
				.connectTimeout(java.time.Duration.ofSeconds(30)).build();
	}

	private String generarJwtApp() throws Exception {
		return jwtGenerator.generarJWT();
	}

	private JsonNode ejecutarPost(String endpoint, Object body) throws Exception {
		String url = bankingProperties.getBaseUrl() + endpoint;
		String jwt = generarJwtApp();
		String jsonBody = objectMapper.writeValueAsString(body);

		System.out.println("POST: " + url);
		System.out.println("JWT: " + jwt.substring(0, Math.min(50, jwt.length())) + "...");
		System.out.println("Body enviado: " + jsonBody);

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bearer " + jwt)
				.header("Content-Type", "application/json").header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		System.out.println("Status code: " + response.statusCode());
		System.out.println("Respuesta completa:");
		System.out.println(response.body());

		if (response.statusCode() >= 400) {
			throw new RuntimeException("Error HTTP " + response.statusCode() + ": " + response.body());
		}

		return objectMapper.readTree(response.body());
	}

	public JsonNode iniciarAutenticacionMock(String redirectUri, String state) throws Exception {
		Map<String, Object> access = new HashMap<>();
		access.put("balances", true);
		access.put("transactions", true);

		String validUntil = java.time.Instant.now().plus(java.time.Duration.ofDays(179)).toString();

		access.put("valid_until", validUntil);

		Map<String, String> aspsp = new HashMap<>();
		aspsp.put("country", "ES");
		aspsp.put("name", "Mock ASPSP");

		Map<String, Object> body = new HashMap<>();
		body.put("access", access);
		body.put("aspsp", aspsp);
		body.put("psu_type", "personal");
		body.put("redirect_url", redirectUri);
		body.put("state", state);

		System.out.println("Fecha enviada: " + validUntil);
		System.out.println("Body completo: " + new ObjectMapper().writeValueAsString(body));

		return ejecutarPost("/auth", body);
	}

	/**
	 * Obtener token de acceso (canjear código)
	 */
	public JsonNode obtenerToken(String code) throws Exception {
		System.out.println("EJECUTANDO obtenerToken con code: " + code);

		Map<String, String> body = new HashMap<>();
		body.put("code", code);

		try {
			JsonNode response = ejecutarPost("/sessions", body);
			System.out.println("RESPUESTA COMPLETA DE /sessions:");
			System.out.println(response.toPrettyString()); // ← Más legible
			return response;
		} catch (Exception e) {
			System.err.println("Error en obtenerToken: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Obtener cuentas del usuario
	 */
	public JsonNode obtenerCuentas(String accessToken) throws Exception {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(bankingProperties.getBaseUrl() + "/accounts"))
				.header("Authorization", "Bearer " + accessToken).header("Accept", "application/json").GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() >= 400) {
			throw new RuntimeException("Error HTTP " + response.statusCode() + ": " + response.body());
		}

		return objectMapper.readTree(response.body());
	}
}