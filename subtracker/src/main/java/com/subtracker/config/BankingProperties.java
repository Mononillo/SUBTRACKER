package com.subtracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "enablebanking")
public class BankingProperties {

	private String applicationId = "4760207b-a5f9-4a5e-a6fb-aa36b41a81b2";
	private String privateKeyPath = "src/main/resources/keys/4760207b-a5f9-4a5e-a6fb-aa36b41a81b2.pem";
	private String baseUrl = "https://api.enablebanking.com"; 

}