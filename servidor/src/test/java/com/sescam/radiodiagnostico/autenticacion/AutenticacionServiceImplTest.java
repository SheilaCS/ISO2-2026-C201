package com.sescam.radiodiagnostico.autenticacion;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AutenticacionServiceImplTest {

    private final IAutenticacionService servicio = new AutenticacionServiceImpl();

    @Test
    void credencialesCorrectasDevuelvenSesionConSuRol() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", "demo".toCharArray());

        assertTrue(sesion.isPresent());
        assertEquals("medico.demo", sesion.get().identificador());
        assertEquals(Rol.MEDICO, sesion.get().rol());
    }

    @Test
    void cadaUsuarioDePruebaTieneSuRol() {
        assertEquals(Rol.PACIENTE,
                servicio.iniciarSesion("paciente.demo", "demo".toCharArray()).orElseThrow().rol());
        assertEquals(Rol.ADMINISTRATIVO,
                servicio.iniciarSesion("administrativo.demo", "demo".toCharArray()).orElseThrow().rol());
        assertEquals(Rol.ADMINISTRADOR,
                servicio.iniciarSesion("admin.demo", "demo".toCharArray()).orElseThrow().rol());
    }

    @Test
    void claveIncorrectaRechazaAcceso() {
        assertTrue(servicio.iniciarSesion("medico.demo", "incorrecta".toCharArray()).isEmpty());
    }

    @Test
    void usuarioDesconocidoRechazaAcceso() {
        assertTrue(servicio.iniciarSesion("desconocido", "demo".toCharArray()).isEmpty());
    }

    @Test
    void datosNulosRechazanAcceso() {
        assertTrue(servicio.iniciarSesion(null, "demo".toCharArray()).isEmpty());
        assertTrue(servicio.iniciarSesion("medico.demo", null).isEmpty());
    }
}
