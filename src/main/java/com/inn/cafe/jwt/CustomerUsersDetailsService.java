package com.inn.cafe.jwt;

import com.inn.cafe.dao.UserDao;
import com.inn.cafe.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CustomerUsersDetailsService implements UserDetailsService {

    @Autowired
    private UserDao userDao;

    private User currentUser; // Variable para mantener el usuario autenticado

    /**
     * Método para cargar detalles del usuario por nombre de usuario (correo electrónico).
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Inside loadUserByUsername {}", username);

        // Busca el usuario en la base de datos
        currentUser = userDao.findByEmailId(username);

        if (currentUser != null) {
            // Devuelve un UserDetails con los datos del usuario encontrado
            return new org.springframework.security.core.userdetails.User(
                    currentUser.getEmail(),
                    currentUser.getPassword(), // Contraseña codificada
                    getAuthorities(currentUser.getRole()) // Rol del usuario
            );
        } else {
            log.error("User not found with email: {}", username);
            throw new UsernameNotFoundException("User not found.");
        }
    }

    /**
     * Método para asignar roles al usuario.
     */
    private List<SimpleGrantedAuthority> getAuthorities(String role) {
        // Convierte el rol almacenado en la tabla `user` a un SimpleGrantedAuthority
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    /**
     * Método para obtener el usuario autenticado actual.
     */
    public User getAuthenticatedUser() {
        return currentUser;
    }
}