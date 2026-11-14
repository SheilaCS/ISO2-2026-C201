package com.sescam.radiodiagnostico.cliente.autenticacion;

import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicioAutenticacionServidorTest {

    @Test
    @DisplayName("El adaptador convierte la sesión del servidor conservando identificador y rol")
    void sesionDelServidorSeConvierteEnSesionDeUsuario() {
        // Preparar
        IAutenticacionService servidorFalso =
                (usuario, clave) ->
                        Optional.of(new Sesion(usuario, Rol.MEDICO));

        ServicioAutenticacion servicio =
                new ServicioAutenticacionServidor(servidorFalso);

        String usuario = "medico.demo";
        char[] clave = "demo".toCharArray();

        // Ejecutar
        SesionUsuario resultado =
                servicio.iniciarSesion(usuario, clave);

        // Comprobar
        assertNotNull(resultado, "Debe devolver una sesión de usuario");

        assertAll(
                () -> assertEquals(usuario, resultado.identificador()),
                () -> assertEquals("MEDICO", resultado.rol())
        );
    }

    @Test
    @DisplayName("El adaptador devuelve null cuando el servidor rechaza el acceso")
    void rechazoDelServidorDevuelveNull() {
        // Preparar
        IAutenticacionService servidorFalso =
                (usuario, clave) -> Optional.empty();

        ServicioAutenticacion servicio =
                new ServicioAutenticacionServidor(servidorFalso);

        String usuario = "medico.demo";
        char[] clave = "incorrecta".toCharArray();

        // Ejecutar
        SesionUsuario resultado =
                servicio.iniciarSesion(usuario, clave);

        // Comprobar
        assertNull(
                resultado,
                "No debe devolver una sesión si el servidor rechaza el acceso"
        );
    }
}