
package com.sescam.radiodiagnostico.cliente.miscitas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ValidacionesMisCitasTest {

    // ------------------------------------
    // PRUEBAS DE VALIDACION DE ID
    // ------------------------------------

    @Test
    @DisplayName("Aceptar un ID positivo")
    public void testIdCorrecto() {
        assertTrue(ValidacionesMisCitas.validarId("5"));
    }

    @Test
    @DisplayName("Aceptar el ID minimo valido")
    public void testIdMinimo() {
        assertTrue(ValidacionesMisCitas.validarId("1"));
    }

    @Test
    @DisplayName("Rechazar un ID igual a cero")
    public void testIdCero() {
        assertFalse(ValidacionesMisCitas.validarId("0"));
    }

    @Test
    @DisplayName("Rechazar un ID negativo")
    public void testIdNegativo() {
        assertFalse(ValidacionesMisCitas.validarId("-1"));
    }

    @Test
    @DisplayName("Rechazar un ID con letras")
    public void testIdConLetras() {
        assertFalse(ValidacionesMisCitas.validarId("abc"));
    }

    @Test
    @DisplayName("Rechazar un ID vacio")
    public void testIdVacio() {
        assertFalse(ValidacionesMisCitas.validarId(""));
    }

    @Test
    @DisplayName("Rechazar un ID nulo")
    public void testIdNulo() {
        assertFalse(ValidacionesMisCitas.validarId(null));
    }

    @Test
    @DisplayName("Aceptar un ID con espacios")
    public void testIdConEspacios() {
        assertTrue(ValidacionesMisCitas.validarId(" 5 "));
    }

    @Test
    @DisplayName("Rechazar un ID decimal")
    public void testIdDecimal() {
        assertFalse(ValidacionesMisCitas.validarId("1.5"));
    }

    // ------------------------------------
    // PRUEBAS DE VALIDACION DEL MOTIVO
    // ------------------------------------

    @Test
    @DisplayName("Aceptar un motivo correcto")
    public void testMotivoCorrecto() {
        assertTrue(ValidacionesMisCitas.validarMotivo(
                "No puedo asistir ese dia"));
    }

    @Test
    @DisplayName("Aceptar un motivo de un solo caracter")
    public void testMotivoMinimo() {
        assertTrue(ValidacionesMisCitas.validarMotivo("a"));
    }

    @Test
    @DisplayName("Aceptar un motivo con la longitud maxima")
    public void testMotivoLongitudMaxima() {
        assertTrue(ValidacionesMisCitas.validarMotivo(
                "a".repeat(200)));
    }

    @Test
    @DisplayName("Rechazar un motivo demasiado largo")
    public void testMotivoDemasiadoLargo() {
        assertFalse(ValidacionesMisCitas.validarMotivo(
                "a".repeat(201)));
    }

    @Test
    @DisplayName("Rechazar un motivo vacio")
    public void testMotivoVacio() {
        assertFalse(ValidacionesMisCitas.validarMotivo(""));
    }

    @Test
    @DisplayName("Rechazar un motivo con solo espacios")
    public void testMotivoSoloEspacios() {
        assertFalse(ValidacionesMisCitas.validarMotivo("   "));
    }

    @Test
    @DisplayName("Rechazar un motivo nulo")
    public void testMotivoNulo() {
        assertFalse(ValidacionesMisCitas.validarMotivo(null));
    }

    @Test
    @DisplayName("Aceptar un motivo de 199 caracteres")
    public void testMotivoUnoMenosQueElMaximo() {
        assertTrue(ValidacionesMisCitas.validarMotivo(
                "a".repeat(199)));
    }

    @Test
    @DisplayName("Aceptar un motivo de 200 caracteres con espacios")
    public void testMotivoLongitudMaximaConEspacios() {
        assertTrue(ValidacionesMisCitas.validarMotivo(
                "  " + "a".repeat(200) + "  "));
    }

    @Test
    @DisplayName("Rechazar un motivo con solo saltos de linea y tabuladores")
    public void testMotivoSoloSeparadores() {
        assertFalse(ValidacionesMisCitas.validarMotivo("\n\t \n"));
    }

    @Test
    @DisplayName("Aceptar un motivo con acentos y signos")
    public void testMotivoConAcentos() {
        assertTrue(ValidacionesMisCitas.validarMotivo(
                "Revisión médica, ¿otro día?"));
    }

    @Test
    @DisplayName("La longitud maxima del motivo es 200")
    public void testLongitudMaximaDelMotivo() {
        assertEquals(200, ValidacionesMisCitas.LONGITUD_MAXIMA_MOTIVO);
    }

    // ------------------------------------
    // PRUEBAS DE LOS LIMITES DEL ID
    // ------------------------------------

    @Test
    @DisplayName("Aceptar el mayor entero como ID")
    public void testIdMaximo() {
        assertTrue(ValidacionesMisCitas.validarId("2147483647"));
    }

    @Test
    @DisplayName("Rechazar un ID mayor que el maximo entero")
    public void testIdDesbordado() {
        assertFalse(ValidacionesMisCitas.validarId("2147483648"));
    }

    @Test
    @DisplayName("Rechazar un ID formado solo por espacios")
    public void testIdSoloEspacios() {
        assertFalse(ValidacionesMisCitas.validarId("   "));
    }

    // ------------------------------------
    // PRUEBA DEL CONSTRUCTOR
    // ------------------------------------

    @Test
    @DisplayName("Crear un objeto de la clase de validaciones")
    public void testConstructor() {
        assertNotNull(new ValidacionesMisCitas());
    }
}
