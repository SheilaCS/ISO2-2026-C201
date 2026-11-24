package com.sescam.radiodiagnostico.realizacion;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Proveedor HTTP de realizacion (CDU7)")
class ProveedorRealizacionHttpTest {

    private HttpServer servidor;

    @AfterEach
    void parar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Comprueba existencia de cita (200 y 404) e ID no positivo")
    void compruebaCitaExistenteYNoExistente() throws Exception {
        // Preparar
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/citas", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            int codigo = (query != null && query.contains("idCita=10")) ? 200 : 404;
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        servidor.start();

        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/citas";
        ProveedorRealizacionHttp proveedor = new ProveedorRealizacionHttp(url);

        // Ejecutar y Comprobar
        assertAll(
                () -> assertTrue(proveedor.existeCita(10)),
                () -> assertFalse(proveedor.existeCita(99)),
                () -> assertFalse(proveedor.existeCita(0)),
                () -> assertFalse(proveedor.existeCita(-1))
        );
    }

    @Test
    @DisplayName("Lanza IllegalStateException cuando el servidor de citas no esta disponible")
    void lanzaExcepcionSiServidorCaido() {
        // Preparar
        ProveedorRealizacionHttp proveedor = new ProveedorRealizacionHttp("http://127.0.0.1:1/citas");

        // Ejecutar y Comprobar
        assertThrows(IllegalStateException.class, () -> proveedor.existeCita(10));
    }
}