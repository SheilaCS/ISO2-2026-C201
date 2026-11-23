package com.sescam.radiodiagnostico.cliente.listaespera;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * CLIENTE HTTP DE LISTAS DE ESPERA Y HUECOS.
 * Iteracion 15 - RF15.
 *
 * Usa el servidor de listas de espera con credenciales Basic y traduce
 * sus respuestas: listas, mensajes de confirmacion o mensajes de error
 * con el codigo HTTP.
 */
public class ListaEsperaHttpClient {

    // Direccion normal del servidor de listas de espera
    public static final String URL_POR_DEFECTO =
            "http://localhost:8086/listaespera";

    private final String urlBase;

    // Cliente utilizado para enviar peticiones HTTP
    private final HttpClient cliente = HttpClient.newHttpClient();

    // Constructor normal de la aplicacion
    public ListaEsperaHttpClient() {
        this(URL_POR_DEFECTO);
    }

    // Constructor para utilizar un servidor de pruebas
    public ListaEsperaHttpClient(String urlBase) {
        this.urlBase = urlBase;
    }

    // ------------------------------------
    // PETICIONES
    // ------------------------------------

    public RespuestaListaEspera consultarLista(String usuario, char[] clave)
            throws IOException, InterruptedException {
        return enviar("GET", "", null, usuario, clave);
    }

    // La fecha es opcional (null o vacia para todos los huecos)
    public RespuestaListaEspera consultarHuecos(String usuario, char[] clave,
            String fecha) throws IOException, InterruptedException {

        String consulta = (fecha == null || fecha.isBlank())
                ? ""
                : "?fecha=" + codificar(fecha.trim());

        return enviar("GET", "/huecos" + consulta, null, usuario, clave);
    }

    public RespuestaListaEspera consultarPropuesta(String usuario,
            char[] clave) throws IOException, InterruptedException {
        return enviar("GET", "/propuesta", null, usuario, clave);
    }

    public RespuestaListaEspera cambiarPrioridad(String usuario, char[] clave,
            String idSolicitud, String prioridad)
            throws IOException, InterruptedException {

        return enviar("POST", "/prioridad",
                "solicitud=" + codificar(idSolicitud)
                        + "&prioridad=" + codificar(prioridad),
                usuario, clave);
    }

    public RespuestaListaEspera retirar(String usuario, char[] clave,
            String idSolicitud) throws IOException, InterruptedException {

        return enviar("POST", "/retirar",
                "solicitud=" + codificar(idSolicitud), usuario, clave);
    }

    public RespuestaListaEspera asignar(String usuario, char[] clave,
            String idSolicitud, String idMedico, String fecha, String hora)
            throws IOException, InterruptedException {

        return enviar("POST", "/asignar",
                "solicitud=" + codificar(idSolicitud)
                        + "&medico=" + codificar(idMedico)
                        + "&fecha=" + codificar(fecha)
                        + "&hora=" + codificar(hora),
                usuario, clave);
    }

    private RespuestaListaEspera enviar(String metodo, String ruta,
            String cuerpo, String usuario, char[] clave)
            throws IOException, InterruptedException {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", cabeceraBasic(usuario, clave));

        if (cuerpo == null) {
            peticion.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            peticion.method(metodo, HttpRequest.BodyPublishers.ofString(
                    cuerpo, StandardCharsets.UTF_8));
        }

        HttpResponse<String> respuesta = cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        return new RespuestaListaEspera(
                respuesta.statusCode(), respuesta.body());
    }

    // ------------------------------------
    // RESPUESTAS
    // ------------------------------------

    // Solicitudes (una por linea: id;paciente;tipo;prioridad;fecha)
    public static List<SolicitudLista> parsearSolicitudes(String cuerpo) {

        List<SolicitudLista> solicitudes = new ArrayList<>();

        for (String[] campos : separarLineas(cuerpo, 5)) {
            solicitudes.add(solicitud(campos, 0));
        }

        return solicitudes;
    }

    // Huecos (uno por linea: medico;fecha;inicio;fin)
    public static List<HuecoLista> parsearHuecos(String cuerpo) {

        List<HuecoLista> huecos = new ArrayList<>();

        for (String[] campos : separarLineas(cuerpo, 4)) {
            huecos.add(hueco(campos, 0));
        }

        return huecos;
    }

    // Propuesta (una linea con los 5 campos de la solicitud y los 4 del
    // hueco). Vacia si el servidor no tiene propuesta.
    public static Optional<PropuestaLista> parsearPropuesta(String cuerpo) {

        List<String[]> lineas = separarLineas(cuerpo, 9);

        if (lineas.isEmpty()) {
            return Optional.empty();
        }

        String[] campos = lineas.get(0);

        return Optional.of(new PropuestaLista(
                solicitud(campos, 0), hueco(campos, 5)));
    }

    private static SolicitudLista solicitud(String[] c, int desde) {
        return new SolicitudLista(c[desde], c[desde + 1], c[desde + 2],
                c[desde + 3], c[desde + 4]);
    }

    private static HuecoLista hueco(String[] c, int desde) {
        return new HuecoLista(c[desde], c[desde + 1], c[desde + 2],
                c[desde + 3]);
    }

    // Separar el cuerpo en lineas con el numero de campos esperado
    private static List<String[]> separarLineas(String cuerpo, int campos) {

        List<String[]> lineas = new ArrayList<>();

        if (cuerpo == null || cuerpo.isBlank()) {
            return lineas;
        }

        for (String linea : cuerpo.split("\n")) {
            String[] partes = linea.split(";", -1);

            if (partes.length != campos) {
                throw new IllegalArgumentException(
                        "La respuesta del servidor tiene un formato incorrecto");
            }

            lineas.add(partes);
        }

        return lineas;
    }

    // Mensaje de confirmacion de una operacion correcta
    public static String mensajeCorrecto(RespuestaListaEspera respuesta) {

        String mensaje = extraer(respuesta.cuerpo(), "mensaje");

        return mensaje.isEmpty() ? "Operacion realizada." : mensaje;
    }

    // Mensaje de error para el usuario: explica que ha pasado, anade el
    // motivo del servidor si lo hay y deja el codigo HTTP al final
    public static String mensajeError(RespuestaListaEspera respuesta) {

        StringBuilder mensaje =
                new StringBuilder(explicacion(respuesta.codigo()));

        String motivo = extraer(respuesta.cuerpo(), "error");

        if (!motivo.isEmpty()) {
            mensaje.append(" Motivo: ").append(motivo);
        }

        return mensaje.append(" (Codigo HTTP ")
                .append(respuesta.codigo()).append(")").toString();
    }

    // Leer el valor de "clave=..." del cuerpo (vacio si no esta)
    private static String extraer(String cuerpo, String clave) {

        if (cuerpo == null) {
            return "";
        }

        for (String parte : cuerpo.split("&")) {
            String[] par = parte.split("=", 2);

            if (par.length == 2 && URLDecoder.decode(
                    par[0], StandardCharsets.UTF_8).equals(clave)) {
                return URLDecoder.decode(par[1], StandardCharsets.UTF_8);
            }
        }

        return "";
    }

    // Explicacion en lenguaje normal de lo que significa cada codigo
    private static String explicacion(int codigo) {
        return switch (codigo) {
            case 400 -> "Los datos enviados no son validos.";
            case 401 -> "El usuario o la contrasena no son correctos.";
            case 403 -> "No tienes permiso para realizar esta operacion.";
            case 404 -> "El servicio no ha encontrado lo solicitado.";
            case 405 -> "La operacion solicitada no esta permitida.";
            case 409 -> "La operacion no se ha podido realizar.";
            case 500 -> "El servidor ha tenido un problema interno. "
                    + "Intentalo de nuevo mas tarde.";
            default -> "El servidor ha respondido de forma inesperada.";
        };
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    // Cabecera Authorization: Basic usuario:clave en Base64
    private static String cabeceraBasic(String usuario, char[] clave) {

        byte[] credenciales =
                (usuario + ":" + new String(clave))
                        .getBytes(StandardCharsets.UTF_8);

        try {
            return "Basic "
                    + Base64.getEncoder().encodeToString(credenciales);
        } finally {
            Arrays.fill(credenciales, (byte) 0);
        }
    }
}
