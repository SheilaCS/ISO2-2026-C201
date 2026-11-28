package com.sescam.radiodiagnostico.informes;

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
import java.util.stream.Collectors;

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
 * Servidor HTTP de informes e imágenes de pacientes (CDU8, RF09).
 *
 * GET  /informes                          -> comprobar que el servidor funciona
 * POST /informes/crear                    -> crear el informe de una prueba
 * GET  /informes/consultar?id=            -> consultar un informe
 * GET  /informes/paciente?idPaciente=     -> informes de un paciente
 *
 * El usuario se identifica con el CDU1 y debe tener el permiso
 * REGISTRAR_REALIZACION del CDU10 (por defecto, el médico).
 */
public class InformeHttpHandler implements HttpHandler {

    private final IInformeService servicio;
    private final IAutenticacionService autenticacion;
    private final IRolesService roles;

    public InformeHttpHandler(IInformeService servicio,
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

            if ("GET".equals(metodo) && ruta.equals("/informes")) {
                enviarRespuesta(exchange, 200, "Servidor de informes SESCAM funcionando");
                return;
            }

            boolean crear = "POST".equals(metodo) && ruta.equals("/informes/crear");
            boolean consultar = "GET".equals(metodo) && ruta.equals("/informes/consultar");
            boolean paciente = "GET".equals(metodo) && ruta.equals("/informes/paciente");
            if (!crear && !consultar && !paciente) {
                enviarRespuesta(exchange, 404, "No encontrado");
                return;
            }

            Optional<Sesion> sesion = autenticar(exchange);
            if (sesion.isEmpty()) {
                exchange.getResponseHeaders().set("WWW-Authenticate",
                        "Basic realm=\"informes\", charset=\"UTF-8\"");
                enviarRespuesta(exchange, 401, "Credenciales incorrectas");
                return;
            }
            String medico = sesion.get().identificador();
            if (!roles.tienePermiso(medico, Permiso.REGISTRAR_REALIZACION)) {
                enviarRespuesta(exchange, 403,
                        "Acceso denegado: se requiere el permiso REGISTRAR_REALIZACION");
                return;
            }

            if (crear) {
                crear(exchange, medico);
            } else if (consultar) {
                consultar(exchange);
            } else {
                informesPaciente(exchange);
            }
        } catch (RuntimeException e) {
            enviarRespuesta(exchange, 500, "Error interno del servidor");
        } finally {
            exchange.close();
        }
    }

    private void crear(HttpExchange exchange, String medico) throws IOException {
        String cuerpo = new String(
                exchange.getRequestBody().readNBytes(65_536), StandardCharsets.UTF_8);
        Map<String, String> p = parsearQuery(cuerpo);
        try {
            Informe informe = servicio.crearInforme(medico, p.get("idPaciente"),
                    p.get("idPrueba"), p.get("tipoPrueba"), p.get("contenido"),
                    p.get("imagenes"));
            enviarRespuesta(exchange, 201, "Informe " + informe.id() + " creado correctamente");
        } catch (IllegalArgumentException e) {
            enviarRespuesta(exchange, 400, e.getMessage());
        }
    }

    private void consultar(HttpExchange exchange) throws IOException {
        Integer id = leerEntero(exchange, "id");
        if (id == null) {
            enviarRespuesta(exchange, 400, "El ID de informe debe ser un número entero");
            return;
        }
        Optional<Informe> informe = servicio.consultarInforme(id);
        if (informe.isEmpty()) {
            enviarRespuesta(exchange, 404, "No existe un informe con ese ID");
        } else {
            enviarRespuesta(exchange, 200, formatear(informe.get()));
        }
    }

    private void informesPaciente(HttpExchange exchange) throws IOException {
        Integer idPaciente = leerEntero(exchange, "idPaciente");
        if (idPaciente == null) {
            enviarRespuesta(exchange, 400, "El ID de paciente debe ser un número entero");
            return;
        }
        List<Informe> lista = servicio.consultarInformesPaciente(idPaciente);
        if (lista.isEmpty()) {
            enviarRespuesta(exchange, 200, "El paciente no tiene informes.");
        } else {
            enviarRespuesta(exchange, 200, lista.stream()
                    .map(InformeHttpHandler::formatear)
                    .collect(Collectors.joining("\n\n")));
        }
    }

    private Integer leerEntero(HttpExchange exchange, String campo) {
        String texto = parsearQuery(exchange.getRequestURI().getRawQuery()).get(campo);
        try {
            return Integer.parseInt(texto == null ? "" : texto.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatear(Informe i) {
        return "Informe " + i.id()
                + "\nPaciente: " + i.idPaciente()
                + "\nPrueba: " + i.idPrueba() + " (" + i.tipoPrueba() + ")"
                + "\nMédico: " + i.medico()
                + "\nFecha: " + i.fecha().withNano(0)
                + "\nInforme: " + i.contenido()
                + "\nImágenes: " + (i.imagenes().isEmpty()
                        ? "ninguna" : String.join(", ", i.imagenes()));
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
        server.createContext("/informes", new InformeHttpHandler(
                new InformeServiceImpl(),
                new AutenticacionServiceImpl(),
                new RolesServiceImpl(new RepositorioRolesEnMemoria())));
        return server;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = crearServidor(Integer.getInteger("informes.puerto", 8085));
        server.start();
        System.out.println("Informes: http://127.0.0.1:"
                + server.getAddress().getPort() + "/informes");
    }
}
