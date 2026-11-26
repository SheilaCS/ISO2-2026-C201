package com.sescam.radiodiagnostico.cliente.realizacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validaciones de realizacion de pruebas (CDU7)")
class ValidacionesRealizacionTest {

    @Test
    @DisplayName("Valida IDs positivos, fechas no futuras y observaciones o incidencias")
    void validaCamposCorrectosEIncorrectos() {
        assertAll(
                () -> assertTrue(ValidacionesRealizacion.validarIdPositivo("1")),
                () -> assertFalse(ValidacionesRealizacion.validarIdPositivo(null)),
                () -> assertFalse(ValidacionesRealizacion.validarIdPositivo("   ")),
                () -> assertFalse(ValidacionesRealizacion.validarIdPositivo("0")),
                () -> assertFalse(ValidacionesRealizacion.validarIdPositivo("abc")),

                () -> assertTrue(ValidacionesRealizacion.validarFechaRealizacion(LocalDate.now().toString())),
                () -> assertFalse(ValidacionesRealizacion.validarFechaRealizacion(null)),
                () -> assertFalse(ValidacionesRealizacion.validarFechaRealizacion("  ")),
                () -> assertFalse(ValidacionesRealizacion.validarFechaRealizacion(LocalDate.now().plusDays(1).toString())),
                () -> assertFalse(ValidacionesRealizacion.validarFechaRealizacion("fecha-mal")),

                () -> assertTrue(ValidacionesRealizacion.validarObservacionesOIncidencia("OK", "")),
                () -> assertTrue(ValidacionesRealizacion.validarObservacionesOIncidencia("", "Incidencia")),
                () -> assertTrue(ValidacionesRealizacion.validarObservacionesOIncidencia(null, "Incidencia")),
                () -> assertFalse(ValidacionesRealizacion.validarObservacionesOIncidencia(null, null)),
                () -> assertFalse(ValidacionesRealizacion.validarObservacionesOIncidencia("", "   "))
        );
    }
}