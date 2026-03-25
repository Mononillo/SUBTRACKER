package com.subtracker.service.banking;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import com.subtracker.config.BankingProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtGenerator {
	private final BankingProperties bankingProperties;

	public JwtGenerator(BankingProperties bankingProperties) {
		this.bankingProperties = bankingProperties;
	}

	private static final long EXPIRATION_TIME = 3600 * 1000;

	public String generarJWT() throws Exception {
		try {
			PrivateKey privateKey = obtenerClavePrivada();

			long ahora = System.currentTimeMillis();
			Date fechaEmision = new Date(ahora);
			Date fechaExpiracion = new Date(ahora + EXPIRATION_TIME);

			return Jwts.builder().setHeaderParam("typ", "JWT").setHeaderParam("alg", "RS256")
					.setHeaderParam("kid", bankingProperties.getApplicationId()).setIssuer("enablebanking.com")
					.setAudience("api.enablebanking.com").setIssuedAt(fechaEmision).setExpiration(fechaExpiracion)
					.signWith(privateKey, SignatureAlgorithm.RS256).compact();
		} catch (FileNotFoundException e) {
			throw new Exception("Error de configuración: No se encuentra el archivo de clave", e);
		} catch (Exception e) {
			throw new Exception("Error al cargar la clave de autenticación", e);
		}
	}

	/**
	 * Carga la clave privada desde el archivo .pem
	 */
	private PrivateKey obtenerClavePrivada() throws Exception {
		// Leer el archivo .pem
		File pemFile = ResourceUtils.getFile(bankingProperties.getPrivateKeyPath());
		String clavePem = new String(Files.readAllBytes(pemFile.toPath()));

		// Limpiar el formato PEM (eliminar cabeceras y saltos de línea)
		String claveLimpia = clavePem.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");

		// Decodificar Base64
		byte[] claveBytes = Base64.getDecoder().decode(claveLimpia);

		// Crear la especificación de la clave PKCS#8
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(claveBytes);

		// Obtener la fábrica de claves RSA
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

		// Generar la clave privada
		return keyFactory.generatePrivate(spec);
	}

}
