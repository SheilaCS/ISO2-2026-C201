package com.sescam.radiodiagnostico.cliente.solicitudes;

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
 * Cliente HTTP del servidor de solicitudes (CDU2, RF06).
 */
public class SolicitudHttpClient {

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

    public SolicitudHttpClient() {
        this("http://localhost:8083/solicitudes");
    }

    public SolicitudHttpClient(String urlBase) {
        this.urlBase = urlBase;
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public void setCredenciales(String usuario, String clave) {
        this.usuario = usuario == null ? "" : usuario.strip();
        this.clave = clave == null ? "" : clave;
    }

    public Respuesta crearSolicitud(int idPaciente, String tipoPrueba,
            String indicacionClinica, String justificacion, String prioridad)
            throws IOException, InterruptedException {
        String datos = "idPaciente=" + idPaciente
                + "&tipoPrueba=" + codificar(tipoPrueba)
                + "&indicacionClinica=" + codificar(indicacionClinica)
                + "&justificacion=" + codificar(justificacion)
                + "&prioridad=" + codificar(prioridad);
        return enviar(HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/crear"))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(datos, StandardCharsets.UTF_8)));
    }

    public Respuesta consultarSolicitud(int idSolicitud)
            throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/consultar?id=" + idSolicitud))
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
        return URLEncoder.encode(texto == null ? "" : texto.strip(), StandardCharsets.UTF_8);
    }
}
