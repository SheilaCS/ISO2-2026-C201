package com.sescam.radiodiagnostico.roles;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP de la gestión de roles y permisos (CDU10, RF18).
 * Solo pueden usarlo los usuarios con el permiso GESTIONAR_ROLES.
 */
public class GestionRolesHttpServer {

    private static final String USUARIOS = "/roles/usuarios";
    private static final String PERMISOS = "/roles/permisos";

    private GestionRolesHttpServer() {
    }

    public static HttpServer crearServidor(int puerto) throws IOException {
        return crearServidor(
                puerto,
                new RolesServiceImpl(new RepositorioRolesEnMemoria()),
                new AutenticacionServiceImpl());
    }

    public static HttpServer crearServidor(
            int puerto,
            IRolesService servicio,
            IAutenticacionService autenticacion) throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0);

        servidor.createContext("/roles", peticion -> {
            try {
                Optional<Sesion> sesion = autenticar(peticion, autenticacion);

                if (sesion.isEmpty()) {
                    peticion.getResponseHeaders().set(
                            "WWW-Authenticate",
                            "Basic realm=\"roles\", charset=\"UTF-8\"");
                    responder(peticion, 401,
                            Map.of("error", "Credenciales incorrectas."));
                } else if (!servicio.tienePermiso(
                        sesion.get().identificador(), Permiso.GESTIONAR_ROLES)) {
                    responder(peticion, 403,
                            Map.of("error", "Acceso reservado a la gestión de roles."));
                } else {
                    atender(peticion, servicio, sesion.get().identificador());
                }
            } catch (RolesException e) {
                int codigo = switch (e.getCodigo()) {
                    case DATOS_INVALIDOS -> 400;
                    case USUARIO_NO_ENCONTRADO -> 404;
                    case OPERACION_NO_PERMITIDA -> 409;
                };
                responder(peticion, codigo, Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) {
                responder(peticion, 400,
                        Map.of("error", "Datos incorrectos o incompletos."));
            } catch (RuntimeException e) {
                responder(peticion, 500,
                        Map.of("error", "Error interno del servidor."));
            } finally {
                peticion.close();
            }
        });

        return servidor;
    }

    private static void atender(
            HttpExchange peticion,
            IRolesService servicio,
            String administrador) throws IOException {

        String ruta = peticion.getRequestURI().getPath();
        String metodo = peticion.getRequestMethod();
        String[] partes = ruta.split("/");

        // /roles/usuarios  y  /roles/usuarios/{usuario}
        if (ruta.equals(USUARIOS) && metodo.equals("GET")) {
            Map<String, String> datos = leerParametros(
                    peticion.getRequestURI().getRawQuery());
            String usuario = datos.get("usuario");

            if (usuario == null) {
                responder(peticion, 200, listarUsuarios(servicio));
            } else {
                Rol rol = servicio.consultarRol(usuario);
                responder(peticion, 200, datosUsuario(usuario.strip(), rol));
            }
            return;
        }

        if (ruta.startsWith(USUARIOS + "/") && partes.length == 4
                && metodo.equals("PUT")) {
            String usuario = URLDecoder.decode(partes[3], StandardCharsets.UTF_8);
            Map<String, String> datos = leerFormulario(peticion);
            Rol rol = leerRol(obligatorio(datos, "rol"));

            servicio.cambiarRol(administrador, usuario, rol);
            responder(peticion, 200, datosUsuario(usuario.strip(), rol));
            return;
        }

        // /roles/permisos  y  /roles/permisos/{rol}/{permiso}
        if (ruta.equals(PERMISOS) && metodo.equals("GET")) {
            Map<String, String> datos = leerParametros(
                    peticion.getRequestURI().getRawQuery());
            Rol rol = leerRol(obligatorio(datos, "rol"));

            responder(peticion, 200, datosPermisos(rol, servicio.consultarPermisos(rol)));
            return;
        }

        if (ruta.startsWith(PERMISOS + "/") && partes.length == 5
                && (metodo.equals("POST") || metodo.equals("DELETE"))) {
            Rol rol = leerRol(partes[3]);
            Permiso permiso = Permiso.valueOf(
                    partes[4].strip().toUpperCase(Locale.ROOT));

            Set<Permiso> permisos = metodo.equals("POST")
                    ? servicio.concederPermiso(rol, permiso)
                    : servicio.retirarPermiso(rol, permiso);

            responder(peticion, 200, datosPermisos(rol, permisos));
            return;
        }

        responder(peticion, 404, Map.of("error", "Ruta o método no encontrados."));
    }

    private static Map<String, String> listarUsuarios(IRolesService servicio) {
        Map<String, String> datos = new TreeMap<>();
        servicio.listarUsuarios().forEach(
                (usuario, rol) -> datos.put(usuario, rol.name()));
        return datos;
    }

    private static Map<String, String> datosUsuario(String usuario, Rol rol) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("usuario", usuario);
        datos.put("rol", rol.name());
        return datos;
    }

    private static Map<String, String> datosPermisos(Rol rol, Set<Permiso> permisos) {
        StringJoiner lista = new StringJoiner(",");
        permisos.stream().sorted().forEach(permiso -> lista.add(permiso.name()));

        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("rol", rol.name());
        datos.put("permisos", lista.toString());
        return datos;
    }

    private static Rol leerRol(String texto) {
        return Rol.valueOf(texto.strip().toUpperCase(Locale.ROOT));
    }

    private static Optional<Sesion> autenticar(
            HttpExchange peticion,
            IAutenticacionService autenticacion) {

        String cabecera = peticion.getRequestHeaders().getFirst("Authorization");
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

    private static Map<String, String> leerFormulario(HttpExchange peticion)
            throws IOException {
        String tipo = peticion.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim()
                .equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Se requieren datos de formulario.");
        }
        byte[] cuerpo = peticion.getRequestBody().readNBytes(16_384);
        return leerParametros(new String(cuerpo, StandardCharsets.UTF_8));
    }

    private static Map<String, String> leerParametros(String texto) {
        Map<String, String> datos = new HashMap<>();
        if (texto == null || texto.isEmpty()) {
            return datos;
        }
        for (String parte : texto.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length != 2) {
                throw new IllegalArgumentException("Parámetro mal formado.");
            }
            datos.put(
                    URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(par[1], StandardCharsets.UTF_8));
        }
        return datos;
    }

    private static String obligatorio(Map<String, String> datos, String campo) {
        String valor = datos.get(campo);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el campo " + campo + ".");
        }
        return valor;
    }

    private static void responder(
            HttpExchange peticion,
            int codigo,
            Map<String, String> datos) throws IOException {

        StringJoiner respuesta = new StringJoiner("&");
        datos.forEach((clave, valor) -> respuesta.add(
                URLEncoder.encode(clave, StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(valor, StandardCharsets.UTF_8)));

        byte[] contenido = respuesta.toString().getBytes(StandardCharsets.UTF_8);
        peticion.getResponseHeaders().set("Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8");
        peticion.getResponseHeaders().set("Cache-Control", "no-store");
        peticion.sendResponseHeaders(codigo, contenido.length);
        try (var salida = peticion.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public static void main(String[] args) throws IOException {
        int puerto = Integer.getInteger("roles.puerto", 8091);
        HttpServer servidor = crearServidor(puerto);
        servidor.start();
        System.out.println("Roles: http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/roles");
    }
}
