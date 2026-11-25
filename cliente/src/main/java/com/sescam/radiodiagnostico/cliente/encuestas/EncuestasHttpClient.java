package com.sescam.radiodiagnostico.cliente.encuestas;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.io.IOException;

public class EncuestasHttpClient {
    private final String urlBase;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public EncuestasHttpClient() { this(System.getProperty("encuestas.url", "http://localhost:8090/encuestas")); }
    public EncuestasHttpClient(String urlBase) { this.urlBase = urlBase; }

    public String comunicarIncidencia(int idCita, String descripcion, String usuario, char[] clave) {
        return enviar("/incidencia", "idCita=" + idCita + "&descripcion=" + codificar(descripcion), usuario, clave);
    }
    public String responderEncuesta(int idCita, int puntuacion, String comentario, String usuario, char[] clave) {
        return enviar("/responder", "idCita=" + idCita + "&puntuacion=" + puntuacion
                + "&comentario=" + codificar(comentario), usuario, clave);
    }
    private String codificar(String texto) { return URLEncoder.encode(texto, StandardCharsets.UTF_8); }
    private String enviar(String ruta, String datos, String usuario, char[] clave) {
        String autorizacion = Base64.getEncoder().encodeToString((usuario + ":" + new String(clave)).getBytes(StandardCharsets.UTF_8));
        try {
            var peticion = HttpRequest.newBuilder(URI.create(urlBase + ruta))
                    .timeout(Duration.ofSeconds(8)).header("Authorization", "Basic " + autorizacion)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(datos)).build();
            var respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() == 200) return respuesta.body();
            return "Error: " + respuesta.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return "Consulta interrumpida";
        } catch (IOException e) { return "No se pudo conectar con el servidor de encuestas"; }
    }
}
