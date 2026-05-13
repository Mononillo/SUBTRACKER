package com.subtracker.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.subtracker.model.Suscripcion;
import com.subtracker.model.Usuario;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificacionService {

	private final JavaMailSender mailSender;
	private final SuscripcionService suscripcionService;

	private int diasAntelacion = 5;

	@Value("${spring.mail.username}")
	private String fromEmail;

	public NotificacionService(JavaMailSender mailSender, SuscripcionService suscripcionService,
			UsuarioService usuarioService) {
		this.mailSender = mailSender;
		this.suscripcionService = suscripcionService;
	}

	@Scheduled(cron = "0 0 9 * * *")
	public void notificarPagosProximos() {
		log.info("Iniciando comprobación de pagos próximos...");

		LocalDate fechaObjetivo = LocalDate.now().plusDays(diasAntelacion);

		List<Suscripcion> suscripciones = suscripcionService.buscarSuscripcionesPorFechaRenovacion(fechaObjetivo);

		log.info("Encontradas {} suscripciones que vencen el {}", suscripciones.size(), fechaObjetivo);

		for (Suscripcion suscripcion : suscripciones) {
			try {
				enviarEmailPagoProximo(suscripcion);
				log.info("Email enviado para: {}", suscripcion.getNombreServicio());
			} catch (Exception e) {
				log.error("Error al enviar email para {}: {}", suscripcion.getNombreServicio(), e.getMessage());
			}
		}
	}

	private void enviarEmailPagoProximo(Suscripcion suscripcion) {
		Usuario usuario = suscripcion.getUsuario();

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromEmail);
		message.setTo(usuario.getCorreo());
		message.setSubject("Proximo pago: " + suscripcion.getNombreServicio());

		message.setText(crearContenidoEmail(suscripcion, usuario));

		mailSender.send(message);
	}

	private String crearContenidoEmail(Suscripcion s, Usuario u) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		return "Hola " + u.getNombre() + ",\n\n" + "Te recordamos que en " + diasAntelacion
				+ " dias se realizara el cobro de:\n\n" + "  Servicio: " + s.getNombreServicio() + "\n" + "  Importe: "
				+ String.format("%.2f %s", s.getImporte(), s.getMoneda()) + "\n" + "  Fecha: "
				+ s.getProximaRenovacion().format(formatter) + "\n" + "  Frecuencia: " + s.getFrecuencia() + "\n\n"
				+ "Saludos,\nSubtracker";
	}
}