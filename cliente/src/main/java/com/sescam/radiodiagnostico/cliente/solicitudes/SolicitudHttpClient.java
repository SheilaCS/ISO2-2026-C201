package com.sescam.radiodiagnostico.cliente.solicitudes;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SolicitudHttpClient {

    private final String urlBase;
    private final HttpClient cliente = HttpClient.newHttpClient();

    // Constructor normal
    public SolicitudHttpClient() {
        this("http://localhost:8083/solicitudes"); // Puerto que use el servidor de solicitudes
    }

    // Constructor para pruebas
    public SolicitudHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    // Método para crear una solicitud
    public String crearSolicitud(int idPaciente, String tipoPrueba, String prioridad) 
            throws IOException, InterruptedException {
        
        String datos = "idPaciente=" + idPaciente
                + "&tipoPrueba=" + codificar(tipoPrueba)
                + "&prioridad=" + codificar(prioridad); 

        return enviarPost("/crear", datos);
    }

    // Método para consultar una solicitud
    public String consultarSolicitud(int idSolicitud) 
            throws IOException, InterruptedException {
        
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/consultar?id=" + idSolicitud))
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