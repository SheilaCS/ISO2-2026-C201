
package com.sescam.radiodiagnostico.cliente.agendas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class ValidacionesAgendasTest {

    // ------------------------------------
    // PRUEBAS DE VALIDACIÓN DE ID
    // ------------------------------------

    @Test
    @DisplayName("Aceptar un ID positivo")
    public void testIdCorrecto() {

        // Preparar
        String id = "5";

        // Ejecutar
        boolean resultado = ValidacionesAgendas.validarId(id);

        // Comprobar
        assertTrue(resultado);
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
    @DisplayName("Rechazar un ID que contiene letras")
    public void testIdConLetras() {

        assertFalse(ValidacionesAgendas.validarId("abc"));
    }

    @Test
    @DisplayName("Rechazar un ID nulo")
    public void testIdNulo() {

        assertFalse(ValidacionesAgendas.validarId(null));
    }

    @Test
    @DisplayName("Comprobar varios valores límite de ID")
    public void testLimitesIdConAssertAll() {

        assertAll(
            () -> assertFalse(ValidacionesAgendas.validarId("-1")),
            () -> assertFalse(ValidacionesAgendas.validarId("0")),
            () -> assertTrue(ValidacionesAgendas.validarId("1")),
            () -> assertTrue(ValidacionesAgendas.validarId("2"))
        );
    }

    // ------------------------------------
    // PRUEBAS DE VALIDACIÓN DE FECHA
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
    @DisplayName("Comprobar valores límite de fecha")
    public void testLimitesFechaConAssertAll() {

        String ayer = LocalDate.now().minusDays(1).toString();
        String hoy = LocalDate.now().toString();
        String manana = LocalDate.now().plusDays(1).toString();

        assertAll(
            () -> assertFalse(ValidacionesAgendas.validarFecha(ayer)),
            () -> assertTrue(ValidacionesAgendas.validarFecha(hoy)),
            () -> assertTrue(ValidacionesAgendas.validarFecha(manana))
        );
    }

    // ------------------------------------
    // PRUEBAS DE VALIDACIÓN DE HORA
    // ------------------------------------

    @Test
    @DisplayName("Aceptar una hora correcta")
    public void testHoraCorrecta() {

        assertTrue(ValidacionesAgendas.validarHora("09:00"));
    }

    @Test
    @DisplayName("Rechazar una hora sin dos cifras")
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
    @DisplayName("Comprobar horas límite")
    public void testLimitesHoraConAssertAll() {

        assertAll(
            () -> assertTrue(ValidacionesAgendas.validarHora("00:00")),
            () -> assertTrue(ValidacionesAgendas.validarHora("23:59")),
            () -> assertFalse(ValidacionesAgendas.validarHora("24:00")),
            () -> assertFalse(ValidacionesAgendas.validarHora("12:60"))
        );
    }

    // ------------------------------------
    // PRUEBAS DEL RANGO HORARIO
    // ------------------------------------

    @Test
    @DisplayName("Aceptar un rango horario correcto")
    public void testRangoCorrecto() {

        assertTrue(ValidacionesAgendas.validarRangoHorario(
                "09:00", "10:00"));
    }

    @Test
    @DisplayName("Rechazar horas de inicio y fin iguales")
    public void testRangoHorasIguales() {

        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "09:00", "09:00"));
    }

    @Test
    @DisplayName("Rechazar una hora final anterior a la inicial")
    public void testRangoIncorrecto() {

        assertFalse(ValidacionesAgendas.validarRangoHorario(
                "12:00", "10:00"));
    }

    @Test
    @DisplayName("Rechazar un rango con horas incorrectas")
    public void testRangoConHoraInvalida() {

        assertAll(
            () -> assertFalse(
                ValidacionesAgendas.validarRangoHorario("9:00", "10:00")),
            () -> assertFalse(
                ValidacionesAgendas.validarRangoHorario("09:00", "25:00")),
            () -> assertFalse(
                ValidacionesAgendas.validarRangoHorario(null, "10:00"))
        );
    }
}
