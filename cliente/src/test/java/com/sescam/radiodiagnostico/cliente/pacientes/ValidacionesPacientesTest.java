package com.sescam.radiodiagnostico.cliente.pacientes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ValidacionesPacientesTest {

    @Test
    @DisplayName("Normaliza el CIP eliminando espacios y pasando a mayusculas")
    void normalizarCipLimpiaEspaciosYMayusculas() {
        assertEquals("CIP12345A", ValidacionesPacientes.normalizarCip("  cip12345a \n"));
    }

    @Test
    @DisplayName("Rechaza CIP vacio o solo espacios")
    void validarCipVacioRechaza() {
        assertFalse(ValidacionesPacientes.validarCip("   "));
        assertFalse(ValidacionesPacientes.validarCip(null));
    }

    @Test
    @DisplayName("Acepta fecha de nacimiento hoy o pasada y rechaza fecha futura")
    void validarFechaNacimientoLimites() {
        assertTrue(ValidacionesPacientes.validarFechaNacimiento(LocalDate.now().toString()));
        assertTrue(ValidacionesPacientes.validarFechaNacimiento("1990-05-15"));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento(LocalDate.now().plusDays(1).toString()));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento("fecha-invalida"));
    }

    @Test
    @DisplayName("Valida ID positivo y rechaza cero, negativos o texto")
    void validarIdPositivo() {
        assertTrue(ValidacionesPacientes.validarId("1"));
        assertFalse(ValidacionesPacientes.validarId("0"));
        assertFalse(ValidacionesPacientes.validarId("-3"));
        assertFalse(ValidacionesPacientes.validarId("abc"));
    }
}