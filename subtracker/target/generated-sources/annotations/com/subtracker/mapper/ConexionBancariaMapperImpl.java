package com.subtracker.mapper;

import com.subtracker.dto.ConexionBancariaDTO;
import com.subtracker.model.ConexionBancaria;
import com.subtracker.model.CuentaBancaria;
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
public class ConexionBancariaMapperImpl implements ConexionBancariaMapper {

    @Override
    public ConexionBancariaDTO toDto(ConexionBancaria conexion) {
        if ( conexion == null ) {
            return null;
        }

        ConexionBancariaDTO.ConexionBancariaDTOBuilder conexionBancariaDTO = ConexionBancariaDTO.builder();

        conexionBancariaDTO.usuarioId( conexionUsuarioId( conexion ) );
        conexionBancariaDTO.nombreUsuario( conexionUsuarioNombre( conexion ) );
        conexionBancariaDTO.cuentaBancariaId( conexionCuentaBancariaId( conexion ) );
        conexionBancariaDTO.iban( conexionCuentaBancariaIban( conexion ) );
        conexionBancariaDTO.expiraEn( conexion.getExpiraEn() );
        conexionBancariaDTO.id( conexion.getId() );
        conexionBancariaDTO.idSesion( conexion.getIdSesion() );

        return conexionBancariaDTO.build();
    }

    @Override
    public List<ConexionBancariaDTO> toDtoList(List<ConexionBancaria> conexiones) {
        if ( conexiones == null ) {
            return null;
        }

        List<ConexionBancariaDTO> list = new ArrayList<ConexionBancariaDTO>( conexiones.size() );
        for ( ConexionBancaria conexionBancaria : conexiones ) {
            list.add( toDto( conexionBancaria ) );
        }

        return list;
    }

    @Override
    public ConexionBancaria toEntity(ConexionBancariaDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ConexionBancaria.ConexionBancariaBuilder conexionBancaria = ConexionBancaria.builder();

        conexionBancaria.expiraEn( dto.expiraEn() );
        conexionBancaria.id( dto.id() );
        conexionBancaria.idSesion( dto.idSesion() );

        return conexionBancaria.build();
    }

    private Long conexionUsuarioId(ConexionBancaria conexionBancaria) {
        if ( conexionBancaria == null ) {
            return null;
        }
        Usuario usuario = conexionBancaria.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        Long id = usuario.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String conexionUsuarioNombre(ConexionBancaria conexionBancaria) {
        if ( conexionBancaria == null ) {
            return null;
        }
        Usuario usuario = conexionBancaria.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        String nombre = usuario.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    private Long conexionCuentaBancariaId(ConexionBancaria conexionBancaria) {
        if ( conexionBancaria == null ) {
            return null;
        }
        CuentaBancaria cuentaBancaria = conexionBancaria.getCuentaBancaria();
        if ( cuentaBancaria == null ) {
            return null;
        }
        Long id = cuentaBancaria.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String conexionCuentaBancariaIban(ConexionBancaria conexionBancaria) {
        if ( conexionBancaria == null ) {
            return null;
        }
        CuentaBancaria cuentaBancaria = conexionBancaria.getCuentaBancaria();
        if ( cuentaBancaria == null ) {
            return null;
        }
        String iban = cuentaBancaria.getIban();
        if ( iban == null ) {
            return null;
        }
        return iban;
    }
}
