package com.subtracker.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.subtracker.dto.CuentaBancariaDTO;
import com.subtracker.model.CuentaBancaria;

@Mapper(componentModel = "spring", uses = { UsuarioMapper.class })
public interface CuentaBancariaMapper {

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "nombreUsuario", source = "usuario.nombre")
	CuentaBancariaDTO toDto(CuentaBancaria cuenta);

	List<CuentaBancariaDTO> toDtoList(List<CuentaBancaria> cuentas);

	@Mapping(target = "usuario", ignore = true) // Se asigna en el servicio
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "conexiones", ignore = true)
	@Mapping(target = "uid",ignore = true)
	CuentaBancaria toEntity(CuentaBancariaDTO dto);
}