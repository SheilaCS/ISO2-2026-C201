package com.sescam.radiodiagnostico.pacientes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class GestionPacientesHttpServer {

    public static HttpServer crearServidor(int puerto)
            throws IOException {

        return crearServidor(
                puerto,
                new PacientesServiceImpl(
                        new RepositorioPacientesEnMemoria()),
                new AutenticacionServiceImpl()
        );
    }

    public static HttpServer crearServidor(
            int puerto,
            IPacientesService servicio,
            IAutenticacionService autenticacion)
            throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0);

        servidor.createContext("/pacientes", peticion -> {
            try {
                Optional<Sesion> sesion = autenticar(
                        peticion, autenticacion);

                if (sesion.isEmpty()) {
                    peticion.getResponseHeaders().set(
                            "WWW-Authenticate",
                            "Basic realm=\"pacientes\", charset=\"UTF-8\"");
                    responder(peticion, 401,
                            Map.of("error", "Credenciales incorrectas."));
                } else if (sesion.get().rol() != Rol.ADMINISTRATIVO) {
                    responder(peticion, 403,
                            Map.of("error", "Acceso reservado a administrativos."));
                } else {
                    atender(peticion, servicio);
                }

            } catch (PacienteException e) {
                int codigo = switch (e.getCodigo()) {
                    case DATOS_INVALIDOS -> 400;
                    case PACIENTE_NO_ENCONTRADO -> 404;
                    case CIP_DUPLICADO, PACIENTE_INACTIVO -> 409;
                };
                responder(peticion, codigo,
                        Map.of("error", e.getMessage()));

            } catch (DateTimeParseException | IllegalArgumentException e) {
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
            IPacientesService servicio) throws IOException {

        String ruta = peticion.getRequestURI().getPath();
        String metodo = peticion.getRequestMethod();

        boolean coleccion = ruta.equals("/pacientes");
        boolean individual = ruta.startsWith("/pacientes/")
                && ruta.substring(11).matches("[^/]+");

        if (!coleccion && !individual) {
            responder(peticion, 404,
                    Map.of("error", "Ruta no encontrada."));
            return;
        }

        String permitidos = coleccion ? "GET, POST" : "PUT, DELETE";

        if (!(coleccion && (metodo.equals("GET") || metodo.equals("POST")))
                && !(individual
                && (metodo.equals("PUT") || metodo.equals("DELETE")))) {

            peticion.getResponseHeaders().set("Allow", permitidos);
            responder(peticion, 405,
                    Map.of("error", "Metodo no permitido."));
            return;
        }

        Map<String, String> datos = new HashMap<>();

        if (metodo.equals("GET")) {
            datos = leerParametros(
                    peticion.getRequestURI().getRawQuery());
        } else if (metodo.equals("POST") || metodo.equals("PUT")) {
            String tipo = peticion.getRequestHeaders()
                    .getFirst("Content-Type");

            if (tipo == null || !tipo.split(";", 2)[0].trim()
                    .equalsIgnoreCase("application/x-www-form-urlencoded")) {
                responder(peticion, 415,
                        Map.of("error", "Se requieren datos de formulario."));
                return;
            }

            byte[] cuerpo = peticion.getRequestBody().readNBytes(16_385);
            if (cuerpo.length > 16_384) {
                responder(peticion, 413,
                        Map.of("error", "Peticion demasiado grande."));
                return;
            }
            datos = leerParametros(
                    new String(cuerpo, StandardCharsets.UTF_8));
        }

        Paciente resultado;
        int codigo = 200;

        if (metodo.equals("GET")) {
            resultado = servicio.consultarPorCip(obligatorio(datos, "cip"))
                    .orElseThrow(() -> new PacienteException(
                            PacienteException.Codigo.PACIENTE_NO_ENCONTRADO,
                            "No existe un paciente con ese CIP."));

        } else if (metodo.equals("POST")) {
            comprobarCampos(datos,
                    Set.of("cip", "nombre", "apellidos", "fechaNacimiento"));

            resultado = servicio.altaPaciente(
                    obligatorio(datos, "cip"),
                    obligatorio(datos, "nombre"),
                    obligatorio(datos, "apellidos"),
                    LocalDate.parse(obligatorio(datos, "fechaNacimiento")));
            codigo = 201;

        } else {
            UUID id = UUID.fromString(ruta.substring(11));

            if (metodo.equals("DELETE")) {
                servicio.bajaPaciente(id);
                peticion.sendResponseHeaders(204, -1);
                return;
            }

            comprobarCampos(datos,
                    Set.of("nombre", "apellidos", "fechaNacimiento"));

            resultado = servicio.modificarPaciente(
                    id,
                    obligatorio(datos, "nombre"),
                    obligatorio(datos, "apellidos"),
                    LocalDate.parse(obligatorio(datos, "fechaNacimiento")));
        }

        responder(peticion, codigo, Map.of(
                "id", resultado.id().toString(),
                "cip", resultado.cip(),
                "nombre", resultado.nombre(),
                "apellidos", resultado.apellidos(),
                "fechaNacimiento", resultado.fechaNacimiento().toString(),
                "activo", Boolean.toString(resultado.activo())
        ));
    }

    private static Optional<Sesion> autenticar(
            HttpExchange peticion,
            IAutenticacionService autenticacion) {

        String cabecera = peticion.getRequestHeaders()
                .getFirst("Authorization");

        if (cabecera == null
                || !cabecera.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }

        byte[] contenido;
        try {
            contenido = Base64.getDecoder()
                    .decode(cabecera.substring(6).trim());
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
            return autenticacion.iniciarSesion(
                    texto.substring(0, separador), clave);
        } finally {
            Arrays.fill(contenido, (byte) 0);
            if (clave != null) {
                Arrays.fill(clave, '\0');
            }
        }
    }

    private static Map<String, String> leerParametros(String texto) {
        Map<String, String> datos = new HashMap<>();
        if (texto == null || texto.isEmpty()) {
            return datos;
        }

        for (String parte : texto.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length != 2) {
                throw new IllegalArgumentException();
            }
            String clave = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par[1], StandardCharsets.UTF_8);
            if (datos.putIfAbsent(clave, valor) != null) {
                throw new IllegalArgumentException();
            }
        }
        return datos;
    }

    private static String obligatorio(
            Map<String, String> datos, String campo) {

        String valor = datos.get(campo);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException();
        }
        return valor;
    }

    private static void comprobarCampos(
            Map<String, String> datos, Set<String> permitidos) {

        if (!permitidos.containsAll(datos.keySet())) {
            throw new IllegalArgumentException();
        }
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

        byte[] contenido = respuesta.toString()
                .getBytes(StandardCharsets.UTF_8);

        peticion.getResponseHeaders().set("Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8");
        peticion.getResponseHeaders().set("Cache-Control", "no-store");
        peticion.sendResponseHeaders(codigo, contenido.length);

        try (var salida = peticion.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public static void main(String[] args) throws IOException {
        int puerto = Integer.getInteger("pacientes.puerto", 8092);
        HttpServer servidor = crearServidor(puerto);
        servidor.start();
        System.out.println(
                "Pacientes: http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/pacientes");
    }
}