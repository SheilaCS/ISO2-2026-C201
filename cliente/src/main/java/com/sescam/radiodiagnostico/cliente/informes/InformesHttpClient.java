package com.sescam.radiodiagnostico.cliente.informes;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class InformesHttpClient {

    private final String urlBase;
    private final HttpClient cliente = HttpClient.newHttpClient();

    public InformesHttpClient() {
        this("http://localhost:8085/informes"); // Puerto 8085 para el CDU8
    }

    public InformesHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    public String generarInforme(int idEquipo, String fecha, String descripcion, String rolUsuario) 
            throws IOException, InterruptedException {
        
        String datos = "idEquipo=" + idEquipo
                + "&fecha=" + codificar(fecha)
                + "&descripcion=" + codificar(descripcion)
                + "&rol=" + codificar(rolUsuario);

        return enviarPost("/generar", datos);
    }

    public String consultarInforme(int idInforme, String rolUsuario) 
            throws IOException, InterruptedException {
        
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/consultar?id=" + idInforme + "&rol=" + codificar(rolUsuario)))
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