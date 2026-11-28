package com.sescam.radiodiagnostico.seguridad;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sescam.radiodiagnostico.roles.RepositorioRolesEnMemoria;
import com.sescam.radiodiagnostico.roles.RolesServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP de la auditoría (CDU11, RF19).
 *
 * GET  /seguridad            -> comprobar que el servidor funciona
 * POST /seguridad/registrar  -> registrar una acción del usuario autenticado
 * GET  /seguridad/logs       -> consultar la auditoría (permiso CONSULTAR_AUDITORIA)
 *
 * El usuario se identifica con usuario y contraseña (CDU1, RF01) y los
 * permisos se comprueban con el servicio de roles (CDU10, RF18).
 */
public class SeguridadHttpHandler implements HttpHandler {

    private final ISeguridadService servicio;
    private final IAutenticacionService autenticacion;

    public SeguridadHttpHandler(
            ISeguridadService servicio,
            IAutenticacionService autenticacion) {
        this.servicio = servicio;
        this.autenticacion = autenticacion;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod();
            String ruta = exchange.getRequestURI().getPath();

            if ("GET".equals(metodo) && ruta.equals("/seguridad")) {
                enviarRespuesta(exchange, 200, "Servidor de auditoría SESCAM funcionando");
                return;
            }

            boolean rutaConocida =
                    ("POST".equals(metodo) && ruta.equals("/seguridad/registrar"))
                    || ("GET".equals(metodo) && ruta.equals("/seguridad/logs"));
            if (!rutaConocida) {
                enviarRespuesta(exchange, 404, "No encontrado");
                return;
            }

            // RF01: ¿quién hace la petición?
            Optional<Sesion> sesion = autenticar(exchange);
            if (sesion.isEmpty()) {
                exchange.getResponseHeaders().set("WWW-Authenticate",
                        "Basic realm=\"auditoria\", charset=\"UTF-8\"");
                enviarRespuesta(exchange, 401, "Credenciales incorrectas");
                return;
            }
            String usuario = sesion.get().identificador();

            if (ruta.equals("/seguridad/registrar")) {
                registrar(exchange, usuario);
            } else {
                consultarLogs(exchange, usuario);
            }
        } catch (RuntimeException e) {
            enviarRespuesta(exchange, 500, "Error interno del servidor");
        } finally {
            exchange.close();
        }
    }

    private void registrar(HttpExchange exchange, String usuario) throws IOException {
        String cuerpo = new String(
                exchange.getRequestBody().readNBytes(16_384), StandardCharsets.UTF_8);
        Map<String, String> params = parsearQuery(cuerpo);

        // El usuario es siempre el autenticado, no el que diga el cliente.
        if (servicio.registrarAuditoria(usuario, params.get("accion"), params.get("detalles"))) {
            enviarRespuesta(exchange, 200, "Acción registrada en la auditoría");
        } else {
            enviarRespuesta(exchange, 400, "Falta la acción a registrar");
        }
    }

    private void consultarLogs(HttpExchange exchange, String usuario) throws IOException {
        // RF18: el permiso lo decide el servicio de roles.
        if (!servicio.puedeConsultarAuditoria(usuario)) {
            servicio.registrarAuditoria(usuario, "CONSULTAR_AUDITORIA", "Acceso DENEGADO");
            enviarRespuesta(exchange, 403,
                    "Acceso denegado: se requiere el permiso CONSULTAR_AUDITORIA");
            return;
        }

        List<String> logs = servicio.consultarLogs();
        servicio.registrarAuditoria(usuario, "CONSULTAR_AUDITORIA", "Acceso permitido");

        enviarRespuesta(exchange, 200, logs.isEmpty()
                ? "No hay registros de auditoría."
                : String.join("\n", logs));
    }

    /** Lee la cabecera Authorization: Basic usuario:contraseña (igual que CDU10). */
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
                result.put(
                        URLDecoder.decode(entry[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    public static HttpServer crearServidor(int puerto) throws IOException {
        ISeguridadService servicio = new SeguridadServiceImpl(
                new RolesServiceImpl(new RepositorioRolesEnMemoria()));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        server.createContext("/seguridad",
                new SeguridadHttpHandler(servicio, new AutenticacionServiceImpl()));
        return server;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = crearServidor(Integer.getInteger("seguridad.puerto", 8087));
        server.start();
        System.out.println("Auditoría: http://127.0.0.1:"
                + server.getAddress().getPort() + "/seguridad");
    }
}