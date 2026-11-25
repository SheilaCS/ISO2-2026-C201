package com.sescam.radiodiagnostico.cliente.informes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidacionesInformesTest {

    // ==========================
    // validarId
    // ==========================

    @Test
    @DisplayName("Aceptar ID positivo")
    void testValidarIdCorrecto() {
        assertTrue(
                ValidacionesInformes.validarId("10"));
    }

    @Test
    @DisplayName("Aceptar ID positivo con espacios")
    void testValidarIdConEspacios() {
        assertTrue(
                ValidacionesInformes.validarId(" 10 "));
    }

    @Test
    @DisplayName("Rechazar ID cero")
    void testValidarIdCero() {
        assertFalse(
                ValidacionesInformes.validarId("0"));
    }

    @Test
    @DisplayName("Rechazar ID negativo")
    void testValidarIdNegativo() {
        assertFalse(
                ValidacionesInformes.validarId("-1"));
    }

    @Test
    @DisplayName("Rechazar ID vacío")
    void testValidarIdVacio() {
        assertFalse(
                ValidacionesInformes.validarId(""));
    }

    @Test
    @DisplayName("Rechazar ID no numérico")
    void testValidarIdTexto() {
        assertFalse(
                ValidacionesInformes.validarId("abc"));
    }

    @Test
    @DisplayName("Rechazar ID nulo")
    void testValidarIdNulo() {
        assertFalse(
                ValidacionesInformes.validarId(null));
    }

    // ==========================
    // validarTexto
    // ==========================

    @Test
    @DisplayName("Aceptar texto válido")
    void testValidarTextoCorrecto() {
        assertTrue(
                ValidacionesInformes.validarTexto(
                        "Fallo de calibración"));
    }

    @Test
    @DisplayName("Aceptar texto con espacios")
    void testValidarTextoConEspacios() {
        assertTrue(
                ValidacionesInformes.validarTexto(
                        " Avería "));
    }

    @Test
    @DisplayName("Rechazar texto vacío")
    void testValidarTextoVacio() {
        assertFalse(
                ValidacionesInformes.validarTexto(""));
    }

    @Test
    @DisplayName("Rechazar texto con espacios")
    void testValidarTextoEspacios() {
        assertFalse(
                ValidacionesInformes.validarTexto("   "));
    }

    @Test
    @DisplayName("Rechazar texto nulo")
    void testValidarTextoNulo() {
        assertFalse(
                ValidacionesInformes.validarTexto(null));
    }

    // ==========================
    // validarFecha
    // ==========================

    @Test
    @DisplayName("Aceptar fecha válida")
    void testFechaValida() {
        assertTrue(
                ValidacionesInformes.validarFecha(
                        "2026-12-01"));
    }

    @Test
    @DisplayName("Aceptar fecha bisiesta válida")
    void testFechaBisiesta() {
        assertTrue(
                ValidacionesInformes.validarFecha(
                        "2024-02-29"));
    }

    @Test
    @DisplayName("Rechazar fecha inválida")
    void testFechaInvalida() {
        assertFalse(
                ValidacionesInformes.validarFecha(
                        "2026-15-40"));
    }

    @Test
    @DisplayName("Rechazar formato incorrecto")
    void testFechaFormatoIncorrecto() {
        assertFalse(
                ValidacionesInformes.validarFecha(
                        "01/12/2026"));
    }

    @Test
    @DisplayName("Rechazar fecha vacía")
    void testFechaVacia() {
        assertFalse(
                ValidacionesInformes.validarFecha(""));
    }

    @Test
    @DisplayName("Rechazar fecha nula")
    void testFechaNula() {
        assertFalse(
                ValidacionesInformes.validarFecha(null));
    }

    @Test
    @DisplayName("Aceptar fecha con espacios")
    void testFechaConEspacios() {
        assertTrue(
                ValidacionesInformes.validarFecha(
                        " 2026-12-01 "));
    }

    // ==========================
    // tienePermiso
    // ==========================

    @Test
    @DisplayName("Aceptar rol TECNICO")
    void testPermisoTecnico() {
        assertTrue(
                ValidacionesInformes.tienePermiso(
                        "TECNICO"));
    }

    @Test
    @DisplayName("Aceptar rol ADMIN")
    void testPermisoAdmin() {
        assertTrue(
                ValidacionesInformes.tienePermiso(
                        "ADMIN"));
    }

    @Test
    @DisplayName("Aceptar rol en minúsculas")
    void testPermisoMinusculas() {
        assertTrue(
                ValidacionesInformes.tienePermiso(
                        "tecnico"));
    }

    @Test
    @DisplayName("Aceptar rol con espacios")
    void testPermisoConEspacios() {
        assertTrue(
                ValidacionesInformes.tienePermiso(
                        "  admin  "));
    }

    @Test
    @DisplayName("Rechazar rol distinto")
    void testPermisoRolInvalido() {
        assertFalse(
                ValidacionesInformes.tienePermiso(
                        "USUARIO"));
    }

    @Test
    @DisplayName("Rechazar rol vacío")
    void testPermisoVacio() {
        assertFalse(
                ValidacionesInformes.tienePermiso(""));
    }

    @Test
    @DisplayName("Rechazar rol nulo")
    void testPermisoNulo() {
        assertFalse(
                ValidacionesInformes.tienePermiso(null));
    }
}