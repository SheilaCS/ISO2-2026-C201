package com.sescam.radiodiagnostico.realizacion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProveedorRealizacionHttp implements IRealizacionProveedor {

    private static final Pattern PATRON_SOLICITUD =
            Pattern.compile("(?i)(?:idSolicitud|solicitud)\\s*[:=]?\\s*(\\d+)");

    private final String urlBaseCitas;
    private final HttpClient httpClient;

    public ProveedorRealizacionHttp() {
        this("http://127.0.0.1:" + Integer.getInteger("citas.puerto", 8080) + "/citas/consultar");
    }

    public ProveedorRealizacionHttp(String urlBaseCitas) {
        this.urlBaseCitas = urlBaseCitas;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public boolean existeCita(int idCita) {
        return consultarCita(idCita) != null;
    }

    @Override
    public boolean correspondeCitaASolicitud(int idCita, int idSolicitud) {
        if (idCita <= 0 || idSolicitud <= 0) {
            return false;
        }
        String cuerpo = consultarCita(idCita);
        if (cuerpo == null) {
            return false;
        }
        Matcher matcher = PATRON_SOLICITUD.matcher(cuerpo);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) == idSolicitud;
        }
        return false;
    }

    private String consultarCita(int idCita) {
        if (idCita <= 0) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBaseCitas + "?id=" + idCita))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            String cuerpo = response.body() == null ? "" : response.body().trim();
            String minusculas = cuerpo.toLowerCase();
            if (cuerpo.isEmpty()
                    || minusculas.contains("no existe")
                    || minusculas.contains("no encontrada")
                    || minusculas.contains("error")) {
                return null;
            }
            return cuerpo;
        } catch (IOException e) {
            throw new IllegalStateException("Servidor de citas no disponible.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta al servidor de citas interrumpida.", e);
        }
    }
}