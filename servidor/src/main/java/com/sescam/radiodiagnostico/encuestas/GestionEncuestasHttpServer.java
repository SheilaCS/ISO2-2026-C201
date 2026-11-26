package com.sescam.radiodiagnostico.encuestas;

import com.sescam.radiodiagnostico.autenticacion.*;
import com.sescam.radiodiagnostico.historial.IVinculoPaciente;
import com.sescam.radiodiagnostico.historial.VinculoPacienteEnMemoria;
import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GestionEncuestasHttpServer {
    public static HttpServer crearServidor(int puerto) throws IOException {
        return crearServidor(puerto, new GestionEncuestasServiceImpl(),
                new AutenticacionServiceImpl(), new VinculoPacienteEnMemoria());
    }

    public static HttpServer crearServidor(int puerto, IGestionEncuestasService servicio,
            IAutenticacionService autenticacion, IVinculoPaciente vinculo) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", puerto), 0);
        servidor.createContext("/encuestas", e -> {
            try {
                String ruta = e.getRequestURI().getPath();
                if (!ruta.equals("/encuestas/incidencia") && !ruta.equals("/encuestas/responder")) {
                    responder(e, 404, "Ruta no encontrada"); return;
                }
                if (!e.getRequestMethod().equals("POST")) {
                    responder(e, 405, "Metodo no permitido"); return;
                }
                Optional<Sesion> sesion = autenticar(e, autenticacion);
                if (sesion.isEmpty()) { responder(e, 401, "Credenciales incorrectas"); return; }
                if (sesion.get().rol() != Rol.PACIENTE) { responder(e, 403, "Solo pacientes"); return; }
                var paciente = vinculo.idPacienteDe(sesion.get().identificador());
                if (paciente.isEmpty()) { responder(e, 403, "Paciente no vinculado"); return; }
                String tipo = e.getRequestHeaders().getFirst("Content-Type");
                if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                    responder(e, 415, "Se requiere formulario"); return;
                }
                byte[] cuerpo = e.getRequestBody().readNBytes(8193);
                if (cuerpo.length > 8192) { responder(e, 413, "Peticion demasiado grande"); return; }
                Map<String,String> datos = leerParametros(new String(cuerpo, StandardCharsets.UTF_8));
                int idPaciente = Integer.parseInt(paciente.get());
                int idCita = Integer.parseInt(datos.get("idCita"));
                ResultadoOperacion resultado;
                if (ruta.equals("/encuestas/incidencia")) {
                    resultado = servicio.comunicarIncidencia(idPaciente, idCita, datos.get("descripcion"));
                } else {
                    resultado = servicio.responderEncuesta(idPaciente, idCita,
                            Integer.parseInt(datos.get("puntuacion")), datos.get("comentario"));
                }
                responder(e, resultado.isCorrecto() ? 200 : 400, resultado.getMensaje());
            } catch (IllegalArgumentException ex) {
                responder(e, 400, "Datos incorrectos");
            } catch (IllegalStateException ex) {
                responder(e, 503, "Servicio dependiente no disponible");
            } finally { e.close(); }
        });
        return servidor;
    }

    private static Optional<Sesion> autenticar(HttpExchange e, IAutenticacionService servicio) {
        String cabecera = e.getRequestHeaders().getFirst("Authorization");
        if (cabecera == null || !cabecera.startsWith("Basic ")) return Optional.empty();
        char[] clave = null;
        try {
            String texto = new String(Base64.getDecoder().decode(cabecera.substring(6)), StandardCharsets.UTF_8);
            int separador = texto.indexOf(':');
            if (separador <= 0) return Optional.empty();
            clave = texto.substring(separador + 1).toCharArray();
            return servicio.iniciarSesion(texto.substring(0, separador), clave);
        } catch (IllegalArgumentException ex) { return Optional.empty(); }
        finally { if (clave != null) Arrays.fill(clave, '\0'); }
    }

    private static Map<String,String> leerParametros(String texto) {
        Map<String,String> datos = new HashMap<>();
        for (String parte : texto.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length != 2) throw new IllegalArgumentException();
            String clave = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
            if (datos.putIfAbsent(clave, URLDecoder.decode(par[1], StandardCharsets.UTF_8)) != null)
                throw new IllegalArgumentException();
        }
        return datos;
    }

    private static void responder(HttpExchange e, int codigo, String mensaje) throws IOException {
        byte[] bytes = mensaje.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.sendResponseHeaders(codigo, bytes.length);
        try (var salida = e.getResponseBody()) { salida.write(bytes); }
    }

    public static void main(String[] args) throws IOException {
        HttpServer servidor = crearServidor(Integer.getInteger("encuestas.puerto", 8090));
        servidor.start();
        System.out.println("Encuestas: puerto " + servidor.getAddress().getPort());
    }
}
