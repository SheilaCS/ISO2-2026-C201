package com.sescam.radiodiagnostico.encuestas;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.net.http.*;
import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.historial.VinculoPacienteEnMemoria;
class GestionEncuestasHttpServerTest {
    private HttpServer servidor;
    private String url;
    private final HttpClient cliente = HttpClient.newHttpClient();
    @BeforeEach void setUp() throws Exception {
        var servicio = new GestionEncuestasServiceImpl(new IEncuestasProveedor() {
            public boolean citaPertenecePaciente(int cita, int paciente) { return cita == 12 && paciente == 1; }
            public boolean pruebaRealizada(int cita) { return true; }
        });
        servidor = GestionEncuestasHttpServer.crearServidor(0, servicio,
            new AutenticacionServiceImpl(), new VinculoPacienteEnMemoria("paciente.demo=1"));
        servidor.start(); url = "http://localhost:" + servidor.getAddress().getPort() + "/encuestas";
    }
    @AfterEach void tearDown() { servidor.stop(0); }
    private HttpResponse<String> enviar(String ruta, String metodo, String datos, String usuario, String tipo) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(url + ruta)).timeout(java.time.Duration.ofSeconds(5));
        if (usuario != null) b.header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(usuario.getBytes(StandardCharsets.UTF_8)));
        if (tipo != null) b.header("Content-Type", tipo);
        return cliente.send(b.method(metodo, HttpRequest.BodyPublishers.ofString(datos)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test
    @DisplayName("HTTP 200: Incidencia registrada correctamente")
    void peticion0() throws Exception {
        // Preparar
        String datos = "idCita=12&descripcion=hola";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(200, respuesta.statusCode()), () -> assertEquals("Incidencia registrada correctamente", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 200: Encuesta registrada correctamente")
    void peticion1() throws Exception {
        // Preparar
        String datos = "idCita=12&puntuacion=5&comentario=bien";
        // Ejecutar
        var respuesta = enviar("/responder", "POST", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(200, respuesta.statusCode()), () -> assertEquals("Encuesta registrada correctamente", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 400: La cita no pertenece al paciente")
    void peticion2() throws Exception {
        // Preparar
        String datos = "idCita=13&descripcion=hola";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(400, respuesta.statusCode()), () -> assertEquals("La cita no pertenece al paciente", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 401: Credenciales incorrectas")
    void peticion3() throws Exception {
        // Preparar
        String datos = "idCita=12&descripcion=hola";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, null, "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(401, respuesta.statusCode()), () -> assertEquals("Credenciales incorrectas", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 403: Solo pacientes")
    void peticion4() throws Exception {
        // Preparar
        String datos = "idCita=12&descripcion=hola";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, "medico.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(403, respuesta.statusCode()), () -> assertEquals("Solo pacientes", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 404: Ruta no encontrada")
    void peticion5() throws Exception {
        // Preparar
        String datos = "";
        // Ejecutar
        var respuesta = enviar("/otra", "POST", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(404, respuesta.statusCode()), () -> assertEquals("Ruta no encontrada", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 405: Metodo no permitido")
    void peticion6() throws Exception {
        // Preparar
        String datos = "";
        // Ejecutar
        var respuesta = enviar("/incidencia", "GET", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(405, respuesta.statusCode()), () -> assertEquals("Metodo no permitido", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 415: Se requiere formulario")
    void peticion7() throws Exception {
        // Preparar
        String datos = "idCita=12";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, "paciente.demo:demo", "text/plain");
        // Comprobar
        assertAll(() -> assertEquals(415, respuesta.statusCode()), () -> assertEquals("Se requiere formulario", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 400: Datos incorrectos")
    void peticion8() throws Exception {
        // Preparar
        String datos = "idCita=12&puntuacion=x";
        // Ejecutar
        var respuesta = enviar("/responder", "POST", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(400, respuesta.statusCode()), () -> assertEquals("Datos incorrectos", respuesta.body()));
    }

    @Test
    @DisplayName("HTTP 400: Datos incorrectos")
    void peticion9() throws Exception {
        // Preparar
        String datos = "idCita=12&idCita=13";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, "paciente.demo:demo", "application/x-www-form-urlencoded");
        // Comprobar
        assertAll(() -> assertEquals(400, respuesta.statusCode()), () -> assertEquals("Datos incorrectos", respuesta.body()));
    }


    private static final String FORMULARIO = "application/x-www-form-urlencoded";
    private static final String PACIENTE = "paciente.demo:demo";

    private void comprobar(HttpResponse<String> respuesta, int codigo, String mensaje) {
        assertAll(() -> assertEquals(codigo, respuesta.statusCode()),
                () -> assertEquals(mensaje, respuesta.body()));
    }

    private HttpResponse<String> enviarCabecera(String autorizacion) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create(url + "/incidencia"))
                .timeout(java.time.Duration.ofSeconds(5))
                .header("Authorization", autorizacion)
                .header("Content-Type", FORMULARIO)
                .POST(HttpRequest.BodyPublishers.ofString("idCita=12&descripcion=hola"))
                .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("El servidor por defecto puede arrancar y rechazar peticiones sin autenticar")
    void servidorPorDefecto() throws Exception {
        // Preparar
        assertNotNull(new GestionEncuestasHttpServer());
        servidor.stop(0);
        servidor = GestionEncuestasHttpServer.crearServidor(0);
        servidor.start();
        url = "http://localhost:" + servidor.getAddress().getPort() + "/encuestas";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", "idCita=12", null, FORMULARIO);
        // Comprobar
        comprobar(respuesta, 401, "Credenciales incorrectas");
    }

    @Test
    @DisplayName("HTTP 403: Paciente autenticado sin vinculo")
    void pacienteNoVinculado() throws Exception {
        // Preparar
        servidor.stop(0);
        servidor = GestionEncuestasHttpServer.crearServidor(0, new GestionEncuestasServiceImpl(),
                new AutenticacionServiceImpl(), usuario -> java.util.Optional.empty());
        servidor.start();
        url = "http://localhost:" + servidor.getAddress().getPort() + "/encuestas";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", "idCita=12", PACIENTE, FORMULARIO);
        // Comprobar
        comprobar(respuesta, 403, "Paciente no vinculado");
    }

    @Test
    @DisplayName("HTTP 415: Falta el tipo de contenido")
    void sinTipoContenido() throws Exception {
        // Preparar
        String datos = "idCita=12&descripcion=hola";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, PACIENTE, null);
        // Comprobar
        comprobar(respuesta, 415, "Se requiere formulario");
    }

    @Test
    @DisplayName("HTTP 413: El cuerpo supera 8192 bytes")
    void peticionDemasiadoGrande() throws Exception {
        // Preparar
        String datos = "a".repeat(8193);
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, PACIENTE, FORMULARIO);
        // Comprobar
        comprobar(respuesta, 413, "Peticion demasiado grande");
    }

    @Test
    @DisplayName("HTTP 400: El formulario contiene un parametro sin igual")
    void parametroSinIgual() throws Exception {
        // Preparar
        String datos = "idCita=12&descripcion";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", datos, PACIENTE, FORMULARIO);
        // Comprobar
        comprobar(respuesta, 400, "Datos incorrectos");
    }

    @Test
    @DisplayName("HTTP 401: Esquema de autorizacion incorrecto")
    void esquemaIncorrecto() throws Exception {
        // Preparar
        String cabecera = "Bearer token";
        // Ejecutar
        var respuesta = enviarCabecera(cabecera);
        // Comprobar
        comprobar(respuesta, 401, "Credenciales incorrectas");
    }

    @Test
    @DisplayName("HTTP 401: Credenciales Base64 mal formadas")
    void base64Incorrecto() throws Exception {
        // Preparar
        String cabecera = "Basic !!!";
        // Ejecutar
        var respuesta = enviarCabecera(cabecera);
        // Comprobar
        comprobar(respuesta, 401, "Credenciales incorrectas");
    }

    @Test
    @DisplayName("HTTP 401: Credenciales sin separador")
    void credencialesSinSeparador() throws Exception {
        // Preparar
        String usuario = "paciente.demo";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", "idCita=12", usuario, FORMULARIO);
        // Comprobar
        comprobar(respuesta, 401, "Credenciales incorrectas");
    }

    @Test
    @DisplayName("HTTP 503: Un servicio dependiente no esta disponible")
    void dependenciaNoDisponible() throws Exception {
        // Preparar
        var servicio = new GestionEncuestasServiceImpl(new IEncuestasProveedor() {
            public boolean citaPertenecePaciente(int cita, int paciente) {
                throw new IllegalStateException("Fallo simulado");
            }
            public boolean pruebaRealizada(int cita) { return true; }
        });
        servidor.stop(0);
        servidor = GestionEncuestasHttpServer.crearServidor(0, servicio,
                new AutenticacionServiceImpl(), new VinculoPacienteEnMemoria("paciente.demo=1"));
        servidor.start();
        url = "http://localhost:" + servidor.getAddress().getPort() + "/encuestas";
        // Ejecutar
        var respuesta = enviar("/incidencia", "POST", "idCita=12&descripcion=hola", PACIENTE, FORMULARIO);
        // Comprobar
        comprobar(respuesta, 503, "Servicio dependiente no disponible");
    }

}
