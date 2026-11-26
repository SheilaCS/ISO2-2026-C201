package com.sescam.radiodiagnostico.solicitudes;

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

public class SolicitudHttpHandler implements HttpHandler {

    private final ISolicitudService servicio = new SolicitudServiceImpl();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String ruta = exchange.getRequestURI().getPath();

        if ("POST".equals(metodo) && ruta.equals("/solicitudes/crear")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parsearQuery(body);

            String idPaciente = params.get("idPaciente");
            String tipoPrueba = params.get("tipoPrueba");
            String prioridad = params.get("prioridad");

            boolean exito = servicio.solicitarPrueba(idPaciente, tipoPrueba, prioridad);

            String respuesta;
            if (exito) {
                respuesta = "Solicitud creada correctamente";
            } else {
                respuesta = "Error en los datos de la solicitud"; 
            }
            enviarRespuesta(exchange, 200, respuesta);

        } else if ("GET".equals(metodo) && ruta.equals("/solicitudes/consultar")) {
            enviarRespuesta(exchange, 200, "Consulta de solicitud (pendiente)");
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
                try {
                    result.put(entry[0], URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
                } catch (Exception e) {
                    result.put(entry[0], entry[1]);
                }
            } else if (entry.length == 1) {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/solicitudes", new SolicitudHttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor de Solicitudes escuchando en el puerto 8083...");
    }
}