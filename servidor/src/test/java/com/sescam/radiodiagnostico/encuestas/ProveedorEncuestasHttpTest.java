package com.sescam.radiodiagnostico.encuestas;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
class ProveedorEncuestasHttpTest {
    private HttpServer servidor;
    private ProveedorEncuestasHttp proveedor;
    private int codigo;
    private String cuerpo;
    @BeforeEach void setUp() throws Exception {
        codigo = 200; cuerpo = "Cita 12 | Fecha: hoy";
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", e -> {
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(codigo, bytes.length);
            try (var out = e.getResponseBody()) { out.write(bytes); }
        });
        servidor.start();
        String url = "http://localhost:" + servidor.getAddress().getPort();
        proveedor = new ProveedorEncuestasHttp(url + "/miscitas", url + "/realizaciones", "u", "p");
    }
    @AfterEach void tearDown() { servidor.stop(0); }
    @Test
    @DisplayName("Cita perteneciente al paciente")
    void propia() throws Exception {
        // Preparar
        int id = 12;
        // Ejecutar
        boolean resultado = proveedor.citaPertenecePaciente(id, 1);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("No confunde cita 1 con cita 12")
    void ajena() throws Exception {
        // Preparar
        int id = 1;
        // Ejecutar
        boolean resultado = proveedor.citaPertenecePaciente(id, 1);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Error de servicio de citas")
    void errorCitas() throws Exception {
        // Preparar
        codigo = 503;
        // Ejecutar
        var error = assertThrows(IllegalStateException.class, () -> proveedor.citaPertenecePaciente(12, 1));
        // Comprobar
        assertEquals("Mis citas no disponible", error.getMessage());
    }

    @Test
    @DisplayName("Prueba realizada")
    void realizada() throws Exception {
        // Preparar
        codigo = 200;
        // Ejecutar
        boolean resultado = proveedor.pruebaRealizada(12);
        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Prueba no realizada")
    void pendiente() throws Exception {
        // Preparar
        codigo = 404;
        // Ejecutar
        boolean resultado = proveedor.pruebaRealizada(12);
        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Error de servicio de realización")
    void errorRealizacion() throws Exception {
        // Preparar
        codigo = 503;
        // Ejecutar
        var error = assertThrows(IllegalStateException.class, () -> proveedor.pruebaRealizada(12));
        // Comprobar
        assertEquals("Realizaciones no disponible", error.getMessage());
    }


    @Test
    @DisplayName("El proveedor por defecto utiliza las URL configuradas")
    void constructorPorDefecto() {
        // Preparar
        String anterior = System.getProperty("miscitas.url");
        System.setProperty("miscitas.url", "http://localhost:" + servidor.getAddress().getPort() + "/miscitas");
        try {
            var real = new ProveedorEncuestasHttp();
            // Ejecutar
            boolean resultado = real.citaPertenecePaciente(12, 1);
            // Comprobar
            assertTrue(resultado);
        } finally {
            if (anterior == null) System.clearProperty("miscitas.url");
            else System.setProperty("miscitas.url", anterior);
        }
    }

    @Test
    @DisplayName("Una consulta interrumpida conserva la interrupcion")
    void consultaInterrumpida() {
        // Preparar
        Thread.currentThread().interrupt();
        try {
            // Ejecutar
            var error = assertThrows(IllegalStateException.class, () -> proveedor.pruebaRealizada(12));
            // Comprobar
            assertEquals("Consulta interrumpida", error.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("El proveedor informa de una conexion no disponible")
    void servidorNoDisponible() throws Exception {
        // Preparar: reservar un puerto local y cerrarlo antes de conectar
        int puerto;
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puerto = socket.getLocalPort();
        }
        var desconectado = new ProveedorEncuestasHttp("http://127.0.0.1:" + puerto,
                "http://127.0.0.1:" + puerto, "u", "p");
        // Ejecutar
        var error = assertThrows(IllegalStateException.class, () -> desconectado.pruebaRealizada(12));
        // Comprobar
        assertEquals("Servidor dependiente no disponible", error.getMessage());
        assertTrue(error.getCause() instanceof java.io.IOException);
    }

}
