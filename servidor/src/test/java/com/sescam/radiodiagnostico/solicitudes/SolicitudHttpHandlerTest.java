package com.sescam.radiodiagnostico.solicitudes;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SolicitudHttpHandlerTest {

    private HttpServer servidor;
    private int puerto;

    @BeforeEach
    void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext(
                "/solicitudes",
                new SolicitudHttpHandler());

        servidor.start();

        puerto = servidor.getAddress().getPort();
    }

    @AfterEach
    void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String leerRespuesta(HttpURLConnection conexion)
            throws IOException {

        return new String(
                conexion.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Crear solicitud correcta")
    void testCrearSolicitudCorrecta() throws Exception {

        URL url = new URL(
                "http://127.0.0.1:" + puerto +
                "/solicitudes/crear");

        HttpURLConnection conexion =
                (HttpURLConnection) url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "idPaciente=1" +
                "&tipoPrueba=TAC" +
                "&prioridad=URGENTE";

        conexion.getOutputStream().write(
                datos.getBytes(StandardCharsets.UTF_8));

        assertEquals(200, conexion.getResponseCode());

        assertEquals(
                "Solicitud creada correctamente",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Crear solicitud con prioridad inválida")
    void testCrearSolicitudInvalida() throws Exception {

        URL url = new URL(
                "http://127.0.0.1:" + puerto +
                "/solicitudes/crear");

        HttpURLConnection conexion =
                (HttpURLConnection) url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "idPaciente=1" +
                "&tipoPrueba=TAC" +
                "&prioridad=MALA";

        conexion.getOutputStream().write(
                datos.getBytes(StandardCharsets.UTF_8));

        assertEquals(200, conexion.getResponseCode());

        assertEquals(
                "Error en los datos de la solicitud",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Consultar solicitud")
    void testConsultarSolicitud() throws Exception {

        URL url = new URL(
                "http://127.0.0.1:" + puerto +
                "/solicitudes/consultar");

        HttpURLConnection conexion =
                (HttpURLConnection) url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(200, conexion.getResponseCode());

        assertEquals(
                "Consulta de solicitud (pendiente)",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Ruta inexistente")
    void testRutaNoEncontrada() throws Exception {

        URL url = new URL(
                "http://127.0.0.1:" + puerto +
                "/solicitudes/rutaInexistente");

        HttpURLConnection conexion =
                (HttpURLConnection) url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(404, conexion.getResponseCode());

        String respuesta = new String(
                conexion.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertEquals(
                "No encontrado",
                respuesta);
    }

    @Test
    @DisplayName("Crear solicitud con parámetros vacíos")
    void testParametrosVacios() throws Exception {

        URL url = new URL(
                "http://127.0.0.1:" + puerto +
                "/solicitudes/crear");

        HttpURLConnection conexion =
                (HttpURLConnection) url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        conexion.getOutputStream().write(
                "".getBytes(StandardCharsets.UTF_8));

        assertEquals(200,
                conexion.getResponseCode());

        assertEquals(
                "Error en los datos de la solicitud",
                leerRespuesta(conexion));
    }
}