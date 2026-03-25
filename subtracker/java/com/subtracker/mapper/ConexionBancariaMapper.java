package com.subtracker.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.subtracker.dto.ConexionBancariaDTO;
import com.subtracker.model.ConexionBancaria;

@Mapper(componentModel = "spring", uses = { UsuarioMapper.class })
public interface ConexionBancariaMapper {

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "nombreUsuario", source = "usuario.nombre")
	@Mapping(target = "cuentaBancariaId", source = "cuentaBancaria.id")
	@Mapping(target = "iban", source = "cuentaBancaria.iban")
	ConexionBancariaDTO toDto(ConexionBancaria conexion);

	List<ConexionBancariaDTO> toDtoList(List<ConexionBancaria> conexiones);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "cuentaBancaria", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	@Mapping(target = "tokenAcceso", ignore = true)
	@Mapping(target = "tokenRefresco", ignore = true)
	ConexionBancaria toEntity(ConexionBancariaDTO dto);
}