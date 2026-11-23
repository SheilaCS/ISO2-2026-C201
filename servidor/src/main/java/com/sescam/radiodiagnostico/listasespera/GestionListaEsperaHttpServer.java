package com.sescam.radiodiagnostico.listasespera;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * SERVIDOR HTTP DE LISTAS DE ESPERA Y HUECOS.
 * Iteracion 15 - RF15.
 *
 * Todas las operaciones exigen credenciales Basic de un usuario
 * ADMINISTRATIVO (401 sin credenciales validas, 403 con otro rol).
 *
 * GET  /listaespera            lista de espera: id;paciente;tipo;prioridad;fecha
 * GET  /listaespera/huecos     huecos libres: medico;fecha;inicio;fin
 *                              (parametro opcional fecha=AAAA-MM-DD)
 * GET  /listaespera/propuesta  propuesta: los 5 campos de la solicitud y
 *                              los 4 del hueco (cuerpo vacio si no hay)
 * POST /listaespera/prioridad  cuerpo solicitud=ID&prioridad=PRIORIDAD
 * POST /listaespera/retirar    cuerpo solicitud=ID
 * POST /listaespera/asignar    cuerpo solicitud=ID&medico=ID&fecha=F&hora=HH:MM
 *
 * Las operaciones POST responden 200 con "mensaje=texto" o 409 con
 * "error=texto" si la operacion se rechaza. Otros rechazos: 400 datos
 * incorrectos, 404 ruta, 405 metodo, 500 error interno. Todos los
 * errores devuelven "error=mensaje".
 */
public class GestionListaEsperaHttpServer {

    private static final int MAXIMO_CUERPO = 4096;

    public static HttpServer crearServidor(int puerto) throws IOException {
        ProveedorListaEsperaEnMemoria proveedor =
                new ProveedorListaEsperaEnMemoria();
        return crearServidor(
                puerto,
                new ListaEsperaServiceImpl(proveedor, proveedor,
                        (solicitud, paciente, fecha, hora) -> {
                            throw new IllegalStateException(
                                    "Modulo de citas no conectado");
                        }),
                new AutenticacionServiceImpl());
    }

    public static HttpServer crearServidor(
            int puerto,
            IListaEsperaService servicio,
            IAutenticacionService autenticacion) throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0);

        servidor.createContext("/listaespera", peticion -> {
            try {
                atender(peticion, servicio, autenticacion);
            } catch (IllegalArgumentException e) {
                responderError(peticion, 400,
                        "Datos incorrectos o incompletos.");
            } catch (RuntimeException e) {
                responderError(peticion, 500, "Error interno del servidor.");
            } finally {
                peticion.close();
            }
        });

        return servidor;
    }

    private static void atender(
            HttpExchange peticion,
            IListaEsperaService servicio,
            IAutenticacionService autenticacion) throws IOException {

        Sesion sesion = autenticar(peticion, autenticacion).orElse(null);

        if (sesion == null) {
            peticion.getResponseHeaders().set("WWW-Authenticate",
                    "Basic realm=\"listaespera\", charset=\"UTF-8\"");
            responderError(peticion, 401, "Credenciales incorrectas.");
            return;
        }

        if (sesion.rol() != Rol.ADMINISTRATIVO) {
            responderError(peticion, 403,
                    "Solo el personal administrativo puede gestionar "
                    + "la lista de espera.");
            return;
        }

        switch (peticion.getRequestURI().getPath()) {
            case "/listaespera" -> {
                if (exigirMetodo(peticion, "GET")) {
                    listaEspera(peticion, servicio);
                }
            }
            case "/listaespera/huecos" -> {
                if (exigirMetodo(peticion, "GET")) {
                    huecos(peticion, servicio);
                }
            }
            case "/listaespera/propuesta" -> {
                if (exigirMetodo(peticion, "GET")) {
                    propuesta(peticion, servicio);
                }
            }
            case "/listaespera/prioridad" -> {
                if (exigirMetodo(peticion, "POST")) {
                    cambiarPrioridad(peticion, servicio);
                }
            }
            case "/listaespera/retirar" -> {
                if (exigirMetodo(peticion, "POST")) {
                    retirar(peticion, servicio);
                }
            }
            case "/listaespera/asignar" -> {
                if (exigirMetodo(peticion, "POST")) {
                    asignar(peticion, servicio);
                }
            }
            default -> responderError(peticion, 404, "Ruta no encontrada.");
        }
    }

    // ------------------------------------
    // OPERACIONES
    // ------------------------------------

    private static void listaEspera(
            HttpExchange peticion,
            IListaEsperaService servicio) throws IOException {

        StringJoiner lineas = new StringJoiner("\n");
        for (SolicitudEnEspera s : servicio.consultarListaEspera()) {
            lineas.add(lineaSolicitud(s));
        }

        enviar(peticion, 200, "text/plain; charset=UTF-8", lineas.toString());
    }

    private static void huecos(
            HttpExchange peticion,
            IListaEsperaService servicio) throws IOException {

        Map<String, String> parametros =
                leerParametros(peticion.getRequestURI().getRawQuery(),
                        Set.of(), Set.of("fecha"));

        LocalDate fecha = parametros.containsKey("fecha")
                ? leerFecha(parametros.get("fecha"))
                : null;

        StringJoiner lineas = new StringJoiner("\n");
        for (Hueco h : servicio.consultarHuecos(fecha)) {
            lineas.add(lineaHueco(h));
        }

        enviar(peticion, 200, "text/plain; charset=UTF-8", lineas.toString());
    }

    private static void propuesta(
            HttpExchange peticion,
            IListaEsperaService servicio) throws IOException {

        Optional<Propuesta> propuesta = servicio.proponerAsignacion();

        String cuerpo = propuesta
                .map(p -> lineaSolicitud(p.solicitud()) + ";"
                        + lineaHueco(p.hueco()))
                .orElse("");

        enviar(peticion, 200, "text/plain; charset=UTF-8", cuerpo);
    }

    private static void cambiarPrioridad(
            HttpExchange peticion,
            IListaEsperaService servicio) throws IOException {

        Map<String, String> parametros = leerParametros(
                leerCuerpo(peticion), Set.of("solicitud", "prioridad"),
                Set.of());

        int solicitud = leerEntero(parametros.get("solicitud"));
        Prioridad prioridad = Prioridad.desde(parametros.get("prioridad"))
                .orElseThrow(IllegalArgumentException::new);

        responderOperacion(peticion,
                servicio.cambiarPrioridad(solicitud, prioridad));
    }

    private static void retirar(
            HttpExchange peticion,
            IListaEsperaService servicio) throws IOException {

        Map<String, String> parametros = leerParametros(
                leerCuerpo(peticion), Set.of("solicitud"), Set.of());

        responderOperacion(peticion, servicio.retirarDeLista(
                leerEntero(parametros.get("solicitud"))));
    }

    private static void asignar(
            HttpExchange peticion,
            IListaEsperaService servicio) throws IOException {

        Map<String, String> parametros = leerParametros(
                leerCuerpo(peticion),
                Set.of("solicitud", "medico", "fecha", "hora"), Set.of());

        responderOperacion(peticion, servicio.asignarHueco(
                leerEntero(parametros.get("solicitud")),
                leerEntero(parametros.get("medico")),
                leerFecha(parametros.get("fecha")),
                leerHora(parametros.get("hora"))));
    }

    // ------------------------------------
    // LECTURA DE DATOS
    // ------------------------------------

    private static boolean exigirMetodo(HttpExchange peticion, String metodo)
            throws IOException {

        if (peticion.getRequestMethod().equals(metodo)) {
            return true;
        }

        peticion.getResponseHeaders().set("Allow", metodo);
        responderError(peticion, 405, "Metodo no permitido.");
        return false;
    }

    private static String leerCuerpo(HttpExchange peticion)
            throws IOException {

        byte[] contenido = peticion.getRequestBody()
                .readNBytes(MAXIMO_CUERPO + 1);

        if (contenido.length > MAXIMO_CUERPO) {
            throw new IllegalArgumentException();
        }

        return new String(contenido, StandardCharsets.UTF_8);
    }

    // Lee parametros clave=valor. Exige todos los obligatorios, admite
    // solo los opcionales indicados y rechaza repetidos o mal formados.
    private static Map<String, String> leerParametros(
            String texto,
            Set<String> obligatorios,
            Set<String> opcionales) {

        Map<String, String> parametros = new HashMap<>();

        if (texto != null && !texto.isEmpty()) {
            for (String parte : texto.split("&", -1)) {
                String[] par = parte.split("=", 2);
                if (par.length != 2) {
                    throw new IllegalArgumentException();
                }

                String clave = URLDecoder.decode(par[0],
                        StandardCharsets.UTF_8);
                String valor = URLDecoder.decode(par[1],
                        StandardCharsets.UTF_8);

                boolean permitido = obligatorios.contains(clave)
                        || opcionales.contains(clave);
                if (!permitido || parametros.put(clave, valor) != null) {
                    throw new IllegalArgumentException();
                }
            }
        }

        if (!parametros.keySet().containsAll(obligatorios)) {
            throw new IllegalArgumentException();
        }

        return parametros;
    }

    private static int leerEntero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static LocalTime leerHora(String texto) {
        try {
            return LocalTime.parse(texto.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
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

    // ------------------------------------
    // RESPUESTAS
    // ------------------------------------

    private static String lineaSolicitud(SolicitudEnEspera s) {
        return String.join(";",
                String.valueOf(s.idSolicitud()),
                String.valueOf(s.idPaciente()),
                limpiar(s.tipoPrueba()),
                s.prioridad().name(),
                s.fechaSolicitud().toString());
    }

    private static String lineaHueco(Hueco h) {
        return String.join(";",
                String.valueOf(h.idMedico()),
                h.fecha().toString(),
                h.inicio().toString(),
                h.fin().toString());
    }

    // Evita que un ";" o un salto de linea rompa el formato de linea
    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replace(';', ',')
                .replace('\n', ' ').replace('\r', ' ');
    }

    private static void responderOperacion(
            HttpExchange peticion,
            ResultadoOperacion resultado) throws IOException {

        if (resultado.isCorrecto()) {
            enviar(peticion, 200,
                    "application/x-www-form-urlencoded; charset=UTF-8",
                    "mensaje=" + URLEncoder.encode(resultado.getMensaje(),
                            StandardCharsets.UTF_8));
        } else {
            responderError(peticion, 409, resultado.getMensaje());
        }
    }

    private static void responderError(
            HttpExchange peticion,
            int codigo,
            String mensaje) throws IOException {

        enviar(peticion, codigo,
                "application/x-www-form-urlencoded; charset=UTF-8",
                "error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
    }

    private static void enviar(
            HttpExchange peticion,
            int codigo,
            String tipo,
            String cuerpo) throws IOException {

        byte[] contenido = cuerpo.getBytes(StandardCharsets.UTF_8);

        peticion.getResponseHeaders().set("Content-Type", tipo);
        peticion.getResponseHeaders().set("Cache-Control", "no-store");
        peticion.sendResponseHeaders(codigo, contenido.length);

        try (var salida = peticion.getResponseBody()) {
            salida.write(contenido);
        }
    }

    public static void main(String[] args) throws IOException {
        int puerto = Integer.getInteger("listaespera.puerto", 8086);
        HttpServer servidor = crearServidor(puerto);
        servidor.start();
        System.out.println(
                "Lista de espera: http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/listaespera");
    }
}
