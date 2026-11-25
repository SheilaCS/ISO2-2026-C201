package com.sescam.radiodiagnostico.parametros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParametroServiceImplTest {

    private ParametroServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParametroServiceImpl();
    }

    @Test
    @DisplayName("Guardar y recuperar parámetro")
    void testGuardarParametro() {

        service.guardarParametro(
                "MAX_PACIENTES",
                "100");

        assertEquals(
                "100",
                service.obtenerParametro("MAX_PACIENTES"));
    }

    @Test
    @DisplayName("Recuperar parámetro inexistente")
    void testParametroNoEncontrado() {

        assertEquals(
                "No encontrado",
                service.obtenerParametro("INEXISTENTE"));
    }

    @Test
    @DisplayName("No guardar clave nula")
    void testClaveNula() {

        service.guardarParametro(
                null,
                "100");

        assertEquals(
                "No encontrado",
                service.obtenerParametro(null));
    }

    @Test
    @DisplayName("No guardar valor nulo")
    void testValorNulo() {

        service.guardarParametro(
                "MAX_PACIENTES",
                null);

        assertEquals(
                "No encontrado",
                service.obtenerParametro("MAX_PACIENTES"));
    }

    @Test
    @DisplayName("No guardar clave y valor nulos")
    void testClaveYValorNulos() {

        service.guardarParametro(
                null,
                null);

        assertEquals(
                "No encontrado",
                service.obtenerParametro(null));
    }

    @Test
    @DisplayName("Sobrescribir parámetro existente")
    void testSobrescribirParametro() {

        service.guardarParametro(
                "MAX_PACIENTES",
                "100");

        service.guardarParametro(
                "MAX_PACIENTES",
                "200");

        assertEquals(
                "200",
                service.obtenerParametro("MAX_PACIENTES"));
    }

    @Test
    @DisplayName("Guardar varios parámetros")
    void testGuardarVariosParametros() {

        service.guardarParametro(
                "MAX_PACIENTES",
                "100");

        service.guardarParametro(
                "TIEMPO_ESPERA",
                "30");

        assertEquals(
                "100",
                service.obtenerParametro("MAX_PACIENTES"));

        assertEquals(
                "30",
                service.obtenerParametro("TIEMPO_ESPERA"));
    }
}