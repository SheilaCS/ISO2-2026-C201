package com.sescam.radiodiagnostico.roles;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sescam.radiodiagnostico.autenticacion.AutenticacionServiceImpl;
import com.sun.net.httpserver.HttpServer;

@DisplayName("CDU10 · Servidor · GestionRolesHttpServer")
class GestionRolesHttpServerTest {

    private static final String ADMIN = "admin.demo:demo";
    private static final String FORMULARIO = "application/x-www-form-urlencoded";

    private HttpServer servidor;
    private HttpClient cliente;
    private String base;

    @BeforeEach
    void arrancarServidor() throws Exception {
        servidor = GestionRolesHttpServer.crearServidor(
                0,
                new RolesServiceImpl(new RepositorioRolesEnMemoria()),
                new AutenticacionServiceImpl());
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("H1. El administrador consulta el rol de un usuario y la lista de usuarios")
    void consultaRolYLista() throws Exception {
        HttpResponse<String> uno = enviar("GET", "/roles/usuarios?usuario=medico.demo", null, ADMIN);
        HttpResponse<String> todos = enviar("GET", "/roles/usuarios", null, ADMIN);

        assertAll(
                () -> assertEquals(200, uno.statusCode()),
                () -> assertEquals("MEDICO", leer(uno).get("rol")),
                () -> assertEquals(200, todos.statusCode()),
                () -> assertEquals("PACIENTE", leer(todos).get("paciente.demo")),
                () -> assertEquals(4, leer(todos).size()));
    }

    @Test
    @DisplayName("H2. El administrador cambia el rol de un usuario y el cambio se conserva")
    void cambiaRol() throws Exception {
        HttpResponse<String> cambio = enviar("PUT", "/roles/usuarios/medico.demo",
                "rol=administrativo", ADMIN);
        HttpResponse<String> consulta = enviar("GET", "/roles/usuarios?usuario=medico.demo", null, ADMIN);

        assertAll(
                () -> assertEquals(200, cambio.statusCode()),
                () -> assertEquals("ADMINISTRATIVO", leer(cambio).get("rol")),
                () -> assertEquals("ADMINISTRATIVO", leer(consulta).get("rol")));
    }

    @Test
    @DisplayName("H3. El administrador consulta, concede y retira permisos de un rol")
    void gestionaPermisos() throws Exception {
        HttpResponse<String> consulta = enviar("GET", "/roles/permisos?rol=paciente", null, ADMIN);
        HttpResponse<String> concede = enviar("POST", "/roles/permisos/PACIENTE/SOLICITAR_PRUEBAS", null, ADMIN);
        HttpResponse<String> retira = enviar("DELETE", "/roles/permisos/PACIENTE/CONSULTAR_MIS_CITAS", null, ADMIN);

        assertAll(
                () -> assertEquals(200, consulta.statusCode()),
                () -> assertEquals("CONSULTAR_HISTORIAL,CONSULTAR_MIS_CITAS", leer(consulta).get("permisos")),
                () -> assertEquals(200, concede.statusCode()),
                () -> assertTrue(leer(concede).get("permisos").contains("SOLICITAR_PRUEBAS")),
                () -> assertEquals(200, retira.statusCode()),
                () -> assertEquals("SOLICITAR_PRUEBAS,CONSULTAR_HISTORIAL", leer(retira).get("permisos")));
    }

    @Test
    @DisplayName("H4. Las reglas de negocio se devuelven como 409 y los datos no cambian")
    void reglasDeNegocioDevuelven409() throws Exception {
        HttpResponse<String> propio = enviar("PUT", "/roles/usuarios/admin.demo", "rol=MEDICO", ADMIN);
        HttpResponse<String> gestion = enviar("DELETE", "/roles/permisos/ADMINISTRADOR/GESTIONAR_ROLES", null, ADMIN);
        HttpResponse<String> consulta = enviar("GET", "/roles/usuarios?usuario=admin.demo", null, ADMIN);

        assertAll(
                () -> assertEquals(409, propio.statusCode()),
                () -> assertEquals(409, gestion.statusCode()),
                () -> assertEquals("ADMINISTRADOR", leer(consulta).get("rol")));
    }

    @Test
    @DisplayName("H5. Datos incorrectos devuelven 400, usuarios y rutas inexistentes 404")
    void peticionesIncorrectas() throws Exception {
        HttpResponse<String> rolInventado = enviar("PUT", "/roles/usuarios/medico.demo", "rol=JEFE", ADMIN);
        HttpResponse<String> sinRol = enviar("PUT", "/roles/usuarios/medico.demo", "otro=1", ADMIN);
        HttpResponse<String> permisoInventado = enviar("POST", "/roles/permisos/MEDICO/BORRAR_TODO", null, ADMIN);
        HttpResponse<String> malFormado = enviar("GET", "/roles/permisos?rol", null, ADMIN);
        HttpResponse<String> sinFormulario = enviar("PUT", "/roles/usuarios/medico.demo", null, ADMIN);
        HttpResponse<String> inexistente = enviar("GET", "/roles/usuarios?usuario=nadie.demo", null, ADMIN);
        HttpResponse<String> ruta = enviar("GET", "/roles/otra", null, ADMIN);

        assertAll(
                () -> assertEquals(400, rolInventado.statusCode()),
                () -> assertEquals(400, sinRol.statusCode()),
                () -> assertEquals(400, permisoInventado.statusCode()),
                () -> assertEquals(400, malFormado.statusCode()),
                () -> assertEquals(400, sinFormulario.statusCode()),
                () -> assertEquals(404, inexistente.statusCode()),
                () -> assertEquals(404, ruta.statusCode()));
    }

    @Test
    @DisplayName("H6. Sin credenciales válidas se responde 401")
    void sinCredencialesValidas() throws Exception {
        HttpResponse<String> sinCabecera = enviarCon("GET", "/roles/usuarios", null, null);
        HttpResponse<String> claveMala = enviar("GET", "/roles/usuarios", null, "admin.demo:mala");
        HttpResponse<String> otroEsquema = enviarCon("GET", "/roles/usuarios", null, "Bearer abc");
        HttpResponse<String> base64Malo = enviarCon("GET", "/roles/usuarios", null, "Basic %%%");
        HttpResponse<String> sinSeparador = enviarCon("GET", "/roles/usuarios", null, basic("admin.demo"));

        assertAll(
                () -> assertEquals(401, sinCabecera.statusCode()),
                () -> assertTrue(sinCabecera.headers().firstValue("WWW-Authenticate").isPresent()),
                () -> assertEquals(401, claveMala.statusCode()),
                () -> assertEquals(401, otroEsquema.statusCode()),
                () -> assertEquals(401, base64Malo.statusCode()),
                () -> assertEquals(401, sinSeparador.statusCode()));
    }

    @ParameterizedTest(name = "{0} no puede hacer {1} {2}")
    @MethodSource("accesosNoAutorizados")
    @DisplayName("H7. Los roles sin permiso de gestión de roles reciben 403 y nada cambia")
    void rolesSinPermisoRecibenProhibido(String usuario, String metodo, String ruta, String cuerpo)
            throws Exception {
        HttpResponse<String> respuesta = enviar(metodo, ruta, cuerpo, usuario + ":demo");

        HttpResponse<String> rolMedico = enviar("GET", "/roles/usuarios?usuario=medico.demo", null, ADMIN);
        HttpResponse<String> permisosMedico = enviar("GET", "/roles/permisos?rol=MEDICO", null, ADMIN);

        assertAll(
                () -> assertEquals(403, respuesta.statusCode()),
                () -> assertEquals("MEDICO", leer(rolMedico).get("rol")),
                () -> assertEquals("SOLICITAR_PRUEBAS,REGISTRAR_REALIZACION,CONSULTAR_HISTORIAL",
                        leer(permisosMedico).get("permisos")));
    }

    static Stream<Arguments> accesosNoAutorizados() {
        return Stream.of("medico.demo", "paciente.demo", "administrativo.demo")
                .flatMap(usuario -> Stream.of(
                        Arguments.of(usuario, "GET", "/roles/usuarios", null),
                        Arguments.of(usuario, "PUT", "/roles/usuarios/medico.demo", "rol=ADMINISTRADOR"),
                        Arguments.of(usuario, "POST", "/roles/permisos/MEDICO/GESTIONAR_ROLES", null),
                        Arguments.of(usuario, "DELETE", "/roles/permisos/MEDICO/CONSULTAR_HISTORIAL", null)));
    }

    @Test
    @DisplayName("H8. Si un usuario recibe el permiso de gestión de roles, puede usar el servidor")
    void permisoConcedidoDaAcceso() throws Exception {
        HttpResponse<String> antes = enviar("GET", "/roles/usuarios", null, "medico.demo:demo");
        enviar("POST", "/roles/permisos/MEDICO/GESTIONAR_ROLES", null, ADMIN);
        HttpResponse<String> despues = enviar("GET", "/roles/usuarios", null, "medico.demo:demo");

        assertAll(
                () -> assertEquals(403, antes.statusCode()),
                () -> assertEquals(200, despues.statusCode()));
    }

    // ---------- Métodos de ayuda ----------

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo, String credenciales)
            throws Exception {
        return enviarCon(metodo, ruta, cuerpo, basic(credenciales));
    }

    private HttpResponse<String> enviarCon(String metodo, String ruta, String cuerpo, String autorizacion)
            throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(5));

        if (autorizacion != null) {
            peticion.header("Authorization", autorizacion);
        }
        if (cuerpo != null) {
            peticion.header("Content-Type", FORMULARIO);
        }

        peticion.method(metodo, cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));

        return cliente.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(
                credenciales.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> leer(HttpResponse<String> respuesta) {
        Map<String, String> datos = new HashMap<>();
        for (String parte : respuesta.body().split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length == 2) {
                datos.put(
                        URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(par[1], StandardCharsets.UTF_8));
            }
        }
        return datos;
    }
}