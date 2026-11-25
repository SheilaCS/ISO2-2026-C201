package com.sescam.radiodiagnostico.listasespera;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ASIGNACION REAL DE CITAS CONSULTANDO AL MODULO DE CITAS.
 * Iteracion 15 - RF15.
 *
 * Envia POST /citas/asignar al servidor de citas (iteracion 3,
 * RF13), normalmente en http://127.0.0.1:8080. Se puede cambiar el
 * puerto con la propiedad del sistema citas.puerto.
 *
 * - 200 y "Cita asignada...": la cita se ha asignado (true).
 * - 200 con otro texto, 400 o 409: el modulo de citas la rechaza
 *   (false).
 * - Cualquier otra respuesta, o si no responde: IllegalStateException.
 */
public class AsignadorCitasHttp implements IAsignadorCitas {

    private static final String ERROR_CITAS =
            "Servidor de citas no disponible";

    private final String urlAsignar;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    // Constructor normal
    public AsignadorCitasHttp() {
        this("http://127.0.0.1:"
                + Integer.getInteger("citas.puerto", 8080)
                + "/citas/asignar");
    }

    // Constructor para indicar otra direccion (pruebas)
    public AsignadorCitasHttp(String urlAsignar) {
        this.urlAsignar = urlAsignar;
    }

    @Override
    public boolean asignarCita(int idSolicitud, int idPaciente,
                               LocalDate fecha, LocalTime hora) {

        String datos = "solicitud=" + idSolicitud
                + "&paciente=" + idPaciente
                + "&fecha=" + codificar(fecha.toString())
                + "&hora=" + codificar(hora.toString());

        HttpResponse<String> respuesta = enviar(datos);
        int codigo = respuesta.statusCode();

        if (codigo == 200) {
            return respuesta.body() != null
                    && respuesta.body().trim().startsWith("Cita asignada");
        }

        if (codigo == 400 || codigo == 409) {
            return false;
        }

        throw new IllegalStateException(ERROR_CITAS);
    }

    private HttpResponse<String> enviar(String datos) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(urlAsignar))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type",
                            "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            datos, StandardCharsets.UTF_8))
                    .build();

            return cliente.send(peticion,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException(ERROR_CITAS, e);
        }
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}
