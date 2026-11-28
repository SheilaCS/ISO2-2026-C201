package com.sescam.radiodiagnostico.seguridad;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sun.net.httpserver.HttpServer;

public class SeguridadHttpHandlerTest {

    private HttpServer servidor;
    private int puerto;

    private SeguridadStub servicio;
    private AutenticacionStub autenticacion;

    private static class SeguridadStub
            implements ISeguridadService {

        boolean registrarResultado = true;
        boolean permitirConsulta = true;
        boolean lanzarError = false;

        List<String> logs = new ArrayList<>();

        @Override
        public boolean registrarAuditoria(
                String usuario,
                String accion,
                String detalles) {

            if (lanzarError) {
                throw new RuntimeException();
            }

            return registrarResultado;
        }

        @Override
        public boolean puedeConsultarAuditoria(
                String usuario) {

            return permitirConsulta;
        }

        @Override
        public List<String> consultarLogs() {

            if (lanzarError) {
                throw new RuntimeException();
            }

            return logs;
        }
    }

    private static class AutenticacionStub
            implements IAutenticacionService {

        boolean autenticado = true;

        @Override
        public Optional<Sesion> iniciarSesion(
                String usuario,
                char[] clave) {

            if (!autenticado) {
                return Optional.empty();
            }

            return Optional.of(
                    new Sesion(
                            usuario,
                            Rol.ADMINISTRADOR));
        }
    }

    @BeforeEach
    void preparar() throws IOException {

        servicio = new SeguridadStub();
        autenticacion = new AutenticacionStub();

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext(
                "/seguridad",
                new SeguridadHttpHandler(
                        servicio,
                        autenticacion));

        servidor.start();

        puerto = servidor.getAddress().getPort();
    }

    @AfterEach
    void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String auth() {

        return "Basic " + Base64.getEncoder()
                .encodeToString(
                        "admin.demo:demo"
                                .getBytes(StandardCharsets.UTF_8));
    }

    private String leer(HttpURLConnection conexion)
            throws IOException {

        if (conexion.getResponseCode() >= 400) {

            return new String(
                    conexion.getErrorStream()
                            .readAllBytes(),
                    StandardCharsets.UTF_8);
        }

        return new String(
                conexion.getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Servidor activo")
    void testServidorActivo()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        assertEquals(
                200,
                conexion.getResponseCode());

        assertTrue(
                leer(conexion)
                        .contains("funcionando"));
    }

    @Test
    @DisplayName("Ruta inexistente")
    void testRutaNoEncontrada()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/ruta");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        assertEquals(
                404,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Sin autenticacion")
    void testSinAutenticacion()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        assertEquals(
                401,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Autenticacion incorrecta")
    void testAutenticacionIncorrecta()
            throws Exception {

        autenticacion.autenticado = false;

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestProperty(
                "Authorization",
                auth());

        assertEquals(
                401,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Registrar correctamente")
    void testRegistrarCorrectamente()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/registrar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        conexion.setRequestProperty(
                "Authorization",
                auth());

        conexion.getOutputStream().write(
                "accion=LOGIN&detalles=ok"
                        .getBytes(StandardCharsets.UTF_8));

        assertEquals(
                200,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Registrar sin accion")
    void testRegistrarSinAccion()
            throws Exception {

        servicio.registrarResultado = false;

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/registrar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        conexion.setRequestProperty(
                "Authorization",
                auth());

        conexion.getOutputStream().write(
                "".getBytes(StandardCharsets.UTF_8));

        assertEquals(
                400,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Consultar logs con permiso")
    void testConsultarLogsPermitido()
            throws Exception {

        servicio.logs.add("LOG 1");

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestProperty(
                "Authorization",
                auth());

        assertEquals(
                200,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Consultar logs sin permiso")
    void testConsultarLogsDenegado()
            throws Exception {

        servicio.permitirConsulta = false;

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestProperty(
                "Authorization",
                auth());

        assertEquals(
                403,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Error interno")
    void testErrorInterno()
            throws Exception {

        servicio.lanzarError = true;

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestProperty(
                "Authorization",
                auth());

        assertEquals(
                500,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Crear servidor")
    void testCrearServidor()
            throws Exception {

        HttpServer server =
                SeguridadHttpHandler.crearServidor(0);

        assertNotNull(server);

        server.stop(0);
    }

    @Test
void cabeceraBase64Invalida()
        throws Exception {

    URL url = new URL(
            "http://127.0.0.1:"
                    + puerto
                    + "/seguridad/logs");

    HttpURLConnection conexion =
            (HttpURLConnection)
                    url.openConnection();

    conexion.setRequestProperty(
            "Authorization",
            "Basic ####");

    assertEquals(
            401,
            conexion.getResponseCode());
}

@Test
void credencialesSinSeparador()
        throws Exception {

    String token =
            Base64.getEncoder()
                    .encodeToString(
                            "textoSinDosPuntos"
                                    .getBytes(
                                            StandardCharsets.UTF_8));

    URL url = new URL(
            "http://127.0.0.1:"
            + puerto
            + "/seguridad/logs");

    HttpURLConnection conexion =
            (HttpURLConnection)
                    url.openConnection();

    conexion.setRequestProperty(
            "Authorization",
            "Basic " + token);

    assertEquals(
            401,
            conexion.getResponseCode());
}

@Test
void consultarLogsVacios()
        throws Exception {

    servicio.permitirConsulta = true;
    servicio.logs.clear();

    URL url = new URL(
            "http://127.0.0.1:"
                + puerto
                + "/seguridad/logs");

    HttpURLConnection conexion =
            (HttpURLConnection)
                    url.openConnection();

    conexion.setRequestProperty(
            "Authorization",
            auth());

    assertEquals(
            200,
            conexion.getResponseCode());

    assertEquals(
            "No hay registros de auditoría.",
            leer(conexion));
}

@Test
void testMain()
        throws Exception {

    System.setProperty(
            "seguridad.puerto",
            "0");

    Thread hilo =
            new Thread(() -> {

                try {
                    SeguridadHttpHandler.main(
                            new String[0]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    hilo.setDaemon(true);

    hilo.start();

    Thread.sleep(200);
}
}