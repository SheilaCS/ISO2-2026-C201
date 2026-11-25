package com.sescam.radiodiagnostico.listasespera;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CDU15 · Servidor · AsignadorCitasHttp")
class AsignadorCitasHttpTest {

    private static final LocalDate FECHA = LocalDate.of(2027, 1, 15);
    private static final LocalTime HORA = LocalTime.of(10, 0);

    private HttpServer citas;
    private final AtomicReference<String> cuerpoRecibido =
            new AtomicReference<>();
    private final AtomicReference<String> tipoRecibido =
            new AtomicReference<>();
    private final AtomicReference<String> metodoRecibido =
            new AtomicReference<>();

    @AfterEach
    void parar() {
        if (citas != null) {
            citas.stop(0);
        }
    }

    // Servidor de citas falso que responde siempre lo mismo
    private AsignadorCitasHttp arrancar(int codigo, String cuerpo)
            throws Exception {
        citas = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        citas.createContext("/citas/asignar", ex -> {
            metodoRecibido.set(ex.getRequestMethod());
            tipoRecibido.set(ex.getRequestHeaders().getFirst("Content-Type"));
            cuerpoRecibido.set(new String(
                    ex.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        citas.start();

        return new AsignadorCitasHttp("http://127.0.0.1:"
                + citas.getAddress().getPort() + "/citas/asignar");
    }

    @Test
    @DisplayName("Envía un POST de formulario con solicitud, paciente, fecha y hora")
    void enviaLaPeticionCorrecta() throws Exception {
        // Preparar
        AsignadorCitasHttp asignador =
                arrancar(200, "Cita asignada correctamente");

        // Ejecutar
        boolean resultado = asignador.asignarCita(9, 1, FECHA, HORA);

        // Comprobar
        assertAll(
                () -> assertTrue(resultado),
                () -> assertEquals("POST", metodoRecibido.get()),
                () -> assertEquals("application/x-www-form-urlencoded",
                        tipoRecibido.get()),
                () -> assertEquals(
                        "solicitud=9&paciente=1&fecha=2027-01-15&hora=10%3A00",
                        cuerpoRecibido.get())
        );
    }

    // Clases de equivalencia de la respuesta del modulo de citas
    @ParameterizedTest(name = "{0} \"{1}\" -> asignada = {2}")
    @CsvSource(delimiter = '|', value = {
            "200|Cita asignada correctamente|true",
            "200|No se ha podido asignar la cita|false",
            "200|''|false",
            "400|Datos incorrectos|false",
            "409|Horario ocupado|false"
    })
    @DisplayName("Interpreta la respuesta del módulo de citas: asignada o rechazada")
    void interpretaLaRespuesta(int codigo, String cuerpo, boolean esperado)
            throws Exception {
        // Preparar
        AsignadorCitasHttp asignador = arrancar(codigo, cuerpo);

        // Ejecutar y comprobar
        assertEquals(esperado, asignador.asignarCita(9, 1, FECHA, HORA));
    }

    @ParameterizedTest(name = "codigo {0} lanza IllegalStateException")
    @CsvSource({"401", "404", "500", "503"})
    @DisplayName("Una respuesta inesperada del módulo de citas se comunica como servicio no disponible")
    void respuestaInesperada(int codigo) throws Exception {
        // Preparar
        AsignadorCitasHttp asignador = arrancar(codigo, "error");

        // Ejecutar y comprobar
        assertThrows(IllegalStateException.class,
                () -> asignador.asignarCita(9, 1, FECHA, HORA));
    }

    @Test
    @DisplayName("Si el servidor de citas está caído lanza IllegalStateException")
    void servidorCaido() throws Exception {
        // Preparar
        AsignadorCitasHttp asignador = arrancar(200, "x");
        citas.stop(0);

        // Ejecutar y comprobar
        assertThrows(IllegalStateException.class,
                () -> asignador.asignarCita(9, 1, FECHA, HORA));
    }

    @Test
    @DisplayName("Una petición interrumpida lanza IllegalStateException y conserva la interrupción")
    void peticionInterrumpida() throws Exception {
        // Preparar
        AsignadorCitasHttp asignador =
                arrancar(200, "Cita asignada correctamente");

        // Ejecutar
        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class,
                    () -> asignador.asignarCita(9, 1, FECHA, HORA));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("El constructor normal usa la propiedad citas.puerto")
    void constructorNormalUsaLaPropiedad() throws Exception {
        // Preparar
        arrancar(200, "Cita asignada correctamente");
        String anterior = System.getProperty("citas.puerto");
        System.setProperty("citas.puerto",
                Integer.toString(citas.getAddress().getPort()));

        try {
            // Ejecutar
            AsignadorCitasHttp asignador = new AsignadorCitasHttp();

            // Comprobar
            assertTrue(asignador.asignarCita(9, 1, FECHA, HORA));
        } finally {
            if (anterior == null) {
                System.clearProperty("citas.puerto");
            } else {
                System.setProperty("citas.puerto", anterior);
            }
        }
    }

    @Test
    @DisplayName("El constructor normal se crea sin fallar")
    void constructorNormal() {
        assertDoesNotThrow(() -> {
            new AsignadorCitasHttp();
        });
    }
}
