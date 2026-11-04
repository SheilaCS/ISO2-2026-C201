
package com.sescam.radiodiagnostico.citas;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * SERVIDOR HTTP - Iteracion 3 (RF13).
 * Gestion de citas medicas.
 *
 * Version de desarrollo: almacena citas en memoria.
 */
public class GestionCitasHttpServer {

    public static void main(String[] args) throws IOException {

        // Servicio que contiene la logica de las citas
        IGestionCitasService servicio = new GestionCitasServiceImpl();

        // Crear servidor HTTP local
        HttpServer servidor = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 8080), 0
        );

        servidor.createContext("/citas", peticion -> {

            String respuesta;
            int codigo = 200;

            try {
                String ruta = peticion.getRequestURI().getPath();
                String metodo = peticion.getRequestMethod();

                Map<String, String> datos = leerParametros(
                    peticion.getRequestURI().getRawQuery()
                );

                if (ruta.equals("/citas")
                        && metodo.equals("GET")) {

                    respuesta = "Servidor de citas SESCAM funcionando";

                } else if (ruta.equals("/citas/asignar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                        new String(
                            peticion.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                        )
                    ));

                    boolean correcto = servicio.asignarCita(
                        Integer.parseInt(datos.get("solicitud")),
                        Integer.parseInt(datos.get("paciente")),
                        datos.get("fecha"),
                        datos.get("hora")
                    );

                    respuesta = correcto
                        ? "Cita asignada correctamente"
                        : "No se ha podido asignar la cita";

                } else if (ruta.equals("/citas/reprogramar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                        new String(
                            peticion.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                        )
                    ));

                    boolean correcto = servicio.reprogramarCita(
                        Integer.parseInt(datos.get("id")),
                        datos.get("fecha"),
                        datos.get("hora")
                    );

                    respuesta = correcto
                        ? "Cita reprogramada correctamente"
                        : "No se ha podido reprogramar la cita";

                } else if (ruta.equals("/citas/cancelar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                        new String(
                            peticion.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                        )
                    ));

                    boolean correcto = servicio.cancelarCita(
                        Integer.parseInt(datos.get("id"))
                    );

                    respuesta = correcto
                        ? "Cita cancelada correctamente"
                        : "No se ha podido cancelar la cita";

                } else if (ruta.equals("/citas/consultar")
                        && metodo.equals("GET")) {

                    respuesta = servicio.consultarCita(
                        Integer.parseInt(datos.get("id"))
                    );

                } else {
                    codigo = 404;
                    respuesta = "Operacion no encontrada";
                }

            } catch (IllegalArgumentException | NullPointerException e) {
                codigo = 400;
                respuesta = "Datos incorrectos o incompletos";
            } catch (Exception e) {
                codigo = 500;
                respuesta = "Error interno del servidor";
                e.printStackTrace();
            }

            enviarRespuesta(peticion, codigo, respuesta);
        });

        servidor.start();

        System.out.println("Servidor de citas iniciado");
        System.out.println("http://localhost:8080/citas");
    }

    // Convertir parametros HTTP en pares clave-valor
    private static Map<String, String> leerParametros(String texto) {

        Map<String, String> parametros = new HashMap<>();

        if (texto == null || texto.isEmpty()) {
            return parametros;
        }

        for (String parte : texto.split("&")) {

            String[] dato = parte.split("=", 2);

            if (dato.length == 2) {
                String clave = URLDecoder.decode(
                    dato[0], StandardCharsets.UTF_8
                );

                String valor = URLDecoder.decode(
                    dato[1], StandardCharsets.UTF_8
                );

                parametros.put(clave, valor);
            }
        }

        return parametros;
    }

    // Enviar la respuesta al cliente HTTP
    private static void enviarRespuesta(
            HttpExchange peticion, int codigo, String respuesta)
            throws IOException {

        byte[] contenido = respuesta.getBytes(StandardCharsets.UTF_8);

        peticion.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8"
        );

        peticion.sendResponseHeaders(codigo, contenido.length);

        try (OutputStream salida = peticion.getResponseBody()) {
            salida.write(contenido);
        }
    }
}
