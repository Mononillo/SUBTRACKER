package com.subtracker.mapper;

import com.subtracker.dto.NotificacionDTO;
import com.subtracker.model.Notificacion;
import com.subtracker.model.Suscripcion;
import com.subtracker.model.Usuario;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T01:51:57+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.50.v20260317-1325, environment: Java 23.0.1 (Eclipse Adoptium)"
)
@Component
public class NotificacionMapperImpl implements NotificacionMapper {

    @Override
    public NotificacionDTO toDto(Notificacion notificacion) {
        if ( notificacion == null ) {
            return null;
        }

        NotificacionDTO.NotificacionDTOBuilder notificacionDTO = NotificacionDTO.builder();

        notificacionDTO.usuarioId( notificacionUsuarioId( notificacion ) );
        notificacionDTO.nombreUsuario( notificacionUsuarioNombre( notificacion ) );
        String id1 = notificacionSuscripcionId( notificacion );
        if ( id1 != null ) {
            notificacionDTO.suscripcionId( Long.parseLong( id1 ) );
        }
        notificacionDTO.nombreServicio( notificacionSuscripcionNombreServicio( notificacion ) );
        notificacionDTO.enviada( notificacion.getEnviada() );
        notificacionDTO.fechaNotificacion( notificacion.getFechaNotificacion() );
        if ( notificacion.getId() != null ) {
            notificacionDTO.id( String.valueOf( notificacion.getId() ) );
        }
        notificacionDTO.mensaje( notificacion.getMensaje() );

        return notificacionDTO.build();
    }

    @Override
    public List<NotificacionDTO> toDtoList(List<Notificacion> notificaciones) {
        if ( notificaciones == null ) {
            return null;
        }

        List<NotificacionDTO> list = new ArrayList<NotificacionDTO>( notificaciones.size() );
        for ( Notificacion notificacion : notificaciones ) {
            list.add( toDto( notificacion ) );
        }

        return list;
    }

    @Override
    public Notificacion toEntity(NotificacionDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Notificacion.NotificacionBuilder notificacion = Notificacion.builder();

        notificacion.enviada( dto.enviada() );
        notificacion.fechaNotificacion( dto.fechaNotificacion() );
        if ( dto.id() != null ) {
            notificacion.id( Long.parseLong( dto.id() ) );
        }
        notificacion.mensaje( dto.mensaje() );

        return notificacion.build();
    }

    private Long notificacionUsuarioId(Notificacion notificacion) {
        if ( notificacion == null ) {
            return null;
        }
        Usuario usuario = notificacion.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        Long id = usuario.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String notificacionUsuarioNombre(Notificacion notificacion) {
        if ( notificacion == null ) {
            return null;
        }
        Usuario usuario = notificacion.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        String nombre = usuario.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    private String notificacionSuscripcionId(Notificacion notificacion) {
        if ( notificacion == null ) {
            return null;
        }
        Suscripcion suscripcion = notificacion.getSuscripcion();
        if ( suscripcion == null ) {
            return null;
        }
        String id = suscripcion.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String notificacionSuscripcionNombreServicio(Notificacion notificacion) {
        if ( notificacion == null ) {
            return null;
        }
        Suscripcion suscripcion = notificacion.getSuscripcion();
        if ( suscripcion == null ) {
            return null;
        }
        String nombreServicio = suscripcion.getNombreServicio();
        if ( nombreServicio == null ) {
            return null;
        }
        return nombreServicio;
    }
}
