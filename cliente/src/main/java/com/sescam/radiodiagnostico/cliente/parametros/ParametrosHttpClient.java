package com.sescam.radiodiagnostico.cliente.parametros;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ParametrosHttpClient {

    private final String urlBase;
    private final HttpClient cliente = HttpClient.newHttpClient();

    // Constructor normal
    public ParametrosHttpClient() {
        this("http://localhost:8084/parametros");
    }

    // Constructor para pruebas
    public ParametrosHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    public String guardarParametro(String clave, String valor) 
            throws IOException, InterruptedException {
        
        String datos = "clave=" + codificar(clave) + "&valor=" + codificar(valor);

        return enviarPost("/guardar", datos);
    }

    public String consultarParametro(String clave) 
            throws IOException, InterruptedException {
        
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/consultar?clave=" + codificar(clave)))
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