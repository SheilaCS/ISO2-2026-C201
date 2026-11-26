package com.sescam.radiodiagnostico.parametros;

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

public class ParametroHttpHandlerTest {

    private HttpServer servidor;
    private int puerto;

    @BeforeEach
    public void preparar() throws IOException {

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext(
                "/parametros",
                new ParametroHttpHandler());

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

    @Test
    @DisplayName("Guardar parametro correctamente")
    public void testGuardarParametro()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/parametros/guardar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "clave=MAX_PACIENTES"
                + "&valor=100";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        assertEquals(
                200,
                conexion.getResponseCode());

        String respuesta =
                leerRespuesta(conexion);

        assertEquals(
                "Parámetro 'MAX_PACIENTES' guardado correctamente",
                respuesta);
    }

    @Test
    @DisplayName("Consultar parametro")
    public void testConsultarParametro()
            throws Exception {

        // Guardar primero un valor real

        URL guardar = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/parametros/guardar");

        HttpURLConnection conexionGuardar =
                (HttpURLConnection)
                        guardar.openConnection();

        conexionGuardar.setRequestMethod("POST");
        conexionGuardar.setDoOutput(true);

        conexionGuardar.getOutputStream().write(
                "clave=MAX_PACIENTES&valor=100"
                        .getBytes(StandardCharsets.UTF_8));

        leerRespuesta(conexionGuardar);

        // Consultarlo después

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/parametros/consultar?clave=MAX_PACIENTES");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                200,
                conexion.getResponseCode());

        String respuesta =
                leerRespuesta(conexion);

        assertEquals(
                "100",
                respuesta);
    }

    @Test
    @DisplayName("Consultar parametro inexistente")
    public void testConsultarParametroVacio()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/parametros/consultar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                200,
                conexion.getResponseCode());

        String respuesta =
                leerRespuesta(conexion);

        assertEquals(
                "No encontrado",
                respuesta);
    }

    @Test
    @DisplayName("Ruta inexistente")
    public void testRutaNoEncontrada()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/parametros/inexistente");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("GET");

        assertEquals(
                404,
                conexion.getResponseCode());

        String respuesta =
                new String(
                        conexion.getErrorStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8);

        assertEquals(
                "No encontrado",
                respuesta);
    }

    @Test
    @DisplayName("Guardar parametro con espacios")
    public void testGuardarParametroConEspacios()
            throws Exception {

        URL url = new URL(
                "http://127.0.0.1:"
                + puerto
                + "/parametros/guardar");

        HttpURLConnection conexion =
                (HttpURLConnection)
                        url.openConnection();

        conexion.setRequestMethod("POST");
        conexion.setDoOutput(true);

        String datos =
                "clave=MAX+PACIENTES"
                + "&valor=VALOR+PRUEBA";

        conexion.getOutputStream().write(
                datos.getBytes(
                        StandardCharsets.UTF_8));

        String respuesta =
                leerRespuesta(conexion);

        assertEquals(
                "Parámetro 'MAX PACIENTES' guardado correctamente",
                respuesta);
    }
}