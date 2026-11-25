package com.sescam.radiodiagnostico.historial;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * SERVIDOR HTTP DEL HISTORIAL DE PRUEBAS.
 * Iteracion 12 - RF04.
 *
 * Operacion: GET /historial?paciente=ID, con credenciales Basic.
 * Respuesta correcta (200): una prueba por linea con los campos
 * id;fecha;tipo;estado;informe (cuerpo vacio si no hay pruebas).
 * Rechazos: 400 datos incorrectos, 401 sin credenciales validas,
 * 403 rol sin permiso, 405 metodo no permitido, 503 origen caido.
 * Los rechazos devuelven "error=mensaje".
 */
public class GestionHistorialHttpServer {

    public static HttpServer crearServidor(int puerto) throws IOException {
        return crearServidor(
                puerto,
                new HistorialServiceImpl(
                        new ProveedorHistorialRealizacion(),
                        new RegistroConsultas(),
                        new VinculoPacienteEnMemoria()),
                new AutenticacionServiceImpl());
    }

    public static HttpServer crearServidor(
            int puerto,
            IHistorialService servicio,
            IAutenticacionService autenticacion) throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", puerto), 0);

        servidor.createContext("/historial", peticion -> {
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
            IHistorialService servicio,
            IAutenticacionService autenticacion) throws IOException {

        Sesion sesion = autenticar(peticion, autenticacion).orElse(null);

        if (sesion == null) {
            peticion.getResponseHeaders().set("WWW-Authenticate",
                    "Basic realm=\"historial\", charset=\"UTF-8\"");
            responderError(peticion, 401, "Credenciales incorrectas.");
            return;
        }

        if (!peticion.getRequestURI().getPath().equals("/historial")) {
            responderError(peticion, 404, "Ruta no encontrada.");
            return;
        }

        if (!peticion.getRequestMethod().equals("GET")) {
            peticion.getResponseHeaders().set("Allow", "GET");
            responderError(peticion, 405, "Metodo no permitido.");
            return;
        }

        String idPaciente = leerPaciente(peticion.getRequestURI().getRawQuery());
        ResultadoHistorial resultado =
                servicio.consultarHistorial(sesion, idPaciente);

        switch (resultado.estado()) {
            case CORRECTO -> responderPruebas(peticion, resultado);
            case NO_AUTENTICADO -> responderError(peticion, 401,
                    resultado.mensaje());
            case PROHIBIDO -> responderError(peticion, 403,
                    resultado.mensaje());
            case DATOS_INVALIDOS -> responderError(peticion, 400,
                    resultado.mensaje());
            case NO_DISPONIBLE -> responderError(peticion, 503,
                    resultado.mensaje());
        }
    }

    // Exige exactamente un parametro: paciente=ID
    private static String leerPaciente(String consulta) {
        if (consulta == null || consulta.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String[] partes = consulta.split("&");
        if (partes.length != 1) {
            throw new IllegalArgumentException();
        }

        String[] par = partes[0].split("=", 2);
        if (par.length != 2
                || !URLDecoder.decode(par[0], StandardCharsets.UTF_8)
                        .equals("paciente")) {
            throw new IllegalArgumentException();
        }

        return URLDecoder.decode(par[1], StandardCharsets.UTF_8);
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

    private static void responderPruebas(
            HttpExchange peticion,
            ResultadoHistorial resultado) throws IOException {

        StringJoiner lineas = new StringJoiner("\n");
        for (PruebaRealizada p : resultado.pruebas()) {
            lineas.add(String.join(";",
                    limpiar(p.id()),
                    p.fecha().toString(),
                    limpiar(p.tipo()),
                    limpiar(p.estado()),
                    limpiar(p.informe())));
        }

        enviar(peticion, 200, "text/plain; charset=UTF-8",
                lineas.toString());
    }

    // Evita que un ";" o un salto de linea rompa el formato de linea
    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replace(';', ',')
                .replace('\n', ' ').replace('\r', ' ');
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
        int puerto = Integer.getInteger("historial.puerto", 8088);
        HttpServer servidor = crearServidor(puerto);
        servidor.start();
        System.out.println(
                "Historial: http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/historial");
    }
}
