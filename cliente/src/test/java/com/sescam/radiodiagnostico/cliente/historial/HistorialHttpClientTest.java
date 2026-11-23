package com.sescam.radiodiagnostico.cliente.historial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CDU12 · Cliente · HistorialHttpClient")
class HistorialHttpClientTest {

    private HttpServer servidor;
    private HistorialHttpClient cliente;

    private String metodoRecibido;
    private String rutaRecibida;
    private String consultaRecibida;
    private String autorizacionRecibida;

    private int codigoRespuesta;
    private String cuerpoRespuesta;

    @BeforeEach
    void preparar() throws IOException {
        codigoRespuesta = 200;
        cuerpoRespuesta = "";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/historial", exchange -> {
            metodoRecibido = exchange.getRequestMethod();
            rutaRecibida = exchange.getRequestURI().getPath();
            consultaRecibida = exchange.getRequestURI().getRawQuery();
            autorizacionRecibida = exchange.getRequestHeaders()
                    .getFirst("Authorization");

            byte[] contenido =
                    cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(codigoRespuesta, contenido.length);

            try (var salida = exchange.getResponseBody()) {
                salida.write(contenido);
            }
        });
        servidor.start();

        cliente = new HistorialHttpClient("http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/historial");
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("Envía un GET a /historial con el paciente y las credenciales Basic")
    void enviaPeticionCorrecta() throws Exception {
        // Ejecutar
        cliente.consultarHistorial(
                "medico.demo", "demo".toCharArray(), "paciente.demo");

        // Comprobar
        String esperada = "Basic " + Base64.getEncoder().encodeToString(
                "medico.demo:demo".getBytes(StandardCharsets.UTF_8));
        assertAll(
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("/historial", rutaRecibida),
                () -> assertEquals("paciente=paciente.demo", consultaRecibida),
                () -> assertEquals(esperada, autorizacionRecibida)
        );
    }

    @Test
    @DisplayName("Codifica los caracteres especiales del identificador")
    void codificaElIdentificador() throws Exception {
        // Ejecutar
        cliente.consultarHistorial(
                "medico.demo", "demo".toCharArray(), "a b&c");

        // Comprobar
        assertEquals("paciente=a+b%26c", consultaRecibida);
    }

    @Test
    @DisplayName("Devuelve el código y el cuerpo de una respuesta correcta")
    void devuelveRespuestaCorrecta() throws Exception {
        // Preparar
        cuerpoRespuesta = "P1;2026-01-10;Radiografia;REALIZADA;Sin hallazgos";

        // Ejecutar
        RespuestaHistorial respuesta = cliente.consultarHistorial(
                "medico.demo", "demo".toCharArray(), "paciente.demo");

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.codigo()),
                () -> assertEquals(cuerpoRespuesta, respuesta.cuerpo()),
                () -> assertTrue(respuesta.correcta())
        );
    }

    @Test
    @DisplayName("Devuelve el código y el cuerpo de una respuesta rechazada")
    void devuelveRespuestaRechazada() throws Exception {
        // Preparar
        codigoRespuesta = 403;
        cuerpoRespuesta = "error=Acceso+reservado";

        // Ejecutar
        RespuestaHistorial respuesta = cliente.consultarHistorial(
                "paciente.demo", "demo".toCharArray(), "otro.paciente");

        // Comprobar
        assertAll(
                () -> assertEquals(403, respuesta.codigo()),
                () -> assertFalse(respuesta.correcta()),
                () -> assertEquals("Error HTTP 403: Acceso reservado",
                        HistorialHttpClient.mensajeError(respuesta))
        );
    }

    @Test
    @DisplayName("Si el servidor no está disponible se lanza una IOException")
    void servidorCaido() {
        // Preparar
        servidor.stop(0);

        // Ejecutar y comprobar
        assertThrows(IOException.class, () -> cliente.consultarHistorial(
                "medico.demo", "demo".toCharArray(), "paciente.demo"));
    }

    @Test
    @DisplayName("El cliente normal apunta al puerto 8085")
    void direccionPorDefecto() {
        assertAll(
                () -> assertEquals("http://localhost:8085/historial",
                        HistorialHttpClient.URL_POR_DEFECTO),
                () -> assertNotNull(new HistorialHttpClient())
        );
    }

    @Test
    @DisplayName("Convierte cada línea del cuerpo en una prueba")
    void parseaPruebas() {
        // Ejecutar
        List<PruebaHistorial> pruebas = HistorialHttpClient.parsearPruebas(
                "P2;2026-03-05;Ecografia;REALIZADA;Control\n"
                + "P1;2026-01-10;Radiografia;REALIZADA;");

        // Comprobar
        assertAll(
                () -> assertEquals(2, pruebas.size()),
                () -> assertEquals(new PruebaHistorial("P2", "2026-03-05",
                        "Ecografia", "REALIZADA", "Control"), pruebas.get(0)),
                () -> assertEquals("", pruebas.get(1).informe()),
                () -> assertEquals("Radiografia", pruebas.get(1).tipo())
        );
    }

    @Test
    @DisplayName("Un cuerpo nulo o vacío no tiene pruebas")
    void parseaCuerpoVacio() {
        assertAll(
                () -> assertTrue(HistorialHttpClient
                        .parsearPruebas(null).isEmpty()),
                () -> assertTrue(HistorialHttpClient
                        .parsearPruebas("").isEmpty()),
                () -> assertTrue(HistorialHttpClient
                        .parsearPruebas("   ").isEmpty())
        );
    }

    @Test
    @DisplayName("Una línea con campos de más o de menos es un formato incorrecto")
    void parseaFormatoIncorrecto() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> HistorialHttpClient.parsearPruebas("P1;2026-01-10;Rx"));

        assertEquals("La respuesta del servidor tiene un formato incorrecto",
                error.getMessage());
    }

    @Test
    @DisplayName("El mensaje de error usa el texto del servidor, decodificado")
    void mensajeConTextoDelServidor() {
        RespuestaHistorial respuesta = new RespuestaHistorial(
                403, "error=Un+paciente+solo+puede+ver+su+historial%2E");

        assertEquals("Error HTTP 403: Un paciente solo puede ver su historial.",
                HistorialHttpClient.mensajeError(respuesta));
    }

    @ParameterizedTest
    @CsvSource({
            "400, Datos incorrectos.",
            "401, Credenciales incorrectas.",
            "403, Acceso no permitido.",
            "405, Metodo no permitido.",
            "503, Servicio no disponible.",
            "500, Error inesperado del servidor."
    })
    @DisplayName("Sin texto del servidor se usa un mensaje según el código HTTP")
    void mensajePorCodigo(int codigo, String esperado) {
        assertEquals("Error HTTP " + codigo + ": " + esperado,
                HistorialHttpClient.mensajeError(
                        new RespuestaHistorial(codigo, "")));
    }

    @Test
    @DisplayName("Un cuerpo nulo o sin campo error usa el mensaje por defecto")
    void mensajeConCuerpoSinError() {
        assertAll(
                () -> assertEquals("Error HTTP 401: Credenciales incorrectas.",
                        HistorialHttpClient.mensajeError(
                                new RespuestaHistorial(401, null))),
                () -> assertEquals("Error HTTP 400: Datos incorrectos.",
                        HistorialHttpClient.mensajeError(
                                new RespuestaHistorial(400, "otro=valor"))),
                () -> assertEquals("Error HTTP 400: Datos incorrectos.",
                        HistorialHttpClient.mensajeError(
                                new RespuestaHistorial(400, "sinIgual")))
        );
    }

    @Test
    @DisplayName("Solo el código 200 es una respuesta correcta")
    void soloElCodigo200EsCorrecto() {
        assertAll(
                () -> assertTrue(new RespuestaHistorial(200, "").correcta()),
                () -> assertFalse(new RespuestaHistorial(201, "").correcta()),
                () -> assertFalse(new RespuestaHistorial(403, "").correcta())
        );
    }
}
