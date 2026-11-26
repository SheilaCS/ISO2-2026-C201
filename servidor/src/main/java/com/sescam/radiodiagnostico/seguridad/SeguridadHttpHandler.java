package com.sescam.radiodiagnostico.seguridad;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SeguridadHttpHandler implements HttpHandler {

    private final SeguridadServiceImpl servicio = new SeguridadServiceImpl();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String ruta = exchange.getRequestURI().getPath();
        Map<String, String> params;

        if ("POST".equals(metodo)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            params = parsearQuery(body);
        } else {
            params = parsearQuery(exchange.getRequestURI().getQuery());
        }

        if ("POST".equals(metodo) && ruta.equals("/seguridad/verificar")) {
            String usuario = params.get("usuario");
            String rol = params.get("rol");
            String accion = params.get("accion");

            boolean permitido = servicio.verificarPermiso(usuario, rol, accion);
            
            if (permitido) {
                servicio.registrarAuditoria(usuario, accion, "Acceso permitido");
                enviarRespuesta(exchange, 200, "Permiso concedido");
            } else {
                servicio.registrarAuditoria(usuario, accion, "Acceso DENEGADO");
                enviarRespuesta(exchange, 403, "Acceso denegado: No tiene permisos suficientes");
            }

        } else if ("GET".equals(metodo) && ruta.equals("/seguridad/logs")) {
            // Requiere rol de ADMINISTRADOR para ver los logs (Seguridad)
            String rol = params.get("rol");
            if (rol != null && rol.equalsIgnoreCase("ADMINISTRADOR")) {
                enviarRespuesta(exchange, 200, servicio.obtenerLogs());
            } else {
                enviarRespuesta(exchange, 403, "Acceso denegado: Se requiere rol ADMINISTRADOR");
            }
        } else {
            enviarRespuesta(exchange, 404, "No encontrado");
        }
    }

    private void enviarRespuesta(HttpExchange exchange, int codigo, String mensaje) throws IOException {
        byte[] respuesta = mensaje.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(codigo, respuesta.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respuesta);
        }
    }

    private Map<String, String> parsearQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=", 2);
            if (entry.length > 1) {
                result.put(entry[0], URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8087), 0);
        server.createContext("/seguridad", new SeguridadHttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor de Seguridad escuchando en el puerto 8087...");
    }
}