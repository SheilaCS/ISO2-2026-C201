package com.sescam.radiodiagnostico.cliente.realizacion;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cliente HTTP de realizacion de pruebas (CDU7)")
class RealizacionHttpClientTest {

    private HttpServer servidor;
    private RealizacionHttpClient cliente;

    @BeforeEach
    void iniciar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/realizaciones", ex -> {
            byte[] resp = "id=1111&idCita=10&estado=REALIZADA".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, resp.length);
            ex.getResponseBody().write(resp);
            ex.close();
        });
        servidor.start();
        cliente = new RealizacionHttpClient("http://127.0.0.1:" + servidor.getAddress().getPort() + "/realizaciones");
        cliente.setCredenciales("medico.demo", "1234");
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("Envia peticiones POST y GET y devuelve la respuesta del servidor")
    void enviaPeticionesHttp() throws Exception {
        String r1 = cliente.registrarRealizacion(10, 2, "2026-11-20", "OK", null);
        String r2 = cliente.consultarPorCita(10);
        String r3 = cliente.listarRealizaciones();

        assertAll(
                () -> assertTrue(r1.contains("REALIZADA")),
                () -> assertTrue(r2.contains("idCita=10")),
                () -> assertTrue(r3.contains("id=1111"))
        );
    }

    @Test
    @DisplayName("Devuelve mensaje de error cuando el servidor responde con codigo >= 400 y cubre constructor por defecto")
    void procesaRespuestaDeErrorHttpYConstructorPorDefecto() throws Exception {
        RealizacionHttpClient clientePorDefecto = new RealizacionHttpClient();
        assertNotNull(clientePorDefecto);

        HttpServer srvError = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srvError.createContext("/realizaciones", ex -> {
            byte[] resp = "No encontrado".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, resp.length);
            ex.getResponseBody().write(resp);
            ex.close();
        });
        srvError.start();

        try {
            RealizacionHttpClient cli = new RealizacionHttpClient(
                    "http://127.0.0.1:" + srvError.getAddress().getPort() + "/realizaciones");
            cli.setCredenciales(null, null);

            String res = cli.consultarPorCita(99);

            assertTrue(res.startsWith("Error 404:"));
        } finally {
            srvError.stop(0);
        }
    }
}