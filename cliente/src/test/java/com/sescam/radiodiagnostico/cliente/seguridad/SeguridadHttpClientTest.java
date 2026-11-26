package com.sescam.radiodiagnostico.cliente.seguridad;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class SeguridadHttpClientTest {

    private HttpServer servidor;
    private SeguridadHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String datosRecibidos;
    private String tipoContenido;

    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/seguridad/", exchange -> {

            rutaRecibida =
                    exchange.getRequestURI()
                            .toString();

            metodoRecibido =
                    exchange.getRequestMethod();

            tipoContenido =
                    exchange.getRequestHeaders()
                            .getFirst("Content-Type");

            datosRecibidos =
                    new String(
                            exchange.getRequestBody()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8);

            String respuesta =
                    "Operacion correcta";

            byte[] contenido =
                    respuesta.getBytes(
                            StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(
                    200,
                    contenido.length);

            exchange.getResponseBody()
                    .write(contenido);

            exchange.close();
        });

        servidor.start();

        int puerto =
                servidor.getAddress()
                        .getPort();

        cliente = new SeguridadHttpClient(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad");
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Verificar permiso")
    void testVerificarPermiso()
            throws Exception {

        String resultado =
                cliente.verificarPermiso(
                        "juan",
                        "ADMINISTRADOR",
                        "CONSULTAR");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "POST",
                metodoRecibido);

        assertEquals(
                "/seguridad/verificar",
                rutaRecibida);

        String datos =
                URLDecoder.decode(
                        datosRecibidos,
                        StandardCharsets.UTF_8);

        assertEquals(
                "usuario=juan&rol=ADMINISTRADOR&accion=CONSULTAR",
                datos);

        assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                tipoContenido);
    }

    @Test
    @DisplayName("Obtener logs")
    void testObtenerLogs()
            throws Exception {

        String resultado =
                cliente.obtenerLogs(
                        "ADMINISTRADOR");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "GET",
                metodoRecibido);

        assertEquals(
                "/seguridad/logs?rol=ADMINISTRADOR",
                rutaRecibida);
    }

    @Test
    @DisplayName("Codificar espacios")
    void testCodificacionEspacios()
            throws Exception {

        cliente.verificarPermiso(
                "juan perez",
                "ADMINISTRADOR",
                "VER LOGS");

        assertTrue(
                datosRecibidos.contains(
                        "juan+perez"));

        assertTrue(
                datosRecibidos.contains(
                        "VER+LOGS"));
    }

    @Test
    @DisplayName("Codificar caracteres especiales")
    void testCaracteresEspeciales()
            throws Exception {

        cliente.verificarPermiso(
                "José",
                "ADMINISTRADOR",
                "Acción");

        String datos =
                URLDecoder.decode(
                        datosRecibidos,
                        StandardCharsets.UTF_8);

        assertTrue(
                datos.contains("José"));

        assertTrue(
                datos.contains("Acción"));
    }

    @Test
    @DisplayName("Constructor normal")
    void testConstructorNormal() {

        SeguridadHttpClient clienteNormal =
                new SeguridadHttpClient();

        assertNotNull(clienteNormal);
    }

    @Test
    @DisplayName("Constructor parametrizado")
    void testConstructorParametrizado() {

        SeguridadHttpClient clientePrueba =
                new SeguridadHttpClient(
                        "http://localhost:9999");

        assertNotNull(clientePrueba);
    }
}