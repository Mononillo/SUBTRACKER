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
		// Cliente HTTP/2 con timeout de 30 segundos
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
				.connectTimeout(java.time.Duration.ofSeconds(30)).build();
	}

	private String generarJwtApp() throws Exception {
		return jwtGenerator.generarJWT();
	}

	// Metodo reutilizable para cualquier llamada POST a la API de Enable Banking
	private JsonNode ejecutarPost(String endpoint, Object body) throws Exception {
		String url = bankingProperties.getBaseUrl() + endpoint;
		String jwt = generarJwtApp(); // Genera el JWT firmado con la clave privada RSA
		String jsonBody = objectMapper.writeValueAsString(body);

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bearer " + jwt) // Autenticacion
																														// mediante
																														// JWT
				.header("Content-Type", "application/json").header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() >= 400) {
			throw new RuntimeException("Error HTTP " + response.statusCode() + ": " + response.body());
		}

		return objectMapper.readTree(response.body());
	}

	public JsonNode iniciarAutenticacionMock(String redirectUri, String state) throws Exception {
		Map<String, Object> access = new HashMap<>();
		access.put("balances", true);
		access.put("transactions", true);

		// El acceso expira en 179 dias (limite de Enable Banking)
		String validUntil = java.time.Instant.now().plus(java.time.Duration.ofDays(179)).toString();
		access.put("valid_until", validUntil);

		// Banco simulado para entorno de pruebas
		Map<String, String> aspsp = new HashMap<>();
		aspsp.put("country", "ES");
		aspsp.put("name", "Mock ASPSP");

		Map<String, Object> body = new HashMap<>();
		body.put("access", access);
		body.put("aspsp", aspsp);
		body.put("psu_type", "personal");
		body.put("redirect_url", redirectUri); // URL a la que el banco redirigira tras la autorizacion
		body.put("state", state); // Token para verificar el callback

		return ejecutarPost("/auth", body);
	}

	// Canjea el codigo de autorizacion por un token de acceso
	public JsonNode obtenerToken(String code) throws Exception {
		Map<String, String> body = new HashMap<>();
		body.put("code", code);
		return ejecutarPost("/sessions", body);
	}

	// Obtiene las cuentas bancarias usando el token de acceso del usuario (no el JWT de la app)
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