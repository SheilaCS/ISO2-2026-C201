package com.sescam.radiodiagnostico.autenticacion;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutenticacionServiceImplTest {

    private final IAutenticacionService servicio =
            new AutenticacionServiceImpl();

    @Test
    @DisplayName("Las credenciales correctas devuelven una sesión con identificador y rol")
    void credencialesCorrectasDevuelvenSesionConSuRol() {
        // Preparar
        String usuario = "medico.demo";
        char[] clave = "demo".toCharArray();

        // Ejecutar
        Optional<Sesion> resultado =
                servicio.iniciarSesion(usuario, clave);

        // Comprobar
        assertTrue(resultado.isPresent(), "Debe devolver una sesión");

        Sesion sesion = resultado.orElseThrow();

        assertAll(
                () -> assertEquals(usuario, sesion.identificador()),
                () -> assertEquals(Rol.MEDICO, sesion.rol())
        );
    }

    @Test
    @DisplayName("Cada usuario de demostración recibe el rol correspondiente")
    void cadaUsuarioDePruebaTieneSuRol() {
        // Preparar
        char[] clave = "demo".toCharArray();

        // Ejecutar
        Optional<Sesion> paciente =
                servicio.iniciarSesion("paciente.demo", clave);
        Optional<Sesion> administrativo =
                servicio.iniciarSesion("administrativo.demo", clave);
        Optional<Sesion> administrador =
                servicio.iniciarSesion("admin.demo", clave);

        // Comprobar
        assertAll(
                () -> assertEquals(
                        Rol.PACIENTE,
                        paciente.orElseThrow().rol()
                ),
                () -> assertEquals(
                        Rol.ADMINISTRATIVO,
                        administrativo.orElseThrow().rol()
                ),
                () -> assertEquals(
                        Rol.ADMINISTRADOR,
                        administrador.orElseThrow().rol()
                )
        );
    }

    @Test
    @DisplayName("Una contraseña incorrecta rechaza el acceso")
    void claveIncorrectaRechazaAcceso() {
        // Preparar
        String usuario = "medico.demo";
        char[] clave = "incorrecta".toCharArray();

        // Ejecutar
        Optional<Sesion> resultado =
                servicio.iniciarSesion(usuario, clave);

        // Comprobar
        assertTrue(
                resultado.isEmpty(),
                "No debe crear una sesión con una contraseña incorrecta"
        );
    }

    @Test
    @DisplayName("Un usuario desconocido no puede iniciar sesión")
    void usuarioDesconocidoRechazaAcceso() {
        // Preparar
        String usuario = "desconocido";
        char[] clave = "demo".toCharArray();

        // Ejecutar
        Optional<Sesion> resultado =
                servicio.iniciarSesion(usuario, clave);

        // Comprobar
        assertTrue(
                resultado.isEmpty(),
                "No debe crear una sesión para un usuario desconocido"
        );
    }

    @Test
    @DisplayName("Un usuario nulo o una contraseña nula rechazan el acceso")
    void datosNulosRechazanAcceso() {
        // Preparar
        char[] clave = "demo".toCharArray();

        // Ejecutar
        Optional<Sesion> usuarioNulo =
                servicio.iniciarSesion(null, clave);
        Optional<Sesion> claveNula =
                servicio.iniciarSesion("medico.demo", null);

        // Comprobar
        assertAll(
                () -> assertTrue(
                        usuarioNulo.isEmpty(),
                        "Debe rechazar un usuario nulo"
                ),
                () -> assertTrue(
                        claveNula.isEmpty(),
                        "Debe rechazar una contraseña nula"
                )
        );
    }
}