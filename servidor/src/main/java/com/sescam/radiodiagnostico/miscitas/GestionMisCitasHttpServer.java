
package com.sescam.radiodiagnostico.miscitas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class GestionMisCitasHttpServer {

    private static GestionMisCitasServiceImpl servicio =
            new GestionMisCitasServiceImpl();

    // Guardar la referencia al servidor principal
    static HttpServer servidorPrincipal;

    // Crear el servidor utilizando el puerto indicado
    // El puerto 0 permite utilizar uno libre para las pruebas
    public static HttpServer crearServidor(int puerto) throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("localhost", puerto), 0);

        // -----------------------------------------
        // CONSULTAR MIS CITAS
        // -----------------------------------------

        servidor.createContext("/miscitas/consultar", exchange -> {

            if (!exchange.getRequestMethod().equals("GET")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);

                int idPaciente = Integer.parseInt(datos.get("idPaciente"));

                responder(exchange, 200,
                        servicio.consultarMisCitas(idPaciente));

            } catch (Exception e) {
                responderError(exchange, e);
            }
        });

        // -----------------------------------------
        // CONFIRMAR CITA
        // -----------------------------------------

        servidor.createContext("/miscitas/confirmar", exchange -> {

            if (!exchange.getRequestMethod().equals("POST")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);

                int idCita = Integer.parseInt(datos.get("idCita"));
                int idPaciente = Integer.parseInt(datos.get("idPaciente"));

                ResultadoOperacion resultado = servicio.confirmarCita(
                        idCita, idPaciente);

                responder(exchange,
                        resultado.isCorrecto() ? 200 : 400,
                        resultado.getMensaje());

            } catch (Exception e) {
                responderError(exchange, e);
            }
        });

        // -----------------------------------------
        // SOLICITAR CAMBIO DE CITA
        // -----------------------------------------

        servidor.createContext("/miscitas/cambio", exchange -> {

            if (!exchange.getRequestMethod().equals("POST")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);

                int idCita = Integer.parseInt(datos.get("idCita"));
                int idPaciente = Integer.parseInt(datos.get("idPaciente"));
                String motivo = datos.get("motivo");

                ResultadoOperacion resultado =
                        servicio.solicitarCambioCita(
                                idCita, idPaciente, motivo);

                responder(exchange,
                        resultado.isCorrecto() ? 200 : 400,
                        resultado.getMensaje());

            } catch (Exception e) {
                responderError(exchange, e);
            }
        });

        // -----------------------------------------
        // CANCELAR CITA
        // -----------------------------------------

        servidor.createContext("/miscitas/cancelar", exchange -> {

            if (!exchange.getRequestMethod().equals("POST")) {
                responder(exchange, 405, "Metodo no permitido");
                return;
            }

            try {
                HashMap<String, String> datos = leerParametros(exchange);

                int idCita = Integer.parseInt(datos.get("idCita"));
                int idPaciente = Integer.parseInt(datos.get("idPaciente"));

                ResultadoOperacion resultado = servicio.cancelarCita(
                        idCita, idPaciente);

                responder(exchange,
                        resultado.isCorrecto() ? 200 : 400,
                        resultado.getMensaje());

            } catch (Exception e) {
                responderError(exchange, e);
            }
        });

        // Devolver el servidor sin arrancarlo
        return servidor;
    }

    // -----------------------------------------
    // EJECUTAR EL SERVIDOR
    // -----------------------------------------

    public static void main(String[] args) throws IOException {

        // Puerto 8082 para la aplicacion normal
        // Durante las pruebas se puede configurar otro puerto
        int puerto = Integer.getInteger("miscitas.puerto", 8082);

        servidorPrincipal = crearServidor(puerto);

        servidorPrincipal.start();

        System.out.println(
                "Servidor de mis citas iniciado en http://localhost:"
                + servidorPrincipal.getAddress().getPort());
    }

    // -----------------------------------------
    // LEER PARAMETROS GET O POST
    // -----------------------------------------

    private static HashMap<String, String> leerParametros(
            HttpExchange exchange) throws IOException {

        String texto;

        if (exchange.getRequestMethod().equals("GET")) {

            texto = exchange.getRequestURI().getRawQuery();

        } else {

            texto = new String(
                    exchange.getRequestBody().readAllBytes(),
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

    // -----------------------------------------
    // RESPONDER A UN ERROR
    // -----------------------------------------

    // 503 si falla la conexion con el servidor de citas,
    // 400 si los datos recibidos no son correctos
    private static void responderError(HttpExchange exchange,
                                       Exception e)
            throws IOException {

        if (e instanceof IllegalStateException) {
            responder(exchange, 503, e.getMessage());
        } else {
            responder(exchange, 400, "Datos incorrectos");
        }
    }

    // -----------------------------------------
    // ENVIAR RESPUESTA HTTP
    // -----------------------------------------

    private static void responder(HttpExchange exchange,
                                  int codigo, String mensaje)
            throws IOException {

        byte[] respuesta = mensaje.getBytes(
                StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(codigo, respuesta.length);

        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(respuesta);
        }
    }
}
