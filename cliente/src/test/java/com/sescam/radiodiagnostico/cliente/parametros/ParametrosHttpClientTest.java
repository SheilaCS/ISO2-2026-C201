package com.sescam.radiodiagnostico.cliente.parametros;

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

public class ParametrosHttpClientTest {

    private HttpServer servidor;
    private ParametrosHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String datosRecibidos;
    private String tipoContenido;

    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/parametros/", exchange -> {

            rutaRecibida = exchange.getRequestURI().toString();
            metodoRecibido = exchange.getRequestMethod();

            tipoContenido = exchange.getRequestHeaders()
                    .getFirst("Content-Type");

            datosRecibidos = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            String respuesta = "Operacion correcta";

            byte[] contenido = respuesta.getBytes(
                    StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/plain; charset=UTF-8");

            exchange.sendResponseHeaders(
                    200,
                    contenido.length);

            try (var salida = exchange.getResponseBody()) {
                salida.write(contenido);
            }
        });

        servidor.start();

        int puerto = servidor.getAddress().getPort();

        cliente = new ParametrosHttpClient(
                "http://127.0.0.1:"
                + puerto
                + "/parametros");
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Guardar parametro correctamente")
    public void testGuardarParametro() throws Exception {

        String resultado =
                cliente.guardarParametro(
                        "MAX_PACIENTES",
                        "100");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "POST",
                metodoRecibido);

        assertEquals(
                "/parametros/guardar",
                rutaRecibida);

        String datos = URLDecoder.decode(
                datosRecibidos,
                StandardCharsets.UTF_8);

        assertEquals(
                "clave=MAX_PACIENTES&valor=100",
                datos);

        assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                tipoContenido);
    }

    @Test
    @DisplayName("Consultar parametro correctamente")
    public void testConsultarParametro()
            throws Exception {

        String resultado =
                cliente.consultarParametro(
                        "MAX_PACIENTES");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "GET",
                metodoRecibido);

        assertEquals(
                "/parametros/consultar?clave=MAX_PACIENTES",
                rutaRecibida);
    }

    @Test
    @DisplayName("Codificar espacios")
    public void testCodificacionEspacios()
            throws Exception {

        cliente.guardarParametro(
                "MAX PACIENTES",
                "VALOR CON ESPACIOS");

        assertTrue(
                datosRecibidos.contains(
                        "MAX+PACIENTES"));

        assertTrue(
                datosRecibidos.contains(
                        "VALOR+CON+ESPACIOS"));
    }

    @Test
    @DisplayName("Codificar caracteres especiales")
    public void testCaracteresEspeciales()
            throws Exception {

        cliente.guardarParametro(
                "Tamaño",
                "Radiografía");

        String datos = URLDecoder.decode(
                datosRecibidos,
                StandardCharsets.UTF_8);

        assertTrue(
                datos.contains("Tamaño"));

        assertTrue(
                datos.contains("Radiografía"));
    }

    @Test
    @DisplayName("Consultar parametro con espacios")
    public void testConsultarConEspacios()
            throws Exception {

        cliente.consultarParametro(
                "MAX PACIENTES");

        assertEquals(
                "/parametros/consultar?clave=MAX+PACIENTES",
                rutaRecibida);
    }

    @Test
    @DisplayName("Crear cliente con constructor normal")
    public void testConstructorNormal() {

        ParametrosHttpClient clienteNormal =
                new ParametrosHttpClient();

        assertNotNull(clienteNormal);
    }

    @Test
    @DisplayName("Crear cliente con constructor parametrizado")
    public void testConstructorParametrizado() {

        ParametrosHttpClient clientePrueba =
                new ParametrosHttpClient(
                        "http://localhost:9999");

        assertNotNull(clientePrueba);
    }
}