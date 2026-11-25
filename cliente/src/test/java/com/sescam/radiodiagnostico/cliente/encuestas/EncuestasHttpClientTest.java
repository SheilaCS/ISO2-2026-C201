package com.sescam.radiodiagnostico.cliente.encuestas;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
class EncuestasHttpClientTest {
    private HttpServer servidor;
    private EncuestasHttpClient cliente;
    private int codigo;
    private String cuerpo, ruta, auth, metodo;
    @BeforeEach
    void setUp() throws Exception {
        codigo = 200;
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/encuestas", e -> {
            ruta = e.getRequestURI().getPath(); metodo = e.getRequestMethod();
            auth = e.getRequestHeaders().getFirst("Authorization");
            cuerpo = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = "respuesta".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(codigo, bytes.length);
            try (var out = e.getResponseBody()) { out.write(bytes); }
        });
        servidor.start();
        cliente = new EncuestasHttpClient("http://localhost:" + servidor.getAddress().getPort() + "/encuestas");
    }
    @AfterEach void tearDown() { servidor.stop(0); }
    @Test
    @DisplayName("Envío de incidencia y codificación de caracteres")
    void incidencia() throws Exception {
        // Preparar
        String texto = "a & ñ";
        // Ejecutar
        String resultado = cliente.comunicarIncidencia(1, texto, "u", new char[]{'p'});
        // Comprobar
        assertAll(() -> assertEquals("respuesta", resultado), () -> assertEquals("/encuestas/incidencia", ruta), () -> assertEquals("POST", metodo), () -> assertEquals("Basic dTpw", auth), () -> assertEquals("idCita=1&descripcion=a+%26+%C3%B1", cuerpo));
    }

    @Test
    @DisplayName("Envío de encuesta válida")
    void encuesta() throws Exception {
        // Preparar
        int puntuacion = 5;
        // Ejecutar
        String resultado = cliente.responderEncuesta(1, puntuacion, "", "u", new char[]{'p'});
        // Comprobar
        assertAll(() -> assertEquals("respuesta", resultado), () -> assertEquals("/encuestas/responder", ruta), () -> assertEquals("idCita=1&puntuacion=5&comentario=", cuerpo));
    }

    @Test
    @DisplayName("Respuesta HTTP de error")
    void rechazo() throws Exception {
        // Preparar
        codigo = 400;
        // Ejecutar
        String resultado = cliente.comunicarIncidencia(1, "a", "u", new char[]{'p'});
        // Comprobar
        assertEquals("Error: respuesta", resultado);
    }

    @Test
    @DisplayName("Servidor no disponible")
    void sinServidor() throws Exception {
        // Preparar
        servidor.stop(0);
        // Ejecutar
        String resultado = cliente.comunicarIncidencia(1, "a", "u", new char[]{'p'});
        // Comprobar
        assertEquals("No se pudo conectar con el servidor de encuestas", resultado);
    }


    @Test
    @DisplayName("El constructor por defecto utiliza la URL configurada")
    void constructorPorDefecto() {
        String anterior = System.getProperty("encuestas.url");
        try {
            // Preparar
            System.setProperty("encuestas.url", "http://localhost:" + servidor.getAddress().getPort() + "/encuestas");
            // Ejecutar
            String respuesta = new EncuestasHttpClient().comunicarIncidencia(1, "a", "u", new char[]{'p'});
            // Comprobar
            assertEquals("respuesta", respuesta);
        } finally {
            if (anterior == null) System.clearProperty("encuestas.url");
            else System.setProperty("encuestas.url", anterior);
        }
    }

    @Test
    @DisplayName("Una interrupción conserva el estado de interrupción")
    void consultaInterrumpida() {
        // Preparar
        Thread.currentThread().interrupt();
        try {
            // Ejecutar
            String respuesta = cliente.comunicarIncidencia(1, "a", "u", new char[]{'p'});
            // Comprobar
            assertEquals("Consulta interrumpida", respuesta);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
