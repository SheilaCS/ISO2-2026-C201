package com.sescam.radiodiagnostico.realizacion;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class RealizacionHttpServer {

    public static HttpServer crearServidor(int puerto) throws IOException {
        return crearServidor(
                puerto,
                new RealizacionServiceImpl(new ProveedorRealizacionHttp()),
                new AutenticacionServiceImpl()
        );
    }

    public static HttpServer crearServidor(
            int puerto,
            IRealizacionService servicio,
            IAutenticacionService autenticacion) throws IOException {

        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);

        servidor.createContext("/realizaciones", peticion -> {
            try {
                Optional<Sesion> sesion = autenticar(peticion, autenticacion);

                if (sesion.isEmpty()) {
                    peticion.getResponseHeaders().set(
                            "WWW-Authenticate",
                            "Basic realm=\"realizaciones\", charset=\"UTF-8\"");
                    responder(peticion, 401, Map.of("error", "Credenciales incorrectas."));
                } else if (sesion.get().rol() != Rol.MEDICO) {
                    responder(peticion, 403, Map.of("error", "Acceso reservado a medicos."));
                } else {
                    atender(peticion, servicio);
                }
            } catch (IllegalStateException e) {
                responder(peticion, 409, Map.of("error", e.getMessage()));
            } catch (DateTimeParseException | IllegalArgumentException e) {
                responder(peticion, 400, Map.of("error", "Datos incorrectos o incompletos."));
            } catch (RuntimeException e) {
                responder(peticion, 500, Map.of("error", "Error interno del servidor."));
            } finally {
                peticion.close();
            }
        });

        return servidor;
    }

    private static void atender(HttpExchange peticion, IRealizacionService servicio) throws IOException {
        String ruta = peticion.getRequestURI().getPath();
        String metodo = peticion.getRequestMethod();

        if (!"/realizaciones".equals(ruta)) {
            responder(peticion, 404, Map.of("error", "Ruta no encontrada."));
            return;
        }

        if (!"GET".equals(metodo) && !"POST".equals(metodo)) {
            peticion.getResponseHeaders().set("Allow", "GET, POST");
            responder(peticion, 405, Map.of("error", "Metodo no permitido."));
            return;
        }

        if ("GET".equals(metodo)) {
            Map<String, String> query = leerParametros(peticion.getRequestURI().getRawQuery());
            String idCitaRaw = query.get("idCita");
            if (idCitaRaw == null || idCitaRaw.isBlank()) {
                List<RealizacionPrueba> lista = servicio.listarRealizaciones();
                StringJoiner sj = new StringJoiner("\n");
                for (RealizacionPrueba r : lista) {
                    sj.add(formatearLinea(r));
                }
                responderTexto(peticion, 200, sj.toString());
                return;
            }
            int idCita = Integer.parseInt(idCitaRaw.trim());
            Optional<RealizacionPrueba> encontrada = servicio.consultarPorCita(idCita);
            if (encontrada.isEmpty()) {
                responder(peticion, 404, Map.of("error", "No existe realizacion para esa cita."));
                return;
            }
            responder(peticion, 200, aMapa(encontrada.get()));
            return;
        }

        String tipo = peticion.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim()
                .equalsIgnoreCase("application/x-www-form-urlencoded")) {
            responder(peticion, 415, Map.of("error", "Se requieren datos de formulario."));
            return;
        }

        byte[] cuerpo = peticion.getRequestBody().readNBytes(16_385);
        if (cuerpo.length > 16_384) {
            responder(peticion, 413, Map.of("error", "Peticion demasiado grande."));
            return;
        }

        Map<String, String> datos = leerParametros(new String(cuerpo, StandardCharsets.UTF_8));
        int idCita = Integer.parseInt(obligatorio(datos, "idCita"));
        int idSolicitud = Integer.parseInt(obligatorio(datos, "idSolicitud"));
        LocalDate fecha = LocalDate.parse(obligatorio(datos, "fechaRealizacion"));
        String observaciones = datos.getOrDefault("observaciones", "");
        String incidencia = datos.getOrDefault("incidencia", "");

        RealizacionPrueba creada = servicio.registrarRealizacion(
                idCita, idSolicitud, fecha, observaciones, incidencia);
        responder(peticion, 201, aMapa(creada));
    }

    private static Map<String, String> aMapa(RealizacionPrueba r) {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("id", r.id().toString());
        mapa.put("idCita", Integer.toString(r.idCita()));
        mapa.put("idSolicitud", Integer.toString(r.idSolicitud()));
        mapa.put("fechaRealizacion", r.fechaRealizacion().toString());
        mapa.put("observaciones", r.observaciones());
        mapa.put("incidencia", r.incidencia());
        mapa.put("estado", r.estado());
        return mapa;
    }

    private static String formatearLinea(RealizacionPrueba r) {
        return "id=" + r.id()
                + "&idCita=" + r.idCita()
                + "&idSolicitud=" + r.idSolicitud()
                + "&fechaRealizacion=" + r.fechaRealizacion()
                + "&observaciones=" + URLEncoder.encode(r.observaciones(), StandardCharsets.UTF_8)
                + "&incidencia=" + URLEncoder.encode(r.incidencia(), StandardCharsets.UTF_8)
                + "&estado=" + r.estado();
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

    private static String obligatorio(Map<String, String> datos, String campo) {
        String valor = datos.get(campo);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException();
        }
        return valor.trim();
    }

    private static void responder(HttpExchange peticion, int codigo, Map<String, String> datos) throws IOException {
        StringJoiner respuesta = new StringJoiner("&");
        datos.forEach((clave, valor) -> respuesta.add(
                URLEncoder.encode(clave, StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(valor, StandardCharsets.UTF_8)));
        responderTexto(peticion, codigo, respuesta.toString());
    }

    private static void responderTexto(HttpExchange peticion, int codigo, String texto) throws IOException {
        byte[] contenido = texto.getBytes(StandardCharsets.UTF_8);
        peticion.getResponseHeaders().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        peticion.getResponseHeaders().set("Cache-Control", "no-store");
        peticion.sendResponseHeaders(codigo, contenido.length);
        try (var salida = peticion.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public static void main(String[] args) throws IOException {
        int puerto = Integer.getInteger("realizacion.puerto", 8086);
        HttpServer servidor = crearServidor(puerto);
        servidor.start();
        System.out.println("Realizacion: http://127.0.0.1:" + servidor.getAddress().getPort() + "/realizaciones");
    }
}