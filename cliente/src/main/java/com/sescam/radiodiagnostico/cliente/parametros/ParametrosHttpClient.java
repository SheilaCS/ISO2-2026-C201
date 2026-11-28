package com.sescam.radiodiagnostico.cliente.parametros;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Cliente HTTP del servidor de parámetros (CDU5, RF17).
 */
public class ParametrosHttpClient {

    /** Respuesta del servidor: código HTTP y texto. */
    public record Respuesta(int codigo, String texto) {
        public boolean correcta() {
            return codigo >= 200 && codigo < 300;
        }
    }

    private final String urlBase;
    private final HttpClient cliente;
    private String usuario = "";
    private String clave = "";

    public ParametrosHttpClient() {
        this("http://localhost:8084/parametros");
    }

    public ParametrosHttpClient(String urlBase) {
        this.urlBase = urlBase;
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public void setCredenciales(String usuario, String clave) {
        this.usuario = usuario == null ? "" : usuario.strip();
        this.clave = clave == null ? "" : clave;
    }

    public Respuesta guardarParametro(String categoria, String claveParametro, String valor)
            throws IOException, InterruptedException {
        String datos = "categoria=" + codificar(categoria)
                + "&clave=" + codificar(claveParametro)
                + "&valor=" + codificar(valor);
        return enviar(HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/guardar"))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(datos, StandardCharsets.UTF_8)));
    }

    public Respuesta consultarParametro(String categoria, String claveParametro)
            throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/consultar?categoria=" + codificar(categoria)
                        + "&clave=" + codificar(claveParametro)))
                .GET());
    }

    public Respuesta listarParametros(String categoria)
            throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/listar?categoria=" + codificar(categoria)))
                .GET());
    }

    private Respuesta enviar(HttpRequest.Builder peticion)
            throws IOException, InterruptedException {
        peticion.timeout(Duration.ofSeconds(5));
        if (!usuario.isEmpty()) {
            String token = Base64.getEncoder().encodeToString(
                    (usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
            peticion.header("Authorization", "Basic " + token);
        }
        HttpResponse<String> respuesta = cliente.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Respuesta(respuesta.statusCode(), respuesta.body());
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto == null ? "" : texto.strip(), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
