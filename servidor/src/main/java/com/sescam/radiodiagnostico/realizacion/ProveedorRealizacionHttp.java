package com.sescam.radiodiagnostico.realizacion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ProveedorRealizacionHttp implements IRealizacionProveedor {

    private final String urlBaseCitas;
    private final HttpClient httpClient;

    public ProveedorRealizacionHttp() {
        this("http://127.0.0.1:" + Integer.getInteger("citas.puerto", 8083) + "/citas");
    }

    public ProveedorRealizacionHttp(String urlBaseCitas) {
        this.urlBaseCitas = urlBaseCitas;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public boolean existeCita(int idCita) {
        if (idCita <= 0) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBaseCitas + "?idCita=" + idCita))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException e) {
            throw new IllegalStateException("Servidor de citas no disponible.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta al servidor de citas interrumpida.", e);
        }
    }
}