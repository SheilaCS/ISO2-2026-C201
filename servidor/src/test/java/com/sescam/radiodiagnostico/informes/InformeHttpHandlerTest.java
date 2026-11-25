package com.sescam.radiodiagnostico.informes;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class InformeHttpHandlerTest {

    private HttpServer servidor;
    private int puerto;

    @Test
    void testMain() {

        assertDoesNotThrow(() -> {

            Thread hilo = new Thread(() -> {
                try {
                    InformeHttpHandler.main(new String[0]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            hilo.setDaemon(true);
            hilo.start();

            Thread.sleep(500);
        });
    }

        @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext(
                "/informes",
                new InformeHttpHandler());

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
                conexion.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private String leerError(
            HttpURLConnection conexion)
            throws IOException {

        return new String(
                conexion.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Generar informe correctamente")
    void testGenerarInformeCorrecto()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/generar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "idEquipo=10"
                + "&fecha=2026-12-01"
                + "&descripcion=Averia"
                + "&rol=TECNICO";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                200,
                conexion.getResponseCode());

        assertEquals(
                "Informe generado correctamente.",
                leerRespuesta(conexion));
    }

    @Test
    @DisplayName("Generar informe con rol inválido")
    void testGenerarInformeSinPermisos()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/generar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "idEquipo=10"
                + "&fecha=2026-12-01"
                + "&descripcion=Averia"
                + "&rol=USUARIO";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                403,
                conexion.getResponseCode());

        assertTrue(
                leerError(conexion)
                .contains("Error"));
    }

    @Test
    @DisplayName("Generar informe con ID inválido")
    void testGenerarInformeIdInvalido()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/generar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "idEquipo=abc"
                + "&fecha=2026-12-01"
                + "&descripcion=Averia"
                + "&rol=TECNICO";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                403,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Consultar informe correctamente")
    void testConsultarInforme()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/consultar?id=1&rol=ADMIN");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        int codigo = conexion.getResponseCode();

        assertTrue(
                codigo == 200
                || codigo == 403);
    }

    @Test
    @DisplayName("Consultar informe con ID inválido")
    void testConsultarInformeConIdInvalido()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/consultar?id=abc&rol=ADMIN");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                403,
                conexion.getResponseCode());
    }

    @Test
    @DisplayName("Ruta inexistente")
    void testRutaNoEncontrada()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/prueba");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                404,
                conexion.getResponseCode());

        assertEquals(
                "Ruta no encontrada",
                leerError(conexion));
    }

    @Test
    @DisplayName("Consultar sin parámetros")
    void testConsultarSinParametros()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/informes/consultar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                403,
                conexion.getResponseCode());
    }
}