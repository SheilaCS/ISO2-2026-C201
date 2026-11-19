
package com.sescam.radiodiagnostico.citas;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas HTTP de la Iteracion 3 - RF13.
 *
 * Se prueban:
 * - Asignar citas
 * - Reprogramar citas
 * - Cancelar citas
 * - Consultar citas
 * - Parametros incorrectos
 * - Rutas y metodos no permitidos
 */
public class GestionCitasHttpServerTest {

    private HttpServer servidor;
    private HttpClient cliente;
    private String urlBase;
    private String fecha;

    // Crear un servidor independiente antes de cada prueba
    @BeforeEach
    public void preparar() throws Exception {

        servidor = GestionCitasHttpServer.crearServidor(0);
        servidor.start();

        int puerto = servidor.getAddress().getPort();

        urlBase = "http://127.0.0.1:" + puerto + "/citas";
        cliente = HttpClient.newHttpClient();

        fecha = LocalDate.now().plusDays(10).toString();
    }

    // Detener el servidor despues de cada prueba
    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // Enviar una peticion GET
    private HttpResponse<String> get(String ruta)
            throws Exception {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta))
                .GET()
                .build();

        return cliente.send(
                peticion,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));
    }

    // Enviar una peticion POST
    private HttpResponse<String> post(
            String ruta, String datos) throws Exception {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta))
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        datos, StandardCharsets.UTF_8))
                .build();

        return cliente.send(
                peticion,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));
    }

    // Datos correctos para asignar una cita
    private String datosAsignar() {

        return "solicitud=1&paciente=2&fecha="
                + fecha + "&hora=10%3A00";
    }

    // -----------------------------------------
    // PRUEBAS DEL SERVIDOR
    // -----------------------------------------

    @Test
    @DisplayName("Comprobar que el servidor funciona")
    public void testServidorFuncionando() throws Exception {

        HttpResponse<String> respuesta = get("");

        assertEquals(200, respuesta.statusCode());

        assertEquals(
                "Servidor de citas SESCAM funcionando",
                respuesta.body());
    }

    // -----------------------------------------
    // ASIGNAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Asignar una cita correctamente")
    public void testAsignarCorrectamente() throws Exception {

        HttpResponse<String> respuesta =
                post("/asignar", datosAsignar());

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "Cita asignada correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar un paciente incorrecto")
    public void testAsignarPacienteIncorrecto() throws Exception {

        String datos = "solicitud=1&paciente=0&fecha="
                + fecha + "&hora=10:00";

        HttpResponse<String> respuesta =
                post("/asignar", datos);

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "No se ha podido asignar la cita",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos no numericos al asignar")
    public void testAsignarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                post("/asignar", "solicitud=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals(
                "Datos incorrectos o incompletos",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar horario ocupado")
    public void testAsignarHorarioOcupado() throws Exception {

        HttpResponse<String> primera =
                post("/asignar", datosAsignar());

        HttpResponse<String> segunda =
                post("/asignar", datosAsignar());

        assertEquals(200, primera.statusCode());
        assertEquals(200, segunda.statusCode());

        assertEquals(
                "No se ha podido asignar la cita",
                segunda.body());
    }

    // -----------------------------------------
    // CONSULTAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Consultar una cita existente")
    public void testConsultarCorrectamente() throws Exception {

        post("/asignar", datosAsignar());

        HttpResponse<String> respuesta =
                get("/consultar?id=1");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("ID cita: 1"));
        assertTrue(respuesta.body().contains("PROGRAMADA"));
    }

    @Test
    @DisplayName("Consultar una cita inexistente")
    public void testConsultarInexistente() throws Exception {

        HttpResponse<String> respuesta =
                get("/consultar?id=99");

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "No existe una cita con ese ID.",
                respuesta.body());
    }

    @Test
    @DisplayName("Consultar con un ID incorrecto")
    public void testConsultarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                get("/consultar?id=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals(
                "Datos incorrectos o incompletos",
                respuesta.body());
    }

    @Test
    @DisplayName("Consultar sin parametros")
    public void testConsultarSinParametros() throws Exception {

        HttpResponse<String> respuesta =
                get("/consultar");

        assertEquals(400, respuesta.statusCode());
    }

    // -----------------------------------------
    // REPROGRAMAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Reprogramar una cita correctamente")
    public void testReprogramarCorrectamente() throws Exception {

        post("/asignar", datosAsignar());

        String datos = "id=1&fecha=" + fecha
                + "&hora=11%3A00";

        HttpResponse<String> respuesta =
                post("/reprogramar", datos);

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "Cita reprogramada correctamente",
                respuesta.body());

        HttpResponse<String> consulta =
                get("/consultar?id=1");

        assertTrue(consulta.body().contains("11:00"));
    }

    @Test
    @DisplayName("No reprogramar una cita inexistente")
    public void testReprogramarInexistente() throws Exception {

        String datos = "id=99&fecha=" + fecha
                + "&hora=11:00";

        HttpResponse<String> respuesta =
                post("/reprogramar", datos);

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "No se ha podido reprogramar la cita",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al reprogramar")
    public void testReprogramarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                post("/reprogramar", "id=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals(
                "Datos incorrectos o incompletos",
                respuesta.body());
    }

    // -----------------------------------------
    // CANCELAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Cancelar una cita correctamente")
    public void testCancelarCorrectamente() throws Exception {

        post("/asignar", datosAsignar());

        HttpResponse<String> respuesta =
                post("/cancelar", "id=1");

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "Cita cancelada correctamente",
                respuesta.body());

        HttpResponse<String> consulta =
                get("/consultar?id=1");

        assertTrue(consulta.body().contains("CANCELADA"));
    }

    @Test
    @DisplayName("Rechazar cancelacion de cita inexistente")
    public void testCancelarInexistente() throws Exception {

        HttpResponse<String> respuesta =
                post("/cancelar", "id=99");

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "No se ha podido cancelar la cita",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al cancelar")
    public void testCancelarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                post("/cancelar", "id=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals(
                "Datos incorrectos o incompletos",
                respuesta.body());
    }

    // -----------------------------------------
    // RUTAS Y PARAMETROS
    // -----------------------------------------

    @Test
    @DisplayName("Rechazar una ruta inexistente")
    public void testRutaInexistente() throws Exception {

        HttpResponse<String> respuesta =
                get("/desconocida");

        assertEquals(404, respuesta.statusCode());
        assertEquals(
                "Operacion no encontrada",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET en la ruta asignar")
    public void testMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                get("/asignar");

        assertEquals(404, respuesta.statusCode());
        assertEquals(
                "Operacion no encontrada",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar parametros sin signo igual")
    public void testParametroSinIgual() throws Exception {

        HttpResponse<String> respuesta =
                post("/asignar", "solicitud");

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    @DisplayName("Comprobar decodificacion de parametros")
    public void testParametrosCodificados() throws Exception {

        String datos = "solicitud=%31&paciente=%32&fecha="
                + fecha + "&hora=10%3A00";

        HttpResponse<String> respuesta =
                post("/asignar", datos);

        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "Cita asignada correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Comprobar cabecera de respuesta")
    public void testCabeceraRespuesta() throws Exception {

        HttpResponse<String> respuesta = get("");

        assertTrue(respuesta.headers()
                .firstValue("Content-Type")
                .orElse("")
                .contains("text/plain"));
    }

    // -----------------------------------------
    // PRUEBAS ADICIONALES PARA JACOCO
    // -----------------------------------------

    @Test
    @DisplayName("Rechazar POST en la ruta principal")
    public void testPostRutaPrincipal() throws Exception {

        HttpResponse<String> respuesta =
                post("", "");

        assertEquals(404, respuesta.statusCode());
        assertEquals(
                "Operacion no encontrada",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET al reprogramar")
    public void testGetReprogramar() throws Exception {

        HttpResponse<String> respuesta =
                get("/reprogramar");

        assertEquals(404, respuesta.statusCode());
        assertEquals(
                "Operacion no encontrada",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET al cancelar")
    public void testGetCancelar() throws Exception {

        HttpResponse<String> respuesta =
                get("/cancelar");

        assertEquals(404, respuesta.statusCode());
        assertEquals(
                "Operacion no encontrada",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar POST al consultar")
    public void testPostConsultar() throws Exception {

        HttpResponse<String> respuesta =
                post("/consultar", "id=1");

        assertEquals(404, respuesta.statusCode());
        assertEquals(
                "Operacion no encontrada",
                respuesta.body());
    }

    @Test
    @DisplayName("Consultar con parametros vacios")
    public void testConsultarParametrosVacios() throws Exception {

        HttpResponse<String> respuesta =
                get("/consultar?");

        assertEquals(400, respuesta.statusCode());
        assertEquals(
                "Datos incorrectos o incompletos",
                respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS FINALES PARA JACOCO
    // -----------------------------------------

    @Test
    @DisplayName("Comprobar constructor del servidor")
    public void testConstructorServidor() {

        GestionCitasHttpServer objeto =
                new GestionCitasHttpServer();

        assertNotNull(objeto);
    }

    @Test
    @DisplayName("Comprobar el metodo main")
    public void testMainServidor() throws Exception {

        String puertoAnterior =
                System.getProperty("citas.puerto");

        System.setProperty("citas.puerto", "0");

        try {

            GestionCitasHttpServer.main(new String[0]);

            assertNotNull(
                    GestionCitasHttpServer.servidorPrincipal);

            assertTrue(
                    GestionCitasHttpServer.servidorPrincipal
                            .getAddress().getPort() > 0);

        } finally {

            if (GestionCitasHttpServer.servidorPrincipal != null) {

                GestionCitasHttpServer.servidorPrincipal.stop(0);
                GestionCitasHttpServer.servidorPrincipal = null;
            }

            if (puertoAnterior == null) {
                System.clearProperty("citas.puerto");
            } else {
                System.setProperty("citas.puerto", puertoAnterior);
            }
        }
    }

    @Test
    @DisplayName("Comprobar error interno HTTP 500")
    public void testErrorInternoServidor() throws Exception {

        IGestionCitasService servicioConError =
                new IGestionCitasService() {

            @Override
            public boolean asignarCita(
                    int solicitud, int paciente,
                    String fecha, String hora) {
                return false;
            }

            @Override
            public boolean reprogramarCita(
                    int id, String fecha, String hora) {
                return false;
            }

            @Override
            public boolean cancelarCita(int id) {
                return false;
            }

            @Override
            public String consultarCita(int id) {

                throw new IllegalStateException(
                        "Fallo interno simulado");
            }
        };

        HttpServer servidorError =
                GestionCitasHttpServer.crearServidor(
                        0, servicioConError);

        servidorError.start();

        try {

            int puerto =
                    servidorError.getAddress().getPort();

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:"
                            + puerto
                            + "/citas/consultar?id=1"))
                    .GET()
                    .build();

            HttpResponse<String> respuesta =
                    cliente.send(
                            peticion,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8));

            assertEquals(500, respuesta.statusCode());

            assertEquals(
                    "Error interno del servidor",
                    respuesta.body());

        } finally {
            servidorError.stop(0);
        }
    }

    @Test
    @DisplayName("Comprobar parametros vacios")
    public void testLeerParametrosVacios() throws Exception {

        java.lang.reflect.Method metodo =
                GestionCitasHttpServer.class
                        .getDeclaredMethod(
                                "leerParametros",
                                String.class);

        metodo.setAccessible(true);

        Object resultado = metodo.invoke(null, "");

        assertNotNull(resultado);

        assertTrue(
                ((java.util.Map<?, ?>) resultado).isEmpty());
    }


}
