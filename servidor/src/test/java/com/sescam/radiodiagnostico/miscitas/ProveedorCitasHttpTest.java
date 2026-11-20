package com.sescam.radiodiagnostico.miscitas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.sescam.radiodiagnostico.citas.GestionCitasHttpServer;
import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la comunicacion entre "Mis citas" (RF02) y el servidor
 * de citas (RF13): se usa el servidor de citas real.
 */
public class ProveedorCitasHttpTest {

    private HttpServer servidorCitas;
    private String urlCitas;
    private ProveedorCitasHttp proveedor;
    private String fechaFutura;

    // Servidor falso que responde siempre con un codigo fijo
    private HttpServer servidorFalso;

    // Datos recibidos por el servidor falso
    private volatile String rutaRecibida;
    private volatile String cuerpoRecibido;

    @BeforeEach
    public void preparar() throws Exception {

        servidorCitas = GestionCitasHttpServer.crearServidor(0);
        servidorCitas.start();

        urlCitas = "http://localhost:"
                + servidorCitas.getAddress().getPort() + "/citas";

        proveedor = new ProveedorCitasHttp(urlCitas);
        fechaFutura = LocalDate.now().plusDays(5).toString();

        // Cita 1 del paciente 1, creada como lo haria RF13
        asignarCita(1, "10:00");
    }

    @AfterEach
    public void finalizar() {

        servidorCitas.stop(0);

        if (servidorFalso != null) {
            servidorFalso.stop(0);
        }
    }

    // Asignar una cita en el servidor de citas del paciente 1
    private void asignarCita(int idPaciente, String hora)
            throws Exception {

        java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(urlCitas + "/asignar"))
                        .header("Content-Type",
                                "application/x-www-form-urlencoded")
                        .POST(java.net.http.HttpRequest.BodyPublishers
                                .ofString("solicitud=1&paciente="
                                        + idPaciente + "&fecha="
                                        + fechaFutura + "&hora=" + hora))
                        .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    // Arrancar un servidor falso que responde con el codigo indicado
    private ProveedorCitasHttp proveedorConRespuesta(int codigo)
            throws IOException {

        servidorFalso = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidorFalso.createContext("/citas", exchange -> {

            rutaRecibida = exchange.getRequestURI().toString();

            cuerpoRecibido = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            byte[] contenido = "Respuesta".getBytes(
                    StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(codigo, contenido.length);
            exchange.getResponseBody().write(contenido);
            exchange.close();
        });

        servidorFalso.start();

        return new ProveedorCitasHttp("http://127.0.0.1:"
                + servidorFalso.getAddress().getPort() + "/citas");
    }

    // -----------------------------------------
    // COMUNICACION CON EL SERVIDOR DE CITAS REAL
    // -----------------------------------------

    @Test
    @DisplayName("Obtener las citas creadas en el modulo de citas")
    public void testListarCitasReales() {

        assertEquals("Cita 1 | Fecha: " + fechaFutura
                + " | Hora: 10:00 | Estado: PROGRAMADA",
                proveedor.listarCitasPaciente(1));
    }

    @Test
    @DisplayName("Obtener un texto vacio si el paciente no tiene citas")
    public void testListarPacienteSinCitas() {

        assertEquals("", proveedor.listarCitasPaciente(9));
    }

    @Test
    @DisplayName("Confirmar una cita real")
    public void testConfirmarCitaReal() {

        assertTrue(proveedor.confirmarCita(1, 1).isCorrecto());
        assertTrue(proveedor.listarCitasPaciente(1)
                .contains("CONFIRMADA"));
    }

    @Test
    @DisplayName("Devolver false al confirmar la cita de otro paciente")
    public void testConfirmarCitaDeOtroPaciente() {

        assertFalse(proveedor.confirmarCita(1, 2).isCorrecto());
    }

    @Test
    @DisplayName("Solicitar el cambio de una cita real")
    public void testSolicitarCambioCitaReal() {

        assertTrue(proveedor.solicitarCambioCita(
                1, 1, "Revisión médica & análisis").isCorrecto());

        assertTrue(proveedor.listarCitasPaciente(1).contains(
                "Motivo: Revisión médica & análisis"));
    }

    @Test
    @DisplayName("Devolver false al solicitar un cambio rechazado")
    public void testSolicitarCambioRechazado() {

        assertFalse(proveedor.solicitarCambioCita(1, 2, "Motivo").isCorrecto());
    }

    @Test
    @DisplayName("Cancelar una cita real")
    public void testCancelarCitaReal() {

        assertTrue(proveedor.cancelarCita(1, 1).isCorrecto());
        assertTrue(proveedor.listarCitasPaciente(1)
                .contains("CANCELADA"));
    }

    @Test
    @DisplayName("Devolver false al cancelar la cita de otro paciente")
    public void testCancelarCitaDeOtroPaciente() {

        assertFalse(proveedor.cancelarCita(1, 2).isCorrecto());
    }

    @Test
    @DisplayName("Ver una cita reprogramada por el administrativo")
    public void testVerCitaReprogramada() throws Exception {

        proveedor.confirmarCita(1, 1);

        // El administrativo la reprograma (RF13)
        java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(
                                urlCitas + "/reprogramar"))
                        .header("Content-Type",
                                "application/x-www-form-urlencoded")
                        .POST(java.net.http.HttpRequest.BodyPublishers
                                .ofString("id=1&fecha=" + fechaFutura
                                        + "&hora=12:00"))
                        .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());

        assertTrue(proveedor.listarCitasPaciente(1).contains(
                "Hora: 12:00 | Estado: PROGRAMADA"));
    }

    // -----------------------------------------
    // DATOS ENVIADOS
    // -----------------------------------------

    @Test
    @DisplayName("Enviar la peticion de listado a la ruta del paciente")
    public void testRutaListar() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(200);

        falso.listarCitasPaciente(7);

        assertEquals("/citas/paciente?paciente=7", rutaRecibida);
    }

    @Test
    @DisplayName("Enviar los datos de la confirmacion")
    public void testDatosConfirmar() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(200);

        falso.confirmarCita(3, 5);

        assertEquals("/citas/paciente/confirmar", rutaRecibida);
        assertEquals("id=3&paciente=5", cuerpoRecibido);
    }

    @Test
    @DisplayName("Enviar los datos del cambio con el motivo codificado")
    public void testDatosCambio() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(200);

        falso.solicitarCambioCita(3, 5, "a b&c");

        assertEquals("/citas/paciente/cambio", rutaRecibida);
        assertEquals("id=3&paciente=5&motivo=a+b%26c", cuerpoRecibido);
    }

    @Test
    @DisplayName("Enviar los datos de la cancelacion")
    public void testDatosCancelar() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(200);

        falso.cancelarCita(3, 5);

        assertEquals("/citas/paciente/cancelar", rutaRecibida);
        assertEquals("id=3&paciente=5", cuerpoRecibido);
    }

    // -----------------------------------------
    // RESPUESTAS INESPERADAS
    // -----------------------------------------

    @Test
    @DisplayName("Fallar si el listado responde con un error")
    public void testListarRespuestaInesperada() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(500);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> falso.listarCitasPaciente(1));

        assertEquals("No se pudo conectar con el servidor de citas",
                error.getMessage());
    }

    @Test
    @DisplayName("Fallar si una operacion responde con un error")
    public void testOperacionRespuestaInesperada() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(500);

        assertThrows(IllegalStateException.class,
                () -> falso.confirmarCita(1, 1));
        assertThrows(IllegalStateException.class,
                () -> falso.solicitarCambioCita(1, 1, "Motivo"));
        assertThrows(IllegalStateException.class,
                () -> falso.cancelarCita(1, 1));
    }

    @Test
    @DisplayName("Fallar si una operacion responde con datos incorrectos")
    public void testOperacionDatosIncorrectos() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(400);

        assertThrows(IllegalStateException.class,
                () -> falso.confirmarCita(1, 1));
    }

    @Test
    @DisplayName("Interpretar 409 como una operacion rechazada")
    public void testOperacionRechazada() throws Exception {

        ProveedorCitasHttp falso = proveedorConRespuesta(409);

        assertFalse(falso.confirmarCita(1, 1).isCorrecto());
        assertFalse(falso.solicitarCambioCita(1, 1, "Motivo").isCorrecto());
        assertFalse(falso.cancelarCita(1, 1).isCorrecto());
    }

    // -----------------------------------------
    // SERVIDOR DE CITAS NO DISPONIBLE
    // -----------------------------------------

    @Test
    @DisplayName("Fallar si el servidor de citas esta apagado")
    public void testServidorDeCitasApagado() {

        servidorCitas.stop(0);

        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> proveedor.listarCitasPaciente(1)),
                () -> assertThrows(IllegalStateException.class,
                        () -> proveedor.confirmarCita(1, 1)),
                () -> assertThrows(IllegalStateException.class,
                        () -> proveedor.solicitarCambioCita(
                                1, 1, "Motivo")),
                () -> assertThrows(IllegalStateException.class,
                        () -> proveedor.cancelarCita(1, 1))
        );
    }

    @Test
    @DisplayName("Mantener la interrupcion del hilo si se interrumpe la peticion")
    public void testPeticionInterrumpida() {

        Thread.currentThread().interrupt();

        try {

            assertThrows(IllegalStateException.class,
                    () -> proveedor.listarCitasPaciente(1));

            assertTrue(Thread.currentThread().isInterrupted());

        } finally {

            // Limpiar la marca para no afectar a otras pruebas
            Thread.interrupted();
        }
    }

    // -----------------------------------------
    // CONSTRUCTOR NORMAL
    // -----------------------------------------

    @Test
    @DisplayName("Usar la direccion indicada en la propiedad citas.url")
    public void testConstructorConPropiedad() {

        String anterior = System.getProperty("citas.url");

        System.setProperty("citas.url", urlCitas);

        try {

            ProveedorCitasHttp normal = new ProveedorCitasHttp();

            assertTrue(normal.listarCitasPaciente(1)
                    .contains("Cita 1"));

        } finally {

            if (anterior == null) {
                System.clearProperty("citas.url");
            } else {
                System.setProperty("citas.url", anterior);
            }
        }
    }

    @Test
    @DisplayName("Crear el proveedor con la direccion por defecto")
    public void testConstructorPorDefecto() {

        assertNotNull(new ProveedorCitasHttp());
    }

    // -----------------------------------------
    // MENSAJES DEL SERVIDOR DE CITAS
    // -----------------------------------------

    @Test
    @DisplayName("Devolver los mensajes del servidor de citas real")
    public void testMensajesReales() {

        ResultadoOperacion confirmada = proveedor.confirmarCita(1, 1);
        ResultadoOperacion repetida = proveedor.confirmarCita(1, 1);
        ResultadoOperacion cambio =
                proveedor.solicitarCambioCita(1, 1, "Motivo");
        ResultadoOperacion ajena = proveedor.cancelarCita(1, 2);
        ResultadoOperacion cancelada = proveedor.cancelarCita(1, 1);

        assertAll(
                () -> assertEquals("Cita confirmada correctamente",
                        confirmada.getMensaje()),
                () -> assertEquals("No se pudo confirmar la cita: "
                        + "la cita ya esta confirmada",
                        repetida.getMensaje()),
                () -> assertEquals(
                        "Solicitud de cambio registrada correctamente",
                        cambio.getMensaje()),
                () -> assertEquals("No se pudo cancelar la cita: "
                        + "la cita no existe o no pertenece a este "
                        + "paciente", ajena.getMensaje()),
                () -> assertEquals("Cita cancelada correctamente",
                        cancelada.getMensaje())
        );
    }

    @Test
    @DisplayName("Usar el cuerpo de la respuesta como mensaje")
    public void testMensajeDeLaRespuesta() throws Exception {

        ProveedorCitasHttp aceptada = proveedorConRespuesta(200);

        assertEquals("Respuesta",
                aceptada.confirmarCita(1, 1).getMensaje());

        servidorFalso.stop(0);

        ProveedorCitasHttp rechazada = proveedorConRespuesta(409);

        assertEquals("Respuesta",
                rechazada.confirmarCita(1, 1).getMensaje());
    }
}
