package com.subtracker.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.subtracker.dto.UsuarioDTO;
import com.subtracker.model.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

	UsuarioDTO toDto(Usuario usuario);

	List<UsuarioDTO> toDtoList(List<Usuario> usuarios);

	@Mapping(target = "fechaRegistro", ignore = true)
	@Mapping(target = "cuentasBancarias", ignore = true)
	@Mapping(target = "suscripciones", ignore = true)
	@Mapping(target = "notificaciones", ignore = true)
	@Mapping(target = "conexionesBancarias", ignore = true)
	@Mapping(target = "hashContrasena", ignore = true)
	Usuario toEntity(UsuarioDTO dto);
}