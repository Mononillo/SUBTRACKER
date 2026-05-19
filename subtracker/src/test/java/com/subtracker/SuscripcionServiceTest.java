package com.subtracker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.subtracker.config.BankingProperties;
import com.subtracker.dto.SuscripcionDTO;
import com.subtracker.model.Comercio;
import com.subtracker.model.CuentaBancaria;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Suscripcion.Confianza;
import com.subtracker.model.Suscripcion.EstadoSuscripcion;
import com.subtracker.model.Suscripcion.Frecuencia;
import com.subtracker.model.Usuario;
import com.subtracker.repository.SuscripcionRepository;
import com.subtracker.repository.SuscripcionTransaccionRepository;
import com.subtracker.repository.TransaccionRepository;
import com.subtracker.service.ComercioService;
import com.subtracker.service.IaNormalizacionService;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.banking.JwtGenerator;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

	@Mock
	private SuscripcionRepository suscripcionRepository;
	@Mock
	private TransaccionRepository transaccionRepository;
	@Mock
	private SuscripcionTransaccionRepository suscripcionTransaccionRepository;
	@Mock
	private IaNormalizacionService iaNormalizacionService;
	@Mock
	private ComercioService comercioService;
	@Mock
	private BankingProperties bankingProperties;
	@Mock
	private JwtGenerator jwtGenerator;
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private SuscripcionService suscripcionService;

	private Usuario usuario;
	private CuentaBancaria cuenta;
	private Comercio comercio;
	private Suscripcion suscripcion;

	@BeforeEach
	void setUp() {
		usuario = new Usuario();
		usuario.setId(1L);
		usuario.setCorreo("test@test.com");

		cuenta = new CuentaBancaria();
		cuenta.setId(1L);
		cuenta.setUsuario(usuario);

		comercio = Comercio.builder().id(1L).nombre("Netflix").patron("NETFLIX INTERNATIONAL BV")
				.fechaCreacion(LocalDateTime.now()).build();

		suscripcion = Suscripcion.builder().id("Netflix-MENSUAL-2024-01-01").usuario(usuario).comercio(comercio)
				.nombreServicio("Netflix").importe(17.99).moneda("EUR").frecuencia(Frecuencia.MENSUAL)
				.estado(EstadoSuscripcion.ACTIVA).confianza(Confianza.ALTA).fechaInicio(LocalDate.of(2024, 1, 1))
				.proximaRenovacion(LocalDate.now().plusDays(5)).fechaCreacion(LocalDateTime.now()).build();
	}

	// ══════════════════════════════════════════════════════════════
	// CONSULTAS BÁSICAS
	// ══════════════════════════════════════════════════════════════

	@Test
	void obtenerNumeroSuscripcionesActivas_deberiaRetornarConteo() {
		when(suscripcionRepository.countByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA)).thenReturn(5L);

		Long resultado = suscripcionService.obtenerNumeroSuscripcionesActivas(1L);

		assertEquals(5L, resultado);
		verify(suscripcionRepository).countByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA);
	}

	@Test
	void obtenerNumeroSuscripcionesActivas_conUsuarioIdNulo_debeLanzarExcepcion() {
		assertThrows(NullPointerException.class, () -> suscripcionService.obtenerNumeroSuscripcionesActivas(null));
	}

	@Test
	void obtenerListaSuscripciones_deberiaDevolverLista() {
		when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(List.of(suscripcion));

		List<Suscripcion> resultado = suscripcionService.obtenerListaSuscripciones(1L);

		assertEquals(1, resultado.size());
		assertEquals("Netflix", resultado.get(0).getNombreServicio());
	}

	@Test
	void obtenerListaSuscripciones_conUsuarioIdNulo_debeLanzarExcepcion() {
		assertThrows(NullPointerException.class, () -> suscripcionService.obtenerListaSuscripciones(null));
	}

	@Test
	void obtenerGastoMensual_deberiaDevolverSuma() {
		when(suscripcionRepository.sumImporteMesActual(1L)).thenReturn(142.50);

		Double resultado = suscripcionService.obtenerGastoMensual(1L);

		assertEquals(142.50, resultado);
	}

	@Test
	void obtenerGastoMensual_conUsuarioIdNulo_debeLanzarExcepcion() {
		assertThrows(NullPointerException.class, () -> suscripcionService.obtenerGastoMensual(null));
	}

	@Test
	void obtenerGastoAnual_deberiaDevolverSuma() {
		when(suscripcionRepository.sumGastoAnual(1L)).thenReturn(1704.0);

		Double resultado = suscripcionService.obtenerGastoAnual(1L);

		assertEquals(1704.0, resultado);
	}

	@Test
	void obtenerGastoAnual_conUsuarioIdNulo_debeLanzarExcepcion() {
		assertThrows(NullPointerException.class, () -> suscripcionService.obtenerGastoAnual(null));
	}

	@Test
	void obtenerProximoPago_sinRenovacion_deberiaRetornarCero() {
		when(suscripcionRepository.findProximoPago(1L)).thenReturn(Optional.empty());

		Long dias = suscripcionService.obtenerProximoPago(1L);

		assertEquals(0L, dias);
	}

	// ══════════════════════════════════════════════════════════════
	// SSE — SERVER SENT EVENTS
	// ══════════════════════════════════════════════════════════════

	@Test
	void crearEmitter_deberiaRetornarEmitterYAlmacenarlo() {
		SseEmitter emitter = suscripcionService.crearEmitter(1L);

		assertNotNull(emitter);
	}

	@Test
	void notificarActualizacion_sinEmitterRegistrado_noDebeLanzarExcepcion() {
		// No hay emitter registrado para el usuario 99L
		assertDoesNotThrow(() -> suscripcionService.notificarActualizacion(99L));
	}

	// ══════════════════════════════════════════════════════════════
	// OBTENER SUSCRIPCIÓN POR ID
	// ══════════════════════════════════════════════════════════════

	@Test
	void obtenerSuscripcionPorId_deberiaRetornarSuscripcion() {
		when(suscripcionRepository.findById("Netflix-MENSUAL-2024-01-01")).thenReturn(Optional.of(suscripcion));

		Suscripcion resultado = suscripcionService.obtenerSuscripcionPorId("Netflix-MENSUAL-2024-01-01");

		assertNotNull(resultado);
		assertEquals("Netflix", resultado.getNombreServicio());
		assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstado());
	}

	// ══════════════════════════════════════════════════════════════
	// CANCELAR SUSCRIPCIÓN
	// ══════════════════════════════════════════════════════════════

	@Test
	void cancelarSuscripcion_deberiaCambiarEstadoACancelada() {
		when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(List.of(suscripcion));

		suscripcionService.cancelarSuscripcion("Netflix-MENSUAL-2024-01-01", 1L);

		assertEquals(EstadoSuscripcion.CANCELADA, suscripcion.getEstado());
		verify(suscripcionRepository).save(suscripcion);
	}

	@Test
	void cancelarSuscripcion_conIdInexistente_noDebeCambiarNada() {
		when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(List.of(suscripcion));

		suscripcionService.cancelarSuscripcion("id-que-no-existe", 1L);

		assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
		verify(suscripcionRepository, never()).save(any());
	}

	// ══════════════════════════════════════════════════════════════
	// GUARDAR SUSCRIPCIÓN
	// ══════════════════════════════════════════════════════════════

	@Test
	void guardar_deberiaLlamarAlRepositorio() {
		suscripcionService.guardar(suscripcion);

		verify(suscripcionRepository).save(suscripcion);
	}

	// ══════════════════════════════════════════════════════════════
	// BUSCAR POR FECHA DE RENOVACIÓN
	// ══════════════════════════════════════════════════════════════

	@Test
	void buscarSuscripcionesPorFechaRenovacion_deberiaRetornarLista() {
		LocalDate fecha = LocalDate.now().plusDays(3);
		when(suscripcionRepository.findByProximaRenovacionAndEstado(fecha, EstadoSuscripcion.ACTIVA))
				.thenReturn(List.of(suscripcion));

		List<Suscripcion> resultado = suscripcionService.buscarSuscripcionesPorFechaRenovacion(fecha);

		assertEquals(1, resultado.size());
		assertEquals("Netflix", resultado.get(0).getNombreServicio());
	}

	@Test
	void buscarSuscripcionesPorFechaRenovacion_sinResultados_deberiaRetornarListaVacia() {
		LocalDate fecha = LocalDate.now().plusDays(10);
		when(suscripcionRepository.findByProximaRenovacionAndEstado(fecha, EstadoSuscripcion.ACTIVA))
				.thenReturn(List.of());

		List<Suscripcion> resultado = suscripcionService.buscarSuscripcionesPorFechaRenovacion(fecha);

		assertTrue(resultado.isEmpty());
	}

	// ══════════════════════════════════════════════════════════════
	// OBTENER SUSCRIPCIONES ACTIVAS DTO
	// ══════════════════════════════════════════════════════════════

	@Test
	void obtenerSuscripcionesActivasDTO_deberiaRetornarListaDTOs() {
		when(suscripcionRepository.findByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA))
				.thenReturn(List.of(suscripcion));

		List<SuscripcionDTO> resultado = suscripcionService.obtenerSuscripcionesActivasDTO(1L);

		assertEquals(1, resultado.size());
		assertEquals("Netflix", resultado.get(0).nombreServicio());
		assertEquals("ACTIVA", resultado.get(0).estado());
		assertEquals("MENSUAL", resultado.get(0).frecuencia());
		assertEquals(17.99, resultado.get(0).importe());
	}

	@Test
	void obtenerSuscripcionesActivasDTO_sinSuscripciones_deberiaRetornarListaVacia() {
		when(suscripcionRepository.findByUsuarioIdAndEstado(1L, EstadoSuscripcion.ACTIVA)).thenReturn(List.of());

		List<SuscripcionDTO> resultado = suscripcionService.obtenerSuscripcionesActivasDTO(1L);

		assertTrue(resultado.isEmpty());
	}
}