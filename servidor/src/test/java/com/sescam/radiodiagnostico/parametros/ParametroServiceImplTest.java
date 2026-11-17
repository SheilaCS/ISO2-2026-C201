package com.sescam.radiodiagnostico.parametros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParametroServiceImplTest {

    private ParametroServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParametroServiceImpl();
    }

    @Test
    void guardaYObtieneUnParametro() {
        service.guardarParametro("idioma", "es");

        assertEquals("es", service.obtenerParametro("idioma"));
    }

    @Test
    void devuelveNoEncontradoSiLaClaveNoExiste() {
        assertEquals("No encontrado", service.obtenerParametro("inexistente"));
    }

    @Test
    void reemplazaElValorAlGuardarDeNuevoLaMismaClave() {
        service.guardarParametro("idioma", "es");
        service.guardarParametro("idioma", "en");

        assertEquals("en", service.obtenerParametro("idioma"));
    }

    @Test
    void ignoraElGuardadoSiLaClaveEsNula() {
        service.guardarParametro(null, "valor");

        assertEquals("No encontrado", service.obtenerParametro("clave"));
    }

    @Test
    void ignoraElGuardadoSiElValorEsNulo() {
        service.guardarParametro("clave", null);

        assertEquals("No encontrado", service.obtenerParametro("clave"));
    }

    @Test
    void mantieneSeparadosLosValoresDeClavesDistintas() {
        service.guardarParametro("idioma", "es");
        service.guardarParametro("tema", "oscuro");

        assertEquals("es", service.obtenerParametro("idioma"));
        assertEquals("oscuro", service.obtenerParametro("tema"));
    }
}
