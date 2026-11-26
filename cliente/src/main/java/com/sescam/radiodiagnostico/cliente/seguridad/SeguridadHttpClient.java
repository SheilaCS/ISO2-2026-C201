package com.sescam.radiodiagnostico.cliente.seguridad;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SeguridadHttpClient {

    private final String urlBase;
    private final HttpClient cliente = HttpClient.newHttpClient();

    public SeguridadHttpClient() {
        this("http://localhost:8087/seguridad");
    }

    public SeguridadHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    public String verificarPermiso(String usuario, String rol, String accion) throws IOException, InterruptedException {
        String datos = "usuario=" + codificar(usuario) + "&rol=" + codificar(rol) + "&accion=" + codificar(accion);
        return enviarPost("/verificar", datos);
    }

    public String obtenerLogs(String rol) throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/logs?rol=" + codificar(rol)))
                .GET()
                .build();
        HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
        return respuesta.body();
    }

    private String enviarPost(String ruta, String datos) throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(datos))
                .build();
        HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
        return respuesta.body();
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}