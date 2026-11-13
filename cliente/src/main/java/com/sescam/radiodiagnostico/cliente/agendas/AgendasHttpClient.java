
package com.sescam.radiodiagnostico.cliente.agendas;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class AgendasHttpClient {

    // Direccion del servidor de agendas
    private static final String URL_BASE = "http://localhost:8081/agendas";

    private HttpClient cliente = HttpClient.newHttpClient();

    // Crear un horario para un medico
    public String crearHorario(int idMedico, String fecha,
                               String inicio, String fin)
            throws IOException, InterruptedException {

        String datos = "idMedico=" + idMedico
                + "&fecha=" + codificar(fecha)
                + "&inicio=" + codificar(inicio)
                + "&fin=" + codificar(fin);

        return enviarPost("/crear", datos);
    }

    // Consultar los horarios de un medico
    public String consultarAgenda(int idMedico)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/consultar?idMedico=" + idMedico))
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(
                peticion, HttpResponse.BodyHandlers.ofString());

        return respuesta.body();
    }

    // Modificar un horario existente
    public String modificarHorario(int idHorario, String fecha,
                                   String inicio, String fin)
            throws IOException, InterruptedException {

        String datos = "idHorario=" + idHorario
                + "&fecha=" + codificar(fecha)
                + "&inicio=" + codificar(inicio)
                + "&fin=" + codificar(fin);

        return enviarPost("/modificar", datos);
    }

    // Eliminar un horario existente
    public String eliminarHorario(int idHorario)
            throws IOException, InterruptedException {

        return enviarPost("/eliminar", "idHorario=" + idHorario);
    }

    // Enviar peticiones POST al servidor
    private String enviarPost(String ruta, String datos)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + ruta))
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
