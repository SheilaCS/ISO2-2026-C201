package com.sescam.radiodiagnostico.miscitas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Obtiene las citas del paciente consultando al servidor de citas
 * (iteracion 3, RF13) mediante HTTP.
 *
 * Direccion normal: http://localhost:8080/citas
 * Se puede cambiar con la propiedad del sistema citas.url.
 */
public class ProveedorCitasHttp implements ICitasPacienteProveedor {

    private static final String URL_POR_DEFECTO =
            "http://localhost:8080/citas";

    private static final String ERROR_CONEXION =
            "No se pudo conectar con el servidor de citas";

    private final String urlBase;
    private final HttpClient cliente = HttpClient.newHttpClient();

    // Constructor normal
    public ProveedorCitasHttp() {
        this(System.getProperty("citas.url", URL_POR_DEFECTO));
    }

    // Constructor para indicar otra direccion (pruebas)
    public ProveedorCitasHttp(String urlBase) {
        this.urlBase = urlBase;
    }

    @Override
    public String listarCitasPaciente(int idPaciente) {

        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder()
                        .uri(URI.create(urlBase
                                + "/paciente?paciente=" + idPaciente))
                        .GET()
                        .build());

        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException(ERROR_CONEXION);
        }

        return respuesta.body();
    }

    @Override
    public ResultadoOperacion confirmarCita(int idCita, int idPaciente) {

        return enviarOperacion("/paciente/confirmar",
                "id=" + idCita + "&paciente=" + idPaciente);
    }

    @Override
    public ResultadoOperacion solicitarCambioCita(int idCita,
                                                  int idPaciente,
                                                  String motivo) {

        return enviarOperacion("/paciente/cambio",
                "id=" + idCita + "&paciente=" + idPaciente
                + "&motivo=" + URLEncoder.encode(
                        motivo, StandardCharsets.UTF_8));
    }

    @Override
    public ResultadoOperacion cancelarCita(int idCita, int idPaciente) {

        return enviarOperacion("/paciente/cancelar",
                "id=" + idCita + "&paciente=" + idPaciente);
    }

    // -----------------------------------------
    // METODOS AUXILIARES
    // -----------------------------------------

    // Enviar una operacion POST.
    // 200 = realizada, 409 = rechazada por las reglas de citas.
    // En ambos casos el cuerpo de la respuesta es el mensaje.
    private ResultadoOperacion enviarOperacion(String ruta,
                                               String datos) {

        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder()
                        .uri(URI.create(urlBase + ruta))
                        .header("Content-Type",
                                "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                datos, StandardCharsets.UTF_8))
                        .build());

        if (respuesta.statusCode() == 200) {
            return ResultadoOperacion.correcta(respuesta.body());
        }

        if (respuesta.statusCode() == 409) {
            return ResultadoOperacion.rechazada(respuesta.body());
        }

        throw new IllegalStateException(ERROR_CONEXION);
    }

    // Enviar una peticion y devolver la respuesta
    private HttpResponse<String> enviar(HttpRequest peticion) {

        try {
            return cliente.send(peticion,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException(ERROR_CONEXION, e);
        }
    }
}
