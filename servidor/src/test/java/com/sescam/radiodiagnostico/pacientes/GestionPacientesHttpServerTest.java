package com.sescam.radiodiagnostico.pacientes;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class GestionPacientesHttpServerTest {

    private HttpServer servidor;
    private HttpClient cliente;
    private String base;

    private static final String TIPO_FORMULARIO =
            "application/x-www-form-urlencoded";

    private static final String DATOS_VALIDOS =
            "cip=PAC001&nombre=Ana&apellidos=Garcia"
            + "&fechaNacimiento=2000-05-15";

    @BeforeEach
    void prepararServidor() throws Exception {
        servidor = GestionPacientesHttpServer.crearServidor(0);
        servidor.start();

        base = "http://127.0.0.1:"
                + servidor.getAddress().getPort();

        cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("HTTP permite dar de alta, consultar, modificar y dar de baja")
    void cicloCompletoDelPaciente() throws Exception {
        // Preparar y ejecutar el alta
        HttpResponse<String> alta = enviar(
                "POST", "/pacientes", DATOS_VALIDOS
        );

        assertEquals(201, alta.statusCode());

        Map<String, String> creado = leerRespuesta(alta);
        String id = creado.get("id");
        assertNotNull(id);

        // Consultar
        HttpResponse<String> consulta = enviar(
                "GET", "/pacientes?cip=PAC001", null
        );

        assertAll(
                () -> assertEquals(200, consulta.statusCode()),
                () -> assertEquals(id, leerRespuesta(consulta).get("id")),
                () -> assertEquals(
                        "true", leerRespuesta(consulta).get("activo")
                )
        );

        // Modificar
        HttpResponse<String> modificacion = enviar(
                "PUT",
                "/pacientes/" + id,
                "nombre=Luis&apellidos=Perez&fechaNacimiento=1995-03-10"
        );

        assertAll(
                () -> assertEquals(200, modificacion.statusCode()),
                () -> assertEquals(
                        "Luis", leerRespuesta(modificacion).get("nombre")
                ),
                () -> assertEquals(
                        "PAC001", leerRespuesta(modificacion).get("cip")
                ),
                () -> assertEquals(
                        id, leerRespuesta(modificacion).get("id")
                )
        );

        // Dar de baja y comprobar que los datos siguen disponibles
        HttpResponse<String> baja = enviar(
                "DELETE", "/pacientes/" + id, null
        );

        HttpResponse<String> consultaFinal = enviar(
                "GET", "/pacientes?cip=PAC001", null
        );

        assertAll(
                () -> assertEquals(204, baja.statusCode()),
                () -> assertTrue(baja.body().isEmpty()),
                () -> assertEquals(200, consultaFinal.statusCode()),
                () -> assertEquals(
                        "false", leerRespuesta(consultaFinal).get("activo")
                ),
                () -> assertEquals(
                        "Luis", leerRespuesta(consultaFinal).get("nombre")
                )
        );
    }

    @Test
    @DisplayName("HTTP rechaza CIP duplicados y operaciones sobre pacientes inactivos")
    void conflictosDeNegocio() throws Exception {
        // Preparar
        HttpResponse<String> alta = enviar(
                "POST", "/pacientes", DATOS_VALIDOS
        );
        assertEquals(201, alta.statusCode());

        String ruta = "/pacientes/" + leerRespuesta(alta).get("id");

        // Ejecutar y comprobar el duplicado
        assertEquals(
                409,
                enviar("POST", "/pacientes", DATOS_VALIDOS).statusCode()
        );

        assertEquals(204, enviar("DELETE", ruta, null).statusCode());

        // Comprobar las restricciones tras la baja
        assertAll(
                () -> assertEquals(
                        409, enviar("DELETE", ruta, null).statusCode()
                ),
                () -> assertEquals(
                        409,
                        enviar(
                                "PUT", ruta,
                                "nombre=Luis&apellidos=Perez"
                                + "&fechaNacimiento=2000-05-15"
                        ).statusCode()
                ),
                () -> assertEquals(
                        409,
                        enviar(
                                "POST", "/pacientes", DATOS_VALIDOS
                        ).statusCode()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("peticionesIncorrectas")
    @DisplayName("HTTP devuelve el estado adecuado para peticiones incorrectas")
    void rechazaPeticionIncorrecta(
            String metodo,
            String ruta,
            String cuerpo,
            int esperado) throws Exception {

        // Ejecutar
        HttpResponse<String> respuesta = enviar(metodo, ruta, cuerpo);

        // Comprobar
        assertAll(
                () -> assertEquals(esperado, respuesta.statusCode()),
                () -> assertTrue(
                        leerRespuesta(respuesta).containsKey("error")
                )
        );
    }

    static Stream<Arguments> peticionesIncorrectas() {
        String inexistente =
                "/pacientes/00000000-0000-0000-0000-000000000001";

        return Stream.of(
                Arguments.of(
                        "GET", "/pacientes?cip=NOEXISTE", null, 404
                ),
                Arguments.of("GET", "/pacientes", null, 400),
                Arguments.of(
                        "GET", "/pacientes?cip=%20", null, 400
                ),
                Arguments.of(
                        "GET", "/pacientes?cip=A&cip=B", null, 400
                ),
                Arguments.of(
        "POST", "/pacientes", "cip=%ZZ", 400
),
                Arguments.of("POST", "/pacientes", "", 400),
                Arguments.of(
                        "POST", "/pacientes", "datoSinIgual", 400
                ),
                Arguments.of(
                        "POST", "/pacientes",
                        DATOS_VALIDOS + "&activo=false", 400
                ),
                Arguments.of(
                        "POST", "/pacientes",
                        DATOS_VALIDOS.replace("2000-05-15", "fecha"), 400
                ),
                Arguments.of(
                        "POST", "/pacientes",
                        DATOS_VALIDOS.replace("2000-05-15", "2999-01-01"),
                        400
                ),
                Arguments.of(
                        "POST", "/pacientes",
                        DATOS_VALIDOS.replace("nombre=Ana", "nombre=%20"),
                        400
                ),
                Arguments.of(
                        "PUT", inexistente,
                        "nombre=Ana&apellidos=Garcia"
                        + "&fechaNacimiento=2000-05-15",
                        404
                ),
                Arguments.of("DELETE", inexistente, null, 404),
                Arguments.of(
                        "PUT", inexistente,
                        "nombre=Ana&apellidos=Garcia"
                        + "&fechaNacimiento=2000-05-15&cip=OTRO",
                        400
                ),
                Arguments.of(
                        "DELETE", "/pacientes/no-es-un-uuid", null, 400
                ),
                Arguments.of("GET", "/pacientes/", null, 404),
                Arguments.of("GET", "/pacientes/uno/dos", null, 404),
                Arguments.of("GET", "/pacientesOtraRuta", null, 404),
                Arguments.of("PATCH", "/pacientes", "", 405),
                Arguments.of("GET", inexistente, null, 405)
        );
    }

    @ParameterizedTest
    @MethodSource("credencialesIncorrectas")
    @DisplayName("HTTP rechaza credenciales inválidas y roles no autorizados")
    void compruebaAutenticacionYPermisos(
            String autorizacion,
            int esperado) throws Exception {

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET",
                "/pacientes?cip=PAC001",
                null,
                autorizacion,
                null
        );

        // Comprobar
        assertEquals(esperado, respuesta.statusCode());

        if (esperado == 401) {
            assertTrue(
                    respuesta.headers()
                            .firstValue("WWW-Authenticate").isPresent()
            );
        }
    }

    static Stream<Arguments> credencialesIncorrectas() {
        return Stream.of(
                Arguments.of(null, 401),
                Arguments.of("Bearer token", 401),
                Arguments.of("Basic !!!", 401),
                Arguments.of(basic("sinSeparador"), 401),
                Arguments.of(basic(":demo"), 401),
                Arguments.of(basic("administrativo.demo:incorrecta"), 401),
                Arguments.of(basic("medico.demo:demo"), 403),
                Arguments.of(basic("paciente.demo:demo"), 403),
                Arguments.of(basic("admin.demo:demo"), 403)
        );
    }

    @Test
    @DisplayName("HTTP rechaza cuerpos sin el formato de formulario")
    void rechazaFormatoIncorrecto() throws Exception {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(
                        415,
                        enviar(
                                "POST", "/pacientes", DATOS_VALIDOS,
                                basic("administrativo.demo:demo"),
                                "application/json"
                        ).statusCode()
                ),
                () -> assertEquals(
                        415,
                        enviar(
                                "POST", "/pacientes", DATOS_VALIDOS,
                                basic("administrativo.demo:demo"),
                                null
                        ).statusCode()
                )
        );
    }

    @Test
    @DisplayName("HTTP limita el tamaño de las peticiones")
    void rechazaCuerpoDemasiadoGrande() throws Exception {
        // Preparar
        String cuerpo = "nombre=" + "a".repeat(16_385);

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "POST", "/pacientes", cuerpo
        );

        // Comprobar
        assertEquals(413, respuesta.statusCode());
    }

    @Test
    @DisplayName("Un fallo inesperado devuelve 500 sin exponer detalles internos")
    void controlaErrorInterno() throws Exception {
        // Preparar un servicio que falla de forma controlada en el test
        servidor.stop(0);

        IPacientesService servicioFalso = new PacientesServiceImpl(
                new RepositorioPacientesEnMemoria()) {

            @Override
            public Optional<Paciente> consultarPorCip(String cip) {
                throw new IllegalStateException("Detalle interno privado");
            }
        };

        servidor = GestionPacientesHttpServer.crearServidor(
                0, servicioFalso, new AutenticacionServiceImpl()
        );
        servidor.start();

        base = "http://127.0.0.1:"
                + servidor.getAddress().getPort();

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/pacientes?cip=PAC001", null
        );

        // Comprobar
        assertAll(
                () -> assertEquals(500, respuesta.statusCode()),
                () -> assertEquals(
                        "Error interno del servidor.",
                        leerRespuesta(respuesta).get("error")
                ),
                () -> assertFalse(
                        respuesta.body().contains("Detalle interno privado")
                )
        );
    }

    private HttpResponse<String> enviar(
            String metodo,
            String ruta,
            String cuerpo) throws Exception {

        return enviar(
                metodo,
                ruta,
                cuerpo,
                basic("administrativo.demo:demo"),
                cuerpo == null ? null : TIPO_FORMULARIO
        );
    }

    private HttpResponse<String> enviar(
            String metodo,
            String ruta,
            String cuerpo,
            String autorizacion,
            String tipo) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(5));

        if (autorizacion != null) {
            peticion.header("Authorization", autorizacion);
        }

        if (tipo != null) {
            peticion.header("Content-Type", tipo);
        }

        peticion.method(
                metodo,
                cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(
                                cuerpo, StandardCharsets.UTF_8)
        );

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(
                credenciales.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static Map<String, String> leerRespuesta(
            HttpResponse<String> respuesta) {

        Map<String, String> datos = new HashMap<>();

        for (String parte : respuesta.body().split("&")) {
            String[] par = parte.split("=", 2);

            if (par.length == 2) {
                datos.put(
                        URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(par[1], StandardCharsets.UTF_8)
                );
            }
        }

        return datos;
    }
}