package com.sescam.radiodiagnostico.cliente.solicitudes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidacionesSolicitudesTest {

    // ========= validarId =========

    @Test
    @DisplayName("Validar ID positivo")
    void testValidarIdCorrecto() {
        assertTrue(
            ValidacionesSolicitudes.validarId("123")
        );
    }

    @Test
    @DisplayName("Validar ID positivo con espacios")
    void testValidarIdConEspacios() {
        assertTrue(
            ValidacionesSolicitudes.validarId(" 123 ")
        );
    }

    @Test
    @DisplayName("Rechazar ID cero")
    void testValidarIdCero() {
        assertFalse(
            ValidacionesSolicitudes.validarId("0")
        );
    }

    @Test
    @DisplayName("Rechazar ID negativo")
    void testValidarIdNegativo() {
        assertFalse(
            ValidacionesSolicitudes.validarId("-1")
        );
    }

    @Test
    @DisplayName("Rechazar ID vacío")
    void testValidarIdVacio() {
        assertFalse(
            ValidacionesSolicitudes.validarId("")
        );
    }

    @Test
    @DisplayName("Rechazar ID con espacios")
    void testValidarIdSoloEspacios() {
        assertFalse(
            ValidacionesSolicitudes.validarId("   ")
        );
    }

    @Test
    @DisplayName("Rechazar ID no numérico")
    void testValidarIdTexto() {
        assertFalse(
            ValidacionesSolicitudes.validarId("abc")
        );
    }

    @Test
    @DisplayName("Rechazar ID nulo")
    void testValidarIdNulo() {
        assertFalse(
            ValidacionesSolicitudes.validarId(null)
        );
    }

    // ========= validarTexto =========

    @Test
    @DisplayName("Validar texto correcto")
    void testValidarTextoCorrecto() {
        assertTrue(
            ValidacionesSolicitudes.validarTexto("TAC")
        );
    }

    @Test
    @DisplayName("Validar texto con espacios alrededor")
    void testValidarTextoConEspacios() {
        assertTrue(
            ValidacionesSolicitudes.validarTexto(" TAC ")
        );
    }

    @Test
    @DisplayName("Rechazar texto vacío")
    void testValidarTextoVacio() {
        assertFalse(
            ValidacionesSolicitudes.validarTexto("")
        );
    }

    @Test
    @DisplayName("Rechazar texto solo espacios")
    void testValidarTextoSoloEspacios() {
        assertFalse(
            ValidacionesSolicitudes.validarTexto("   ")
        );
    }

    @Test
    @DisplayName("Rechazar texto nulo")
    void testValidarTextoNulo() {
        assertFalse(
            ValidacionesSolicitudes.validarTexto(null)
        );
    }

    // ========= validarPrioridad =========

    @Test
    @DisplayName("Aceptar prioridad URGENTE")
    void testPrioridadUrgente() {
        assertTrue(
            ValidacionesSolicitudes.validarPrioridad("URGENTE")
        );
    }

    @Test
    @DisplayName("Aceptar prioridad PREFERENTE")
    void testPrioridadPreferente() {
        assertTrue(
            ValidacionesSolicitudes.validarPrioridad("PREFERENTE")
        );
    }

    @Test
    @DisplayName("Aceptar prioridad ORDINARIO")
    void testPrioridadOrdinario() {
        assertTrue(
            ValidacionesSolicitudes.validarPrioridad("ORDINARIO")
        );
    }

    @Test
    @DisplayName("Aceptar prioridad en minúsculas")
    void testPrioridadMinusculas() {
        assertTrue(
            ValidacionesSolicitudes.validarPrioridad("urgente")
        );
    }

    @Test
    @DisplayName("Aceptar prioridad con espacios")
    void testPrioridadConEspacios() {
        assertTrue(
            ValidacionesSolicitudes.validarPrioridad("  preferente  ")
        );
    }

    @Test
    @DisplayName("Rechazar prioridad vacía")
    void testPrioridadVacia() {
        assertFalse(
            ValidacionesSolicitudes.validarPrioridad("")
        );
    }

    @Test
    @DisplayName("Rechazar prioridad solo espacios")
    void testPrioridadSoloEspacios() {
        assertFalse(
            ValidacionesSolicitudes.validarPrioridad("   ")
        );
    }

    @Test
    @DisplayName("Rechazar prioridad no permitida")
    void testPrioridadInvalida() {
        assertFalse(
            ValidacionesSolicitudes.validarPrioridad("MUY_URGENTE")
        );
    }

    @Test
    @DisplayName("Rechazar prioridad nula")
    void testPrioridadNula() {
        assertFalse(
            ValidacionesSolicitudes.validarPrioridad(null)
        );
    }
}