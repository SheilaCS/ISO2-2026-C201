package com.sescam.radiodiagnostico.cliente.listaespera;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU15 · Cliente · ListaEsperaHttpClient")
class ListaEsperaHttpClientTest {

    private static final char[] CLAVE = "demo".toCharArray();
    private static final String USUARIO = "administrativo.demo";

    private HttpServer servidor;
    private ListaEsperaHttpClient cliente;

    private String metodoRecibido;
    private String rutaRecibida;
    private String consultaRecibida;
    private String autorizacionRecibida;
    private String tipoRecibido;
    private String cuerpoRecibido;

    private int codigoRespuesta;
    private String cuerpoRespuesta;

    @BeforeEach
    void preparar() throws IOException {
        codigoRespuesta = 200;
        cuerpoRespuesta = "";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/listaespera", exchange -> {
            metodoRecibido = exchange.getRequestMethod();
            rutaRecibida = exchange.getRequestURI().getPath();
            consultaRecibida = exchange.getRequestURI().getRawQuery();
            autorizacionRecibida = exchange.getRequestHeaders()
                    .getFirst("Authorization");
            tipoRecibido = exchange.getRequestHeaders()
                    .getFirst("Content-Type");
            cuerpoRecibido = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            byte[] contenido =
                    cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(codigoRespuesta, contenido.length);

            try (var salida = exchange.getResponseBody()) {
                salida.write(contenido);
            }
        });
        servidor.start();

        cliente = new ListaEsperaHttpClient("http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/listaespera");
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    // ------------------------------------
    // PETICIONES
    // ------------------------------------

    @Test
    @DisplayName("La lista se pide con un GET a /listaespera y credenciales Basic")
    void consultarLista() throws Exception {
        cliente.consultarLista(USUARIO, CLAVE);

        String esperada = "Basic " + Base64.getEncoder().encodeToString(
                (USUARIO + ":demo").getBytes(StandardCharsets.UTF_8));
        assertAll(
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("/listaespera", rutaRecibida),
                () -> assertNull(consultaRecibida),
                () -> assertEquals(esperada, autorizacionRecibida),
                () -> assertNull(tipoRecibido)
        );
    }

    @Test
    @DisplayName("Los huecos de una fecha se piden con el parámetro fecha")
    void consultarHuecosConFecha() throws Exception {
        cliente.consultarHuecos(USUARIO, CLAVE, " 2026-12-10 ");

        assertAll(
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("/listaespera/huecos", rutaRecibida),
                () -> assertEquals("fecha=2026-12-10", consultaRecibida)
        );
    }

    @Test
    @DisplayName("Sin fecha o con la fecha vacía se piden todos los huecos")
    void consultarHuecosSinFecha() throws Exception {
        cliente.consultarHuecos(USUARIO, CLAVE, null);
        String primera = consultaRecibida;

        cliente.consultarHuecos(USUARIO, CLAVE, "  ");

        assertAll(
                () -> assertNull(primera),
                () -> assertNull(consultaRecibida),
                () -> assertEquals("/listaespera/huecos", rutaRecibida)
        );
    }

    @Test
    @DisplayName("La propuesta se pide con un GET a /listaespera/propuesta")
    void consultarPropuesta() throws Exception {
        cliente.consultarPropuesta(USUARIO, CLAVE);

        assertAll(
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("/listaespera/propuesta", rutaRecibida)
        );
    }

    @Test
    @DisplayName("Cambiar la prioridad envía un POST con la solicitud y la prioridad")
    void cambiarPrioridad() throws Exception {
        cliente.cambiarPrioridad(USUARIO, CLAVE, "1", "URGENTE");

        assertAll(
                () -> assertEquals("POST", metodoRecibido),
                () -> assertEquals("/listaespera/prioridad", rutaRecibida),
                () -> assertEquals("solicitud=1&prioridad=URGENTE",
                        cuerpoRecibido),
                () -> assertTrue(tipoRecibido.startsWith(
                        "application/x-www-form-urlencoded"))
        );
    }

    @Test
    @DisplayName("Retirar envía un POST con la solicitud")
    void retirar() throws Exception {
        cliente.retirar(USUARIO, CLAVE, "2");

        assertAll(
                () -> assertEquals("POST", metodoRecibido),
                () -> assertEquals("/listaespera/retirar", rutaRecibida),
                () -> assertEquals("solicitud=2", cuerpoRecibido)
        );
    }

    @Test
    @DisplayName("Asignar envía un POST con solicitud, médico, fecha y hora")
    void asignar() throws Exception {
        cliente.asignar(USUARIO, CLAVE, "2", "5", "2026-12-10", "09:00");

        assertAll(
                () -> assertEquals("POST", metodoRecibido),
                () -> assertEquals("/listaespera/asignar", rutaRecibida),
                () -> assertEquals(
                        "solicitud=2&medico=5&fecha=2026-12-10&hora=09%3A00",
                        cuerpoRecibido)
        );
    }

    @Test
    @DisplayName("Codifica los caracteres especiales de los datos enviados")
    void codificaLosDatos() throws Exception {
        cliente.cambiarPrioridad(USUARIO, CLAVE, "a b&c", "X=Y");

        assertEquals("solicitud=a+b%26c&prioridad=X%3DY", cuerpoRecibido);
    }

    @Test
    @DisplayName("Devuelve el código y el cuerpo de la respuesta")
    void devuelveLaRespuesta() throws Exception {
        codigoRespuesta = 409;
        cuerpoRespuesta = "error=Hueco+ocupado.";

        RespuestaListaEspera respuesta = cliente.retirar(USUARIO, CLAVE, "1");

        assertAll(
                () -> assertEquals(409, respuesta.codigo()),
                () -> assertEquals("error=Hueco+ocupado.", respuesta.cuerpo()),
                () -> assertFalse(respuesta.correcta())
        );
    }

    @Test
    @DisplayName("Una respuesta 200 se considera correcta")
    void respuestaCorrecta() throws Exception {
        assertTrue(cliente.consultarLista(USUARIO, CLAVE).correcta());
    }

    @Test
    @DisplayName("Con el servidor parado se lanza una excepción de entrada/salida")
    void servidorParado() {
        servidor.stop(0);

        assertThrows(IOException.class,
                () -> cliente.consultarLista(USUARIO, CLAVE));
    }

    @Test
    @DisplayName("El cliente normal apunta a la dirección por defecto")
    void direccionPorDefecto() {
        new ListaEsperaHttpClient();

        assertEquals("http://localhost:8086/listaespera",
                ListaEsperaHttpClient.URL_POR_DEFECTO);
    }

    // ------------------------------------
    // INTERPRETAR RESPUESTAS
    // ------------------------------------

    @Test
    @DisplayName("Convierte el cuerpo en solicitudes")
    void parsearSolicitudes() {
        List<SolicitudLista> lista = ListaEsperaHttpClient.parsearSolicitudes(
                "2;12;Ecografia;URGENTE;2026-11-05\n"
                + "1;11;Radiografia;ORDINARIO;2026-11-01");

        assertAll(
                () -> assertEquals(2, lista.size()),
                () -> assertEquals(new SolicitudLista(
                        "2", "12", "Ecografia", "URGENTE", "2026-11-05"),
                        lista.get(0)),
                () -> assertEquals("ORDINARIO", lista.get(1).prioridad())
        );
    }

    @Test
    @DisplayName("Un cuerpo vacío, en blanco o nulo da una lista vacía")
    void parsearVacio() {
        assertAll(
                () -> assertTrue(
                        ListaEsperaHttpClient.parsearSolicitudes("").isEmpty()),
                () -> assertTrue(
                        ListaEsperaHttpClient.parsearSolicitudes("  ").isEmpty()),
                () -> assertTrue(
                        ListaEsperaHttpClient.parsearSolicitudes(null).isEmpty()),
                () -> assertTrue(
                        ListaEsperaHttpClient.parsearHuecos(null).isEmpty()),
                () -> assertTrue(
                        ListaEsperaHttpClient.parsearPropuesta(null).isEmpty()),
                () -> assertTrue(
                        ListaEsperaHttpClient.parsearPropuesta("").isEmpty())
        );
    }

    @Test
    @DisplayName("Convierte el cuerpo en huecos")
    void parsearHuecos() {
        List<HuecoLista> lista = ListaEsperaHttpClient.parsearHuecos(
                "5;2026-12-10;09:00;10:00\n6;2026-12-11;08:00;09:00");

        assertAll(
                () -> assertEquals(2, lista.size()),
                () -> assertEquals(new HuecoLista(
                        "5", "2026-12-10", "09:00", "10:00"), lista.get(0)),
                () -> assertEquals("6", lista.get(1).medico())
        );
    }

    @Test
    @DisplayName("Convierte el cuerpo en una propuesta con solicitud y hueco")
    void parsearPropuesta() {
        Optional<PropuestaLista> propuesta =
                ListaEsperaHttpClient.parsearPropuesta(
                        "2;12;Ecografia;URGENTE;2026-11-05;"
                        + "6;2026-12-11;08:00;09:00");

        assertAll(
                () -> assertTrue(propuesta.isPresent()),
                () -> assertEquals("2", propuesta.get().solicitud().id()),
                () -> assertEquals("Ecografia",
                        propuesta.get().solicitud().tipo()),
                () -> assertEquals("6", propuesta.get().hueco().medico()),
                () -> assertEquals("09:00", propuesta.get().hueco().fin())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"basura", "1;2;3", "1;2;3;4;5;6"})
    @DisplayName("Un cuerpo con formato incorrecto lanza IllegalArgumentException")
    void formatoIncorrecto(String cuerpo) {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ListaEsperaHttpClient.parsearSolicitudes(cuerpo)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ListaEsperaHttpClient.parsearHuecos(cuerpo)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ListaEsperaHttpClient.parsearPropuesta(cuerpo))
        );
    }

    @Test
    @DisplayName("El mensaje de una operación correcta sale del cuerpo")
    void mensajeCorrecto() {
        assertEquals("Cita asignada.", ListaEsperaHttpClient.mensajeCorrecto(
                new RespuestaListaEspera(200, "mensaje=Cita+asignada.")));
    }

    @Test
    @DisplayName("Sin mensaje en el cuerpo se usa uno por defecto")
    void mensajeCorrectoPorDefecto() {
        assertAll(
                () -> assertEquals("Operacion realizada.",
                        ListaEsperaHttpClient.mensajeCorrecto(
                                new RespuestaListaEspera(200, ""))),
                () -> assertEquals("Operacion realizada.",
                        ListaEsperaHttpClient.mensajeCorrecto(
                                new RespuestaListaEspera(200, null))),
                () -> assertEquals("Operacion realizada.",
                        ListaEsperaHttpClient.mensajeCorrecto(
                                new RespuestaListaEspera(200, "otro=1")))
        );
    }

    @Test
    @DisplayName("El mensaje de error incluye el código HTTP y el motivo del servidor")
    void mensajeErrorConDetalle() {
        assertEquals("La operacion no se ha podido realizar. "
                + "Motivo: Hueco ocupado. (Codigo HTTP 409)",
                ListaEsperaHttpClient.mensajeError(new RespuestaListaEspera(
                        409, "otro=1&error=Hueco+ocupado.")));
    }

    @ParameterizedTest
    @CsvSource({
            "400, Los datos enviados no son validos.",
            "401, El usuario o la contrasena no son correctos.",
            "403, No tienes permiso para realizar esta operacion.",
            "404, El servicio no ha encontrado lo solicitado.",
            "405, La operacion solicitada no esta permitida.",
            "409, La operacion no se ha podido realizar.",
            "500, El servidor ha tenido un problema interno. Intentalo de nuevo mas tarde.",
            "999, El servidor ha respondido de forma inesperada."
    })
    @DisplayName("Sin motivo en el cuerpo se explica qué ha pasado según el código")
    void mensajeErrorPorDefecto(int codigo, String esperado) {
        String mensaje = esperado + " (Codigo HTTP " + codigo + ")";

        assertAll(
                () -> assertEquals(mensaje, ListaEsperaHttpClient.mensajeError(
                        new RespuestaListaEspera(codigo, ""))),
                () -> assertEquals(mensaje, ListaEsperaHttpClient.mensajeError(
                        new RespuestaListaEspera(codigo, null)))
        );
    }
}
