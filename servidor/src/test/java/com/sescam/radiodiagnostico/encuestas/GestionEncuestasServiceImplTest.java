package com.sescam.radiodiagnostico.encuestas;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class GestionEncuestasServiceImplTest {
    private GestionEncuestasServiceImpl servicio;
    private boolean propia, realizada;
    @BeforeEach
    void setUp() {
        propia = true; realizada = true;
        servicio = new GestionEncuestasServiceImpl(new IEncuestasProveedor() {
            public boolean citaPertenecePaciente(int idCita, int idPaciente) { return propia; }
            public boolean pruebaRealizada(int idCita) { return realizada; }
        });
    }
    @Test
    @DisplayName("Incidencia: Incidencia registrada correctamente (caso 1)")
    void incidencia0() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 1, "a");
        // Comprobar
        assertAll(() -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Incidencia registrada correctamente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: Identificadores no validos (caso 2)")
    void incidencia1() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(0, 1, "a");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Identificadores no validos", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: Identificadores no validos (caso 3)")
    void incidencia2() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 0, "a");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Identificadores no validos", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: Descripcion obligatoria (maximo 500 caracteres) (caso 4)")
    void incidencia3() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 1, null);
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Descripcion obligatoria (maximo 500 caracteres)", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: Descripcion obligatoria (maximo 500 caracteres) (caso 5)")
    void incidencia4() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 1, "  ");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Descripcion obligatoria (maximo 500 caracteres)", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: Incidencia registrada correctamente (caso 6)")
    void incidencia5() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 1, "a".repeat(500));
        // Comprobar
        assertAll(() -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Incidencia registrada correctamente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: Descripcion obligatoria (maximo 500 caracteres) (caso 7)")
    void incidencia6() throws Exception {
        // Preparar
        propia = true;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 1, "a".repeat(501));
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Descripcion obligatoria (maximo 500 caracteres)", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Incidencia: La cita no pertenece al paciente (caso 8)")
    void incidencia7() throws Exception {
        // Preparar
        propia = false;
        // Ejecutar
        var resultado = servicio.comunicarIncidencia(1, 1, "a");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("La cita no pertenece al paciente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: Encuesta registrada correctamente (caso 1)")
    void encuesta0() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 1, null);
        // Comprobar
        assertAll(() -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Encuesta registrada correctamente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: Encuesta registrada correctamente (caso 2)")
    void encuesta1() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 3, "bien");
        // Comprobar
        assertAll(() -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Encuesta registrada correctamente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: Encuesta registrada correctamente (caso 3)")
    void encuesta2() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 5, "a".repeat(500));
        // Comprobar
        assertAll(() -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Encuesta registrada correctamente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: Identificadores no validos (caso 4)")
    void encuesta3() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(0, 1, 3, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Identificadores no validos", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: Identificadores no validos (caso 5)")
    void encuesta4() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 0, 3, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Identificadores no validos", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: La puntuacion debe estar entre 1 y 5 (caso 6)")
    void encuesta5() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 0, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("La puntuacion debe estar entre 1 y 5", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: La puntuacion debe estar entre 1 y 5 (caso 7)")
    void encuesta6() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 6, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("La puntuacion debe estar entre 1 y 5", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: Comentario demasiado largo (caso 8)")
    void encuesta7() throws Exception {
        // Preparar
        propia = true; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 3, "a".repeat(501));
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Comentario demasiado largo", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: La cita no pertenece al paciente (caso 9)")
    void encuesta8() throws Exception {
        // Preparar
        propia = false; realizada = true;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 3, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("La cita no pertenece al paciente", resultado.getMensaje()));
    }

    @Test
    @DisplayName("Encuesta: La prueba todavia no se ha realizado (caso 10)")
    void encuesta9() throws Exception {
        // Preparar
        propia = true; realizada = false;
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 3, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("La prueba todavia no se ha realizado", resultado.getMensaje()));
    }

    @Test
    @DisplayName("No permite responder dos veces a la misma cita")
    void duplicada() throws Exception {
        // Preparar
        assertTrue(servicio.responderEncuesta(1, 1, 3, "").isCorrecto());
        // Ejecutar
        var resultado = servicio.responderEncuesta(1, 1, 5, "");
        // Comprobar
        assertAll(() -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Ya existe una encuesta para esta cita", resultado.getMensaje()));
    }


    @Test
    @DisplayName("El servicio por defecto rechaza identificadores invalidos")
    void constructorPorDefecto() {
        // Preparar
        var servicioReal = new GestionEncuestasServiceImpl();
        // Ejecutar
        var resultado = servicioReal.comunicarIncidencia(0, 1, "Incidencia");
        // Comprobar
        assertFalse(resultado.isCorrecto());
        assertEquals("Identificadores no validos", resultado.getMensaje());
    }

}
