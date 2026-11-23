package com.sescam.radiodiagnostico.cliente.listaespera;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU15 · Cliente · ValidacionesListaEspera")
class ValidacionesListaEsperaTest {

    @Test
    @DisplayName("Un usuario con texto es válido")
    void usuarioValido() {
        assertTrue(ValidacionesListaEspera.validarUsuario("administrativo.demo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Un usuario nulo, vacío o con solo espacios no es válido")
    void usuarioInvalido(String usuario) {
        assertFalse(ValidacionesListaEspera.validarUsuario(usuario));
    }

    @Test
    @DisplayName("Una contraseña con caracteres es válida")
    void claveValida() {
        assertTrue(ValidacionesListaEspera.validarClave("demo".toCharArray()));
    }

    @Test
    @DisplayName("Una contraseña nula o vacía no es válida")
    void claveInvalida() {
        assertFalse(ValidacionesListaEspera.validarClave(null));
        assertFalse(ValidacionesListaEspera.validarClave(new char[0]));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-12-10", " 2026-12-10 ", "2028-02-29"})
    @DisplayName("Una fecha AAAA-MM-DD que existe es válida")
    void fechaValida(String fecha) {
        assertTrue(ValidacionesListaEspera.validarFecha(fecha));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ", "10-12-2026", "2026/12/10", "ayer",
            "2026-13-01", "2026-02-30", "2027-02-29"})
    @DisplayName("Una fecha nula, mal escrita o inexistente no es válida")
    void fechaInvalida(String fecha) {
        assertFalse(ValidacionesListaEspera.validarFecha(fecha));
    }
}
