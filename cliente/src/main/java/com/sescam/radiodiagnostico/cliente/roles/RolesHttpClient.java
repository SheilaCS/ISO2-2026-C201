package com.sescam.radiodiagnostico.cliente.roles;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP del servidor de roles y permisos (CDU10, RF18).
 */
public class RolesHttpClient {

    /** Respuesta del servidor: código HTTP y campos ya decodificados. */
    public record Respuesta(int codigo, Map<String, String> datos) {

        public boolean correcta() {
            return codigo >= 200 && codigo < 300;
        }

        public String error() {
            return datos.getOrDefault("error", "Petición rechazada.");
        }
    }

    private final String urlBase;
    private final HttpClient http;
    private String usuario = "";
    private String clave = "";

    public RolesHttpClient() {
        this("http://localhost:8091/roles");
    }

    public RolesHttpClient(String urlBase) {
        this.urlBase = urlBase;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public void setCredenciales(String usuario, String clave) {
        this.usuario = usuario == null ? "" : usuario;
        this.clave = clave == null ? "" : clave;
    }

    public Respuesta listarUsuarios() throws IOException, InterruptedException {
        return enviar("GET", "/usuarios", null);
    }

    public Respuesta consultarRol(String usuarioGestionado)
            throws IOException, InterruptedException {
        return enviar("GET", "/usuarios?usuario=" + codificar(usuarioGestionado), null);
    }

    public Respuesta cambiarRol(String usuarioGestionado, String rol)
            throws IOException, InterruptedException {
        return enviar("PUT", "/usuarios/" + codificar(usuarioGestionado),
                "rol=" + codificar(rol));
    }

    public Respuesta consultarPermisos(String rol) throws IOException, InterruptedException {
        return enviar("GET", "/permisos?rol=" + codificar(rol), null);
    }

    public Respuesta concederPermiso(String rol, String permiso)
            throws IOException, InterruptedException {
        return enviar("POST", "/permisos/" + codificar(rol) + "/" + codificar(permiso), null);
    }

    public Respuesta retirarPermiso(String rol, String permiso)
            throws IOException, InterruptedException {
        return enviar("DELETE", "/permisos/" + codificar(rol) + "/" + codificar(permiso), null);
    }

    private Respuesta enviar(String metodo, String ruta, String cuerpo)
            throws IOException, InterruptedException {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(5))
                .method(metodo, cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));

        if (cuerpo != null) {
            peticion.header("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
        }
        if (!usuario.isEmpty()) {
            String token = Base64.getEncoder().encodeToString(
                    (usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
            peticion.header("Authorization", "Basic " + token);
        }

        HttpResponse<String> respuesta = http.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        return new Respuesta(respuesta.statusCode(), leerCampos(respuesta.body()));
    }

    /** Separa primero por & y = y después decodifica cada parte. */
    static Map<String, String> leerCampos(String cuerpo) {
        Map<String, String> datos = new LinkedHashMap<>();
        if (cuerpo == null || cuerpo.isBlank()) {
            return datos;
        }
        for (String parte : cuerpo.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length == 2) {
                datos.put(
                        URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(par[1], StandardCharsets.UTF_8));
            }
        }
        return datos;
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto == null ? "" : texto.strip(), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
