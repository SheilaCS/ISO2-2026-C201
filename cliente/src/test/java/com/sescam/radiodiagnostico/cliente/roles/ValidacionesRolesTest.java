package com.sescam.radiodiagnostico.cliente.roles;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CDU10 · Cliente · ValidacionesRoles")
class ValidacionesRolesTest {

    @Test
    @DisplayName("V1. Las credenciales están completas solo con usuario y contraseña")
    void credencialesCompletas() {
        assertAll(
                () -> assertTrue(ValidacionesRoles.credencialesCompletas("admin.demo", "demo".toCharArray())),
                () -> assertFalse(ValidacionesRoles.credencialesCompletas("  ", "demo".toCharArray())),
                () -> assertFalse(ValidacionesRoles.credencialesCompletas(null, "demo".toCharArray())),
                () -> assertFalse(ValidacionesRoles.credencialesCompletas("admin.demo", new char[0])),
                () -> assertFalse(ValidacionesRoles.credencialesCompletas("admin.demo", null)));
    }

    @Test
    @DisplayName("V2. Un texto obligatorio no puede estar vacío ni ser nulo")
    void textoObligatorio() {
        assertAll(
                () -> assertTrue(ValidacionesRoles.textoObligatorio("medico.demo")),
                () -> assertFalse(ValidacionesRoles.textoObligatorio("   ")),
                () -> assertFalse(ValidacionesRoles.textoObligatorio(null)));
    }
}