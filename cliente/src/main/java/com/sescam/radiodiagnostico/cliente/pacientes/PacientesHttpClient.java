package com.sescam.radiodiagnostico.cliente.pacientes;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * CLIENTE HTTP - Iteracion 4 (RF12).
 * Comunica la interfaz grafica con el servidor de gestion de pacientes.
 */
public class PacientesHttpClient {

    private final String baseUrl;
    private final HttpClient cliente;
    private String usuario;
    private String contrasena;

    /**
     * Constructor utilizado por la aplicacion.
     */
    public PacientesHttpClient() {
        this("http://localhost:8087/pacientes", "", "");
    }

    /**
     * Constructor con credenciales HTTP Basic para entorno local.
     */
    public PacientesHttpClient(String usuario, String contrasena) {
        this("http://localhost:8087/pacientes", usuario, contrasena);
    }

    /**
     * Constructor para pruebas o configuracion personalizada en el mismo paquete.
     */
    PacientesHttpClient(String baseUrl, String usuario, String contrasena) {
        this.baseUrl = baseUrl;
        this.cliente = HttpClient.newHttpClient();
        this.usuario = usuario != null ? usuario : "";
        this.contrasena = contrasena != null ? contrasena : "";
    }

    /**
     * Actualiza las credenciales utilizadas en las peticiones HTTP Basic.
     */
    public void setCredenciales(String usuario, String contrasena) {
        this.usuario = usuario != null ? usuario : "";
        this.contrasena = contrasena != null ? contrasena : "";
    }

    /**
     * Alta de paciente: POST /pacientes
     */
    public String altaPaciente(String cip, String nombre, String apellidos, String fechaNacimiento)
            throws IOException, InterruptedException {
        String cipNormalizado = ValidacionesPacientes.normalizarCip(cip);
        String datos = "cip=" + codificar(cipNormalizado)
                + "&nombre=" + codificar(nombre.trim())
                + "&apellidos=" + codificar(apellidos.trim())
                + "&fechaNacimiento=" + codificar(fechaNacimiento.trim());

        return enviarConCuerpo("POST", "", datos);
    }

    /**
     * Consulta de paciente por CIP: GET /pacientes?cip=...
     */
    public String consultarPorCip(String cip) throws IOException, InterruptedException {
        String cipNormalizado = ValidacionesPacientes.normalizarCip(cip);
        String uri = baseUrl + "?cip=" + codificar(cipNormalizado);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET();

        agregarCabeceraAuth(builder);

        HttpResponse<String> respuesta = cliente.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        return formatearRespuesta(respuesta);
    }

    /**
     * Modificacion de paciente: PUT /pacientes/{id}
     * Conserva identificador y CIP; actualiza nombre, apellidos y fecha de nacimiento.
     */
    public String modificarPaciente(String id, String nombre, String apellidos, String fechaNacimiento)
            throws IOException, InterruptedException {
        String idLimpio = id != null ? id.trim() : "";
        String datos = "nombre=" + codificar(nombre.trim())
                + "&apellidos=" + codificar(apellidos.trim())
                + "&fechaNacimiento=" + codificar(fechaNacimiento.trim());

        return enviarConCuerpo("PUT", "/" + codificar(idLimpio), datos);
    }

    /**
     * Baja logica de paciente: DELETE /pacientes/{id}
     */
    public String darBajaPaciente(String id) throws IOException, InterruptedException {
        String idLimpio = id != null ? id.trim() : "";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + codificar(idLimpio)))
                .DELETE();

        agregarCabeceraAuth(builder);

        HttpResponse<String> respuesta = cliente.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        return formatearRespuesta(respuesta);
    }

    private String enviarConCuerpo(String metodo, String sufijoRuta, String datos)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + sufijoRuta))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        agregarCabeceraAuth(builder);

        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(datos, StandardCharsets.UTF_8);
        if ("PUT".equals(metodo)) {
            builder.PUT(publisher);
        } else {
            builder.POST(publisher);
        }

        HttpResponse<String> respuesta = cliente.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        return formatearRespuesta(respuesta);
    }

    private void agregarCabeceraAuth(HttpRequest.Builder builder) {
        if (!usuario.isEmpty() || !contrasena.isEmpty()) {
            String token = Base64.getEncoder()
                    .encodeToString((usuario + ":" + contrasena).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
    }

    private String formatearRespuesta(HttpResponse<String> respuesta) {
        int codigo = respuesta.statusCode();
        String cuerpo = respuesta.body() != null ? respuesta.body().trim() : "";

        if (codigo == 401) {
            return "Error 401: Credenciales invalidas." + (cuerpo.isEmpty() ? "" : " " + cuerpo);
        }
        if (codigo == 403) {
            return "Error 403: Acceso denegado. Se requiere rol ADMINISTRATIVO."
                    + (cuerpo.isEmpty() ? "" : " " + cuerpo);
        }
        if (codigo >= 400) {
            return "Error " + codigo + ": " + (cuerpo.isEmpty() ? "Peticion rechazada." : cuerpo);
        }
        return cuerpo.isEmpty() ? "Operacion realizada correctamente." : cuerpo;
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }
}