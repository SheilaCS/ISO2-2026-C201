
package com.sescam.radiodiagnostico.cliente.agendas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ValidacionesAgendasTest {

    // ------------------------------------
    // PRUEBAS DE VALIDACION DE ID
    // ------------------------------------

    @Test
    @DisplayName("Aceptar un ID positivo")
    public void testIdCorrecto() {
        assertTrue(ValidacionesAgendas.validarId("5"));
    }

    @Test
    @DisplayName("Rechazar un ID igual a cero")
    public void testIdCero() {
        assertFalse(ValidacionesAgendas.validarId("0"));
    }

    @Test
    @DisplayName("Rechazar un ID negativo")
    public void testIdNegativo() {
        assertFalse(ValidacionesAgendas.validarId("-1"));
    }

    @Test
    @DisplayName("Rechazar un ID con letras")
    public void testIdConLetras() {
        assertFalse(ValidacionesAgendas.validarId("abc"));
    }

    @Test
    @DisplayName("Rechazar un ID nulo")
    public void testIdNulo() {
        assertFalse(ValidacionesAgendas.validarId(null));
    }

    @Test
    @DisplayName("Aceptar un ID con espacios")
    public void testIdConEspacios() {
        assertTrue(ValidacionesAgendas.validarId(" 5 "));
    }

    @Test
    @DisplayName("Rechazar un ID vacio")
    public void testIdVacio() {
        assertFalse(ValidacionesAgendas.validarId(""));
    }

    @Test
    @DisplayName("Rechazar un ID demasiado grande")
    public void testIdMuyGrande() {
        assertFalse(ValidacionesAgendas.validarId(
                "999999999999999999999"));
    }

    // ------------------------------------
    // PRUEBAS DE VALIDACION DE FECHA
    // ------------------------------------

    @Test
    @DisplayName("Aceptar una fecha futura")
    public void testFechaFutura() {
        String fecha = LocalDate.now().plusDays(5).toString();

        assertTrue(ValidacionesAgendas.validarFecha(fecha));
    }

    @Test
    @DisplayName("Aceptar la fecha actual")
    public void testFechaActual() {
        String fecha = LocalDate.now().toString();

        assertTrue(ValidacionesAgendas.validarFecha(fecha));
    }

    @Test
    @DisplayName("Rechazar una fecha pasada")
    public void testFechaPasada() {
        String fecha = LocalDate.now().minusDays(1).toString();

        assertFalse(ValidacionesAgendas.validarFecha(fecha));
    }

    @Test
    @DisplayName("Rechazar una fecha con formato incorrecto")
    public void testFechaIncorrecta() {
        assertFalse(ValidacionesAgendas.validarFecha("20/11/2026"));
    }

    @Test
    @DisplayName("Rechazar una fecha nula")
    public void testFechaNula() {
        assertFalse(ValidacionesAgendas.validarFecha(null));
    }

    @Test
    @DisplayName("Rechazar una fecha vacia")
    public void testFechaVacia() {
        assertFalse(ValidacionesAgendas.validarFecha(""));
    }

    @Test
    @DisplayName("Rechazar una fecha imposible")
    public void testFechaImposible() {
        assertFalse(ValidacionesAgendas.validarFecha("2026-13-40"));
    }

    // ------------------------------------
    // PRUEBAS DE VALIDACION DE HORA
    // ------------------------------------

    @Test
    @DisplayName("Aceptar una hora correcta")
    public void testHoraCorrecta() {
        assertTrue(ValidacionesAgendas.validarHora("09:00"));
    }

    @Test
    @DisplayName("Rechazar una hora con formato incorrecto")
    public void testHoraFormatoIncorrecto() {
        assertFalse(ValidacionesAgendas.validarHora("9:00"));
    }

    @Test
    @DisplayName("Rechazar una hora fuera de rango")
    public void testHoraFueraDeRango() {
        assertFalse(ValidacionesAgendas.validarHora("25:00"));
    }

    @Test
    @DisplayName("Rechazar una hora nula")
    public void testHoraNula() {
        assertFalse(ValidacionesAgendas.validarHora(null));
    }

    @Test
    @DisplayName("Rechazar una hora vacia")
    public void testHoraVacia() {
        assertFalse(ValidacionesAgendas.validarHora(""));
    }

    @Test
    @DisplayName("Rechazar una hora con letras")
    public void testHoraConLetras() {
        assertFalse(ValidacionesAgendas.validarHora("aa:bb"));
    }

    @Test
    @DisplayName("Comprobar los valores limite de hora")
    public void testHoraLimite() {
        assertAll(
            () -> assertTrue(
                    ValidacionesAgendas.validarHora("00:00")),

            () -> assertTrue(
                    ValidacionesAgendas.validarHora("23:59")),

            () -> assertFalse(
                    ValidacionesAgendas.validarHora("24:00")),

            () -> assertFalse(
                    ValidacionesAgendas.validarHora("12:60"))
        );
    }

    // ------------------------------------
    // PRUEBAS DE RANGO HORARIO
    // ------------------------------------

    @Test
    @DisplayName("Aceptar un rango horario correcto")
    public void testRangoCorrecto() {
        assertTrue(ValidacionesAgendas.validarRangoHorario(
                "09:00", "10:00"));
    }

    @Test
    @DisplayName("Rechazar horas iguales")
    public void testRangoHorasIguales() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "09:00", "09:00"));
    }

    @Test
    @DisplayName("Rechazar un rango horario invertido")
    public void testRangoInvertido() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "12:00", "10:00"));
    }

    @Test
    @DisplayName("Rechazar una hora inicial incorrecta")
    public void testRangoInicioIncorrecto() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "25:00", "12:00"));
    }

    @Test
    @DisplayName("Rechazar una hora final incorrecta")
    public void testRangoFinIncorrecto() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "09:00", "25:00"));
    }

    @Test
    @DisplayName("Rechazar una hora inicial nula")
    public void testRangoInicioNulo() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                null, "10:00"));
    }

    @Test
    @DisplayName("Rechazar una hora final nula")
    public void testRangoFinNulo() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "09:00", null));
    }

    @Test
    @DisplayName("Rechazar ambas horas nulas")
    public void testRangoHorasNulas() {
        assertFalse(ValidacionesAgendas.validarRangoHorario(
                null, null));
    }

    // ------------------------------------
    // PRUEBA DEL CONSTRUCTOR
    // ------------------------------------

    @Test
    @DisplayName("Comprobar el constructor")
    public void testConstructor() {

        // Preparar y ejecutar
        ValidacionesAgendas validaciones =
                new ValidacionesAgendas();

        // Comprobar
        assertNotNull(validaciones);
    }
}
