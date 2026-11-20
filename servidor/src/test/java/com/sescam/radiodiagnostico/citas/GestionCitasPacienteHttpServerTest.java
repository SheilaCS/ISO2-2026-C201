package com.sescam.radiodiagnostico.citas;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las rutas del paciente (iteracion 9, RF02)
 * en el servidor HTTP de citas.
 */
public class GestionCitasPacienteHttpServerTest {

    private HttpServer servidor;
    private HttpClient cliente;
    private String urlBase;
    private String fechaFutura;

    // -----------------------------------------
    // PREPARAR Y FINALIZAR
    // -----------------------------------------

    @BeforeEach
    public void preparar() throws Exception {

        servidor = GestionCitasHttpServer.crearServidor(0);
        servidor.start();

        urlBase = "http://localhost:"
                + servidor.getAddress().getPort() + "/citas";

        cliente = HttpClient.newHttpClient();
        fechaFutura = LocalDate.now().plusDays(5).toString();

        // Cita 1 del paciente 1
        enviar("POST", "/asignar",
                "solicitud=1&paciente=1&fecha=" + fechaFutura
                + "&hora=10:00");
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // Enviar peticiones GET, POST u otro metodo
    private HttpResponse<String> enviar(
            String metodo, String ruta, String datos)
            throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta));

        if (metodo.equals("POST")) {

            peticion.header("Content-Type",
                    "application/x-www-form-urlencoded");

            peticion.POST(HttpRequest.BodyPublishers.ofString(
                    datos, StandardCharsets.UTF_8));

        } else if (metodo.equals("GET")) {

            peticion.GET();

        } else {

            peticion.method(metodo,
                    HttpRequest.BodyPublishers.noBody());
        }

        return cliente.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));
    }

    // -----------------------------------------
    // PRUEBAS DE LISTAR LAS CITAS DEL PACIENTE
    // -----------------------------------------

    @Test
    @DisplayName("Listar las citas de un paciente")
    public void testListarCorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/paciente?paciente=1", "");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita 1 | Fecha: " + fechaFutura
                + " | Hora: 10:00 | Estado: PROGRAMADA",
                respuesta.body());
    }

    @Test
    @DisplayName("Listar un paciente sin citas")
    public void testListarSinCitas() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/paciente?paciente=9", "");

        assertEquals(200, respuesta.statusCode());
        assertEquals("", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar listar con datos incorrectos")
    public void testListarDatosIncorrectos() throws Exception {

        assertEquals(400, enviar("GET",
                "/paciente?paciente=abc", "").statusCode());
    }

    @Test
    @DisplayName("Rechazar listar sin el ID del paciente")
    public void testListarSinParametros() throws Exception {

        assertEquals(400, enviar("GET", "/paciente", "").statusCode());
    }

    // -----------------------------------------
    // PRUEBAS DE CONFIRMAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Confirmar una cita correctamente")
    public void testConfirmarCorrecto() throws Exception {

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/confirmar", "id=1&paciente=1");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita confirmada correctamente",
                respuesta.body());

        assertTrue(enviar("GET", "/paciente?paciente=1", "")
                .body().contains("CONFIRMADA"));
    }

    @Test
    @DisplayName("Rechazar confirmar la cita de otro paciente")
    public void testConfirmarCitaDeOtroPaciente() throws Exception {

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/confirmar", "id=1&paciente=2");

        assertEquals(409, respuesta.statusCode());
        assertEquals("No se pudo confirmar la cita: la cita no "
                + "existe o no pertenece a este paciente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar confirmar una cita dos veces")
    public void testConfirmarDosVeces() throws Exception {

        enviar("POST", "/paciente/confirmar", "id=1&paciente=1");

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/confirmar", "id=1&paciente=1");

        assertEquals(409, respuesta.statusCode());
        assertEquals("No se pudo confirmar la cita: "
                + "la cita ya esta confirmada", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar confirmar con datos incompletos")
    public void testConfirmarDatosIncompletos() throws Exception {

        assertEquals(400, enviar("POST", "/paciente/confirmar",
                "id=1").statusCode());

        assertEquals(400, enviar("POST", "/paciente/confirmar",
                "paciente=1").statusCode());

        assertEquals(400, enviar("POST", "/paciente/confirmar",
                "id=abc&paciente=1").statusCode());
    }

    @Test
    @DisplayName("Rechazar confirmar con el metodo GET")
    public void testConfirmarMetodoIncorrecto() throws Exception {

        assertEquals(404, enviar("GET",
                "/paciente/confirmar?id=1&paciente=1", "")
                .statusCode());
    }

    // -----------------------------------------
    // PRUEBAS DE SOLICITAR CAMBIO
    // -----------------------------------------

    @Test
    @DisplayName("Solicitar el cambio de una cita correctamente")
    public void testCambioCorrecto() throws Exception {

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/cambio",
                "id=1&paciente=1&motivo=No+puedo+asistir");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Solicitud de cambio registrada correctamente",
                respuesta.body());

        assertTrue(enviar("GET", "/paciente?paciente=1", "")
                .body().contains("Motivo: No puedo asistir"));
    }

    @Test
    @DisplayName("Rechazar el cambio sin motivo")
    public void testCambioSinMotivo() throws Exception {

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/cambio", "id=1&paciente=1");

        assertEquals(409, respuesta.statusCode());
        assertEquals("No se pudo solicitar el cambio de la cita: "
                + "el motivo es obligatorio y no puede superar 200 "
                + "caracteres", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar el cambio con un motivo en blanco")
    public void testCambioMotivoEnBlanco() throws Exception {

        assertEquals(409, enviar("POST", "/paciente/cambio",
                "id=1&paciente=1&motivo=+++").statusCode());
    }

    @Test
    @DisplayName("Rechazar el cambio de la cita de otro paciente")
    public void testCambioCitaDeOtroPaciente() throws Exception {

        assertEquals(409, enviar("POST", "/paciente/cambio",
                "id=1&paciente=2&motivo=Motivo").statusCode());
    }

    @Test
    @DisplayName("Rechazar el cambio con datos incompletos")
    public void testCambioDatosIncompletos() throws Exception {

        assertEquals(400, enviar("POST", "/paciente/cambio",
                "paciente=1&motivo=Motivo").statusCode());
    }

    // -----------------------------------------
    // PRUEBAS DE CANCELAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Cancelar una cita correctamente")
    public void testCancelarCorrecto() throws Exception {

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/cancelar", "id=1&paciente=1");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita cancelada correctamente",
                respuesta.body());

        assertTrue(enviar("GET", "/paciente?paciente=1", "")
                .body().contains("CANCELADA"));
    }

    @Test
    @DisplayName("Rechazar cancelar la cita de otro paciente")
    public void testCancelarCitaDeOtroPaciente() throws Exception {

        HttpResponse<String> respuesta = enviar("POST",
                "/paciente/cancelar", "id=1&paciente=2");

        assertEquals(409, respuesta.statusCode());
        assertEquals("No se pudo cancelar la cita: la cita no "
                + "existe o no pertenece a este paciente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar cancelar una cita dos veces")
    public void testCancelarDosVeces() throws Exception {

        enviar("POST", "/paciente/cancelar", "id=1&paciente=1");

        assertEquals(409, enviar("POST", "/paciente/cancelar",
                "id=1&paciente=1").statusCode());
    }

    @Test
    @DisplayName("Rechazar cancelar con datos incompletos")
    public void testCancelarDatosIncompletos() throws Exception {

        assertEquals(400, enviar("POST", "/paciente/cancelar",
                "id=1").statusCode());
    }

    // -----------------------------------------
    // PRUEBAS DE RUTAS Y ERRORES
    // -----------------------------------------

    @Test
    @DisplayName("Rechazar una ruta del paciente que no existe")
    public void testRutaInexistente() throws Exception {

        assertEquals(404, enviar("POST", "/paciente/otra",
                "id=1&paciente=1").statusCode());
    }

    @Test
    @DisplayName("Rechazar cambio y cancelacion con el metodo GET")
    public void testCambioYCancelarMetodoIncorrecto() throws Exception {

        assertEquals(404, enviar("GET",
                "/paciente/cambio?id=1&paciente=1&motivo=Motivo", "")
                .statusCode());

        assertEquals(404, enviar("GET",
                "/paciente/cancelar?id=1&paciente=1", "")
                .statusCode());
    }

    @Test
    @DisplayName("Rechazar POST al listar las citas del paciente")
    public void testListarMetodoIncorrecto() throws Exception {

        assertEquals(404, enviar("POST", "/paciente",
                "paciente=1").statusCode());
    }

    @Test
    @DisplayName("No mezclar las rutas del paciente con las de administracion")
    public void testCancelarCitaAdministrativaSigueFuncionando()
            throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/cancelar", "id=1");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita cancelada correctamente", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBA DE LOS METODOS POR DEFECTO DEL CONTRATO
    // -----------------------------------------

    @Test
    @DisplayName("Un servicio sin operaciones de paciente las rechaza")
    public void testMetodosPorDefecto() {

        IGestionCitasService servicioAntiguo =
                new IGestionCitasService() {

            @Override
            public boolean asignarCita(int idSolicitud,
                    int idPaciente, String fecha, String hora) {
                return false;
            }

            @Override
            public boolean reprogramarCita(int idCita,
                    String nuevaFecha, String nuevaHora) {
                return false;
            }

            @Override
            public boolean cancelarCita(int idCita) {
                return false;
            }

            @Override
            public String consultarCita(int idCita) {
                return "";
            }
        };

        assertAll(
                () -> assertEquals("",
                        servicioAntiguo.listarCitasPaciente(1)),
                () -> assertFalse(servicioAntiguo
                        .confirmarCitaPaciente(1, 1).isCorrecto()),
                () -> assertFalse(servicioAntiguo
                        .solicitarCambioCitaPaciente(1, 1, "Motivo")
                        .isCorrecto()),
                () -> assertEquals("Operacion no disponible",
                        servicioAntiguo.cancelarCitaPaciente(1, 1)
                                .getMensaje())
        );
    }
}
