package com.sescam.radiodiagnostico.parametros;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

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
 * Servidor HTTP de configuración de parámetros (CDU5, RF17).
 *
 * GET  /parametros                              -> comprobar que el servidor funciona
 * POST /parametros/guardar                      -> guardar (permiso CONFIGURAR_PARAMETROS)
 * GET  /parametros/consultar?categoria=&clave=  -> consultar (usuario autenticado)
 * GET  /parametros/listar?categoria=            -> listar una categoría (usuario autenticado)
 */
public class ParametroHttpHandler implements HttpHandler {

    private final IParametroService servicio;
    private final IAutenticacionService autenticacion;
    private final IRolesService roles;

    public ParametroHttpHandler(IParametroService servicio,
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

            if ("GET".equals(metodo) && ruta.equals("/parametros")) {
                enviarRespuesta(exchange, 200, "Servidor de parámetros SESCAM funcionando");
                return;
            }

            boolean guardar = "POST".equals(metodo) && ruta.equals("/parametros/guardar");
            boolean consultar = "GET".equals(metodo) && ruta.equals("/parametros/consultar");
            boolean listar = "GET".equals(metodo) && ruta.equals("/parametros/listar");
            if (!guardar && !consultar && !listar) {
                enviarRespuesta(exchange, 404, "No encontrado");
                return;
            }

            Optional<Sesion> sesion = autenticar(exchange);
            if (sesion.isEmpty()) {
                exchange.getResponseHeaders().set("WWW-Authenticate",
                        "Basic realm=\"parametros\", charset=\"UTF-8\"");
                enviarRespuesta(exchange, 401, "Credenciales incorrectas");
                return;
            }

            if (guardar) {
                if (!roles.tienePermiso(sesion.get().identificador(),
                        Permiso.CONFIGURAR_PARAMETROS)) {
                    enviarRespuesta(exchange, 403,
                            "Acceso denegado: se requiere el permiso CONFIGURAR_PARAMETROS");
                    return;
                }
                guardar(exchange);
            } else if (consultar) {
                consultar(exchange);
            } else {
                listar(exchange);
            }
        } catch (RuntimeException e) {
            enviarRespuesta(exchange, 500, "Error interno del servidor");
        } finally {
            exchange.close();
        }
    }

    private void guardar(HttpExchange exchange) throws IOException {
        String cuerpo = new String(
                exchange.getRequestBody().readNBytes(16_384), StandardCharsets.UTF_8);
        Map<String, String> p = parsearQuery(cuerpo);
        try {
            CategoriaParametro categoria = leerCategoria(p.get("categoria"));
            servicio.guardarParametro(categoria, p.get("clave"), p.get("valor"));
            enviarRespuesta(exchange, 200, "Parámetro " + categoria + "/"
                    + p.get("clave").strip() + " guardado correctamente");
        } catch (IllegalArgumentException e) {
            enviarRespuesta(exchange, 400, e.getMessage());
        }
    }

    private void consultar(HttpExchange exchange) throws IOException {
        Map<String, String> p = parsearQuery(exchange.getRequestURI().getRawQuery());
        try {
            CategoriaParametro categoria = leerCategoria(p.get("categoria"));
            String clave = p.get("clave");
            if (clave == null || clave.isBlank()) {
                enviarRespuesta(exchange, 400, "Falta la clave del parámetro");
                return;
            }
            Optional<String> valor = servicio.obtenerParametro(categoria, clave);
            if (valor.isEmpty()) {
                enviarRespuesta(exchange, 404, "No existe el parámetro");
            } else {
                enviarRespuesta(exchange, 200, valor.get());
            }
        } catch (IllegalArgumentException e) {
            enviarRespuesta(exchange, 400, e.getMessage());
        }
    }

    private void listar(HttpExchange exchange) throws IOException {
        Map<String, String> p = parsearQuery(exchange.getRequestURI().getRawQuery());
        try {
            CategoriaParametro categoria = leerCategoria(p.get("categoria"));
            Map<String, String> lista = servicio.listarParametros(categoria);
            if (lista.isEmpty()) {
                enviarRespuesta(exchange, 200, "No hay parámetros en la categoría " + categoria);
                return;
            }
            StringJoiner texto = new StringJoiner("\n");
            lista.forEach((clave, valor) -> texto.add(clave + " = " + valor));
            enviarRespuesta(exchange, 200, texto.toString());
        } catch (IllegalArgumentException e) {
            enviarRespuesta(exchange, 400, e.getMessage());
        }
    }

    private static CategoriaParametro leerCategoria(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Falta la categoría del parámetro");
        }
        try {
            return CategoriaParametro.valueOf(texto.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Categoría no válida. Use: "
                    + Arrays.toString(CategoriaParametro.values()));
        }
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
        server.createContext("/parametros", new ParametroHttpHandler(
                new ParametroServiceImpl(),
                new AutenticacionServiceImpl(),
                new RolesServiceImpl(new RepositorioRolesEnMemoria())));
        return server;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = crearServidor(Integer.getInteger("parametros.puerto", 8084));
        server.start();
        System.out.println("Parámetros: http://127.0.0.1:"
                + server.getAddress().getPort() + "/parametros");
    }
}
