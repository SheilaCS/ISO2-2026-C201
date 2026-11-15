package com.sescam.radiodiagnostico.cliente.autenticacion;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del servicio simulado del cliente, que permite usar la pantalla sin servidor (CDU1, RF01).
 */
@DisplayName("CDU1 · Cliente · ServicioAutenticacionSimulado")
class ServicioAutenticacionSimuladoTest {

    private ServicioAutenticacion servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioAutenticacionSimulado();
    }

    @Test
    @DisplayName("S1. Médico con clave correcta obtiene sesión con rol MEDICO")
    void medicoConClaveCorrectaObtieneSesion() {
        // Ejecutar
        SesionUsuario sesion = servicio.iniciarSesion("medico.demo", "demo".toCharArray());

        // Comprobar
        assertNotNull(sesion);
        assertAll(
                () -> assertEquals("medico.demo", sesion.identificador()),
                () -> assertEquals("MEDICO", sesion.rol()));
    }

    @Test
    @DisplayName("S2. Clave incorrecta: devuelve null")
    void claveIncorrectaDevuelveNull() {
        assertNull(servicio.iniciarSesion("medico.demo", "mal".toCharArray()));
    }

    @Test
    @DisplayName("S3. Usuario desconocido: devuelve null")
    void usuarioDesconocidoDevuelveNull() {
        assertNull(servicio.iniciarSesion("desconocido", "demo".toCharArray()));
    }

    @Test
    @DisplayName("S4. Otro usuario de demostración: el simulado solo conoce al médico")
    void otroUsuarioDemoDevuelveNull() {
        assertNull(servicio.iniciarSesion("paciente.demo", "demo".toCharArray()));
    }

    @Test
    @DisplayName("S5. Clave vacía: devuelve null")
    void claveVaciaDevuelveNull() {
        assertNull(servicio.iniciarSesion("medico.demo", new char[0]));
    }

    @Test
    @DisplayName("S6. Usuario o clave nulos: devuelve null sin lanzar excepción")
    void datosNulosDevuelvenNull() {
        assertAll(
                () -> assertNull(servicio.iniciarSesion(null, "demo".toCharArray())),
                () -> assertNull(servicio.iniciarSesion("medico.demo", null)));
    }
}
