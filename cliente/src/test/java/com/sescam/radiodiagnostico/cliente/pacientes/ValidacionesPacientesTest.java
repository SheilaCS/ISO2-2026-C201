package com.sescam.radiodiagnostico.cliente.pacientes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validaciones de pacientes en cliente (CDU4)")
class ValidacionesPacientesTest {

    @Test
    @DisplayName("Normaliza el CIP eliminando espacios y pasando a mayúsculas")
    void normalizarCipLimpiaEspaciosYMayusculas() {
        assertEquals("CIP12345A", ValidacionesPacientes.normalizarCip("  cip12345a \n"));
        assertEquals("", ValidacionesPacientes.normalizarCip(null));
    }

    @Test
    @DisplayName("Valida o rechaza el CIP según tenga contenido")
    void validarCipCasos() {
        assertTrue(ValidacionesPacientes.validarCip("cip001"));
        assertFalse(ValidacionesPacientes.validarCip("   "));
        assertFalse(ValidacionesPacientes.validarCip(null));
    }

    @Test
    @DisplayName("Valida o rechaza textos obligatorios como nombre y apellidos")
    void validarTextoObligatorioCasos() {
        assertTrue(ValidacionesPacientes.validarTextoObligatorio("Ana"));
        assertFalse(ValidacionesPacientes.validarTextoObligatorio("   "));
        assertFalse(ValidacionesPacientes.validarTextoObligatorio(""));
        assertFalse(ValidacionesPacientes.validarTextoObligatorio(null));
    }

    @Test
    @DisplayName("Acepta fechas de nacimiento de hoy o pasadas y rechaza futuras, vacías o inválidas")
    void validarFechaNacimientoLimites() {
        assertTrue(ValidacionesPacientes.validarFechaNacimiento(LocalDate.now().toString()));
        assertTrue(ValidacionesPacientes.validarFechaNacimiento("1990-05-15"));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento(LocalDate.now().plusDays(1).toString()));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento("fecha-invalida"));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento("   "));
        assertFalse(ValidacionesPacientes.validarFechaNacimiento(null));
    }

    @Test
    @DisplayName("Valida UUID de paciente y rechaza cadenas vacías, nulas o no UUID")
    void validarUuidPaciente() {
        String uuidValido = UUID.randomUUID().toString();
        assertTrue(ValidacionesPacientes.validarId(uuidValido));
        assertTrue(ValidacionesPacientes.validarId("  " + uuidValido + "  "));
        assertFalse(ValidacionesPacientes.validarId("1"));
        assertFalse(ValidacionesPacientes.validarId("0"));
        assertFalse(ValidacionesPacientes.validarId("-3"));
        assertFalse(ValidacionesPacientes.validarId("   "));
        assertFalse(ValidacionesPacientes.validarId(null));
        assertFalse(ValidacionesPacientes.validarId("abc"));
    }
}