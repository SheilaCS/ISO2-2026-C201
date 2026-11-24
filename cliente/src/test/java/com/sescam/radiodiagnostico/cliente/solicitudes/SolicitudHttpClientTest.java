package com.sescam.radiodiagnostico.cliente.solicitudes;

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

public class SolicitudHttpClientTest {

    private HttpServer servidor;
    private SolicitudHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String datosRecibidos;
    private String tipoContenido;

    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/solicitudes/", exchange -> {

            rutaRecibida = exchange.getRequestURI().toString();
            metodoRecibido = exchange.getRequestMethod();

            tipoContenido = exchange.getRequestHeaders()
                    .getFirst("Content-Type");

            datosRecibidos = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            String respuesta = "Operacion correcta";

            byte[] contenido =
                    respuesta.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(200,
                    contenido.length);

            exchange.getResponseBody().write(contenido);
            exchange.close();
        });

        servidor.start();

        int puerto = servidor.getAddress().getPort();

        cliente = new SolicitudHttpClient(
                "http://127.0.0.1:" + puerto + "/solicitudes");
    }

    @AfterEach
    public void finalizar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Crear solicitud correctamente")
    void testCrearSolicitud() throws Exception {

        String resultado = cliente.crearSolicitud(
                5,
                "TAC",
                "URGENTE");

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "POST",
                metodoRecibido);

        assertEquals(
                "/solicitudes/crear",
                rutaRecibida);

        String datos = URLDecoder.decode(
                datosRecibidos,
                StandardCharsets.UTF_8);

        assertEquals(
                "idPaciente=5&tipoPrueba=TAC&prioridad=URGENTE",
                datos);

        assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                tipoContenido);
    }

    @Test
    @DisplayName("Consultar solicitud correctamente")
    void testConsultarSolicitud() throws Exception {

        String resultado =
                cliente.consultarSolicitud(15);

        assertEquals(
                "Operacion correcta",
                resultado);

        assertEquals(
                "GET",
                metodoRecibido);

        assertEquals(
                "/solicitudes/consultar?id=15",
                rutaRecibida);
    }

    @Test
    @DisplayName("Codificar caracteres especiales")
    void testCodificacion() throws Exception {

        cliente.crearSolicitud(
                1,
                "TAC craneal",
                "MUY URGENTE");

        assertTrue(
                datosRecibidos.contains(
                        "tipoPrueba=TAC+craneal"));

        assertTrue(
                datosRecibidos.contains(
                        "prioridad=MUY+URGENTE"));
    }

    @Test
    @DisplayName("Constructor normal")
    void testConstructorNormal() {

        SolicitudHttpClient clienteNormal =
                new SolicitudHttpClient();

        assertNotNull(clienteNormal);
    }

    @Test
    @DisplayName("Crear solicitud con caracteres UTF-8")
    void testCaracteresEspeciales() throws Exception {

        cliente.crearSolicitud(
                7,
                "Radiografía tórax",
                "PREFERENTE");

        String datos = URLDecoder.decode(
                datosRecibidos,
                StandardCharsets.UTF_8);

        assertTrue(
                datos.contains(
                        "tipoPrueba=Radiografía tórax"));
    }
}