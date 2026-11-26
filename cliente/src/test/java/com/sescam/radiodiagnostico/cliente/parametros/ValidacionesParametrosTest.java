package com.sescam.radiodiagnostico.cliente.parametros;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidacionesParametrosTest {

    // ==========================
    // validarTexto
    // ==========================

    @Test
    @DisplayName("Aceptar texto válido")
    void testValidarTextoCorrecto() {
        assertTrue(
                ValidacionesParametros.validarTexto("MAX_PACIENTES"));
    }

    @Test
    @DisplayName("Aceptar texto con espacios alrededor")
    void testValidarTextoConEspacios() {
        assertTrue(
                ValidacionesParametros.validarTexto("  MAX_PACIENTES  "));
    }

    @Test
    @DisplayName("Rechazar texto vacío")
    void testValidarTextoVacio() {
        assertFalse(
                ValidacionesParametros.validarTexto(""));
    }

    @Test
    @DisplayName("Rechazar texto formado solo por espacios")
    void testValidarTextoEspacios() {
        assertFalse(
                ValidacionesParametros.validarTexto("     "));
    }

    @Test
    @DisplayName("Rechazar texto nulo")
    void testValidarTextoNulo() {
        assertFalse(
                ValidacionesParametros.validarTexto(null));
    }

    // ==========================
    // validarNumeroPositivo
    // ==========================

    @Test
    @DisplayName("Aceptar número positivo")
    void testNumeroPositivo() {
        assertTrue(
                ValidacionesParametros.validarNumeroPositivo("10"));
    }

    @Test
    @DisplayName("Aceptar cero")
    void testNumeroCero() {
        assertTrue(
                ValidacionesParametros.validarNumeroPositivo("0"));
    }

    @Test
    @DisplayName("Aceptar número positivo con espacios")
    void testNumeroConEspacios() {
        assertTrue(
                ValidacionesParametros.validarNumeroPositivo(" 25 "));
    }

    @Test
    @DisplayName("Rechazar número negativo")
    void testNumeroNegativo() {
        assertFalse(
                ValidacionesParametros.validarNumeroPositivo("-1"));
    }

    @Test
    @DisplayName("Rechazar cadena vacía")
    void testNumeroVacio() {
        assertFalse(
                ValidacionesParametros.validarNumeroPositivo(""));
    }

    @Test
    @DisplayName("Rechazar espacios")
    void testNumeroSoloEspacios() {
        assertFalse(
                ValidacionesParametros.validarNumeroPositivo("    "));
    }

    @Test
    @DisplayName("Rechazar texto no numérico")
    void testNumeroTexto() {
        assertFalse(
                ValidacionesParametros.validarNumeroPositivo("abc"));
    }

    @Test
    @DisplayName("Rechazar valor nulo")
    void testNumeroNulo() {
        assertFalse(
                ValidacionesParametros.validarNumeroPositivo(null));
    }
}