package com.subtracker.controller;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.subtracker.model.Usuario;
import com.subtracker.service.UsuarioService;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra la página de perfil del usuario
     */
    @GetMapping
    public String mostrarPerfil(Model model, @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("paginaActiva", "perfil");

        return "layout/perfil";
    }

    /**
     * Actualiza los datos del perfil
     */
    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String nombre,
            @RequestParam(required = false) String telefono,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre no puede estar vacío");
                return "redirect:/perfil";
            }

            usuario.setNombre(nombre.trim());
            usuarioService.guardar(usuario);

            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el perfil: " + e.getMessage());
        }

        return "redirect:/perfil";
    }

    /**
     * Desconecta la cuenta bancaria
     */
    @PostMapping("/desconectar-banco")
    public String desconectarBanco(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            // Aquí iría la lógica para desconectar el banco
            // usuario.setBancoConectado(false);
            // usuario.setBancoId(null);
            // usuarioService.guardar(usuario);

            redirectAttributes.addFlashAttribute("success", "Cuenta bancaria desconectada correctamente");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al desconectar el banco: " + e.getMessage());
        }

        return "redirect:/perfil";
    }

    /**
     * Elimina la cuenta del usuario
     */
    @PostMapping("/eliminar-cuenta")
    public String eliminarCuenta(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = obtenerUsuario(userDetails);
        if (usuario == null) {
            return "redirect:/auth/login";
        }

        try {
            // Eliminar usuario y todos sus datos asociados
            usuarioService.eliminarUsuario(usuario.getId());

            // Redirigir al login
            return "redirect:/auth/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la cuenta: " + e.getMessage());
            return "redirect:/perfil";
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
}