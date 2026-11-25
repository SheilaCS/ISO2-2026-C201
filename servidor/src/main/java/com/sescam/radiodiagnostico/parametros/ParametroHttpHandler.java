package com.sescam.radiodiagnostico.parametros;

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

public class ParametroHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String ruta = exchange.getRequestURI().getPath();

        if ("POST".equals(metodo) && ruta.equals("/parametros/guardar")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parsearQuery(body);

            String clave = params.get("clave");
            String valor = params.get("valor");

            // Simulamos que lo guardamos correctamente
            String respuesta = "Parámetro '" + clave + "' guardado con valor '" + valor + "'";
            enviarRespuesta(exchange, 200, respuesta);

        } else if ("GET".equals(metodo) && ruta.equals("/parametros/consultar")) {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parsearQuery(query);
            String clave = params.get("clave");

            enviarRespuesta(exchange, 200, "Valor del parámetro '" + clave + "': [Simulado]");
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
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8084), 0);
        server.createContext("/parametros", new ParametroHttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Servidor de Parámetros escuchando en el puerto 8084...");
    }
}