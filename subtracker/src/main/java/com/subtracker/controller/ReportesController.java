package com.subtracker.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.subtracker.dto.SuscripcionDTO;
import com.subtracker.model.Usuario;
import com.subtracker.service.SuscripcionService;
import com.subtracker.service.UsuarioService;

@Controller
public class ReportesController {

    private final SuscripcionService suscripcionService;
    private final UsuarioService usuarioService;

    public ReportesController(SuscripcionService suscripcionService, UsuarioService usuarioService) {
        this.suscripcionService = suscripcionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/reportes")
    public String reportes(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(defaultValue = "12") String meses,
                           Model model) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        int numMeses = Integer.parseInt(meses);
        
        // Obtener todas las suscripciones activas del usuario
        List<SuscripcionDTO> todas = suscripcionService.obtenerSuscripcionesActivasDTO(usuario.getId());
        
        // Filtrar solo suscripciones activas
        List<SuscripcionDTO> suscripcionesActivas = todas.stream()
                .filter(s -> s.estado() != null && "ACTIVA".equals(s.estado()))
                .collect(Collectors.toList());

        // 1. Datos para el gráfico de gasto mensual (calculando el gasto real mes a mes)
        List<Double> gastoMensual = calcularGastoMensualReal(suscripcionesActivas, numMeses);
        List<String> mesesLabels = obtenerMeses(numMeses);
        
        // 2. Datos para el gráfico de distribución por frecuencia
        List<Map<String, Object>> distribucionFrecuencia = obtenerDistribucionPorFrecuencia(suscripcionesActivas);
        
        // 3. Datos para el gráfico de distribución por comercio (top 10)
        List<Map<String, Object>> distribucionComercios = obtenerDistribucionPorComercio(suscripcionesActivas);
        
        // Estadísticas generales
        model.addAttribute("usuario", usuario);
        model.addAttribute("totalSuscripciones", suscripcionesActivas.size());
        model.addAttribute("gastoMensualPromedio", calcularGastoMensualPromedio(suscripcionesActivas));
        model.addAttribute("gastoAnual", calcularGastoAnual(suscripcionesActivas));
        model.addAttribute("suscripcionMasCara", suscripcionesActivas.stream()
                .mapToDouble(SuscripcionDTO::importe)
                .max().orElse(0.0));
        
        // Datos para gráficos
        model.addAttribute("gastoMensual", gastoMensual);
        model.addAttribute("mesesLabels", mesesLabels);
        model.addAttribute("distribucionFrecuencia", distribucionFrecuencia);
        model.addAttribute("distribucionComercios", distribucionComercios);
        model.addAttribute("mesesSeleccionados", numMeses);
        
        return "layout/reportes";
    }

    private Usuario obtenerUsuario(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return null;
        }
        Optional<Usuario> usuarioOpt = usuarioService.buscarUsuarioPorCorreo(userDetails.getUsername());
        return usuarioOpt.orElse(null);
    }

    /**
     * Calcula el gasto real mes a mes para los últimos N meses
     */
    private List<Double> calcularGastoMensualReal(List<SuscripcionDTO> suscripciones, int numMeses) {
        List<Double> gastos = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        
        for (int i = numMeses - 1; i >= 0; i--) {
            LocalDate mes = hoy.minusMonths(i);
            double gastoMes = 0;
            
            for (SuscripcionDTO s : suscripciones) {
                gastoMes += calcularGastoEnMes(s, mes);
            }
            gastos.add(gastoMes);
        }
        return gastos;
    }
    
    /**
     * Calcula cuánto gasta una suscripción en un mes específico
     */
    private double calcularGastoEnMes(SuscripcionDTO suscripcion, LocalDate mes) {
        if (suscripcion.fechaInicio() == null) return 0;
        
        LocalDate fechaInicio = suscripcion.fechaInicio();
        if (fechaInicio.isAfter(mes.withDayOfMonth(mes.lengthOfMonth()))) {
            return 0;
        }
        
        String frecuencia = suscripcion.frecuencia();
        double importe = suscripcion.importe();
        
        switch (frecuencia) {
            case "MENSUAL":
                return importe;
            case "BIMESTRAL":
                // Paga cada 2 meses, desde la fecha de inicio
                long mesesDesdeInicio = java.time.temporal.ChronoUnit.MONTHS.between(
                    fechaInicio.withDayOfMonth(1), 
                    mes.withDayOfMonth(1));
                return (mesesDesdeInicio % 2 == 0) ? importe : 0;
            case "TRIMESTRAL":
                long mesesDesdeInicioQ = java.time.temporal.ChronoUnit.MONTHS.between(
                    fechaInicio.withDayOfMonth(1), 
                    mes.withDayOfMonth(1));
                return (mesesDesdeInicioQ % 3 == 0) ? importe : 0;
            case "SEMESTRAL":
                long mesesDesdeInicioS = java.time.temporal.ChronoUnit.MONTHS.between(
                    fechaInicio.withDayOfMonth(1), 
                    mes.withDayOfMonth(1));
                return (mesesDesdeInicioS % 6 == 0) ? importe : 0;
            case "ANUAL":
                // Solo paga en el mes de inicio y luego cada 12 meses
                if (mes.getMonthValue() == fechaInicio.getMonthValue()) {
                    return importe;
                }
                return 0;
            default:
                return importe;
        }
    }
    
    /**
     * Calcula el gasto mensual promedio
     */
    private double calcularGastoMensualPromedio(List<SuscripcionDTO> suscripciones) {
        double gastoAnual = 0;
        for (SuscripcionDTO s : suscripciones) {
            switch (s.frecuencia()) {
                case "MENSUAL": gastoAnual += s.importe() * 12; break;
                case "BIMESTRAL": gastoAnual += s.importe() * 6; break;
                case "TRIMESTRAL": gastoAnual += s.importe() * 4; break;
                case "SEMESTRAL": gastoAnual += s.importe() * 2; break;
                case "ANUAL": gastoAnual += s.importe(); break;
                default: gastoAnual += s.importe() * 12;
            }
        }
        return gastoAnual / 12;
    }
    
    /**
     * Calcula el gasto anual total
     */
    private double calcularGastoAnual(List<SuscripcionDTO> suscripciones) {
        double total = 0;
        for (SuscripcionDTO s : suscripciones) {
            switch (s.frecuencia()) {
                case "MENSUAL": total += s.importe() * 12; break;
                case "BIMESTRAL": total += s.importe() * 6; break;
                case "TRIMESTRAL": total += s.importe() * 4; break;
                case "SEMESTRAL": total += s.importe() * 2; break;
                case "ANUAL": total += s.importe(); break;
                default: total += s.importe() * 12;
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }
    
    /**
     * Obtiene distribución por frecuencia
     */
    private List<Map<String, Object>> obtenerDistribucionPorFrecuencia(List<SuscripcionDTO> suscripciones) {
        Map<String, Double> sumaPorFrecuencia = suscripciones.stream()
                .filter(s -> s.frecuencia() != null)
                .collect(Collectors.groupingBy(
                        SuscripcionDTO::frecuencia,
                        Collectors.summingDouble(SuscripcionDTO::importe)
                ));
        
        double total = sumaPorFrecuencia.values().stream().mapToDouble(Double::doubleValue).sum();
        
        Map<String, String> nombres = new HashMap<>();
        nombres.put("MENSUAL", "Mensual");
        nombres.put("BIMESTRAL", "Bimestral");
        nombres.put("TRIMESTRAL", "Trimestral");
        nombres.put("SEMESTRAL", "Semestral");
        nombres.put("ANUAL", "Anual");
        nombres.put("DESCONOCIDA", "Desconocida");
        
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sumaPorFrecuencia.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nombre", nombres.getOrDefault(entry.getKey(), entry.getKey()));
            item.put("total", entry.getValue());
            item.put("porcentaje", total > 0 ? (entry.getValue() / total) * 100 : 0);
            resultado.add(item);
        }
        
        resultado.sort((a, b) -> Double.compare((Double) b.get("total"), (Double) a.get("total")));
        return resultado;
    }
    
    /**
     * Obtiene distribución por comercio (top 10)
     */
    private List<Map<String, Object>> obtenerDistribucionPorComercio(List<SuscripcionDTO> suscripciones) {
        Map<String, Double> sumaPorComercio = suscripciones.stream()
                .filter(s -> s.nombreComercio() != null && !s.nombreComercio().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        SuscripcionDTO::nombreComercio,
                        Collectors.summingDouble(SuscripcionDTO::importe)
                ));
        
        if (sumaPorComercio.isEmpty()) {
            // Si no hay comercios asociados, usar el nombre del servicio
            sumaPorComercio = suscripciones.stream()
                    .collect(Collectors.groupingBy(
                            SuscripcionDTO::nombreServicio,
                            Collectors.summingDouble(SuscripcionDTO::importe)
                    ));
        }
        
        double total = sumaPorComercio.values().stream().mapToDouble(Double::doubleValue).sum();
        
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sumaPorComercio.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            String nombre = entry.getKey();
            if (nombre.length() > 20) {
                nombre = nombre.substring(0, 20) + "...";
            }
            item.put("nombre", nombre);
            item.put("total", entry.getValue());
            item.put("porcentaje", total > 0 ? (entry.getValue() / total) * 100 : 0);
            resultado.add(item);
        }
        
        resultado.sort((a, b) -> Double.compare((Double) b.get("total"), (Double) a.get("total")));
        // Limitar a top 10
        return resultado.stream().limit(10).collect(Collectors.toList());
    }
    
    /**
     * Obtiene lista de meses para los ejes
     */
    private List<String> obtenerMeses(int numMeses) {
        List<String> resultado = new ArrayList<>();
        LocalDate fecha = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM ''yy");
        
        for (int i = numMeses - 1; i >= 0; i--) {
            resultado.add(fecha.minusMonths(i).format(formatter));
        }
        return resultado;
    }
}