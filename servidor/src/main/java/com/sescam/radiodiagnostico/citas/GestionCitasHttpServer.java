
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

    // Referencia al servidor principal para poder detenerlo en las pruebas
    static HttpServer servidorPrincipal;

    /**
     * Crear un servidor con el servicio normal de citas.
     *
     * Puerto 8080: aplicacion normal.
     * Puerto 0: puerto libre para pruebas JUnit.
     */
    public static HttpServer crearServidor(int puerto)
            throws IOException {

        return crearServidor(
                puerto,
                new GestionCitasServiceImpl());
    }

    /**
     * Crear un servidor con un servicio configurable.
     *
     * Permite utilizar un servicio simulado en las pruebas
     * para comprobar errores internos.
     */
    static HttpServer crearServidor(
            int puerto,
            IGestionCitasService servicio)
            throws IOException {

        // Crear servidor HTTP local
        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0
        );

        // -----------------------------------------
        // OPERACIONES HTTP
        // -----------------------------------------

        servidor.createContext("/citas", peticion -> {

            String respuesta;
            int codigo = 200;

            try {

                String ruta =
                        peticion.getRequestURI().getPath();

                String metodo =
                        peticion.getRequestMethod();

                Map<String, String> datos = leerParametros(
                        peticion.getRequestURI().getRawQuery()
                );

                // -----------------------------------------
                // COMPROBAR SERVIDOR
                // -----------------------------------------

                if (ruta.equals("/citas")
                        && metodo.equals("GET")) {

                    respuesta =
                            "Servidor de citas SESCAM funcionando";

                // -----------------------------------------
                // ASIGNAR CITA
                // -----------------------------------------

                } else if (ruta.equals("/citas/asignar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                            new String(
                                    peticion.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    ));

                    boolean correcto = servicio.asignarCita(
                            Integer.parseInt(
                                    datos.get("solicitud")),
                            Integer.parseInt(
                                    datos.get("paciente")),
                            datos.get("fecha"),
                            datos.get("hora")
                    );

                    respuesta = correcto
                            ? "Cita asignada correctamente"
                            : "No se ha podido asignar la cita";

                // -----------------------------------------
                // REPROGRAMAR CITA
                // -----------------------------------------

                } else if (ruta.equals("/citas/reprogramar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                            new String(
                                    peticion.getRequestBody()
                                            .readAllBytes(),
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

                // -----------------------------------------
                // CANCELAR CITA
                // -----------------------------------------

                } else if (ruta.equals("/citas/cancelar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                            new String(
                                    peticion.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    ));

                    boolean correcto = servicio.cancelarCita(
                            Integer.parseInt(datos.get("id"))
                    );

                    respuesta = correcto
                            ? "Cita cancelada correctamente"
                            : "No se ha podido cancelar la cita";

                // -----------------------------------------
                // CONSULTAR CITA
                // -----------------------------------------

                } else if (ruta.equals("/citas/consultar")
                        && metodo.equals("GET")) {

                    respuesta = servicio.consultarCita(
                            Integer.parseInt(datos.get("id"))
                    );

                // -----------------------------------------
                // LISTAR LAS CITAS DE UN PACIENTE (RF02)
                // -----------------------------------------

                } else if (ruta.equals("/citas/paciente")
                        && metodo.equals("GET")) {

                    respuesta = servicio.listarCitasPaciente(
                            Integer.parseInt(datos.get("paciente"))
                    );

                // -----------------------------------------
                // CONFIRMAR UNA CITA PROPIA (RF02)
                // -----------------------------------------

                } else if (ruta.equals("/citas/paciente/confirmar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                            new String(
                                    peticion.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    ));

                    ResultadoOperacion resultado =
                            servicio.confirmarCitaPaciente(
                                    Integer.parseInt(datos.get("id")),
                                    Integer.parseInt(
                                            datos.get("paciente")));

                    respuesta = resultado.getMensaje();

                    // Un rechazo de negocio se indica con 409
                    codigo = resultado.isCorrecto() ? 200 : 409;

                // -----------------------------------------
                // SOLICITAR EL CAMBIO DE UNA CITA PROPIA (RF02)
                // -----------------------------------------

                } else if (ruta.equals("/citas/paciente/cambio")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                            new String(
                                    peticion.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    ));

                    ResultadoOperacion resultado =
                            servicio.solicitarCambioCitaPaciente(
                                    Integer.parseInt(datos.get("id")),
                                    Integer.parseInt(
                                            datos.get("paciente")),
                                    datos.get("motivo"));

                    respuesta = resultado.getMensaje();
                    codigo = resultado.isCorrecto() ? 200 : 409;

                // -----------------------------------------
                // CANCELAR UNA CITA PROPIA (RF02)
                // -----------------------------------------

                } else if (ruta.equals("/citas/paciente/cancelar")
                        && metodo.equals("POST")) {

                    datos.putAll(leerParametros(
                            new String(
                                    peticion.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    ));

                    ResultadoOperacion resultado =
                            servicio.cancelarCitaPaciente(
                                    Integer.parseInt(datos.get("id")),
                                    Integer.parseInt(
                                            datos.get("paciente")));

                    respuesta = resultado.getMensaje();
                    codigo = resultado.isCorrecto() ? 200 : 409;

                // -----------------------------------------
                // RUTA O METODO INCORRECTO
                // -----------------------------------------

                } else {

                    codigo = 404;
                    respuesta = "Operacion no encontrada";
                }

            } catch (IllegalArgumentException e) {

                codigo = 400;
                respuesta = "Datos incorrectos o incompletos";

            } catch (Exception e) {

                codigo = 500;
                respuesta = "Error interno del servidor";

                e.printStackTrace();
            }

            // Enviar la respuesta HTTP
            enviarRespuesta(peticion, codigo, respuesta);
        });

        // Devolver el servidor sin arrancarlo
        return servidor;
    }

    // -----------------------------------------
    // METODO PRINCIPAL
    // -----------------------------------------

    /**
     * Iniciar normalmente el servidor de citas.
     *
     * El puerto predeterminado es 8080.
     * Para las pruebas se puede configurar otro puerto.
     */
    public static void main(String[] args) throws IOException {

        // Usar 8080 normalmente y permitir
        // la propiedad citas.puerto durante las pruebas
        int puerto = Integer.getInteger(
                "citas.puerto", 8080);

        servidorPrincipal = crearServidor(puerto);

        servidorPrincipal.start();

        System.out.println("Servidor de citas iniciado");

        System.out.println(
                "http://localhost:"
                + servidorPrincipal.getAddress().getPort()
                + "/citas");
    }

    // -----------------------------------------
    // LEER PARAMETROS HTTP
    // -----------------------------------------

    /**
     * Convertir parametros HTTP en pares clave-valor.
     */
    private static Map<String, String> leerParametros(
            String texto) {

        Map<String, String> parametros = new HashMap<>();

        // Comprobar si no hay parametros
        if (texto == null || texto.isEmpty()) {
            return parametros;
        }

        // Recorrer los parametros recibidos
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

    // -----------------------------------------
    // ENVIAR RESPUESTA HTTP
    // -----------------------------------------

    /**
     * Enviar la respuesta al cliente HTTP.
     */
    private static void enviarRespuesta(
            HttpExchange peticion,
            int codigo,
            String respuesta)
            throws IOException {

        byte[] contenido = respuesta.getBytes(
                StandardCharsets.UTF_8);

        peticion.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        peticion.sendResponseHeaders(
                codigo, contenido.length);

        try (OutputStream salida =
                     peticion.getResponseBody()) {

            salida.write(contenido);
        }
    }
}
