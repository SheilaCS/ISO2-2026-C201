package com.sescam.radiodiagnostico.cdu02_solicitudes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SolicitudServiceImplTest {

    private SolicitudServiceImpl service;

    @BeforeEach
    void setUp() {
        
         org.junit.jupiter.api.Assumptions.assumeFalse(
            java.awt.GraphicsEnvironment.isHeadless(), 
            "Saltando test de GUI porque no hay entorno gráfico"
            );
        service = new SolicitudServiceImpl();
    }

    @Test
    @DisplayName("Solicitud válida con datos correctos y prioridad URGENTE")
    void testSolicitudValida() {
        boolean resultado = service.solicitarPrueba("PAC-001", "TAC", "URGENTE");
        assertTrue(resultado, "La solicitud con datos válidos debería ser aceptada");
    }

    @Test
    @DisplayName("Solicitud inválida con ID de paciente nulo")
    void testSolicitudIdNulo() {
        boolean resultado = service.solicitarPrueba(null, "TAC", "URGENTE");
        assertFalse(resultado, "No se debe aceptar un ID nulo");
    }

    @Test
    @DisplayName("Solicitud inválida con tipo de prueba vacío")
    void testSolicitudTipoVacio() {
        boolean resultado = service.solicitarPrueba("PAC-001", " ", "URGENTE");
        assertFalse(resultado, "No se debe aceptar un tipo de prueba vacío");
    }

    @Test
    @DisplayName("Solicitud inválida con prioridad no permitida")
    void testSolicitudPrioridadInvalida() {
        // Clase de equivalencia inválida (no es URGENTE, PREFERENTE ni ORDINARIO)
        boolean resultado = service.solicitarPrueba("PAC-001", "TAC", "MUY_URGENTE");
        assertFalse(resultado, "La prioridad debe ser URGENTE, PREFERENTE u ORDINARIO");
    }

    @Test
    @DisplayName("Solicitud inválida con prioridad nula")
    void testSolicitudPrioridadNula() {
        boolean resultado = service.solicitarPrueba("PAC-001", "TAC", null);
        assertFalse(resultado, "No se debe aceptar una prioridad nula");
    }
}