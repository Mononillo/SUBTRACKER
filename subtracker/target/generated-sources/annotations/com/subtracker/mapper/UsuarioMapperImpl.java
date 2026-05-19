package com.subtracker.mapper;

import com.subtracker.dto.UsuarioDTO;
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
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public UsuarioDTO toDto(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UsuarioDTO.UsuarioDTOBuilder usuarioDTO = UsuarioDTO.builder();

        usuarioDTO.correo( usuario.getCorreo() );
        usuarioDTO.fechaRegistro( usuario.getFechaRegistro() );
        usuarioDTO.id( usuario.getId() );
        usuarioDTO.nombre( usuario.getNombre() );

        return usuarioDTO.build();
    }

    @Override
    public List<UsuarioDTO> toDtoList(List<Usuario> usuarios) {
        if ( usuarios == null ) {
            return null;
        }

        List<UsuarioDTO> list = new ArrayList<UsuarioDTO>( usuarios.size() );
        for ( Usuario usuario : usuarios ) {
            list.add( toDto( usuario ) );
        }

        return list;
    }

    @Override
    public Usuario toEntity(UsuarioDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Usuario.UsuarioBuilder usuario = Usuario.builder();

        usuario.correo( dto.correo() );
        usuario.id( dto.id() );
        usuario.nombre( dto.nombre() );

        return usuario.build();
    }
}
