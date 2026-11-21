
package com.sescam.radiodiagnostico.miscitas;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.sescam.radiodiagnostico.citas.GestionCitasHttpServer;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GestionMisCitasHttpServerTest {

    private HttpServer servidor;
    private HttpServer servidorCitas;
    private HttpClient cliente;
    private String urlBase;

    // -----------------------------------------
    // PREPARAR Y FINALIZAR
    // -----------------------------------------

    @BeforeEach
    public void preparar() throws Exception {

        cliente = HttpClient.newHttpClient();

        // Arrancar un servidor de citas real (RF13) con una cita
        // del paciente 1, como la crearia el administrativo
        servidorCitas = GestionCitasHttpServer.crearServidor(0);
        servidorCitas.start();

        String urlCitas = "http://localhost:"
                + servidorCitas.getAddress().getPort() + "/citas";

        cliente.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(urlCitas + "/asignar"))
                        .header("Content-Type",
                                "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "solicitud=1&paciente=1&fecha="
                                + LocalDate.now().plusDays(5)
                                + "&hora=10:00"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // "Mis citas" obtiene las citas de ese servidor
        GestionMisCitasServiceImpl servicio =
                new GestionMisCitasServiceImpl(
                        new ProveedorCitasHttp(urlCitas));

        Field campo = GestionMisCitasHttpServer.class
                .getDeclaredField("servicio");

        campo.setAccessible(true);
        campo.set(null, servicio);

        // Crear servidor con un puerto libre
        servidor = GestionMisCitasHttpServer.crearServidor(0);
        servidor.start();

        int puerto = servidor.getAddress().getPort();

        urlBase = "http://localhost:" + puerto + "/miscitas";
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }

        if (servidorCitas != null) {
            servidorCitas.stop(0);
        }
    }

    // -----------------------------------------
    // METODOS AUXILIARES
    // -----------------------------------------

    // Enviar peticiones GET y POST
    private HttpResponse<String> enviar(
            String metodo, String ruta, String datos)
            throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta));

        if (metodo.equals("POST")) {

            peticion.header(
                    "Content-Type",
                    "application/x-www-form-urlencoded");

            peticion.POST(
                    HttpRequest.BodyPublishers.ofString(
                            datos, StandardCharsets.UTF_8));

        } else if (metodo.equals("GET")) {

            peticion.GET();

        } else {

            peticion.method(
                    metodo,
                    HttpRequest.BodyPublishers.noBody());
        }

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));
    }

    // -----------------------------------------
    // PRUEBAS DE CONSULTAR MIS CITAS
    // -----------------------------------------

    @Test
    @DisplayName("Consultar las citas de un paciente")
    public void testConsultarCorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idPaciente=1", "");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Cita 1"));
        assertTrue(respuesta.body().contains("PROGRAMADA"));
    }

    @Test
    @DisplayName("Consultar un paciente sin citas")
    public void testConsultarSinCitas() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idPaciente=9", "");

        assertEquals(200, respuesta.statusCode());
        assertEquals("No hay citas para este paciente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al consultar")
    public void testConsultarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idPaciente=abc", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar una consulta sin parametros")
    public void testConsultarSinParametros() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar POST en la ruta consultar")
    public void testConsultarMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/consultar", "idPaciente=1");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE CONFIRMAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Confirmar una cita correctamente")
    public void testConfirmarCorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/confirmar", "idCita=1&idPaciente=1");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita confirmada correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar la confirmacion de la cita de otro paciente")
    public void testConfirmarCitaDeOtroPaciente() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/confirmar", "idCita=1&idPaciente=2");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo confirmar la cita: "
                + "la cita no existe o no pertenece a este paciente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al confirmar")
    public void testConfirmarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/confirmar", "idCita=abc&idPaciente=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET en la ruta confirmar")
    public void testConfirmarMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/confirmar", "");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE SOLICITAR CAMBIO
    // -----------------------------------------

    @Test
    @DisplayName("Solicitar el cambio de una cita correctamente")
    public void testCambioCorrecto() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=No+puedo+asistir");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Solicitud de cambio registrada correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Guardar el motivo con acentos y caracteres especiales")
    public void testCambioMotivoConAcentos() throws Exception {

        enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=Revisi%C3%B3n+m%C3%A9dica");

        HttpResponse<String> consulta =
                enviar("GET", "/consultar?idPaciente=1", "");

        assertTrue(consulta.body().contains(
                "Motivo: Revisión médica"));
    }

    @Test
    @DisplayName("Rechazar un cambio sin motivo")
    public void testCambioSinMotivo() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=");

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    @DisplayName("Rechazar un cambio con un motivo vacio")
    public void testCambioMotivoVacio() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=+++");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo solicitar el cambio de la cita: "
                + "el motivo es obligatorio y no puede superar 200 "
                + "caracteres", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al solicitar un cambio")
    public void testCambioDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=abc&idPaciente=1&motivo=Motivo");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET en la ruta cambio")
    public void testCambioMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/cambio", "");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE CANCELAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Cancelar una cita correctamente")
    public void testCancelarCorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/cancelar", "idCita=1&idPaciente=1");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita cancelada correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar cancelar una cita ya cancelada")
    public void testCancelarDosVeces() throws Exception {

        enviar("POST", "/cancelar", "idCita=1&idPaciente=1");

        HttpResponse<String> respuesta =
                enviar("POST", "/cancelar", "idCita=1&idPaciente=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo cancelar la cita: "
                + "la cita esta cancelada", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar POST sin parametros al cancelar")
    public void testCancelarSinParametros() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/cancelar", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET en la ruta cancelar")
    public void testCancelarMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/cancelar", "");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DEL SERVIDOR
    // -----------------------------------------

    @Test
    @DisplayName("Mostrar el estado de la cita tras gestionarla")
    public void testFlujoCompleto() throws Exception {

        enviar("POST", "/confirmar", "idCita=1&idPaciente=1");

        HttpResponse<String> consulta =
                enviar("GET", "/consultar?idPaciente=1", "");

        assertTrue(consulta.body().contains("CONFIRMADA"));
    }

    @Test
    @DisplayName("Rechazar una ruta que no existe")
    public void testRutaInexistente() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/otra", "");

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    @DisplayName("Rechazar un metodo no permitido en todas las rutas")
    public void testMetodoPutNoPermitido() throws Exception {

        assertEquals(405,
                enviar("PUT", "/consultar", "").statusCode());

        assertEquals(405,
                enviar("PUT", "/confirmar", "").statusCode());

        assertEquals(405,
                enviar("PUT", "/cambio", "").statusCode());

        assertEquals(405,
                enviar("PUT", "/cancelar", "").statusCode());
    }

    // -----------------------------------------
    // PRUEBAS DE PARAMETROS INCORRECTOS
    // -----------------------------------------

    @Test
    @DisplayName("Rechazar confirmar sin el ID del paciente")
    public void testConfirmarSinPaciente() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/confirmar", "idCita=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar confirmar sin el ID de la cita")
    public void testConfirmarSinCita() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/confirmar", "idPaciente=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar cancelar sin el ID del paciente")
    public void testCancelarSinPaciente() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/cancelar", "idCita=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar solicitar un cambio sin el ID de la cita")
    public void testCambioSinCita() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idPaciente=1&motivo=Motivo");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar solicitar un cambio sin el parametro motivo")
    public void testCambioSinParametroMotivo() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=1&idPaciente=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo solicitar el cambio de la cita: "
                + "el motivo es obligatorio y no puede superar 200 "
                + "caracteres", respuesta.body());
    }

    @Test
    @DisplayName("Ignorar los parametros sin valor")
    public void testParametroSinValor() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/confirmar",
                "idCita&idPaciente=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Ignorar los parametros que no se usan")
    public void testParametrosExtra() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/confirmar",
                "idCita=1&idPaciente=1&otro=valor");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Cita confirmada correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Consultar con un ID de paciente no valido")
    public void testConsultarIdPacienteNoValido() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idPaciente=0", "");

        assertEquals(200, respuesta.statusCode());
        assertEquals("ID de paciente no válido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE REPETIR OPERACIONES
    // -----------------------------------------

    @Test
    @DisplayName("Rechazar confirmar una cita dos veces")
    public void testConfirmarDosVeces() throws Exception {

        enviar("POST", "/confirmar", "idCita=1&idPaciente=1");

        HttpResponse<String> respuesta =
                enviar("POST", "/confirmar", "idCita=1&idPaciente=1");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo confirmar la cita: "
                + "la cita ya esta confirmada", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar solicitar un cambio dos veces")
    public void testCambioDosVeces() throws Exception {

        enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=Primero");

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=Segundo");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo solicitar el cambio de la cita: "
                + "ya se ha solicitado un cambio para esta cita",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar el cambio de una cita de otro paciente")
    public void testCambioCitaDeOtroPaciente() throws Exception {

        HttpResponse<String> respuesta = enviar("POST", "/cambio",
                "idCita=1&idPaciente=2&motivo=Motivo");

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    @DisplayName("Rechazar cancelar la cita de otro paciente")
    public void testCancelarCitaDeOtroPaciente() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/cancelar", "idCita=1&idPaciente=2");

        assertEquals(400, respuesta.statusCode());
        assertEquals("No se pudo cancelar la cita: "
                + "la cita no existe o no pertenece a este paciente",
                respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE LA CLASE DEL SERVIDOR
    // -----------------------------------------

    @Test
    @DisplayName("Crear un objeto de la clase del servidor")
    public void testConstructor() {

        // Ejecutar
        GestionMisCitasHttpServer objeto =
                new GestionMisCitasHttpServer();

        // Comprobar
        assertNotNull(objeto);
    }

    @Test
    @DisplayName("Ejecutar el metodo main del servidor")
    public void testMainServidor() throws Exception {

        // Guardar la configuracion anterior
        String puertoAnterior =
                System.getProperty("miscitas.puerto");

        // Se usa un puerto libre en vez del 8082 y el servicio
        // conectado al servidor de citas de la prueba
        System.setProperty("miscitas.puerto", "0");

        try {

            // Ejecutar
            GestionMisCitasHttpServer.main(new String[0]);

            // Comprobar
            assertNotNull(
                    GestionMisCitasHttpServer.servidorPrincipal);

            int puerto = GestionMisCitasHttpServer.servidorPrincipal
                    .getAddress().getPort();

            assertTrue(puerto > 0);

            // Comprobar que atiende peticiones con las citas reales
            HttpResponse<String> paciente1 = cliente.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + puerto
                                    + "/miscitas/consultar?idPaciente=1"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            HttpResponse<String> paciente2 = cliente.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + puerto
                                    + "/miscitas/consultar?idPaciente=2"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            assertAll(
                    () -> assertTrue(paciente1.body().contains("Cita 1")),
                    () -> assertEquals("No hay citas para este paciente",
                            paciente2.body())
            );

        } finally {

            // Detener el servidor creado por main
            if (GestionMisCitasHttpServer.servidorPrincipal != null) {

                GestionMisCitasHttpServer.servidorPrincipal.stop(0);

                GestionMisCitasHttpServer.servidorPrincipal = null;
            }

            // Restaurar la configuracion original
            if (puertoAnterior == null) {
                System.clearProperty("miscitas.puerto");
            } else {
                System.setProperty("miscitas.puerto", puertoAnterior);
            }
        }
    }

    // -----------------------------------------
    // COMUNICACION CON EL SERVIDOR DE CITAS
    // -----------------------------------------

    @Test
    @DisplayName("Ver en mis citas una cita cancelada por el administrativo")
    public void testVerCambiosHechosEnElModuloDeCitas() throws Exception {

        // El administrativo cancela la cita en el modulo de citas
        cliente.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:"
                                + servidorCitas.getAddress().getPort()
                                + "/citas/cancelar"))
                        .header("Content-Type",
                                "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers
                                .ofString("id=1"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idPaciente=1", "");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("CANCELADA"));
    }

    @Test
    @DisplayName("Avisar si el servidor de citas no esta disponible al consultar")
    public void testConsultarServidorDeCitasApagado() throws Exception {

        servidorCitas.stop(0);

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idPaciente=1", "");

        assertEquals(503, respuesta.statusCode());
        assertEquals("No se pudo conectar con el servidor de citas",
                respuesta.body());
    }

    @Test
    @DisplayName("Avisar si el servidor de citas no esta disponible al confirmar")
    public void testConfirmarServidorDeCitasApagado() throws Exception {

        servidorCitas.stop(0);

        assertEquals(503, enviar("POST", "/confirmar",
                "idCita=1&idPaciente=1").statusCode());
    }

    @Test
    @DisplayName("Avisar si el servidor de citas no esta disponible al solicitar un cambio")
    public void testCambioServidorDeCitasApagado() throws Exception {

        servidorCitas.stop(0);

        assertEquals(503, enviar("POST", "/cambio",
                "idCita=1&idPaciente=1&motivo=Motivo").statusCode());
    }

    @Test
    @DisplayName("Avisar si el servidor de citas no esta disponible al cancelar")
    public void testCancelarServidorDeCitasApagado() throws Exception {

        servidorCitas.stop(0);

        assertEquals(503, enviar("POST", "/cancelar",
                "idCita=1&idPaciente=1").statusCode());
    }

    @Test
    @DisplayName("Reflejar en mis citas la cancelacion hecha por el paciente")
    public void testCancelarSeVeEnElModuloDeCitas() throws Exception {

        enviar("POST", "/cancelar", "idCita=1&idPaciente=1");

        HttpResponse<String> citas = cliente.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:"
                                + servidorCitas.getAddress().getPort()
                                + "/citas/consultar?id=1"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));

        assertTrue(citas.body().contains("Estado: CANCELADA"));
    }
}
