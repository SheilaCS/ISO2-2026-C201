package com.sescam.radiodiagnostico.cliente.informes;

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

public class InformesHttpClientTest {

    private HttpServer servidor;
    private InformesHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String datosRecibidos;
    private String tipoContenido;

    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/informes/", exchange -> {

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

        cliente = new InformesHttpClient(
                "http://127.0.0.1:" + puerto + "/informes");
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Generar informe correctamente")
    public void testGenerarInforme() throws Exception {

        String resultado = cliente.generarInforme(
                10,
                "2026-12-01",
                "Fallo motor RX",
                "TECNICO");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "POST",
                metodoRecibido);

        assertEquals(
                "/informes/generar",
                rutaRecibida);

        String datos = URLDecoder.decode(
                datosRecibidos,
                StandardCharsets.UTF_8);

        assertEquals(
                "idEquipo=10"
                + "&fecha=2026-12-01"
                + "&descripcion=Fallo motor RX"
                + "&rol=TECNICO",
                datos);

        assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                tipoContenido);
    }

    @Test
    @DisplayName("Consultar informe correctamente")
    public void testConsultarInforme() throws Exception {

        String resultado =
                cliente.consultarInforme(
                        15,
                        "ADMIN");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "GET",
                metodoRecibido);

        assertEquals(
                "/informes/consultar?id=15&rol=ADMIN",
                rutaRecibida);
    }

    @Test
    @DisplayName("Codificar espacios")
    public void testCodificacionEspacios()
            throws Exception {

        cliente.generarInforme(
                10,
                "2026-12-01",
                "Motor RX averiado",
                "TECNICO");

        assertTrue(
                datosRecibidos.contains(
                        "Motor+RX+averiado"));
    }

    @Test
    @DisplayName("Codificar caracteres UTF8")
    public void testCodificacionUTF8()
            throws Exception {

        cliente.generarInforme(
                10,
                "2026-12-01",
                "Radiografía dañada",
                "TÉCNICO");

        String datos = URLDecoder.decode(
                datosRecibidos,
                StandardCharsets.UTF_8);

        assertTrue(
                datos.contains(
                        "Radiografía dañada"));

        assertTrue(
                datos.contains(
                        "TÉCNICO"));
    }

    @Test
    @DisplayName("Consultar informe con rol codificado")
    public void testConsultarRolCodificado()
            throws Exception {

        cliente.consultarInforme(
                5,
                "ADMIN GENERAL");

        assertEquals(
                "/informes/consultar?id=5&rol=ADMIN+GENERAL",
                rutaRecibida);
    }

    @Test
    @DisplayName("Constructor normal")
    public void testConstructorNormal() {

        InformesHttpClient clienteNormal =
                new InformesHttpClient();

        assertNotNull(clienteNormal);
    }

    @Test
    @DisplayName("Constructor parametrizado")
    public void testConstructorParametrizado() {

        InformesHttpClient clientePrueba =
                new InformesHttpClient(
                        "http://localhost:9999");

        assertNotNull(clientePrueba);
    }
}