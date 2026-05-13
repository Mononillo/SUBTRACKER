package com.subtracker.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.subtracker.dto.ComercioDTO;
import com.subtracker.model.Comercio;

@Mapper(componentModel = "spring")
public interface ComercioMapper {

	ComercioDTO toDto(Comercio comercio);

	List<ComercioDTO> toDtoList(List<Comercio> comercios);

	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "suscripciones", ignore = true)
	Comercio toEntity(ComercioDTO dto);
}