
package com.sescam.radiodiagnostico.cliente.agendas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AgendasHttpClientTest {

    private HttpServer servidor;
    private AgendasHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String datosRecibidos;
    private String tipoContenido;

    // Preparar un servidor de prueba antes de cada test
    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/agendas/", exchange -> {

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
                    "Content-Type", "text/plain; charset=UTF-8");

            exchange.sendResponseHeaders(200, contenido.length);

            try (var salida = exchange.getResponseBody()) {
                salida.write(contenido);
            }
        });

        servidor.start();

        int puerto = servidor.getAddress().getPort();

        cliente = new AgendasHttpClient(
                "http://127.0.0.1:" + puerto + "/agendas");
    }

    // Detener el servidor despues de cada test
    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // PRUEBAS DE CREAR HORARIO

    @Test
    @DisplayName("Crear un horario correctamente")
    public void testCrearHorario() throws Exception {

        // Preparar
        int idMedico = 5;
        String fecha = "2026-12-10";
        String inicio = "09:00";
        String fin = "10:00";

        // Ejecutar
        String resultado = cliente.crearHorario(
                idMedico, fecha, inicio, fin);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("POST", metodoRecibido);
        assertEquals("/agendas/crear", rutaRecibida);

        String datos = URLDecoder.decode(
                datosRecibidos, StandardCharsets.UTF_8);

        assertEquals(
                "idMedico=5&fecha=2026-12-10"
                + "&inicio=09:00&fin=10:00", datos);

        assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                tipoContenido);
    }

    // PRUEBAS DE CONSULTAR AGENDA

    @Test
    @DisplayName("Consultar la agenda de un medico")
    public void testConsultarAgenda() throws Exception {

        // Preparar
        int idMedico = 5;

        // Ejecutar
        String resultado = cliente.consultarAgenda(idMedico);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("GET", metodoRecibido);
        assertEquals(
                "/agendas/consultar?idMedico=5",
                rutaRecibida);
    }

    // PRUEBAS DE MODIFICAR HORARIO

    @Test
    @DisplayName("Modificar un horario correctamente")
    public void testModificarHorario() throws Exception {

        // Preparar
        int idHorario = 3;
        String fecha = "2026-12-10";
        String inicio = "11:00";
        String fin = "12:00";

        // Ejecutar
        String resultado = cliente.modificarHorario(
                idHorario, fecha, inicio, fin);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("POST", metodoRecibido);
        assertEquals("/agendas/modificar", rutaRecibida);

        String datos = URLDecoder.decode(
                datosRecibidos, StandardCharsets.UTF_8);

        assertEquals(
                "idHorario=3&fecha=2026-12-10"
                + "&inicio=11:00&fin=12:00", datos);
    }

    // PRUEBAS DE ELIMINAR HORARIO

    @Test
    @DisplayName("Eliminar un horario correctamente")
    public void testEliminarHorario() throws Exception {

        // Preparar
        int idHorario = 3;

        // Ejecutar
        String resultado = cliente.eliminarHorario(idHorario);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("POST", metodoRecibido);
        assertEquals("/agendas/eliminar", rutaRecibida);
        assertEquals("idHorario=3", datosRecibidos);
    }

    // PRUEBAS DE CODIFICACION

    @Test
    @DisplayName("Codificar correctamente los parametros HTTP")
    public void testCodificacion() throws Exception {

        // Ejecutar
        cliente.crearHorario(
                5, "2026-12-10", "09:00", "10:00");

        // Comprobar
        assertTrue(datosRecibidos.contains("inicio=09%3A00"));
        assertTrue(datosRecibidos.contains("fin=10%3A00"));
    }

    // PRUEBA DEL CONSTRUCTOR NORMAL

    @Test
    @DisplayName("Crear un cliente HTTP con el constructor normal")
    public void testConstructorNormal() {

        // Ejecutar
        AgendasHttpClient clienteNormal =
                new AgendasHttpClient();

        // Comprobar
        assertNotNull(clienteNormal);
    }
}
