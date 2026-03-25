package com.subtracker.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.subtracker.dto.NotificacionDTO;
import com.subtracker.model.Notificacion;

@Mapper(componentModel = "spring", uses = { UsuarioMapper.class, SuscripcionMapper.class })
public interface NotificacionMapper {

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "nombreUsuario", source = "usuario.nombre")
	@Mapping(target = "suscripcionId", source = "suscripcion.id")
	@Mapping(target = "nombreServicio", source = "suscripcion.nombreServicio")
	NotificacionDTO toDto(Notificacion notificacion);

	List<NotificacionDTO> toDtoList(List<Notificacion> notificaciones);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "suscripcion", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	Notificacion toEntity(NotificacionDTO dto);
}