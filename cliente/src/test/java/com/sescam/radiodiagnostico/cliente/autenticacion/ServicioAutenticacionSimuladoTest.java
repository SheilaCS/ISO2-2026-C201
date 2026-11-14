package com.sescam.radiodiagnostico.cliente.autenticacion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicioAutenticacionSimuladoTest {

    private final ServicioAutenticacion servicio =
            new ServicioAutenticacionSimulado();

    @Test
    void credencialesCorrectasDevuelvenSesion() {
        SesionUsuario sesion = servicio.iniciarSesion(
                "medico.demo", "demo".toCharArray());

        assertNotNull(sesion);
        assertEquals("medico.demo", sesion.identificador());
        assertEquals("MEDICO", sesion.rol());
    }

    @Test
    void claveIncorrectaRechazaAcceso() {
        SesionUsuario sesion = servicio.iniciarSesion(
                "medico.demo", "incorrecta".toCharArray());

        assertNull(sesion);
    }

    @Test
    void usuarioDesconocidoRechazaAcceso() {
        SesionUsuario sesion = servicio.iniciarSesion(
                "desconocido", "demo".toCharArray());

        assertNull(sesion);
    }
}