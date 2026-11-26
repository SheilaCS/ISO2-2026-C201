package com.sescam.radiodiagnostico.seguridad;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class SeguridadHttpHandlerTest {

    private HttpServer servidor;
    private int puerto;

    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext(
                "/seguridad",
                new SeguridadHttpHandler());

        servidor.start();

        puerto = servidor.getAddress().getPort();
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String leerRespuesta(
            HttpURLConnection conexion)
            throws IOException {

        return new String(
                conexion.getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private String leerError(
            HttpURLConnection conexion)
            throws IOException {

        return new String(
                conexion.getErrorStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Administrador puede gestionar usuarios")
    void testPermisoConcedido()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/verificar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "usuario=juan"
                + "&rol=ADMINISTRADOR"
                + "&accion=GESTIONAR_USUARIOS";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                200,
                conexion.getResponseCode());

        assertEquals(
                "Permiso concedido",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Paciente no puede gestionar usuarios")
    void testPermisoDenegado()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/verificar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "usuario=juan"
                + "&rol=PACIENTE"
                + "&accion=GESTIONAR_USUARIOS";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                403,
                conexion.getResponseCode());

        assertTrue(
                leerError(conexion)
                        .contains(
                                "Acceso denegado"));
    }

    @Test
    @DisplayName("Paciente puede consultar historial")
    void testPacienteVerHistorial()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/verificar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "usuario=juan"
                + "&rol=PACIENTE"
                + "&accion=VER_HISTORIAL";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                200,
                conexion.getResponseCode());

        assertEquals(
                "Permiso concedido",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Ver logs como administrador")
    void testVerLogsAdministrador()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs?rol=ADMINISTRADOR");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                200,
                conexion.getResponseCode());

        assertNotNull(
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Logs vacíos")
    void testLogsVacios()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs?rol=ADMINISTRADOR");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                200,
                conexion.getResponseCode());

        assertEquals(
                "No hay registros de auditoría.",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Ver logs sin permisos")
    void testVerLogsSinPermisos()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs?rol=PACIENTE");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                403,
                conexion.getResponseCode());

        assertEquals(
                "Acceso denegado: Se requiere rol ADMINISTRADOR",
                leerError(conexion));
    }

    @Test
    @DisplayName("Ver logs sin rol")
    void testVerLogsSinRol()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/logs");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                403,
                conexion.getResponseCode());

        assertEquals(
                "Acceso denegado: Se requiere rol ADMINISTRADOR",
                leerError(conexion));
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

        conexion.setRequestMethod("GET");

        assertEquals(
                404,
                conexion.getResponseCode());

        assertEquals(
                "No encontrado",
                leerError(conexion));
    }

    @Test
    @DisplayName("Verificar sin parámetros")
    void testVerificarSinParametros()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                        + puerto
                        + "/seguridad/verificar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        conexion.getOutputStream().write(
                "".getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                403,
                conexion.getResponseCode());
    }
}