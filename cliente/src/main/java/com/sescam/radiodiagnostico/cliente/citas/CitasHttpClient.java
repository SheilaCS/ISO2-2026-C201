
package com.sescam.radiodiagnostico.cliente.citas;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * CLIENTE HTTP - Iteracion 3 (RF13).
 * Comunica la interfaz grafica con el servidor de citas.
 */
public class CitasHttpClient {

    // Direccion configurable del servidor
    private final String url;

    private final HttpClient cliente = HttpClient.newHttpClient();

    /**
     * Constructor utilizado por la aplicacion.
     */
    public CitasHttpClient() {
        this("http://localhost:8080/citas");
    }

    /**
     * Constructor para pruebas, en el mismo paquete.
     * Permite utilizar un servidor HTTP simulado.
     */
    CitasHttpClient(String url) {
        this.url = url;
    }

    /**
     * Consultar una cita.
     */
    public String consultarCita(int idCita)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
            .uri(URI.create(url + "/consultar?id=" + idCita))
            .GET()
            .build();

        HttpResponse<String> respuesta = cliente.send(
            peticion,
            HttpResponse.BodyHandlers.ofString()
        );

        return respuesta.body();
    }

    /**
     * Asignar una cita.
     */
    public String asignarCita(int solicitud, int paciente,
                              String fecha, String hora)
            throws IOException, InterruptedException {

        String datos = "solicitud=" + solicitud
            + "&paciente=" + paciente
            + "&fecha=" + codificar(fecha)
            + "&hora=" + codificar(hora);

        return enviarPost("/asignar", datos);
    }

    /**
     * Reprogramar una cita.
     */
    public String reprogramarCita(int idCita, String fecha,
                                  String hora)
            throws IOException, InterruptedException {

        String datos = "id=" + idCita
            + "&fecha=" + codificar(fecha)
            + "&hora=" + codificar(hora);

        return enviarPost("/reprogramar", datos);
    }

    /**
     * Cancelar una cita.
     */
    public String cancelarCita(int idCita)
            throws IOException, InterruptedException {

        return enviarPost("/cancelar", "id=" + idCita);
    }

    /**
     * Enviar una peticion POST al servidor.
     */
    private String enviarPost(String ruta, String datos)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
            .uri(URI.create(url + ruta))
            .header(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            .POST(HttpRequest.BodyPublishers.ofString(datos))
            .build();

        HttpResponse<String> respuesta = cliente.send(
            peticion,
            HttpResponse.BodyHandlers.ofString()
        );

        return respuesta.body();
    }

    /**
     * Codificar texto para enviarlo mediante HTTP.
     */
    private String codificar(String texto) {
        return URLEncoder.encode(
            texto, StandardCharsets.UTF_8
        );
    }
}
