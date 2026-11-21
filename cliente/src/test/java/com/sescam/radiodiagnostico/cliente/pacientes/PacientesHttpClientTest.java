package com.sescam.radiodiagnostico.cliente.pacientes;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PacientesHttpClientTest {

    private HttpServer servidor;
    private String urlBase;
    private final AtomicReference<String> ultimoMetodo = new AtomicReference<>();
    private final AtomicReference<String> ultimaRuta = new AtomicReference<>();
    private final AtomicReference<String> ultimoCuerpo = new AtomicReference<>();
    private final AtomicReference<String> ultimaAuth = new AtomicReference<>();
    private int codigoRespuesta = 200;
    private String cuerpoRespuesta = "Operacion realizada correctamente.";

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/pacientes", exchange -> {
            ultimoMetodo.set(exchange.getRequestMethod());
            ultimaRuta.set(exchange.getRequestURI().toString());
            ultimaAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            ultimoCuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] bytes = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            int longitud = bytes.length == 0 ? -1 : bytes.length;
            exchange.sendResponseHeaders(codigoRespuesta, longitud);
            if (bytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.getResponseBody().close();
            }
        });
        servidor.start();
        urlBase = "http://localhost:" + servidor.getAddress().getPort() + "/pacientes";
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void altaPacienteEnviaPostConAuthYDatosNormalizados() throws Exception {
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "admin", "1234");
        String resp = client.altaPaciente("  cip99a ", "Ana", "Lopez", "1995-03-10");

        assertEquals("POST", ultimoMetodo.get());
        assertEquals("/pacientes", ultimaRuta.get());
        assertNotNull(ultimaAuth.get());
        assertTrue(ultimaAuth.get().startsWith("Basic "));
        assertTrue(ultimoCuerpo.get().contains("cip=CIP99A"));
        assertTrue(ultimoCuerpo.get().contains("nombre=Ana"));
        assertEquals("Operacion realizada correctamente.", resp);
    }

    @Test
    void respuesta200VaciaDevuelveMensajePorDefecto() throws Exception {
        codigoRespuesta = 200;
        cuerpoRespuesta = "   ";
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "", "");
        String resp = client.consultarPorCip("cip99a");

        assertEquals("GET", ultimoMetodo.get());
        assertEquals("/pacientes?cip=CIP99A", ultimaRuta.get());
        assertNull(ultimaAuth.get());
        assertEquals("Operacion realizada correctamente.", resp);
    }

    @Test
    void consultarPorCipEnviaGetConQueryYAuthSoloPassword() throws Exception {
        cuerpoRespuesta = "ID: 1 | CIP: CIP99A";
        PacientesHttpClient client = new PacientesHttpClient(urlBase, null, null);
        client.setCredenciales(null, "soloPass");
        String resp = client.consultarPorCip("cip99a");

        assertEquals("GET", ultimoMetodo.get());
        assertEquals("/pacientes?cip=CIP99A", ultimaRuta.get());
        assertNotNull(ultimaAuth.get());
        assertTrue(ultimaAuth.get().startsWith("Basic "));
        assertEquals("ID: 1 | CIP: CIP99A", resp);
    }

    @Test
    void modificarPacienteEnviaPutConId() throws Exception {
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "admin", "1234");
        client.setCredenciales("admin2", "abcd");
        String resp = client.modificarPaciente(5, "Ana Maria", "Lopez Ruiz", "1995-03-10");

        assertEquals("PUT", ultimoMetodo.get());
        assertEquals("/pacientes/5", ultimaRuta.get());
        assertTrue(ultimoCuerpo.get().contains("nombre=Ana+Maria"));
        assertEquals("Operacion realizada correctamente.", resp);
    }

    @Test
    void darBajaPacienteEnviaDeleteConId() throws Exception {
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "admin", "1234");
        String resp = client.darBajaPaciente(7);

        assertEquals("DELETE", ultimoMetodo.get());
        assertEquals("/pacientes/7", ultimaRuta.get());
        assertEquals("Operacion realizada correctamente.", resp);
    }

    @Test
    void manejaErroresHttp401_403_y_400ConYCuerpoVacio() throws Exception {
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "user", "bad");

        codigoRespuesta = 401;
        cuerpoRespuesta = "Token invalido";
        assertTrue(client.consultarPorCip("CIP1").contains("Error 401: Credenciales invalidas. Token invalido"));
        cuerpoRespuesta = "";
        assertEquals("Error 401: Credenciales invalidas.", client.consultarPorCip("CIP1"));

        codigoRespuesta = 403;
        cuerpoRespuesta = "Rol no permitido";
        assertTrue(client.consultarPorCip("CIP1").contains("Error 403: Acceso denegado. Se requiere rol ADMINISTRATIVO. Rol no permitido"));
        cuerpoRespuesta = "";
        assertEquals("Error 403: Acceso denegado. Se requiere rol ADMINISTRATIVO.", client.consultarPorCip("CIP1"));

        codigoRespuesta = 400;
        cuerpoRespuesta = "CIP duplicado";
        assertEquals("Error 400: CIP duplicado", client.altaPaciente("CIP1", "Juan", "Perez", "1990-01-01"));
        cuerpoRespuesta = "";
        assertEquals("Error 400: Peticion rechazada.", client.altaPaciente("CIP1", "Juan", "Perez", "1990-01-01"));
    }

    @Test
    void constructorPorDefectoNoFalla() {
        assertNotNull(new PacientesHttpClient());
        assertNotNull(new PacientesHttpClient("admin", "1234"));
    }
}