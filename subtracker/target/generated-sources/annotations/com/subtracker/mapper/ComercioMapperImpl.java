package com.subtracker.mapper;

import com.subtracker.dto.ComercioDTO;
import com.subtracker.model.Comercio;
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
public class ComercioMapperImpl implements ComercioMapper {

    @Override
    public ComercioDTO toDto(Comercio comercio) {
        if ( comercio == null ) {
            return null;
        }

        ComercioDTO.ComercioDTOBuilder comercioDTO = ComercioDTO.builder();

        comercioDTO.id( comercio.getId() );
        comercioDTO.nombre( comercio.getNombre() );
        comercioDTO.patron( comercio.getPatron() );

        return comercioDTO.build();
    }

    @Override
    public List<ComercioDTO> toDtoList(List<Comercio> comercios) {
        if ( comercios == null ) {
            return null;
        }

        List<ComercioDTO> list = new ArrayList<ComercioDTO>( comercios.size() );
        for ( Comercio comercio : comercios ) {
            list.add( toDto( comercio ) );
        }

        return list;
    }

    @Override
    public Comercio toEntity(ComercioDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Comercio.ComercioBuilder comercio = Comercio.builder();

        comercio.id( dto.id() );
        comercio.nombre( dto.nombre() );
        comercio.patron( dto.patron() );

        return comercio.build();
    }
}
