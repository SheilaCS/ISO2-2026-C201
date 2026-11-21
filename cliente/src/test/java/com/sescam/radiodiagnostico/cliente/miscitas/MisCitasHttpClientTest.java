
package com.sescam.radiodiagnostico.cliente.miscitas;

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

public class MisCitasHttpClientTest {

    private HttpServer servidor;
    private MisCitasHttpClient cliente;

    private String rutaRecibida;
    private String metodoRecibido;
    private String datosRecibidos;
    private String tipoContenido;

    // Preparar un servidor de prueba antes de cada test
    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/miscitas/", exchange -> {

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

        cliente = new MisCitasHttpClient(
                "http://127.0.0.1:" + puerto + "/miscitas");
    }

    // Detener el servidor despues de cada test
    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // PRUEBAS DE CONSULTAR MIS CITAS

    @Test
    @DisplayName("Consultar las citas de un paciente")
    public void testConsultarMisCitas() throws Exception {

        // Preparar
        int idPaciente = 5;

        // Ejecutar
        String resultado = cliente.consultarMisCitas(idPaciente);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("GET", metodoRecibido);
        assertEquals(
                "/miscitas/consultar?idPaciente=5",
                rutaRecibida);
    }

    // PRUEBAS DE CONFIRMAR CITA

    @Test
    @DisplayName("Confirmar una cita correctamente")
    public void testConfirmarCita() throws Exception {

        // Ejecutar
        String resultado = cliente.confirmarCita(3, 5);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("POST", metodoRecibido);
        assertEquals("/miscitas/confirmar", rutaRecibida);
        assertEquals("idCita=3&idPaciente=5", datosRecibidos);

        assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                tipoContenido);
    }

    // PRUEBAS DE SOLICITAR CAMBIO

    @Test
    @DisplayName("Solicitar el cambio de una cita")
    public void testSolicitarCambioCita() throws Exception {

        // Ejecutar
        String resultado = cliente.solicitarCambioCita(
                3, 5, "No puedo asistir");

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("POST", metodoRecibido);
        assertEquals("/miscitas/cambio", rutaRecibida);

        String datos = URLDecoder.decode(
                datosRecibidos, StandardCharsets.UTF_8);

        assertEquals(
                "idCita=3&idPaciente=5&motivo=No puedo asistir",
                datos);
    }

    // PRUEBAS DE CANCELAR CITA

    @Test
    @DisplayName("Cancelar una cita correctamente")
    public void testCancelarCita() throws Exception {

        // Ejecutar
        String resultado = cliente.cancelarCita(3, 5);

        // Comprobar
        assertEquals("Operacion correcta", resultado);
        assertEquals("POST", metodoRecibido);
        assertEquals("/miscitas/cancelar", rutaRecibida);
        assertEquals("idCita=3&idPaciente=5", datosRecibidos);
    }

    // PRUEBAS DE CODIFICACION

    @Test
    @DisplayName("Codificar correctamente el motivo del cambio")
    public void testCodificacion() throws Exception {

        // Ejecutar
        cliente.solicitarCambioCita(
                3, 5, "Revisión médica & análisis");

        // Comprobar
        assertTrue(datosRecibidos.contains(
                "motivo=Revisi%C3%B3n+m%C3%A9dica+%26+an%C3%A1lisis"));
    }

    // PRUEBAS DE DATOS ENVIADOS

    @Test
    @DisplayName("Enviar el motivo vacio sin romper la peticion")
    public void testMotivoVacio() throws Exception {

        // Ejecutar
        cliente.solicitarCambioCita(3, 5, "");

        // Comprobar
        assertEquals("idCita=3&idPaciente=5&motivo=", datosRecibidos);
    }

    @Test
    @DisplayName("No enviar cuerpo en la consulta")
    public void testConsultarSinCuerpo() throws Exception {

        // Ejecutar
        cliente.consultarMisCitas(1);

        // Comprobar
        assertEquals("", datosRecibidos);
    }

    // PRUEBAS CON EL SERVIDOR APAGADO

    @Test
    @DisplayName("Lanzar un error al consultar con el servidor apagado")
    public void testConsultarServidorApagado() {

        servidor.stop(0);

        assertThrows(IOException.class,
                () -> cliente.consultarMisCitas(1));
    }

    @Test
    @DisplayName("Lanzar un error al confirmar con el servidor apagado")
    public void testConfirmarServidorApagado() {

        servidor.stop(0);

        assertThrows(IOException.class,
                () -> cliente.confirmarCita(1, 1));
    }

    @Test
    @DisplayName("Lanzar un error al solicitar un cambio con el servidor apagado")
    public void testCambioServidorApagado() {

        servidor.stop(0);

        assertThrows(IOException.class,
                () -> cliente.solicitarCambioCita(1, 1, "Motivo"));
    }

    @Test
    @DisplayName("Lanzar un error al cancelar con el servidor apagado")
    public void testCancelarServidorApagado() {

        servidor.stop(0);

        assertThrows(IOException.class,
                () -> cliente.cancelarCita(1, 1));
    }

    // PRUEBA DEL CONSTRUCTOR NORMAL

    @Test
    @DisplayName("Crear un cliente HTTP con el constructor normal")
    public void testConstructorNormal() {

        MisCitasHttpClient otro = new MisCitasHttpClient();

        assertNotNull(otro);
    }
}
