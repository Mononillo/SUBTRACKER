package com.subtracker.mapper;

import com.subtracker.dto.SuscripcionDTO;
import com.subtracker.model.Comercio;
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
public class SuscripcionMapperImpl implements SuscripcionMapper {

    @Override
    public SuscripcionDTO toDto(Suscripcion suscripcion) {
        if ( suscripcion == null ) {
            return null;
        }

        SuscripcionDTO.SuscripcionDTOBuilder suscripcionDTO = SuscripcionDTO.builder();

        suscripcionDTO.usuarioId( suscripcionUsuarioId( suscripcion ) );
        suscripcionDTO.nombreUsuario( suscripcionUsuarioNombre( suscripcion ) );
        suscripcionDTO.comercioId( suscripcionComercioId( suscripcion ) );
        suscripcionDTO.nombreComercio( suscripcionComercioNombre( suscripcion ) );
        if ( suscripcion.getFrecuencia() != null ) {
            suscripcionDTO.frecuencia( suscripcion.getFrecuencia().name() );
        }
        if ( suscripcion.getEstado() != null ) {
            suscripcionDTO.estado( suscripcion.getEstado().name() );
        }
        if ( suscripcion.getConfianza() != null ) {
            suscripcionDTO.confianza( suscripcion.getConfianza().name() );
        }
        suscripcionDTO.fechaInicio( suscripcion.getFechaInicio() );
        suscripcionDTO.proximaRenovacion( suscripcion.getProximaRenovacion() );
        suscripcionDTO.id( suscripcion.getId() );
        suscripcionDTO.importe( suscripcion.getImporte() );
        suscripcionDTO.moneda( suscripcion.getMoneda() );
        suscripcionDTO.nombreServicio( suscripcion.getNombreServicio() );
        suscripcionDTO.patronComercio( suscripcion.getPatronComercio() );

        return suscripcionDTO.build();
    }

    @Override
    public List<SuscripcionDTO> toDtoList(List<Suscripcion> suscripciones) {
        if ( suscripciones == null ) {
            return null;
        }

        List<SuscripcionDTO> list = new ArrayList<SuscripcionDTO>( suscripciones.size() );
        for ( Suscripcion suscripcion : suscripciones ) {
            list.add( toDto( suscripcion ) );
        }

        return list;
    }

    @Override
    public Suscripcion toEntity(SuscripcionDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Suscripcion.SuscripcionBuilder suscripcion = Suscripcion.builder();

        if ( dto.confianza() != null ) {
            suscripcion.confianza( Enum.valueOf( Suscripcion.Confianza.class, dto.confianza() ) );
        }
        if ( dto.estado() != null ) {
            suscripcion.estado( Enum.valueOf( Suscripcion.EstadoSuscripcion.class, dto.estado() ) );
        }
        suscripcion.fechaInicio( dto.fechaInicio() );
        if ( dto.frecuencia() != null ) {
            suscripcion.frecuencia( Enum.valueOf( Suscripcion.Frecuencia.class, dto.frecuencia() ) );
        }
        suscripcion.id( dto.id() );
        suscripcion.importe( dto.importe() );
        suscripcion.moneda( dto.moneda() );
        suscripcion.nombreServicio( dto.nombreServicio() );
        suscripcion.patronComercio( dto.patronComercio() );
        suscripcion.proximaRenovacion( dto.proximaRenovacion() );

        return suscripcion.build();
    }

    private Long suscripcionUsuarioId(Suscripcion suscripcion) {
        if ( suscripcion == null ) {
            return null;
        }
        Usuario usuario = suscripcion.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        Long id = usuario.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String suscripcionUsuarioNombre(Suscripcion suscripcion) {
        if ( suscripcion == null ) {
            return null;
        }
        Usuario usuario = suscripcion.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        String nombre = usuario.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    private Long suscripcionComercioId(Suscripcion suscripcion) {
        if ( suscripcion == null ) {
            return null;
        }
        Comercio comercio = suscripcion.getComercio();
        if ( comercio == null ) {
            return null;
        }
        Long id = comercio.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String suscripcionComercioNombre(Suscripcion suscripcion) {
        if ( suscripcion == null ) {
            return null;
        }
        Comercio comercio = suscripcion.getComercio();
        if ( comercio == null ) {
            return null;
        }
        String nombre = comercio.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }
}
