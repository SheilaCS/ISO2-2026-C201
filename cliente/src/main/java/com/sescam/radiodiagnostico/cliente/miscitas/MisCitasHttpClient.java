
package com.sescam.radiodiagnostico.cliente.miscitas;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class MisCitasHttpClient {

    // Direccion del servidor de mis citas
    private final String urlBase;

    // Cliente utilizado para enviar peticiones HTTP
    private final HttpClient cliente = HttpClient.newHttpClient();

    // Constructor normal de la aplicacion
    public MisCitasHttpClient() {
        this("http://localhost:8082/miscitas");
    }

    // Constructor para utilizar un servidor de pruebas
    public MisCitasHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    // Consultar las citas de un paciente
    public String consultarMisCitas(int idPaciente)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase
                        + "/consultar?idPaciente=" + idPaciente))
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                peticion, HttpResponse.BodyHandlers.ofString());

        return respuesta.body();
    }

    // Confirmar una cita
    public String confirmarCita(int idCita, int idPaciente)
            throws IOException, InterruptedException {

        String datos = "idCita=" + idCita
                + "&idPaciente=" + idPaciente;

        return enviarPost("/confirmar", datos);
    }

    // Solicitar el cambio de una cita
    public String solicitarCambioCita(int idCita, int idPaciente,
                                      String motivo)
            throws IOException, InterruptedException {

        String datos = "idCita=" + idCita
                + "&idPaciente=" + idPaciente
                + "&motivo=" + codificar(motivo);

        return enviarPost("/cambio", datos);
    }

    // Cancelar una cita
    public String cancelarCita(int idCita, int idPaciente)
            throws IOException, InterruptedException {

        String datos = "idCita=" + idCita
                + "&idPaciente=" + idPaciente;

        return enviarPost("/cancelar", datos);
    }

    // Enviar peticiones POST al servidor
    private String enviarPost(String ruta, String datos)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta))
                .header("Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(datos))
                .build();

        HttpResponse<String> respuesta = cliente.send(
                peticion, HttpResponse.BodyHandlers.ofString());

        return respuesta.body();
    }

    // Codificar los parametros para enviarlos por HTTP
    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}
