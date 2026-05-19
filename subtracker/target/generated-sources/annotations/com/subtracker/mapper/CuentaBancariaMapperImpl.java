package com.subtracker.mapper;

import com.subtracker.dto.CuentaBancariaDTO;
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
public class CuentaBancariaMapperImpl implements CuentaBancariaMapper {

    @Override
    public CuentaBancariaDTO toDto(CuentaBancaria cuenta) {
        if ( cuenta == null ) {
            return null;
        }

        CuentaBancariaDTO.CuentaBancariaDTOBuilder cuentaBancariaDTO = CuentaBancariaDTO.builder();

        cuentaBancariaDTO.usuarioId( cuentaUsuarioId( cuenta ) );
        cuentaBancariaDTO.nombreUsuario( cuentaUsuarioNombre( cuenta ) );
        cuentaBancariaDTO.iban( cuenta.getIban() );
        cuentaBancariaDTO.id( cuenta.getId() );
        cuentaBancariaDTO.nombreBanco( cuenta.getNombreBanco() );

        return cuentaBancariaDTO.build();
    }

    @Override
    public List<CuentaBancariaDTO> toDtoList(List<CuentaBancaria> cuentas) {
        if ( cuentas == null ) {
            return null;
        }

        List<CuentaBancariaDTO> list = new ArrayList<CuentaBancariaDTO>( cuentas.size() );
        for ( CuentaBancaria cuentaBancaria : cuentas ) {
            list.add( toDto( cuentaBancaria ) );
        }

        return list;
    }

    @Override
    public CuentaBancaria toEntity(CuentaBancariaDTO dto) {
        if ( dto == null ) {
            return null;
        }

        CuentaBancaria.CuentaBancariaBuilder cuentaBancaria = CuentaBancaria.builder();

        cuentaBancaria.iban( dto.iban() );
        cuentaBancaria.id( dto.id() );
        cuentaBancaria.nombreBanco( dto.nombreBanco() );

        return cuentaBancaria.build();
    }

    private Long cuentaUsuarioId(CuentaBancaria cuentaBancaria) {
        if ( cuentaBancaria == null ) {
            return null;
        }
        Usuario usuario = cuentaBancaria.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        Long id = usuario.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String cuentaUsuarioNombre(CuentaBancaria cuentaBancaria) {
        if ( cuentaBancaria == null ) {
            return null;
        }
        Usuario usuario = cuentaBancaria.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        String nombre = usuario.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }
}
