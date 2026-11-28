package com.sescam.radiodiagnostico.solicitudes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sescam.radiodiagnostico.roles.IRolesService;
import com.sescam.radiodiagnostico.roles.Permiso;
import com.sescam.radiodiagnostico.roles.RepositorioRolesEnMemoria;
import com.sescam.radiodiagnostico.roles.RolesServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP de solicitudes de pruebas (CDU2, RF06).
 *
 * GET  /solicitudes                -> comprobar que el servidor funciona
 * POST /solicitudes/crear          -> crear una solicitud
 * GET  /solicitudes/consultar?id=  -> consultar una solicitud
 *
 * El usuario se identifica con el CDU1 y debe tener el permiso
 * SOLICITAR_PRUEBAS del CDU10.
 */
public class SolicitudHttpHandler implements HttpHandler {

    private final ISolicitudService servicio;
    private final IAutenticacionService autenticacion;
    private final IRolesService roles;

    public SolicitudHttpHandler(ISolicitudService servicio,
            IAutenticacionService autenticacion, IRolesService roles) {
        this.servicio = servicio;
        this.autenticacion = autenticacion;
        this.roles = roles;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod();
            String ruta = exchange.getRequestURI().getPath();

            if ("GET".equals(metodo) && ruta.equals("/solicitudes")) {
                enviarRespuesta(exchange, 200, "Servidor de solicitudes SESCAM funcionando");
                return;
            }

            boolean crear = "POST".equals(metodo) && ruta.equals("/solicitudes/crear");
            boolean consultar = "GET".equals(metodo) && ruta.equals("/solicitudes/consultar");
            if (!crear && !consultar) {
                enviarRespuesta(exchange, 404, "No encontrado");
                return;
            }

            Optional<Sesion> sesion = autenticar(exchange);
            if (sesion.isEmpty()) {
                exchange.getResponseHeaders().set("WWW-Authenticate",
                        "Basic realm=\"solicitudes\", charset=\"UTF-8\"");
                enviarRespuesta(exchange, 401, "Credenciales incorrectas");
                return;
            }
            String medico = sesion.get().identificador();
            if (!roles.tienePermiso(medico, Permiso.SOLICITAR_PRUEBAS)) {
                enviarRespuesta(exchange, 403,
                        "Acceso denegado: se requiere el permiso SOLICITAR_PRUEBAS");
                return;
            }

            if (crear) {
                crear(exchange, medico);
            } else {
                consultar(exchange);
            }
        } catch (RuntimeException e) {
            enviarRespuesta(exchange, 500, "Error interno del servidor");
        } finally {
            exchange.close();
        }
    }

    private void crear(HttpExchange exchange, String medico) throws IOException {
        String cuerpo = new String(
                exchange.getRequestBody().readNBytes(16_384), StandardCharsets.UTF_8);
        Map<String, String> p = parsearQuery(cuerpo);
        try {
            Solicitud s = servicio.solicitarPrueba(medico, p.get("idPaciente"),
                    p.get("tipoPrueba"), p.get("indicacionClinica"),
                    p.get("justificacion"), p.get("prioridad"));
            enviarRespuesta(exchange, 201, "Solicitud " + s.id() + " creada correctamente");
        } catch (IllegalArgumentException e) {
            enviarRespuesta(exchange, 400, e.getMessage());
        }
    }

    private void consultar(HttpExchange exchange) throws IOException {
        String texto = parsearQuery(exchange.getRequestURI().getRawQuery()).get("id");
        int id;
        try {
            id = Integer.parseInt(texto == null ? "" : texto.strip());
        } catch (NumberFormatException e) {
            enviarRespuesta(exchange, 400, "El ID de solicitud debe ser un número entero");
            return;
        }
        Optional<Solicitud> solicitud = servicio.consultarSolicitud(id);
        if (solicitud.isEmpty()) {
            enviarRespuesta(exchange, 404, "No existe una solicitud con ese ID");
        } else {
            enviarRespuesta(exchange, 200, formatear(solicitud.get()));
        }
    }

    private static String formatear(Solicitud s) {
        return "Solicitud " + s.id()
                + "\nPaciente: " + s.idPaciente()
                + "\nMédico: " + s.medico()
                + "\nTipo de prueba: " + s.tipoPrueba()
                + "\nIndicación clínica: " + s.indicacionClinica()
                + "\nJustificación: " + s.justificacion()
                + "\nPrioridad: " + s.prioridad()
                + "\nFecha: " + s.fecha().withNano(0);
    }

    /** Lee la cabecera Authorization: Basic usuario:contraseña (CDU1). */
    private Optional<Sesion> autenticar(HttpExchange exchange) {
        String cabecera = exchange.getRequestHeaders().getFirst("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }
        byte[] contenido;
        try {
            contenido = Base64.getDecoder().decode(cabecera.substring(6).trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        char[] clave = null;
        try {
            String texto = new String(contenido, StandardCharsets.UTF_8);
            int separador = texto.indexOf(':');
            if (separador <= 0) {
                return Optional.empty();
            }
            clave = texto.substring(separador + 1).toCharArray();
            return autenticacion.iniciarSesion(texto.substring(0, separador), clave);
        } finally {
            Arrays.fill(contenido, (byte) 0);
            if (clave != null) {
                Arrays.fill(clave, '\0');
            }
        }
    }

    private void enviarRespuesta(HttpExchange exchange, int codigo, String mensaje)
            throws IOException {
        byte[] respuesta = mensaje.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(codigo, respuesta.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respuesta);
        }
    }

    private Map<String, String> parsearQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=", 2);
            if (entry.length > 1) {
                result.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    public static HttpServer crearServidor(int puerto) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        server.createContext("/solicitudes", new SolicitudHttpHandler(
                new SolicitudServiceImpl(),
                new AutenticacionServiceImpl(),
                new RolesServiceImpl(new RepositorioRolesEnMemoria())));
        return server;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = crearServidor(Integer.getInteger("solicitudes.puerto", 8083));
        server.start();
        System.out.println("Solicitudes: http://127.0.0.1:"
                + server.getAddress().getPort() + "/solicitudes");
    }
}
