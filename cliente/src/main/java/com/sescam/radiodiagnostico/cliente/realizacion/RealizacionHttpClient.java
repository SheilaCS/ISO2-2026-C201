package com.sescam.radiodiagnostico.cliente.realizacion;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class RealizacionHttpClient {

    private final String urlBase;
    private final HttpClient httpClient;
    private String usuario = "";
    private String password = "";

    public RealizacionHttpClient() {
        this("http://127.0.0.1:" + Integer.getInteger("realizacion.puerto", 8086) + "/realizaciones");
    }

    public RealizacionHttpClient(String urlBase) {
        this.urlBase = urlBase;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void setCredenciales(String usuario, String password) {
        this.usuario = usuario == null ? "" : usuario;
        this.password = password == null ? "" : password;
    }

    public String registrarRealizacion(
            int idCita,
            int idSolicitud,
            String fechaRealizacion,
            String observaciones,
            String incidencia) throws IOException, InterruptedException {

        String cuerpo = "idCita=" + idCita
                + "&idSolicitud=" + idSolicitud
                + "&fechaRealizacion=" + codificar(fechaRealizacion)
                + "&observaciones=" + codificar(observaciones)
                + "&incidencia=" + codificar(incidencia);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        aplicarAuth(builder);

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return procesarRespuesta(resp);
    }

    public String consultarPorCita(int idCita) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "?idCita=" + idCita))
                .GET();
        aplicarAuth(builder);

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return procesarRespuesta(resp);
    }

    public String listarRealizaciones() throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlBase))
                .GET();
        aplicarAuth(builder);

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return procesarRespuesta(resp);
    }

    private void aplicarAuth(HttpRequest.Builder builder) {
        if (!usuario.isBlank()) {
            String token = Base64.getEncoder().encodeToString(
                    (usuario + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
    }

    private String procesarRespuesta(HttpResponse<String> resp) {
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return resp.body();
        }
        return "Error " + resp.statusCode() + ": " + resp.body();
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}