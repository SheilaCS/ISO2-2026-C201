package com.sescam.radiodiagnostico.historial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CDU12 · Servidor · ProveedorHistorialRealizacion")
class ProveedorHistorialRealizacionTest {

    // Realizaciones de ejemplo (formato real de CDU7)
    private static final String REALIZACIONES =
            "id=a1&idCita=10&idSolicitud=2&fechaRealizacion=2026-03-01"
                    + "&observaciones=Sin+hallazgos&incidencia=&estado=REALIZADA\n"
                    + "id=a2&idCita=11&idSolicitud=3&fechaRealizacion=2026-03-05"
                    + "&observaciones=Control&incidencia=Paciente+movido"
                    + "&estado=REALIZADA_CON_INCIDENCIA\n"
                    + "id=a3&idCita=12&idSolicitud=4&fechaRealizacion=2026-03-09"
                    + "&observaciones=&incidencia=&estado=REALIZADA";

    private HttpServer realizaciones;
    private HttpServer citas;
    private final AtomicInteger consultasCita = new AtomicInteger();
    private final AtomicReference<String> cabeceraRecibida =
            new AtomicReference<>();

    @AfterEach
    void parar() {
        if (realizaciones != null) {
            realizaciones.stop(0);
        }
        if (citas != null) {
            citas.stop(0);
        }
    }

    // Servidor de realizaciones falso
    private void arrancarRealizaciones(int codigo, String cuerpo)
            throws Exception {
        realizaciones = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        realizaciones.createContext("/realizaciones", ex -> {
            cabeceraRecibida.set(
                    ex.getRequestHeaders().getFirst("Authorization"));
            responder(ex, codigo, cuerpo);
        });
        realizaciones.start();
    }

    // Servidor de citas falso: cita 10 y 12 de paciente 1, 11 de paciente 2
    private void arrancarCitas(int codigoCitas) throws Exception {
        citas = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        citas.createContext("/citas/consultar", ex -> {
            consultasCita.incrementAndGet();
            String consulta = ex.getRequestURI().getRawQuery();
            String cuerpo;
            if ("id=10".equals(consulta) || "id=12".equals(consulta)) {
                cuerpo = "ID cita: 10\nID solicitud: 2\nID paciente: 1\n"
                        + "Fecha: 2026-03-01\nEstado: ACTIVA";
            } else if ("id=11".equals(consulta)) {
                cuerpo = "ID cita: 11\nID solicitud: 3\nID paciente: 2\n"
                        + "Fecha: 2026-03-05\nEstado: ACTIVA";
            } else {
                cuerpo = "No existe una cita con ese ID.";
            }
            responder(ex, codigoCitas, cuerpo);
        });
        citas.start();
    }

    private static void responder(com.sun.net.httpserver.HttpExchange ex,
                                  int codigo, String cuerpo)
            throws java.io.IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(codigo, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private ProveedorHistorialRealizacion proveedor() {
        return new ProveedorHistorialRealizacion(
                "http://127.0.0.1:" + realizaciones.getAddress().getPort()
                        + "/realizaciones",
                "http://127.0.0.1:" + citas.getAddress().getPort()
                        + "/citas/consultar",
                "servicio", "clave");
    }

    @Test
    @DisplayName("Devuelve solo las pruebas del paciente, con tipo, estado e informe")
    void devuelvePruebasDelPaciente() throws Exception {
        // Preparar
        arrancarRealizaciones(200, REALIZACIONES);
        arrancarCitas(200);

        // Ejecutar
        List<PruebaRealizada> paciente1 =
                proveedor().pruebasDePaciente("1");
        List<PruebaRealizada> paciente2 =
                proveedor().pruebasDePaciente("2");

        // Comprobar
        assertAll(
                () -> assertEquals(2, paciente1.size()),
                () -> assertEquals("a1", paciente1.get(0).id()),
                () -> assertEquals(LocalDate.of(2026, 3, 1),
                        paciente1.get(0).fecha()),
                () -> assertEquals("Prueba de la solicitud 2",
                        paciente1.get(0).tipo()),
                () -> assertEquals("REALIZADA", paciente1.get(0).estado()),
                () -> assertEquals("Sin hallazgos",
                        paciente1.get(0).informe()),
                () -> assertEquals("Sin observaciones",
                        paciente1.get(1).informe()),
                () -> assertEquals(1, paciente2.size()),
                () -> assertEquals("REALIZADA_CON_INCIDENCIA",
                        paciente2.get(0).estado()),
                () -> assertEquals("Control. Incidencia: Paciente movido",
                        paciente2.get(0).informe())
        );
    }

    @Test
    @DisplayName("Usa la cuenta de servicio y consulta cada cita una sola vez")
    void usaCuentaDeServicio() throws Exception {
        // Preparar
        arrancarRealizaciones(200, REALIZACIONES);
        arrancarCitas(200);

        // Ejecutar
        proveedor().pruebasDePaciente("1");

        // Comprobar: 3 realizaciones, 3 citas distintas
        String esperado = "Basic " + Base64.getEncoder().encodeToString(
                "servicio:clave".getBytes(StandardCharsets.UTF_8));
        assertAll(
                () -> assertEquals(esperado, cabeceraRecibida.get()),
                () -> assertEquals(3, consultasCita.get())
        );
    }

    @Test
    @DisplayName("Un identificador con ceros o no numerico se trata sin fallar")
    void normalizaIdentificador() throws Exception {
        // Preparar
        arrancarRealizaciones(200, REALIZACIONES);
        arrancarCitas(200);
        ProveedorHistorialRealizacion p = proveedor();

        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(2, p.pruebasDePaciente("001").size()),
                () -> assertEquals(2, p.pruebasDePaciente(" 1 ").size()),
                () -> assertTrue(p.pruebasDePaciente("paciente.demo")
                        .isEmpty()),
                () -> assertTrue(p.pruebasDePaciente(null).isEmpty()),
                () -> assertTrue(p.pruebasDePaciente("99").isEmpty())
        );
    }

    @Test
    @DisplayName("Ignora lineas incorrectas y citas que no existen")
    void ignoraLineasIncorrectas() throws Exception {
        // Preparar
        String cuerpo = "\nbasura\n"
                + "id=x1&idCita=abc&idSolicitud=2&fechaRealizacion=2026-03-01"
                + "&estado=REALIZADA\n"
                + "id=x2&idCita=10&idSolicitud=2&fechaRealizacion=no-es-fecha"
                + "&estado=REALIZADA\n"
                + "id=x3&idCita=10&idSolicitud=dos&fechaRealizacion=2026-03-01"
                + "&estado=REALIZADA\n"
                + "id=x4&idCita=10&fechaRealizacion=2026-03-01"
                + "&estado=REALIZADA\n"
                + "id=x5&idCita=10&idSolicitud=2&estado=REALIZADA\n"
                + "id=x6&idCita=99&idSolicitud=2&fechaRealizacion=2026-03-01"
                + "&observaciones=a&estado=REALIZADA\n"
                + "id=ok&idCita=10&idSolicitud=2&fechaRealizacion=2026-03-01"
                + "&observaciones=Bien&estado=REALIZADA";
        arrancarRealizaciones(200, cuerpo);
        arrancarCitas(200);

        // Ejecutar
        List<PruebaRealizada> pruebas = proveedor().pruebasDePaciente("1");

        // Comprobar
        assertAll(
                () -> assertEquals(1, pruebas.size()),
                () -> assertEquals("ok", pruebas.get(0).id())
        );
    }

    @Test
    @DisplayName("Sin realizaciones registradas devuelve una lista vacia")
    void sinRealizaciones() throws Exception {
        // Preparar
        arrancarRealizaciones(200, "");
        arrancarCitas(200);

        // Ejecutar y comprobar
        assertTrue(proveedor().pruebasDePaciente("1").isEmpty());
    }

    @Test
    @DisplayName("La incidencia sin observaciones aparece sola en el informe")
    void soloIncidencia() throws Exception {
        // Preparar
        arrancarRealizaciones(200,
                "id=i1&idCita=10&idSolicitud=2&fechaRealizacion=2026-03-01"
                        + "&observaciones=&incidencia=Fallo+del+equipo"
                        + "&estado=REALIZADA_CON_INCIDENCIA");
        arrancarCitas(200);

        // Ejecutar
        List<PruebaRealizada> pruebas = proveedor().pruebasDePaciente("1");

        // Comprobar
        assertEquals("Incidencia: Fallo del equipo",
                pruebas.get(0).informe());
    }

    @Test
    @DisplayName("Si realizaciones rechaza la cuenta (401/403/409/500) lanza IllegalStateException")
    void realizacionesRechaza() throws Exception {
        arrancarCitas(200);

        for (int codigo : new int[] {401, 403, 409, 500}) {
            if (realizaciones != null) {
                realizaciones.stop(0);
            }
            arrancarRealizaciones(codigo, "error=no");
            ProveedorHistorialRealizacion p = proveedor();

            assertThrows(IllegalStateException.class,
                    () -> p.pruebasDePaciente("1"), "codigo " + codigo);
        }
    }

    @Test
    @DisplayName("Si citas no responde bien lanza IllegalStateException")
    void citasRechaza() throws Exception {
        // Preparar
        arrancarRealizaciones(200, REALIZACIONES);
        arrancarCitas(500);
        ProveedorHistorialRealizacion p = proveedor();

        // Ejecutar y comprobar
        assertThrows(IllegalStateException.class,
                () -> p.pruebasDePaciente("1"));
    }

    @Test
    @DisplayName("Si un servidor esta caido lanza IllegalStateException")
    void servidorCaido() throws Exception {
        // Preparar
        arrancarRealizaciones(200, REALIZACIONES);
        arrancarCitas(200);
        ProveedorHistorialRealizacion p = proveedor();

        // Citas caido (realizaciones sigue en marcha)
        citas.stop(0);
        assertThrows(IllegalStateException.class,
                () -> p.pruebasDePaciente("1"));

        // Realizaciones caido
        realizaciones.stop(0);
        assertThrows(IllegalStateException.class,
                () -> p.pruebasDePaciente("1"));
    }

    @Test
    @DisplayName("Una consulta interrumpida lanza IllegalStateException y mantiene la interrupcion")
    void consultaInterrumpida() throws Exception {
        // Preparar
        arrancarRealizaciones(200, REALIZACIONES);
        arrancarCitas(200);
        ProveedorHistorialRealizacion p = proveedor();

        // Ejecutar
        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class,
                    () -> p.pruebasDePaciente("1"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("El constructor normal se crea con las direcciones por defecto")
    void constructorNormal() {
        assertDoesNotThrow(() -> {
            new ProveedorHistorialRealizacion();
        });
    }
}
