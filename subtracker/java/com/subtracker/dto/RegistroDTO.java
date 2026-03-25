package com.subtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroDTO {
	@NotBlank
	@Email
	private String correo;

	@NotBlank
	@Pattern(regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$", 
    message = "La contraseña debe contener al menos un carácter especial")
	@Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres ")
	private String password;

	@NotBlank
	private String nombre;
}