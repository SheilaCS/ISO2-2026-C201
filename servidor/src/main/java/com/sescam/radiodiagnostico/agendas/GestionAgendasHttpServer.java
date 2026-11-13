
package com.sescam.radiodiagnostico.agendas;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;

public class GestionAgendasHttpServer {

    private static GestionAgendasServiceImpl servicio =
            new GestionAgendasServiceImpl();

    public static void main(String[] args) throws IOException {

        // Puerto 8081 para evitar conflictos con citas
        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("localhost", 8081), 0);

        // Crear un horario
        servidor.createContext("/agendas/crear", exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);

                int idMedico = Integer.parseInt(datos.get("idMedico"));
                LocalDate fecha = LocalDate.parse(datos.get("fecha"));
                LocalTime inicio = LocalTime.parse(datos.get("inicio"));
                LocalTime fin = LocalTime.parse(datos.get("fin"));

                boolean correcto = servicio.crearHorario(
                        idMedico, fecha, inicio, fin);

                responder(exchange, correcto ? 200 : 400,
                        correcto ? "Horario creado correctamente"
                                 : "No se pudo crear el horario");

            } catch (Exception e) {
                responder(exchange, 400, "Datos incorrectos");
            }
        });

        // Consultar la agenda de un medico
        servidor.createContext("/agendas/consultar", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);
                int idMedico = Integer.parseInt(datos.get("idMedico"));

                responder(exchange, 200, servicio.consultarAgenda(idMedico));

            } catch (Exception e) {
                responder(exchange, 400, "Datos incorrectos");
            }
        });

        // Modificar un horario
        servidor.createContext("/agendas/modificar", exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);

                int idHorario = Integer.parseInt(datos.get("idHorario"));
                LocalDate fecha = LocalDate.parse(datos.get("fecha"));
                LocalTime inicio = LocalTime.parse(datos.get("inicio"));
                LocalTime fin = LocalTime.parse(datos.get("fin"));

                boolean correcto = servicio.modificarHorario(
                        idHorario, fecha, inicio, fin);

                responder(exchange, correcto ? 200 : 400,
                        correcto ? "Horario modificado correctamente"
                                 : "No se pudo modificar el horario");

            } catch (Exception e) {
                responder(exchange, 400, "Datos incorrectos");
            }
        });

        // Eliminar un horario
        servidor.createContext("/agendas/eliminar", exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);
                int idHorario = Integer.parseInt(datos.get("idHorario"));

                boolean correcto = servicio.eliminarHorario(idHorario);

                responder(exchange, correcto ? 200 : 400,
                        correcto ? "Horario eliminado correctamente"
                                 : "No se pudo eliminar el horario");

            } catch (Exception e) {
                responder(exchange, 400, "Datos incorrectos");
            }
        });

        servidor.start();

        System.out.println("Servidor de agendas iniciado en http://localhost:8081");
    }

    // Leer parametros enviados por GET o POST
    private static HashMap<String, String> leerParametros(
            HttpExchange exchange) throws IOException {

        String texto;

        if (exchange.getRequestMethod().equals("GET")) {
            texto = exchange.getRequestURI().getRawQuery();
        } else {
            texto = new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
        }

        HashMap<String, String> datos = new HashMap<>();

        if (texto == null || texto.isEmpty()) {
            return datos;
        }

        for (String parametro : texto.split("&")) {
            String[] partes = parametro.split("=", 2);

            if (partes.length == 2) {
                String clave = URLDecoder.decode(
                        partes[0], StandardCharsets.UTF_8);
                String valor = URLDecoder.decode(
                        partes[1], StandardCharsets.UTF_8);

                datos.put(clave, valor);
            }
        }

        return datos;
    }

    // Enviar una respuesta HTTP al cliente
    private static void responder(HttpExchange exchange,
                                  int codigo, String mensaje)
            throws IOException {

        byte[] respuesta = mensaje.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(codigo, respuesta.length);

        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(respuesta);
        }
    }
}
