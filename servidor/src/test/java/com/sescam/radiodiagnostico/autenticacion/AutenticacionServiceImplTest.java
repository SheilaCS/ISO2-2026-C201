package com.sescam.radiodiagnostico.autenticacion;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del servicio de autenticación del servidor (CDU1, RF01).
 * Casos diseñados con clases de equivalencia, valores límite y conjetura de errores
 * (tabla en la wiki, página "Iteración 1", apartado "Pruebas").
 */
@DisplayName("CDU1 · Servidor · AutenticacionServiceImpl")
class AutenticacionServiceImplTest {

    private IAutenticacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AutenticacionServiceImpl();
    }

    // ---------- Clases de equivalencia válidas ----------

    @Test
    @DisplayName("1. Médico con clave correcta obtiene sesión con rol MEDICO")
    void medicoConClaveCorrectaObtieneSesion() {
        // Preparar
        String usuario = "medico.demo";
        char[] clave = "demo".toCharArray();

        // Ejecutar
        Optional<Sesion> sesion = servicio.iniciarSesion(usuario, clave);

        // Comprobar
        assertTrue(sesion.isPresent(), "Debe abrirse una sesión");
        assertAll(
                () -> assertEquals("medico.demo", sesion.get().identificador()),
                () -> assertEquals(Rol.MEDICO, sesion.get().rol()));
    }

    @Test
    @DisplayName("2. Cada usuario de prueba obtiene su propio rol")
    void cadaUsuarioObtieneSuRol() {
        // Preparar: la clave correcta es la misma para todos
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(Rol.PACIENTE,
                        servicio.iniciarSesion("paciente.demo", "demo".toCharArray()).orElseThrow().rol()),
                () -> assertEquals(Rol.ADMINISTRATIVO,
                        servicio.iniciarSesion("administrativo.demo", "demo".toCharArray()).orElseThrow().rol()),
                () -> assertEquals(Rol.ADMINISTRADOR,
                        servicio.iniciarSesion("admin.demo", "demo".toCharArray()).orElseThrow().rol()));
    }

    // ---------- Clave no válida y valores límite de la clave ----------

    @Test
    @DisplayName("3. Clave incorrecta: se rechaza el acceso")
    void claveIncorrectaSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", "mal".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("4. Clave con un carácter de menos ('dem'): se rechaza")
    void claveConUnCaracterMenosSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", "dem".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("5. Clave con un carácter de más ('demo1'): se rechaza")
    void claveConUnCaracterMasSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", "demo1".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("6. Clave vacía: se rechaza")
    void claveVaciaSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", new char[0]);
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("7. Clave en mayúsculas ('DEMO'): se rechaza")
    void claveEnMayusculasSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", "DEMO".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    // ---------- Usuario no válido y valores límite del usuario ----------

    @Test
    @DisplayName("8. Usuario desconocido: se rechaza")
    void usuarioDesconocidoSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("desconocido", "demo".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("9. Usuario en mayúsculas ('MEDICO.DEMO'): se rechaza")
    void usuarioEnMayusculasSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("MEDICO.DEMO", "demo".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("10. Usuario con espacio delante (' medico.demo'): se rechaza")
    void usuarioConEspacioSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion(" medico.demo", "demo".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("11. Usuario vacío: se rechaza")
    void usuarioVacioSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("", "demo".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    // ---------- Datos nulos ----------

    @Test
    @DisplayName("12. Usuario nulo: se rechaza sin lanzar excepción")
    void usuarioNuloSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion(null, "demo".toCharArray());
        assertTrue(sesion.isEmpty());
    }

    @Test
    @DisplayName("13. Clave nula: se rechaza sin lanzar excepción")
    void claveNulaSeRechaza() {
        Optional<Sesion> sesion = servicio.iniciarSesion("medico.demo", null);
        assertTrue(sesion.isEmpty());
    }

    // ---------- Conjetura de errores ----------

    @Test
    @DisplayName("14. El servicio no modifica la clave que recibe")
    void noModificaLaClaveRecibida() {
        // Preparar
        char[] clave = "demo".toCharArray();

        // Ejecutar
        servicio.iniciarSesion("medico.demo", clave);

        // Comprobar
        assertArrayEquals("demo".toCharArray(), clave);
    }
}