package com.sescam.radiodiagnostico.informes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InformeServiceImplTest {

    private InformeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InformeServiceImpl();
    }

    // ==========================
    // generarInforme
    // ==========================

    @Test
    @DisplayName("Generar informe correctamente como TECNICO")
    void testGenerarInformeTecnico() {

        assertTrue(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "Fallo de calibración",
                        "TECNICO"));
    }

    @Test
    @DisplayName("Generar informe correctamente como ADMIN")
    void testGenerarInformeAdmin() {

        assertTrue(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "Fallo de calibración",
                        "ADMIN"));
    }

    @Test
    @DisplayName("Aceptar rol con espacios")
    void testRolConEspacios() {

        assertTrue(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "Avería",
                        "  ADMIN  "));
    }

    @Test
    @DisplayName("Aceptar rol en minúsculas")
    void testRolMinusculas() {

        assertTrue(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "Avería",
                        "tecnico"));
    }

    @Test
    @DisplayName("Rechazar rol nulo")
    void testRolNulo() {

        assertFalse(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "Avería",
                        null));
    }

    @Test
    @DisplayName("Rechazar rol sin permisos")
    void testRolInvalido() {

        assertFalse(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "Avería",
                        "USUARIO"));
    }

    @Test
    @DisplayName("Rechazar ID igual a cero")
    void testIdCero() {

        assertFalse(
                service.generarInforme(
                        0,
                        "2026-12-01",
                        "Avería",
                        "TECNICO"));
    }

    @Test
    @DisplayName("Rechazar ID negativo")
    void testIdNegativo() {

        assertFalse(
                service.generarInforme(
                        -1,
                        "2026-12-01",
                        "Avería",
                        "TECNICO"));
    }

    @Test
    @DisplayName("Rechazar descripción nula")
    void testDescripcionNula() {

        assertFalse(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        null,
                        "TECNICO"));
    }

    @Test
    @DisplayName("Rechazar descripción vacía")
    void testDescripcionVacia() {

        assertFalse(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "",
                        "TECNICO"));
    }

    @Test
    @DisplayName("Rechazar descripción con espacios")
    void testDescripcionEspacios() {

        assertFalse(
                service.generarInforme(
                        1,
                        "2026-12-01",
                        "     ",
                        "TECNICO"));
    }

    @Test
    @DisplayName("Rechazar fecha inválida")
    void testFechaInvalida() {

        assertFalse(
                service.generarInforme(
                        1,
                        "32-12-2026",
                        "Avería",
                        "TECNICO"));
    }

    @Test
    @DisplayName("Rechazar fecha vacía")
    void testFechaVacia() {

        assertFalse(
                service.generarInforme(
                        1,
                        "",
                        "Avería",
                        "TECNICO"));
    }

    // ==========================
    // consultarInforme
    // ==========================

    @Test
    @DisplayName("Consultar informe correctamente")
    void testConsultarInformeCorrecto() {

        String respuesta =
                service.consultarInforme(
                        10,
                        "ADMIN");

        assertTrue(
                respuesta.contains(
                        "Informe 10"));
    }

    @Test
    @DisplayName("Consultar informe con rol en minúsculas")
    void testConsultarRolMinusculas() {

        String respuesta =
                service.consultarInforme(
                        10,
                        "tecnico");

        assertTrue(
                respuesta.contains(
                        "Informe 10"));
    }

    @Test
    @DisplayName("Consultar informe con rol inválido")
    void testConsultarRolInvalido() {

        String respuesta =
                service.consultarInforme(
                        10,
                        "USUARIO");

        assertEquals(
                "Error de seguridad: Acceso denegado.",
                respuesta);
    }

    @Test
    @DisplayName("Consultar informe con rol nulo")
    void testConsultarRolNulo() {

        String respuesta =
                service.consultarInforme(
                        10,
                        null);

        assertEquals(
                "Error de seguridad: Acceso denegado.",
                respuesta);
    }

    @Test
    @DisplayName("Consultar informe con ID inválido")
    void testConsultarIdInvalido() {

        String respuesta =
                service.consultarInforme(
                        -1,
                        "ADMIN");

        assertEquals(
                "Error: ID de informe inválido.",
                respuesta);
    }

    @Test
    @DisplayName("Consultar informe con ID cero")
    void testConsultarIdCero() {

        String respuesta =
                service.consultarInforme(
                        0,
                        "ADMIN");

        assertEquals(
                "Error: ID de informe inválido.",
                respuesta);
    }
}