package com.sescam.radiodiagnostico.historial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CDU12 · Servidor · GestionHistorialHttpServer")
class GestionHistorialHttpServerTest {

    private HttpServer servidor;
    private HttpClient cliente;
    private String base;
    private RegistroConsultas registro;

    @BeforeEach
    void prepararServidor() throws Exception {
        ProveedorHistorialEnMemoria proveedor =
                new ProveedorHistorialEnMemoria();
        proveedor.agregar("paciente.demo", new PruebaRealizada(
                "P1", LocalDate.of(2026, 1, 10), "Radiografia",
                "REALIZADA", "Sin hallazgos"));
        proveedor.agregar("paciente.demo", new PruebaRealizada(
                "P2", LocalDate.of(2026, 3, 5), "Ecografia",
                "REALIZADA", "Control; revisar"));
        proveedor.agregar("otro.paciente", new PruebaRealizada(
                "P3", LocalDate.of(2026, 2, 1), "Resonancia",
                "REALIZADA", "Informe"));

        registro = new RegistroConsultas();
        servidor = GestionHistorialHttpServer.crearServidor(
                0,
                new HistorialServiceImpl(proveedor, registro),
                new AutenticacionServiceImpl());
        servidor.start();

        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
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
    @DisplayName("El paciente recibe su historial, una prueba por línea y la más reciente primero")
    void pacienteConsultaSuHistorial() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic("paciente.demo:demo"));

        // Comprobar
        String[] lineas = respuesta.body().split("\n");
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals(2, lineas.length),
                () -> assertEquals(
                        "P2;2026-03-05;Ecografia;REALIZADA;Control, revisar",
                        lineas[0]),
                () -> assertEquals(
                        "P1;2026-01-10;Radiografia;REALIZADA;Sin hallazgos",
                        lineas[1])
        );
    }

    @Test
    @DisplayName("El médico consulta el historial de otro paciente")
    void medicoConsultaOtroPaciente() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=otro.paciente",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("P3;"))
        );
    }

    @Test
    @DisplayName("Un paciente sin pruebas recibe 200 con el cuerpo vacío")
    void historialVacio() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=nuevo.paciente",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().isEmpty())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "paciente.demo:demo, otro.paciente, 403",
            "administrativo.demo:demo, paciente.demo, 403",
            "admin.demo:demo, paciente.demo, 403"
    })
    @DisplayName("Acceso no autorizado: roles sin permiso o historial ajeno devuelven 403")
    void rechazaRolesSinPermiso(String credenciales, String paciente,
            int esperado) throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=" + paciente,
                basic(credenciales));

        // Comprobar
        assertAll(
                () -> assertEquals(esperado, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("error="))
        );
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "NULL",
            "Bearer token",
            "Basic !!!",
            "Basic cGFjaWVudGUuZGVtbzppbmNvcnJlY3Rh"
    })
    @DisplayName("Acceso no autorizado: sin credenciales válidas devuelve 401")
    void rechazaCredencialesIncorrectas(String autorizacion)
            throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo", autorizacion);

        // Comprobar
        assertAll(
                () -> assertEquals(401, respuesta.statusCode()),
                () -> assertTrue(respuesta.headers()
                        .firstValue("WWW-Authenticate").isPresent())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "/historial",
            "/historial?otro=1",
            "/historial?paciente=a&paciente=b",
            "/historial?paciente=%20",
            "/historial?paciente"
    })
    @DisplayName("Una petición con datos incorrectos devuelve 400")
    void rechazaDatosIncorrectos(String ruta) throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", ruta, basic("medico.demo:demo"));

        // Comprobar
        assertEquals(400, respuesta.statusCode());
    }

    @Test
    @DisplayName("Un método distinto de GET devuelve 405")
    void rechazaMetodoNoPermitido() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "POST", "/historial?paciente=paciente.demo",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(405, respuesta.statusCode()),
                () -> assertEquals("GET", respuesta.headers()
                        .firstValue("Allow").orElse(""))
        );
    }

    @Test
    @DisplayName("Las consultas por HTTP quedan anotadas en el registro")
    void registraLasConsultas() throws Exception {
        // Ejecutar
        enviar("GET", "/historial?paciente=paciente.demo",
                basic("paciente.demo:demo"));
        enviar("GET", "/historial?paciente=otro.paciente",
                basic("paciente.demo:demo"));

        // Comprobar
        assertEquals(2, registro.consultas().size());
    }

    @ParameterizedTest
    @CsvSource({"sinSeparador", ":demo"})
    @DisplayName("Credenciales mal formadas (sin usuario o sin separador) devuelven 401")
    void rechazaCredencialesMalFormadas(String credenciales)
            throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic(credenciales));

        // Comprobar
        assertEquals(401, respuesta.statusCode());
    }

    @Test
    @DisplayName("Una prueba con campos vacíos se envía con los campos en blanco")
    void pruebaConCamposNulos() throws Exception {
        // Preparar
        ProveedorHistorialEnMemoria incompleto =
                new ProveedorHistorialEnMemoria();
        incompleto.agregar("paciente.demo", new PruebaRealizada(
                "P9", LocalDate.of(2026, 1, 1), null, null, null));
        reiniciarServidor(new HistorialServiceImpl(incompleto, registro));

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals("P9;2026-01-01;;;", respuesta.body())
        );
    }

    @Test
    @DisplayName("Una ruta que no es /historial devuelve 404")
    void rutaNoEncontrada() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial/otra", basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(404, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("error="))
        );
    }

    @Test
    @DisplayName("Si el origen de pruebas está caído se devuelve 503")
    void origenCaidoDevuelve503() throws Exception {
        // Preparar
        IHistorialPruebasProveedor caido = idPaciente -> {
            throw new IllegalStateException("Sin conexion");
        };
        reiniciarServidor(new HistorialServiceImpl(caido, registro));

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(503, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("error="))
        );
    }

    @Test
    @DisplayName("Un fallo interno del servicio devuelve 500")
    void falloInternoDevuelve500() throws Exception {
        // Preparar
        IHistorialService roto = (sesion, idPaciente) -> {
            throw new IllegalStateException("Fallo interno simulado");
        };
        reiniciarServidor(roto);

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(500, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("error="))
        );
    }

    @Test
    @DisplayName("Si el servicio indica que no hay sesión se devuelve 401")
    void servicioSinSesionDevuelve401() throws Exception {
        // Preparar
        IHistorialService sinSesion = (sesion, idPaciente) ->
                ResultadoHistorial.rechazado(
                        ResultadoHistorial.Estado.NO_AUTENTICADO,
                        "Sesion no iniciada.");
        reiniciarServidor(sinSesion);

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic("medico.demo:demo"));

        // Comprobar
        assertEquals(401, respuesta.statusCode());
    }

    @Test
    @DisplayName("El servidor creado sin servicio propio responde con historial vacío")
    void servidorPorDefecto() throws Exception {
        // Preparar
        servidor.stop(0);
        servidor = GestionHistorialHttpServer.crearServidor(0);
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();

        // Ejecutar
        HttpResponse<String> respuesta = enviar(
                "GET", "/historial?paciente=paciente.demo",
                basic("medico.demo:demo"));

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().isEmpty())
        );
    }

    @Test
    @DisplayName("El método main arranca el servidor en el puerto indicado")
    void ejecutarMain() throws Exception {
        // Preparar: puerto 0 para no ocupar el 8085
        System.setProperty("historial.puerto", "0");

        try {
            // Ejecutar y comprobar
            assertDoesNotThrow(() -> GestionHistorialHttpServer.main(
                    new String[0]));
        } finally {
            System.clearProperty("historial.puerto");
        }
    }

    // Sustituye el servidor de la prueba por otro con un servicio distinto
    private void reiniciarServidor(IHistorialService servicio)
            throws Exception {
        servidor.stop(0);
        servidor = GestionHistorialHttpServer.crearServidor(
                0, servicio, new AutenticacionServiceImpl());
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private HttpResponse<String> enviar(
            String metodo,
            String ruta,
            String autorizacion) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(5));

        if (autorizacion != null) {
            peticion.header("Authorization", autorizacion);
        }

        peticion.method(metodo, HttpRequest.BodyPublishers.noBody());

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(
                credenciales.getBytes(StandardCharsets.UTF_8));
    }
}
