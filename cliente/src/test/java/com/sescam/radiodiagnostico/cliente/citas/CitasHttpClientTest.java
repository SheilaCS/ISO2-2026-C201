
package com.sescam.radiodiagnostico.cliente.citas;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CitasHttpClientTest {

    private HttpServer servidor;
    private CitasHttpClient clienteHttp;

    private AtomicReference<String> metodo;
    private AtomicReference<String> ruta;
    private AtomicReference<String> datos;

    // Preparar el servidor simulado
    @BeforeEach
    public void preparar() throws Exception {

        metodo = new AtomicReference<>();
        ruta = new AtomicReference<>();
        datos = new AtomicReference<>();

        // Crear un servidor en un puerto libre
        servidor = HttpServer.create(
                new InetSocketAddress("localhost", 0), 0);

        servidor.createContext("/citas/", exchange -> {

            metodo.set(exchange.getRequestMethod());

            ruta.set(exchange.getRequestURI().getRawPath());

            datos.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));

            responder(exchange, "Operacion correcta");
        });

        servidor.start();

        int puerto = servidor.getAddress().getPort();

        clienteHttp = new CitasHttpClient(
                "http://localhost:" + puerto + "/citas");
    }

    // Detener el servidor despues de cada prueba
    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // Enviar una respuesta simulada
    private void responder(
            HttpExchange exchange,
            String mensaje) throws IOException {

        byte[] respuesta = mensaje.getBytes(
                StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                200, respuesta.length);

        try (OutputStream salida =
                     exchange.getResponseBody()) {

            salida.write(respuesta);
        }
    }

    // -----------------------------------------
    // PRUEBAS DE CONSULTAR
    // -----------------------------------------

    @Test
    @DisplayName("Consultar una cita mediante GET")
    public void testConsultarCita() throws Exception {

        // Ejecutar
        String respuesta = clienteHttp.consultarCita(5);

        // Comprobar
        assertEquals("Operacion correcta", respuesta);
        assertEquals("GET", metodo.get());
        assertEquals("/citas/consultar", ruta.get());
    }

    // -----------------------------------------
    // PRUEBAS DE ASIGNAR
    // -----------------------------------------

    @Test
    @DisplayName("Asignar una cita mediante POST")
    public void testAsignarCita() throws Exception {

        // Ejecutar
        String respuesta = clienteHttp.asignarCita(
                2, 3, "2027-01-15", "10:30");

        // Comprobar
        assertEquals("Operacion correcta", respuesta);
        assertEquals("POST", metodo.get());
        assertEquals("/citas/asignar", ruta.get());

        assertEquals(
                "solicitud=2&paciente=3"
                + "&fecha=2027-01-15&hora=10%3A30",
                datos.get());
    }

    // -----------------------------------------
    // PRUEBAS DE REPROGRAMAR
    // -----------------------------------------

    @Test
    @DisplayName("Reprogramar una cita mediante POST")
    public void testReprogramarCita() throws Exception {

        // Ejecutar
        String respuesta = clienteHttp.reprogramarCita(
                5, "2027-02-20", "12:45");

        // Comprobar
        assertEquals("Operacion correcta", respuesta);
        assertEquals("POST", metodo.get());
        assertEquals("/citas/reprogramar", ruta.get());

        assertEquals(
                "id=5&fecha=2027-02-20&hora=12%3A45",
                datos.get());
    }

    // -----------------------------------------
    // PRUEBAS DE CANCELAR
    // -----------------------------------------

    @Test
    @DisplayName("Cancelar una cita mediante POST")
    public void testCancelarCita() throws Exception {

        // Ejecutar
        String respuesta = clienteHttp.cancelarCita(7);

        // Comprobar
        assertEquals("Operacion correcta", respuesta);
        assertEquals("POST", metodo.get());
        assertEquals("/citas/cancelar", ruta.get());
        assertEquals("id=7", datos.get());
    }

    // -----------------------------------------
    // PRUEBAS ADICIONALES
    // -----------------------------------------

    @Test
    @DisplayName("Codificar caracteres especiales")
    public void testCodificarDatos() throws Exception {

        // Ejecutar
        clienteHttp.asignarCita(
                1, 2, "fecha con espacios", "10:30");

        // Comprobar
        assertTrue(datos.get().contains(
                "fecha=fecha+con+espacios"));

        assertTrue(datos.get().contains(
                "hora=10%3A30"));
    }

    @Test
    @DisplayName("Comprobar el constructor principal")
    public void testConstructor() {

        // Ejecutar
        CitasHttpClient cliente = new CitasHttpClient();

        // Comprobar
        assertNotNull(cliente);
    }
}
