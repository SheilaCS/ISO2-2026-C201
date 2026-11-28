package com.sescam.radiodiagnostico.cliente.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SeguridadHttpClientTest {

    private HttpServer servidor;

    private SeguridadHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String authRecibida;
    private String cuerpoRecibido;

    @BeforeEach
    void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext(
                "/seguridad",
                exchange -> {

                    rutaRecibida =
                            exchange.getRequestURI()
                                    .toString();

                    metodoRecibido =
                            exchange.getRequestMethod();

                    authRecibida =
                            exchange.getRequestHeaders()
                                    .getFirst(
                                            "Authorization");

                    cuerpoRecibido =
                            new String(
                                    exchange.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8);

                    byte[] respuesta =
                            "OK".getBytes(
                                    StandardCharsets.UTF_8);

                    exchange.sendResponseHeaders(
                            200,
                            respuesta.length);

                    exchange.getResponseBody()
                            .write(respuesta);

                    exchange.close();
                });

        servidor.start();

        cliente =
                new SeguridadHttpClient(
                        "http://127.0.0.1:"
                        + servidor.getAddress().getPort()
                        + "/seguridad");
    }

    @AfterEach
    void finalizar() {

        servidor.stop(0);
    }

    @Test
    void registrarAccion()
            throws Exception {

        cliente.setCredenciales(
                "admin.demo",
                "demo");

        SeguridadHttpClient.Respuesta r =
                cliente.registrarAccion(
                        "LOGIN",
                        "correcto");

        assertEquals(
                200,
                r.codigo());

        assertEquals(
                "/seguridad/registrar",
                rutaRecibida);

        assertEquals(
                "POST",
                metodoRecibido);

        assertNotNull(
                authRecibida);

        assertTrue(
                cuerpoRecibido.contains(
                        "accion=LOGIN"));
    }

    @Test
    void obtenerLogs()
            throws Exception {

        cliente.setCredenciales(
                "admin.demo",
                "demo");

        SeguridadHttpClient.Respuesta r =
                cliente.obtenerLogs();

        assertEquals(
                200,
                r.codigo());

        assertEquals(
                "/seguridad/logs",
                rutaRecibida);

        assertEquals(
                "GET",
                metodoRecibido);
    }

    @Test
    void credencialesConEspacios() {

        cliente.setCredenciales(
                " admin.demo ",
                "demo");

        assertDoesNotThrow(
                () -> cliente.obtenerLogs());
    }

    @Test
    void constructorNormal() {

        SeguridadHttpClient c =
                new SeguridadHttpClient();

        assertNotNull(c);
    }

    @Test
    void constructorParametrizado() {

        SeguridadHttpClient c =
                new SeguridadHttpClient(
                        "http://localhost");

        assertNotNull(c);
    }

    @Test
    void respuestaCorrecta() {

        SeguridadHttpClient.Respuesta r =
                new SeguridadHttpClient.Respuesta(
                        200,
                        "OK");

        assertTrue(
                r.correcta());
    }

    @Test
    void respuestaIncorrecta() {

        SeguridadHttpClient.Respuesta r =
                new SeguridadHttpClient.Respuesta(
                        403,
                        "Error");

        assertFalse(
                r.correcta());
    }

        
    @Test
void credencialesNulas() {

    cliente.setCredenciales(
            null,
            null);

    assertDoesNotThrow(
            () -> cliente.obtenerLogs());
}

@Test
void respuesta299() {

    SeguridadHttpClient.Respuesta r =
            new SeguridadHttpClient.Respuesta(
                    299,
                    "ok");

    assertTrue(r.correcta());
}

@Test
void respuesta300() {

    SeguridadHttpClient.Respuesta r =
            new SeguridadHttpClient.Respuesta(
                    300,
                    "error");

    assertFalse(
            r.correcta());
}

@Test
void registrarAccionNull()
        throws Exception {

    cliente.setCredenciales(
            "admin.demo",
            "demo");

    assertDoesNotThrow(
            () -> cliente.registrarAccion(
                    null,
                    null));
}
}