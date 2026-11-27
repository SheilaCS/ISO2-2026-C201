package com.sescam.radiodiagnostico.cliente.pacientes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

@DisplayName("Cliente HTTP de pacientes (CDU4)")
class PacientesHttpClientTest {

    private HttpServer servidor;
    private String urlBase;
    private final AtomicReference<String> ultimoMetodo = new AtomicReference<>();
    private final AtomicReference<String> ultimaRuta = new AtomicReference<>();
    private final AtomicReference<String> ultimoCuerpo = new AtomicReference<>();
    private final AtomicReference<String> ultimaAuth = new AtomicReference<>();
    private int codigoRespuesta = 200;
    private String cuerpoRespuesta = "id=11111111-1111-4111-8111-111111111111&cip=CIP99A&estado=ACTIVO";

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
    @DisplayName("Envía POST en alta de paciente con autenticación Basic y CIP normalizado")
    void altaPacienteEnviaPostConAuthYDatosNormalizados() throws Exception {
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "administrativo.demo", "1234");
        String resp = client.altaPaciente("  cip99a ", "Ana", "Lopez", "1995-03-10");

        assertEquals("POST", ultimoMetodo.get());
        assertEquals("/pacientes", ultimaRuta.get());
        assertNotNull(ultimaAuth.get());
        assertTrue(ultimaAuth.get().startsWith("Basic "));
        assertTrue(ultimoCuerpo.get().contains("cip=CIP99A"));
        assertTrue(ultimoCuerpo.get().contains("nombre=Ana"));
        assertEquals(cuerpoRespuesta, resp);
    }

    @Test
    @DisplayName("Devuelve mensaje por defecto cuando el servidor responde 200 con cuerpo vacío")
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
    @DisplayName("Envía GET en consulta por CIP y aplica autenticación cuando solo hay contraseña")
    void consultarPorCipEnviaGetConQueryYAuthSoloPassword() throws Exception {
        cuerpoRespuesta = "id=22222222-2222-4222-8222-222222222222&cip=CIP99A&nombre=Ana&apellidos=Lopez&fechaNacimiento=1995-03-10&estado=ACTIVO";
        PacientesHttpClient client = new PacientesHttpClient(urlBase, null, null);
        client.setCredenciales(null, "soloPass");
        String resp = client.consultarPorCip("cip99a");

        assertEquals("GET", ultimoMetodo.get());
        assertEquals("/pacientes?cip=CIP99A", ultimaRuta.get());
        assertNotNull(ultimaAuth.get());
        assertTrue(ultimaAuth.get().startsWith("Basic "));
        assertTrue(resp.contains("CIP99A"));
    }

    @Test
    @DisplayName("Envía PUT en modificación de paciente utilizando UUID en la ruta")
    void modificarPacienteEnviaPutConUuid() throws Exception {
        String uuid = UUID.randomUUID().toString();
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "administrativo.demo", "1234");
        client.setCredenciales("administrativo.demo", "abcd");
        String resp = client.modificarPaciente(uuid, "Ana Maria", "Lopez Ruiz", "1995-03-10");

        assertEquals("PUT", ultimoMetodo.get());
        assertEquals("/pacientes/" + uuid, ultimaRuta.get());
        assertTrue(ultimoCuerpo.get().contains("nombre=Ana+Maria"));
        assertEquals(cuerpoRespuesta, resp);
    }

    @Test
    @DisplayName("Envía DELETE en baja lógica de paciente utilizando UUID en la ruta")
    void darBajaPacienteEnviaDeleteConUuid() throws Exception {
        String uuid = UUID.randomUUID().toString();
        PacientesHttpClient client = new PacientesHttpClient(urlBase, "administrativo.demo", "1234");
        String resp = client.darBajaPaciente(uuid);

        assertEquals("DELETE", ultimoMetodo.get());
        assertEquals("/pacientes/" + uuid, ultimaRuta.get());
        assertEquals(cuerpoRespuesta, resp);
    }

    @Test
    @DisplayName("Formatea respuestas de error HTTP 401, 403 y >=400 con y sin cuerpo")
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
        cuerpoRespuesta = "Datos+incorrectos+o+incompletos";
        assertEquals("Error 400: Datos+incorrectos+o+incompletos", client.altaPaciente("CIP1", "Juan", "Perez", "1990-01-01"));
        cuerpoRespuesta = "";
        assertEquals("Error 400: Peticion rechazada.", client.altaPaciente("CIP1", "Juan", "Perez", "1990-01-01"));
    }

    @Test
    @DisplayName("Construye instancias con los constructores por defecto configurados en el puerto 8092")
    void constructorPorDefectoNoFalla() {
        assertNotNull(new PacientesHttpClient());
        assertNotNull(new PacientesHttpClient("administrativo.demo", "1234"));
    }
}