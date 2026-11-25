package com.sescam.radiodiagnostico.informes;

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

public class InformeHttpHandler implements HttpHandler {

    // Inyectamos el servicio (Lógica separada)
    private final IInformeService servicio = new InformeServiceImpl();

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

        String rol = params.get("rol");

        if ("POST".equals(metodo) && ruta.equals("/informes/generar")) {
            int idEquipo = parsearEntero(params.get("idEquipo"));
            String fecha = params.get("fecha");
            String descripcion = params.get("descripcion");

            boolean exito = servicio.generarInforme(idEquipo, fecha, descripcion, rol);

            if (exito) {
                enviarRespuesta(exchange, 200, "Informe generado correctamente.");
            } else {
                enviarRespuesta(exchange, 403, "Error: Datos inválidos o acceso denegado.");
            }

        } else if ("GET".equals(metodo) && ruta.equals("/informes/consultar")) {
            int idInforme = parsearEntero(params.get("id"));
            
            String respuesta = servicio.consultarInforme(idInforme, rol);
            
            if (respuesta.contains("Error")) {
                enviarRespuesta(exchange, 403, respuesta);
            } else {
                enviarRespuesta(exchange, 200, respuesta);
            }
            
        } else {
            enviarRespuesta(exchange, 404, "Ruta no encontrada");
        }
    }

    // Método para evitar NumberFormatException si el cliente envía basura
    private int parsearEntero(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            return -1; // Forzamos a que falle la validación del servicio
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
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);
        server.createContext("/informes", new InformeHttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor de Informes escuchando en el puerto 8085...");
    }
}