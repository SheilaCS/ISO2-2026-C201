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
        assertEquals("", ValidacionesPacientes.normalizarCip(null));
    }

    @Test
    @DisplayName("Valida o rechaza CIP segun contenido")
    void validarCipCasos() {
        assertTrue(ValidacionesPacientes.validarCip("cip001"));
        assertFalse(ValidacionesPacientes.validarCip("   "));
        assertFalse(ValidacionesPacientes.validarCip(null));
    }

    @Test
    @DisplayName("Valida texto obligatorio para nombre y apellidos")
    void validarTextoObligatorioCasos() {
        assertTrue(ValidacionesPacientes.validarTextoObligatorio("Ana"));
        assertFalse(ValidacionesPacientes.validarTextoObligatorio("   "));
        assertFalse(ValidacionesPacientes.validarTextoObligatorio(""));
        assertFalse(ValidacionesPacientes.validarTextoObligatorio(null));
    }

    @Test
    @DisplayName("Acepta fecha de nacimiento hoy o pasada y rechaza invalidas, vacias o futuras")
    void validarFechaNacimientoLimites() {
        assertTrue(ValidacionesPacientes.validarFechaNacimiento(LocalDate.now().toString()));
        assertTrue(ValidacionesPacientes.validarFechaNacimiento("1990-05-15"));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento(LocalDate.now().plusDays(1).toString()));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento("fecha-invalida"));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento("   "));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento(null));
    }

    @Test
    @DisplayName("Valida ID positivo y rechaza cero, negativos, vacios, nulos o texto")
    void validarIdPositivo() {
        assertTrue(ValidacionesPacientes.validarId("1"));
        assertTrue(ValidacionesPacientes.validarId(" 42 "));
        assertFalse(ValidacionesPacientes.validarId("0"));
        assertFalse(ValidacionesPacientes.validarId("-3"));
        assertFalse(ValidacionesPacientes.validarId("   "));
        assertFalse(ValidacionesPacientes.validarId(null));
        assertFalse(ValidacionesPacientes.validarId("abc"));
    }
}