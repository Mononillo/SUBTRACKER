package com.subtracker.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.subtracker.model.Comercio;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Suscripcion.Confianza;
import com.subtracker.model.Suscripcion.EstadoSuscripcion;
import com.subtracker.model.Suscripcion.Frecuencia;
import com.subtracker.model.Usuario;
import com.subtracker.service.ComercioService;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.UsuarioService;

@Controller
@RequestMapping("/suscripciones")
public class NuevaSuscripcionController {

    private final SuscripcionService suscripcionService;
    private final UsuarioService usuarioService;
    private final ComercioService comercioService;

    public NuevaSuscripcionController(SuscripcionService suscripcionService, UsuarioService usuarioService,
            ComercioService comercioService) {
        this.suscripcionService = suscripcionService;
        this.usuarioService = usuarioService;
        this.comercioService = comercioService;
    }

    /**
     * Muestra el formulario para crear una nueva suscripción
     */
    @GetMapping("/nueva")
    public String mostrarFormulario(Model model, @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("suscripcion", new Suscripcion());

        return "layout/nueva-suscripcion";
    }

    /**
     * Procesa el formulario de creación de una nueva suscripción
     */
    @PostMapping("/guardar")
    public String guardarSuscripcion(@ModelAttribute Suscripcion suscripcionForm,
            @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            // 1. Validar datos obligatorios
            if (suscripcionForm.getNombreServicio() == null
                    || suscripcionForm.getNombreServicio().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre del servicio es obligatorio");
                return "redirect:/suscripciones/nueva";
            }

            if (suscripcionForm.getImporte() == null || suscripcionForm.getImporte() <= 0) {
                redirectAttributes.addFlashAttribute("error", "El importe debe ser mayor que 0");
                return "redirect:/suscripciones/nueva";
            }

            if (suscripcionForm.getFechaInicio() == null) {
                redirectAttributes.addFlashAttribute("error", "La fecha de inicio es obligatoria");
                return "redirect:/suscripciones/nueva";
            }

            // 2. Buscar o crear el comercio asociado
            Comercio comercio = null;
            if (suscripcionForm.getComercio() != null
                    && suscripcionForm.getComercio().getNombre() != null
                    && !suscripcionForm.getComercio().getNombre().trim().isEmpty()) {
                Optional<Comercio> comercioOpt = comercioService
                        .buscarComercioPorNombre(suscripcionForm.getComercio().getNombre());
                if (comercioOpt.isPresent()) {
                    comercio = comercioOpt.get();
                } else {
                    // CORREGIDO: Usar Comercio.builder() correctamente
                    comercio = Comercio.builder()
                            .nombre(suscripcionForm.getComercio().getNombre())
                            .patron(suscripcionForm.getPatronComercio())
                            .fechaCreacion(LocalDateTime.now())
                            .build();
                    comercio = comercioService.guardar(comercio);
                }
            }

            // 3. Calcular próxima renovación si no se proporcionó
            LocalDate proximaRenovacion = suscripcionForm.getProximaRenovacion();
            if (proximaRenovacion == null) {
                proximaRenovacion = calcularProximaRenovacion(suscripcionForm.getFechaInicio(),
                        suscripcionForm.getFrecuencia());
            }

            // 4. Crear la suscripción
            String id = UUID.randomUUID().toString();
            Suscripcion suscripcion = Suscripcion.builder()
                    .id(id)
                    .usuario(usuario)
                    .nombreServicio(suscripcionForm.getNombreServicio())
                    .comercio(comercio)
                    .patronComercio(suscripcionForm.getPatronComercio())
                    .importe(suscripcionForm.getImporte())
                    .moneda(suscripcionForm.getMoneda() != null ? suscripcionForm.getMoneda() : "EUR")
                    .frecuencia(suscripcionForm.getFrecuencia() != null ? suscripcionForm.getFrecuencia()
                            : Frecuencia.MENSUAL)
                    .estado(EstadoSuscripcion.ACTIVA)
                    .fechaInicio(suscripcionForm.getFechaInicio())
                    .proximaRenovacion(proximaRenovacion)
                    .confianza(Confianza.MUY_ALTA)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            suscripcionService.guardar(suscripcion);

            redirectAttributes.addFlashAttribute("success", "Suscripción creada correctamente");
            return "redirect:/suscripciones";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear la suscripción: " + e.getMessage());
            return "redirect:/suscripciones/nueva";
        }
    }

    /**
     * Obtiene el usuario autenticado
     */
    private Usuario obtenerUsuario(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return null;
        }
        Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
        return usuarioOpt.orElse(null);
    }

    /**
     * Calcula la próxima fecha de renovación basada en la fecha de inicio y la frecuencia
     */
    private LocalDate calcularProximaRenovacion(LocalDate fechaInicio, Frecuencia frecuencia) {
        if (fechaInicio == null || frecuencia == null) {
            return LocalDate.now().plusMonths(1);
        }

        switch (frecuencia) {
        case MENSUAL:
            return fechaInicio.plusMonths(1);
        case BIMESTRAL:
            return fechaInicio.plusMonths(2);
        case TRIMESTRAL:
            return fechaInicio.plusMonths(3);
        case CUATRIMESTRAL:
            return fechaInicio.plusMonths(4);
        case SEMESTRAL:
            return fechaInicio.plusMonths(6);
        case ANUAL:
            return fechaInicio.plusYears(1);
        default:
            return fechaInicio.plusMonths(1);
        }
    }
}