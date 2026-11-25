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
    @DisplayName("Comprueba existencia de cita, correspondencia con solicitud e IDs invalidos")
    void compruebaCitaYSolicitud() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/citas/consultar", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            int codigo = 404;
            String respuesta = "No encontrada";
            if (query != null && query.contains("id=10")) {
                codigo = 200;
                respuesta = "Cita 10 | Solicitud 2 | ACTIVA";
            } else if (query != null && query.contains("id=50")) {
                codigo = 200;
                respuesta = "No existe la cita";
            }
            byte[] body = respuesta.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        servidor.start();

        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/citas/consultar";
        ProveedorRealizacionHttp proveedor = new ProveedorRealizacionHttp(url);

        assertAll(
                () -> assertTrue(proveedor.existeCita(10)),
                () -> assertFalse(proveedor.existeCita(50)),
                () -> assertFalse(proveedor.existeCita(99)),
                () -> assertFalse(proveedor.existeCita(0)),
                () -> assertTrue(proveedor.correspondeCitaASolicitud(10, 2)),
                () -> assertFalse(proveedor.correspondeCitaASolicitud(10, 99)),
                () -> assertFalse(proveedor.correspondeCitaASolicitud(99, 2)),
                () -> assertFalse(proveedor.correspondeCitaASolicitud(0, 2))
        );
    }

    @Test
    @DisplayName("Lanza IllegalStateException cuando el servidor de citas no esta disponible")
    void lanzaExcepcionSiServidorCaido() {
        ProveedorRealizacionHttp proveedor = new ProveedorRealizacionHttp("http://127.0.0.1:1/citas/consultar");

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> proveedor.existeCita(10)),
                () -> assertThrows(IllegalStateException.class, () -> proveedor.correspondeCitaASolicitud(10, 2))
        );
    }
}