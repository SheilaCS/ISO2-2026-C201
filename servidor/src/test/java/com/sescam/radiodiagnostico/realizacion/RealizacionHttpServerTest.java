package com.sescam.radiodiagnostico.realizacion;

import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Servidor HTTP de realizacion de pruebas (CDU7)")
class RealizacionHttpServerTest {

    private HttpServer servidor;
    private String urlBase;
    private HttpClient httpClient;

    @BeforeEach
    void iniciarServidor() throws Exception {
        IAutenticacionService auth = (usuario, clave) -> {
            if ("medico.demo".equals(usuario)) {
                return Optional.of(new Sesion(usuario, Rol.MEDICO));
            }
            if ("paciente.demo".equals(usuario)) {
                return Optional.of(new Sesion(usuario, Rol.PACIENTE));
            }
            if ("admin.demo".equals(usuario)) {
                return Optional.of(new Sesion(usuario, Rol.ADMINISTRADOR));
            }
            if ("administrativo.demo".equals(usuario)) {
                return Optional.of(new Sesion(usuario, Rol.ADMINISTRATIVO));
            }
            return Optional.empty();
        };

        IRealizacionService servicio = new RealizacionServiceImpl(idCita -> idCita == 10);
        servidor = RealizacionHttpServer.crearServidor(0, servicio, auth);
        servidor.start();
        urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/realizaciones";
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @ParameterizedTest(name = "Metodo {0} con usuario {1} debe devolver {2}")
    @CsvSource({
            "GET, '', 401",
            "POST, '', 401",
            "GET, invalido, 401",
            "GET, paciente.demo, 403",
            "POST, paciente.demo, 403",
            "GET, administrativo.demo, 403",
            "POST, admin.demo, 403"
    })
    @DisplayName("Rechaza acceso sin sesion (401) o con rol distinto a MEDICO (403)")
    void rechazaAccesoNoAutorizadoPorRolYOperacion(String metodo, String usuario, int codigoEsperado) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(urlBase));
        if (!usuario.isBlank()) {
            String token = Base64.getEncoder().encodeToString((usuario + ":1234").getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
        if ("POST".equals(metodo)) {
            builder.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("idCita=10&idSolicitud=1&fechaRealizacion=2026-11-20&observaciones=OK"));
        } else {
            builder.GET();
        }

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(codigoEsperado, resp.statusCode());
    }

    @Test
    @DisplayName("Permite a MEDICO registrar, consultar por cita y listar realizaciones")
    void flujoCompletoMedico() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("medico.demo:1234".getBytes(StandardCharsets.UTF_8));
        String body = "idCita=10&idSolicitud=2&fechaRealizacion=" + LocalDate.now() + "&observaciones=Sin+incidencias";

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Authorization", auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> respPost = httpClient.send(post, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> respGet = httpClient.send(
                HttpRequest.newBuilder().uri(URI.create(urlBase + "?idCita=10")).header("Authorization", auth).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> respLista = httpClient.send(
                HttpRequest.newBuilder().uri(URI.create(urlBase)).header("Authorization", auth).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertAll(
                () -> assertEquals(201, respPost.statusCode()),
                () -> assertEquals(200, respGet.statusCode()),
                () -> assertTrue(respGet.body().contains("idCita=10")),
                () -> assertEquals(200, respLista.statusCode())
        );
    }

    @Test
    @DisplayName("Devuelve 404, 405, 415, 400 y 409 ante peticiones incorrectas")
    void cubreCodigosDeErrorHttp() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("medico.demo:1234".getBytes(StandardCharsets.UTF_8));
        String bodyValido = "idCita=10&idSolicitud=2&fechaRealizacion=" + LocalDate.now() + "&observaciones=OK";

        HttpRequest req404 = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/otra"))
                .header("Authorization", auth)
                .GET().build();
        HttpRequest req405 = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Authorization", auth)
                .PUT(HttpRequest.BodyPublishers.ofString("")).build();
        HttpRequest req415 = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Authorization", auth)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        HttpRequest reqGet404 = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "?idCita=999"))
                .header("Authorization", auth)
                .GET().build();
        HttpRequest req400 = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Authorization", auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("idCita=10")).build();

        httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Authorization", auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(bodyValido)).build(),
                HttpResponse.BodyHandlers.ofString());

        HttpRequest req409 = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Authorization", auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(bodyValido)).build();

        assertAll(
                () -> assertEquals(404, httpClient.send(req404, HttpResponse.BodyHandlers.ofString()).statusCode()),
                () -> assertEquals(405, httpClient.send(req405, HttpResponse.BodyHandlers.ofString()).statusCode()),
                () -> assertEquals(415, httpClient.send(req415, HttpResponse.BodyHandlers.ofString()).statusCode()),
                () -> assertEquals(404, httpClient.send(reqGet404, HttpResponse.BodyHandlers.ofString()).statusCode()),
                () -> assertEquals(400, httpClient.send(req400, HttpResponse.BodyHandlers.ofString()).statusCode()),
                () -> assertEquals(409, httpClient.send(req409, HttpResponse.BodyHandlers.ofString()).statusCode())
        );
    }
}