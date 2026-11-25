package com.sescam.radiodiagnostico.encuestas;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class ProveedorEncuestasHttp implements IEncuestasProveedor {
    private final String urlCitas, urlRealizacion, autorizacion;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public ProveedorEncuestasHttp() {
        this(System.getProperty("miscitas.url", "http://localhost:8082/miscitas"),
             System.getProperty("realizacion.url", "http://localhost:8086/realizaciones"),
             System.getProperty("encuestas.servicio.usuario", "medico.demo"),
             System.getProperty("encuestas.servicio.clave", "demo"));
    }

    public ProveedorEncuestasHttp(String urlCitas, String urlRealizacion,
                                  String usuario, String clave) {
        this.urlCitas = urlCitas;
        this.urlRealizacion = urlRealizacion;
        this.autorizacion = "Basic " + Base64.getEncoder().encodeToString(
                (usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean citaPertenecePaciente(int idCita, int idPaciente) {
        var respuesta = enviar(HttpRequest.newBuilder(URI.create(urlCitas
                + "/consultar?idPaciente=" + idPaciente)).GET());
        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException("Mis citas no disponible");
        }
        // Formato real del CDU9: Cita 12 | Fecha: ...
        for (String linea : respuesta.body().split("\\R")) {
            if (linea.startsWith("Cita " + idCita + " |")) return true;
        }
        return false;
    }

    @Override
    public boolean pruebaRealizada(int idCita) {
        var respuesta = enviar(HttpRequest.newBuilder(URI.create(urlRealizacion
                + "?idCita=" + idCita)).header("Authorization", autorizacion).GET());
        if (respuesta.statusCode() == 404) return false;
        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException("Realizaciones no disponible");
        }
        return true;
    }

    private HttpResponse<String> enviar(HttpRequest.Builder peticion) {
        try {
            return cliente.send(peticion.timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta interrumpida", e);
        } catch (IOException e) {
            throw new IllegalStateException("Servidor dependiente no disponible", e);
        }
    }
}
