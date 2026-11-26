package com.sescam.radiodiagnostico.cliente.historial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU12 · Cliente · ValidacionesHistorial")
class ValidacionesHistorialTest {

    @ParameterizedTest
    @ValueSource(strings = {"medico.demo", "a", "  paciente.demo  "})
    @DisplayName("Acepta usuarios con contenido")
    void aceptaUsuariosValidos(String usuario) {
        assertTrue(ValidacionesHistorial.validarUsuario(usuario));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Rechaza usuarios nulos, vacíos o en blanco")
    void rechazaUsuariosInvalidos(String usuario) {
        assertFalse(ValidacionesHistorial.validarUsuario(usuario));
    }

    @Test
    @DisplayName("Acepta una contraseña con contenido")
    void aceptaClaveValida() {
        assertTrue(ValidacionesHistorial.validarClave("demo".toCharArray()));
    }

    @Test
    @DisplayName("Rechaza una contraseña nula o vacía")
    void rechazaClavesInvalidas() {
        assertFalse(ValidacionesHistorial.validarClave(null));
        assertFalse(ValidacionesHistorial.validarClave(new char[0]));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "paciente.demo", "P-1", "a_b", "12345", "x", "  x  ",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("Acepta identificadores válidos, incluido el de longitud máxima")
    void aceptaIdentificadoresValidos(String id) {
        assertTrue(ValidacionesHistorial.validarIdPaciente(id));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ", "a b", "a;b", "a&b", "<x>", "a=b", "ñandú",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("Rechaza identificadores vacíos, con símbolos o demasiado largos")
    void rechazaIdentificadoresInvalidos(String id) {
        assertFalse(ValidacionesHistorial.validarIdPaciente(id));
    }

    @Test
    @DisplayName("La longitud máxima del identificador es de 50 caracteres")
    void longitudMaxima() {
        assertEquals(50, ValidacionesHistorial.LONGITUD_MAXIMA_ID);
        assertTrue(ValidacionesHistorial.validarIdPaciente("a".repeat(50)));
        assertFalse(ValidacionesHistorial.validarIdPaciente("a".repeat(51)));
    }
}
