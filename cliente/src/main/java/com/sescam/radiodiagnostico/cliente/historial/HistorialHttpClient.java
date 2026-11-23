package com.sescam.radiodiagnostico.cliente.historial;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * CLIENTE HTTP DEL HISTORIAL DE PRUEBAS.
 * Iteracion 12 - RF04.
 *
 * Consulta el servidor del historial con credenciales Basic y traduce
 * sus respuestas: lista de pruebas o mensaje de error con codigo HTTP.
 */
public class HistorialHttpClient {

    // Direccion normal del servidor del historial
    public static final String URL_POR_DEFECTO =
            "http://localhost:8085/historial";

    private final String urlBase;

    // Cliente utilizado para enviar peticiones HTTP
    private final HttpClient cliente = HttpClient.newHttpClient();

    // Constructor normal de la aplicacion
    public HistorialHttpClient() {
        this(URL_POR_DEFECTO);
    }

    // Constructor para utilizar un servidor de pruebas
    public HistorialHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    // Consultar el historial de un paciente
    public RespuestaHistorial consultarHistorial(String usuario,
            char[] clave, String idPaciente)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "?paciente="
                        + URLEncoder.encode(idPaciente,
                                StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", cabeceraBasic(usuario, clave))
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        return new RespuestaHistorial(
                respuesta.statusCode(), respuesta.body());
    }

    // Convertir el cuerpo de una respuesta correcta en pruebas
    // (una por linea: id;fecha;tipo;estado;informe)
    public static List<PruebaHistorial> parsearPruebas(String cuerpo) {

        List<PruebaHistorial> pruebas = new ArrayList<>();

        if (cuerpo == null || cuerpo.isBlank()) {
            return pruebas;
        }

        for (String linea : cuerpo.split("\n")) {
            String[] campos = linea.split(";", -1);

            if (campos.length != 5) {
                throw new IllegalArgumentException(
                        "La respuesta del servidor tiene un formato incorrecto");
            }

            pruebas.add(new PruebaHistorial(
                    campos[0], campos[1], campos[2], campos[3], campos[4]));
        }

        return pruebas;
    }

    // Mensaje de error que se muestra al usuario, con el codigo HTTP
    public static String mensajeError(RespuestaHistorial respuesta) {

        String detalle = extraerError(respuesta.cuerpo());

        if (detalle.isEmpty()) {
            detalle = mensajePorDefecto(respuesta.codigo());
        }

        return "Error HTTP " + respuesta.codigo() + ": " + detalle;
    }

    // Leer el valor de "error=..." del cuerpo (vacio si no esta)
    private static String extraerError(String cuerpo) {

        if (cuerpo == null) {
            return "";
        }

        for (String parte : cuerpo.split("&")) {
            String[] par = parte.split("=", 2);

            if (par.length == 2 && URLDecoder.decode(
                    par[0], StandardCharsets.UTF_8).equals("error")) {
                return URLDecoder.decode(par[1], StandardCharsets.UTF_8);
            }
        }

        return "";
    }

    private static String mensajePorDefecto(int codigo) {
        return switch (codigo) {
            case 400 -> "Datos incorrectos.";
            case 401 -> "Credenciales incorrectas.";
            case 403 -> "Acceso no permitido.";
            case 405 -> "Metodo no permitido.";
            case 503 -> "Servicio no disponible.";
            default -> "Error inesperado del servidor.";
        };
    }

    // Cabecera Authorization: Basic usuario:clave en Base64
    private static String cabeceraBasic(String usuario, char[] clave) {

        byte[] credenciales =
                (usuario + ":" + new String(clave))
                        .getBytes(StandardCharsets.UTF_8);

        try {
            return "Basic "
                    + Base64.getEncoder().encodeToString(credenciales);
        } finally {
            Arrays.fill(credenciales, (byte) 0);
        }
    }
}
