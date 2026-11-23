package com.sescam.radiodiagnostico.solicitudes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolicitudServiceImplTest {

    private SolicitudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudServiceImpl();
    }

    @Test
    @DisplayName("Solicitud válida con prioridad URGENTE")
    void testSolicitudUrgente() {
        assertTrue(
            service.solicitarPrueba("PAC-001", "TAC", "URGENTE")
        );
    }

    @Test
    @DisplayName("Solicitud válida con prioridad PREFERENTE")
    void testSolicitudPreferente() {
        assertTrue(
            service.solicitarPrueba("PAC-001", "TAC", "PREFERENTE")
        );
    }

    @Test
    @DisplayName("Solicitud válida con prioridad ORDINARIO")
    void testSolicitudOrdinario() {
        assertTrue(
            service.solicitarPrueba("PAC-001", "TAC", "ORDINARIO")
        );
    }

    @Test
    @DisplayName("Aceptar prioridad en minúsculas")
    void testPrioridadMinusculas() {
        assertTrue(
            service.solicitarPrueba("PAC-001", "TAC", "urgente")
        );
    }

    @Test
    @DisplayName("Aceptar prioridad con espacios")
    void testPrioridadConEspacios() {
        assertTrue(
            service.solicitarPrueba("PAC-001", "TAC", "  preferente  ")
        );
    }

    @Test
    @DisplayName("Tipo de prueba nulo")
    void testTipoNulo() {
        assertFalse(
            service.solicitarPrueba("PAC-001", null, "URGENTE")
        );
    }

    @Test
    @DisplayName("Tipo de prueba vacío")
    void testTipoVacio() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "", "URGENTE")
        );
    }

    @Test
    @DisplayName("Tipo de prueba solo espacios")
    void testTipoEspacios() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "     ", "URGENTE")
        );
    }

    @Test
    @DisplayName("Prioridad nula")
    void testPrioridadNula() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "TAC", null)
        );
    }

    @Test
    @DisplayName("Prioridad vacía")
    void testPrioridadVacia() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "TAC", "")
        );
    }

    @Test
    @DisplayName("Prioridad solo espacios")
    void testPrioridadSoloEspacios() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "TAC", "    ")
        );
    }

    @Test
    @DisplayName("Prioridad inválida")
    void testPrioridadInvalida() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "TAC", "MUY_URGENTE")
        );
    }

    @Test
    @DisplayName("Prioridad distinta de las permitidas")
    void testPrioridadNoReconocida() {
        assertFalse(
            service.solicitarPrueba("PAC-001", "TAC", "MEDIA")
        );
    }

    @Test
    @DisplayName("ID paciente nulo no afecta mientras el resto sea válido")
    void testIdPacienteNulo() {
        assertTrue(
            service.solicitarPrueba(null, "TAC", "URGENTE")
        );
    }

    @Test
    @DisplayName("ID paciente vacío no afecta mientras el resto sea válido")
    void testIdPacienteVacio() {
        assertTrue(
            service.solicitarPrueba("", "TAC", "URGENTE")
        );
    }
}