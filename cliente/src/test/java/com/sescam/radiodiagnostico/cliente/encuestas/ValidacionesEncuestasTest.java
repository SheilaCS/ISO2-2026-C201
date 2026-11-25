package com.sescam.radiodiagnostico.cliente.encuestas;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidacionesEncuestasTest {

    @Test
    @DisplayName("validarId: caso 1, resultado true")
    void validarId0() throws Exception {
        // Preparar
        String entrada = "1";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarId: caso 2, resultado true")
    void validarId1() throws Exception {
        // Preparar
        String entrada = "25";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarId: caso 3, resultado false")
    void validarId2() throws Exception {
        // Preparar
        String entrada = "0";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarId: caso 4, resultado false")
    void validarId3() throws Exception {
        // Preparar
        String entrada = "-1";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarId: caso 5, resultado false")
    void validarId4() throws Exception {
        // Preparar
        String entrada = "abc";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarId: caso 6, resultado false")
    void validarId5() throws Exception {
        // Preparar
        String entrada = null;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarId: caso 7, resultado true")
    void validarId6() throws Exception {
        // Preparar
        String entrada = " 2 ";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarId: caso 8, resultado false")
    void validarId7() throws Exception {
        // Preparar
        String entrada = "2147483648";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarId(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarDescripcion: caso 1, resultado true")
    void validarDescripcion0() throws Exception {
        // Preparar
        String entrada = "a";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarDescripcion(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarDescripcion: caso 2, resultado true")
    void validarDescripcion1() throws Exception {
        // Preparar
        String entrada = "a".repeat(500);
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarDescripcion(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarDescripcion: caso 3, resultado false")
    void validarDescripcion2() throws Exception {
        // Preparar
        String entrada = "a".repeat(501);
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarDescripcion(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarDescripcion: caso 4, resultado false")
    void validarDescripcion3() throws Exception {
        // Preparar
        String entrada = "";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarDescripcion(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarDescripcion: caso 5, resultado false")
    void validarDescripcion4() throws Exception {
        // Preparar
        String entrada = "   ";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarDescripcion(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarDescripcion: caso 6, resultado false")
    void validarDescripcion5() throws Exception {
        // Preparar
        String entrada = null;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarDescripcion(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarPuntuacion: caso 1, resultado false")
    void validarPuntuacion0() throws Exception {
        // Preparar
        int entrada = 0;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarPuntuacion(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarPuntuacion: caso 2, resultado true")
    void validarPuntuacion1() throws Exception {
        // Preparar
        int entrada = 1;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarPuntuacion(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarPuntuacion: caso 3, resultado true")
    void validarPuntuacion2() throws Exception {
        // Preparar
        int entrada = 3;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarPuntuacion(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarPuntuacion: caso 4, resultado true")
    void validarPuntuacion3() throws Exception {
        // Preparar
        int entrada = 5;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarPuntuacion(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarPuntuacion: caso 5, resultado false")
    void validarPuntuacion4() throws Exception {
        // Preparar
        int entrada = 6;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarPuntuacion(entrada);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("validarComentario: caso 1, resultado true")
    void validarComentario0() throws Exception {
        // Preparar
        String entrada = null;
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarComentario(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarComentario: caso 2, resultado true")
    void validarComentario1() throws Exception {
        // Preparar
        String entrada = "";
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarComentario(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarComentario: caso 3, resultado true")
    void validarComentario2() throws Exception {
        // Preparar
        String entrada = "a".repeat(500);
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarComentario(entrada);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("validarComentario: caso 4, resultado false")
    void validarComentario3() throws Exception {
        // Preparar
        String entrada = "a".repeat(501);
        // Ejecutar
        boolean resultado = ValidacionesEncuestas.validarComentario(entrada);
        // Comprobar
        assertFalse(resultado);
    }


    @Test
    @DisplayName("El constructor público permite crear una instancia")
    void constructor() {
        // Preparar y ejecutar
        ValidacionesEncuestas validaciones = new ValidacionesEncuestas();
        // Comprobar
        assertNotNull(validaciones);
    }
}
