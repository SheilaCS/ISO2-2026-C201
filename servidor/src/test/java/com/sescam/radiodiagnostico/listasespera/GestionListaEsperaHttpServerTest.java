package com.sescam.radiodiagnostico.listasespera;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU15 · Servidor · GestionListaEsperaHttpServer")
class GestionListaEsperaHttpServerTest {

    private static final String ADMIN = "administrativo.demo:demo";
    private static final String DIA = "2026-12-10";

    private HttpServer servidor;
    private HttpClient cliente;
    private String base;
    private List<String> asignaciones;

    @BeforeEach
    void prepararServidor() throws Exception {
        ProveedorListaEsperaEnMemoria proveedor =
                new ProveedorListaEsperaEnMemoria();
        proveedor.agregarSolicitud(new SolicitudEnEspera(
                1, 11, "Radiografia; torax", Prioridad.ORDINARIO,
                LocalDate.of(2026, 11, 1)));
        proveedor.agregarSolicitud(new SolicitudEnEspera(
                2, 12, "Ecografia", Prioridad.URGENTE,
                LocalDate.of(2026, 11, 5)));
        proveedor.agregarHueco(new Hueco(
                5, LocalDate.of(2026, 12, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        proveedor.agregarHueco(new Hueco(
                6, LocalDate.of(2026, 12, 11),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        asignaciones = new ArrayList<>();
        servidor = GestionListaEsperaHttpServer.crearServidor(
                0,
                new ListaEsperaServiceImpl(proveedor, proveedor,
                        (s, p, f, h) -> {
                            asignaciones.add(s + "/" + p + "/" + f + "/" + h);
                            return true;
                        }),
                new AutenticacionServiceImpl());
        servidor.start();

        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // ------------------------------------
    // CONSULTAS
    // ------------------------------------

    @Test
    @DisplayName("El administrativo recibe la lista de espera ordenada por prioridad")
    void listaDeEspera() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera", basic(ADMIN), null);

        String[] lineas = respuesta.body().split("\n");
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals(2, lineas.length),
                () -> assertEquals("2;12;Ecografia;URGENTE;2026-11-05",
                        lineas[0]),
                () -> assertEquals(
                        "1;11;Radiografia, torax;ORDINARIO;2026-11-01",
                        lineas[1])
        );
    }

    @Test
    @DisplayName("Los huecos de una fecha se devuelven con médico, fecha, inicio y fin")
    void huecosDeUnaFecha() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera/huecos?fecha=" + DIA, basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals("5;2026-12-10;09:00;10:00",
                        respuesta.body())
        );
    }

    @Test
    @DisplayName("Sin fecha se devuelven todos los huecos")
    void todosLosHuecos() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera/huecos", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals(2, respuesta.body().split("\n").length)
        );
    }

    @Test
    @DisplayName("La propuesta junta la solicitud más prioritaria y el primer hueco")
    void propuesta() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera/propuesta", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals(
                        "2;12;Ecografia;URGENTE;2026-11-05;"
                        + "5;2026-12-10;09:00;10:00",
                        respuesta.body())
        );
    }

    @Test
    @DisplayName("Sin solicitudes la propuesta llega con el cuerpo vacío")
    void propuestaVacia() throws Exception {
        servidor.stop(0);
        servidor = GestionListaEsperaHttpServer.crearServidor(0);
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();

        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera/propuesta", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().isEmpty())
        );
    }

    // ------------------------------------
    // OPERACIONES
    // ------------------------------------

    @Test
    @DisplayName("Cambiar la prioridad responde 200 con mensaje y reordena la lista")
    void cambiarPrioridad() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "POST", "/listaespera/prioridad", basic(ADMIN),
                "solicitud=1&prioridad=urgente");

        HttpResponse<String> lista = enviar(
                "GET", "/listaespera", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals("mensaje=Prioridad+actualizada.",
                        respuesta.body()),
                () -> assertTrue(lista.body().startsWith("1;11;"))
        );
    }

    @Test
    @DisplayName("Retirar una solicitud responde 200 y la quita de la lista")
    void retirar() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "POST", "/listaespera/retirar", basic(ADMIN), "solicitud=2");

        HttpResponse<String> lista = enviar(
                "GET", "/listaespera", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals(1, lista.body().split("\n").length),
                () -> assertTrue(lista.body().startsWith("1;"))
        );
    }

    @Test
    @DisplayName("Asignar un hueco responde 200 y crea la cita en el módulo de citas")
    void asignar() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "POST", "/listaespera/asignar", basic(ADMIN),
                "solicitud=2&medico=5&fecha=" + DIA + "&hora=09:00");

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertEquals("mensaje=Cita+asignada+correctamente.",
                        respuesta.body()),
                () -> assertEquals(List.of("2/12/2026-12-10/09:00"),
                        asignaciones)
        );
    }

    @Test
    @DisplayName("Una operación que el servicio rechaza responde 409 con el motivo")
    void operacionRechazada() throws Exception {
        HttpResponse<String> respuesta = enviar(
                "POST", "/listaespera/retirar", basic(ADMIN), "solicitud=99");

        assertAll(
                () -> assertEquals(409, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("error="))
        );
    }

    @Test
    @DisplayName("Con el módulo de citas sin conectar, asignar responde 409")
    void citasSinConectar() throws Exception {
        servidor.stop(0);
        ProveedorListaEsperaEnMemoria proveedor =
                new ProveedorListaEsperaEnMemoria();
        proveedor.agregarSolicitud(new SolicitudEnEspera(
                1, 11, "TAC", Prioridad.URGENTE, LocalDate.of(2026, 11, 1)));
        proveedor.agregarHueco(new Hueco(5, LocalDate.of(2026, 12, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        servidor = GestionListaEsperaHttpServer.crearServidor(
                0,
                new ListaEsperaServiceImpl(proveedor, proveedor,
                        (s, p, f, h) -> {
                            throw new IllegalStateException("caido");
                        }),
                new AutenticacionServiceImpl());
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();

        HttpResponse<String> respuesta = enviar(
                "POST", "/listaespera/asignar", basic(ADMIN),
                "solicitud=1&medico=5&fecha=" + DIA + "&hora=09:00");

        assertAll(
                () -> assertEquals(409, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().contains("citas"))
        );
    }

    // ------------------------------------
    // CONTROL DE ACCESO
    // ------------------------------------

    @ParameterizedTest
    @CsvSource({
            "paciente.demo:demo",
            "medico.demo:demo",
            "admin.demo:demo"
    })
    @DisplayName("Los usuarios que no son administrativos reciben 403 en todas las rutas")
    void rolSinPermiso(String credenciales) throws Exception {
        assertAll(
                () -> assertEquals(403, enviar("GET", "/listaespera",
                        basic(credenciales), null).statusCode()),
                () -> assertEquals(403, enviar("GET",
                        "/listaespera/huecos", basic(credenciales), null)
                        .statusCode()),
                () -> assertEquals(403, enviar("GET",
                        "/listaespera/propuesta", basic(credenciales), null)
                        .statusCode()),
                () -> assertEquals(403, enviar("POST",
                        "/listaespera/prioridad", basic(credenciales),
                        "solicitud=1&prioridad=URGENTE").statusCode()),
                () -> assertEquals(403, enviar("POST",
                        "/listaespera/retirar", basic(credenciales),
                        "solicitud=1").statusCode()),
                () -> assertEquals(403, enviar("POST",
                        "/listaespera/asignar", basic(credenciales),
                        "solicitud=1&medico=5&fecha=" + DIA + "&hora=09:00")
                        .statusCode())
        );
    }

    @Test
    @DisplayName("Un rechazo por rol no cambia los datos")
    void rolSinPermisoNoModifica() throws Exception {
        enviar("POST", "/listaespera/retirar", basic("medico.demo:demo"),
                "solicitud=2");

        HttpResponse<String> lista = enviar(
                "GET", "/listaespera", basic(ADMIN), null);

        assertEquals(2, lista.body().split("\n").length);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "administrativo.demo:mal",
            "nadie:demo",
            "sin-separador",
            ":demo"
    })
    @DisplayName("Credenciales incorrectas responden 401 con WWW-Authenticate")
    void credencialesIncorrectas(String credenciales) throws Exception {
        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera", basic(credenciales), null);

        assertAll(
                () -> assertEquals(401, respuesta.statusCode()),
                () -> assertTrue(respuesta.headers()
                        .firstValue("WWW-Authenticate").isPresent())
        );
    }

    @Test
    @DisplayName("Sin cabecera de autorización o con otro esquema responde 401")
    void sinAutorizacion() throws Exception {
        assertAll(
                () -> assertEquals(401, enviar("GET", "/listaespera", null,
                        null).statusCode()),
                () -> assertEquals(401, enviar("GET", "/listaespera",
                        "Bearer abc", null).statusCode()),
                () -> assertEquals(401, enviar("GET", "/listaespera",
                        "Basic %%%no-base64", null).statusCode())
        );
    }

    // ------------------------------------
    // DATOS Y RUTAS INCORRECTAS
    // ------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "solicitud=abc&prioridad=URGENTE",
            "solicitud=1&prioridad=MUY_URGENTE",
            "solicitud=1",
            "prioridad=URGENTE",
            "solicitud=1&solicitud=2&prioridad=URGENTE",
            "solicitud=1&prioridad=URGENTE&extra=1",
            "solicitud",
            "solicitud=1&prioridad"
    })
    @DisplayName("Una prioridad con datos incorrectos responde 400")
    void prioridadConDatosIncorrectos(String cuerpo) throws Exception {
        assertEquals(400, enviar("POST", "/listaespera/prioridad",
                basic(ADMIN), cuerpo).statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "solicitud=x",
            "medico=1"
    })
    @DisplayName("Retirar con datos incorrectos responde 400")
    void retirarConDatosIncorrectos(String cuerpo) throws Exception {
        assertEquals(400, enviar("POST", "/listaespera/retirar",
                basic(ADMIN), cuerpo).statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "solicitud=1&medico=5&fecha=10-12-2026&hora=09:00",
            "solicitud=1&medico=5&fecha=" + DIA + "&hora=nueve",
            "solicitud=1&medico=cinco&fecha=" + DIA + "&hora=09:00",
            "solicitud=1&medico=5&fecha=" + DIA,
            "solicitud=1&medico=5&fecha=2026-02-31&hora=09:00"
    })
    @DisplayName("Asignar con datos incorrectos responde 400 y no crea la cita")
    void asignarConDatosIncorrectos(String cuerpo) throws Exception {
        HttpResponse<String> respuesta = enviar(
                "POST", "/listaespera/asignar", basic(ADMIN), cuerpo);

        assertAll(
                () -> assertEquals(400, respuesta.statusCode()),
                () -> assertTrue(asignaciones.isEmpty())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "?fecha=ayer",
            "?fecha=" + DIA + "&fecha=" + DIA,
            "?otro=1",
            "?fecha"
    })
    @DisplayName("Consultar huecos con parámetros incorrectos responde 400")
    void huecosConDatosIncorrectos(String consulta) throws Exception {
        assertEquals(400, enviar("GET", "/listaespera/huecos" + consulta,
                basic(ADMIN), null).statusCode());
    }

    @Test
    @DisplayName("Un cuerpo demasiado grande responde 400")
    void cuerpoDemasiadoGrande() throws Exception {
        String grande = "solicitud=" + "1".repeat(5000);

        assertEquals(400, enviar("POST", "/listaespera/retirar",
                basic(ADMIN), grande).statusCode());
    }

    @Test
    @DisplayName("Una ruta que no existe responde 404")
    void rutaInexistente() throws Exception {
        assertEquals(404, enviar("GET", "/listaespera/otra",
                basic(ADMIN), null).statusCode());
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /listaespera, GET",
            "DELETE, /listaespera/huecos, GET",
            "PUT, /listaespera/propuesta, GET",
            "GET, /listaespera/prioridad, POST",
            "GET, /listaespera/retirar, POST",
            "GET, /listaespera/asignar, POST"
    })
    @DisplayName("Un método no permitido responde 405 e indica el permitido")
    void metodoNoPermitido(String metodo, String ruta, String permitido)
            throws Exception {
        HttpResponse<String> respuesta = enviar(
                metodo, ruta, basic(ADMIN), null);

        assertAll(
                () -> assertEquals(405, respuesta.statusCode()),
                () -> assertEquals(permitido,
                        respuesta.headers().firstValue("Allow").orElse(""))
        );
    }

    @Test
    @DisplayName("Un fallo inesperado del servicio responde 500 sin detalles internos")
    void falloInterno() throws Exception {
        servidor.stop(0);
        servidor = GestionListaEsperaHttpServer.crearServidor(
                0, new ServicioQueFalla(), new AutenticacionServiceImpl());
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();

        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(500, respuesta.statusCode()),
                () -> assertEquals("error=Error+interno+del+servidor.",
                        respuesta.body())
        );
    }

    @Test
    @DisplayName("El servidor se crea con los datos por defecto y atiende peticiones")
    void servidorPorDefecto() throws Exception {
        servidor.stop(0);
        servidor = GestionListaEsperaHttpServer.crearServidor(0);
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();

        HttpResponse<String> respuesta = enviar(
                "GET", "/listaespera", basic(ADMIN), null);

        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().isEmpty())
        );
    }

    @Test
    @DisplayName("El main arranca el servidor en el puerto indicado por propiedad")
    void mainArrancaElServidor() throws Exception {
        System.setProperty("listaespera.puerto", "0");
        try {
            assertDoesNotThrow(() ->
                    GestionListaEsperaHttpServer.main(new String[0]));
        } finally {
            System.clearProperty("listaespera.puerto");
        }
    }

    // ------------------------------------
    // AUXILIARES
    // ------------------------------------

    private HttpResponse<String> enviar(
            String metodo,
            String ruta,
            String autorizacion,
            String cuerpo) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(5));

        if (autorizacion != null) {
            peticion.header("Authorization", autorizacion);
        }

        peticion.method(metodo, cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo));

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(
                credenciales.getBytes(StandardCharsets.UTF_8));
    }

    // Servicio que falla siempre, para probar el error 500
    private static class ServicioQueFalla implements IListaEsperaService {

        @Override
        public List<SolicitudEnEspera> consultarListaEspera() {
            throw new IllegalStateException("detalle interno");
        }

        @Override
        public ResultadoOperacion cambiarPrioridad(int id, Prioridad p) {
            throw new IllegalStateException("detalle interno");
        }

        @Override
        public ResultadoOperacion retirarDeLista(int id) {
            throw new IllegalStateException("detalle interno");
        }

        @Override
        public List<Hueco> consultarHuecos(LocalDate fecha) {
            throw new IllegalStateException("detalle interno");
        }

        @Override
        public Optional<Propuesta> proponerAsignacion() {
            throw new IllegalStateException("detalle interno");
        }

        @Override
        public ResultadoOperacion asignarHueco(int id, int medico,
                LocalDate fecha, LocalTime inicio) {
            throw new IllegalStateException("detalle interno");
        }
    }
}
