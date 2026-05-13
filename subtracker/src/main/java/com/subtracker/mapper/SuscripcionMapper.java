package com.subtracker.mapper;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.subtracker.dto.SuscripcionDTO;
import com.subtracker.model.Suscripcion;

@Mapper(componentModel = "spring", uses = { UsuarioMapper.class, ComercioMapper.class })
public interface SuscripcionMapper {

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "nombreUsuario", source = "usuario.nombre")
	@Mapping(target = "comercioId", source = "comercio.id")
	@Mapping(target = "nombreComercio", source = "comercio.nombre")
	@Mapping(target = "frecuencia", source = "frecuencia")
	@Mapping(target = "estado", source = "estado")
	@Mapping(target = "confianza", source = "confianza")
	@Mapping(target = "fechaInicio", source = "fechaInicio")
	@Mapping(target = "proximaRenovacion", source = "proximaRenovacion")
	SuscripcionDTO toDto(Suscripcion suscripcion);

	List<SuscripcionDTO> toDtoList(List<Suscripcion> suscripciones);

	@Mapping(target = "usuario", ignore = true) 
	@Mapping(target = "comercio", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	Suscripcion toEntity(SuscripcionDTO dto);
}
