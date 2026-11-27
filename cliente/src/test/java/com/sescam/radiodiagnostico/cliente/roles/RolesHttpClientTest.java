package com.sescam.radiodiagnostico.cliente.roles;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sescam.radiodiagnostico.cliente.roles.RolesHttpClient.Respuesta;
import com.sescam.radiodiagnostico.roles.GestionRolesHttpServer;
import com.sun.net.httpserver.HttpServer;

/**
 * Pruebas del cliente HTTP contra el servidor real de roles en un puerto libre.
 */
@DisplayName("CDU10 · Cliente · RolesHttpClient")
class RolesHttpClientTest {

    private HttpServer servidor;
    private RolesHttpClient cliente;

    @BeforeEach
    void setUp() throws Exception {
        servidor = GestionRolesHttpServer.crearServidor(0);
        servidor.start();
        cliente = new RolesHttpClient(
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/roles");
        cliente.setCredenciales("admin.demo", "demo");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("C1. Consulta el rol de un usuario y la lista de usuarios")
    void consultaRolYLista() throws Exception {
        Respuesta rol = cliente.consultarRol(" medico.demo ");
        Respuesta lista = cliente.listarUsuarios();

        assertAll(
                () -> assertTrue(rol.correcta()),
                () -> assertEquals("MEDICO", rol.datos().get("rol")),
                () -> assertEquals(4, lista.datos().size()));
    }

    @Test
    @DisplayName("C2. Cambia el rol de un usuario")
    void cambiaRol() throws Exception {
        Respuesta cambio = cliente.cambiarRol("paciente.demo", "MEDICO");

        assertAll(
                () -> assertEquals(200, cambio.codigo()),
                () -> assertEquals("MEDICO", cliente.consultarRol("paciente.demo").datos().get("rol")));
    }

    @Test
    @DisplayName("C3. Consulta, concede y retira permisos de un rol")
    void gestionaPermisos() throws Exception {
        Respuesta consulta = cliente.consultarPermisos("PACIENTE");
        Respuesta concede = cliente.concederPermiso("PACIENTE", "SOLICITAR_PRUEBAS");
        Respuesta retira = cliente.retirarPermiso("PACIENTE", "SOLICITAR_PRUEBAS");

        assertAll(
                () -> assertEquals("CONSULTAR_HISTORIAL,CONSULTAR_MIS_CITAS", consulta.datos().get("permisos")),
                () -> assertTrue(concede.datos().get("permisos").contains("SOLICITAR_PRUEBAS")),
                () -> assertFalse(retira.datos().get("permisos").contains("SOLICITAR_PRUEBAS")));
    }

    @Test
    @DisplayName("C4. Devuelve el código y el mensaje de error del servidor")
    void devuelveErrores() throws Exception {
        Respuesta noExiste = cliente.consultarRol("nadie.demo");
        Respuesta propio = cliente.cambiarRol("admin.demo", "MEDICO");

        cliente.setCredenciales("medico.demo", "demo");
        Respuesta prohibido = cliente.listarUsuarios();

        cliente.setCredenciales(null, null);
        Respuesta sinCredenciales = cliente.listarUsuarios();

        assertAll(
                () -> assertEquals(404, noExiste.codigo()),
                () -> assertFalse(noExiste.correcta()),
                () -> assertEquals("No existe el usuario indicado.", noExiste.error()),
                () -> assertEquals(409, propio.codigo()),
                () -> assertEquals(403, prohibido.codigo()),
                () -> assertEquals(401, sinCredenciales.codigo()));
    }

    @Test
    @DisplayName("C5. Separa los campos antes de decodificarlos")
    void decodificaCadaCampo() {
        assertAll(
                () -> assertEquals("Pérez & Gómez",
                        RolesHttpClient.leerCampos("apellidos=P%C3%A9rez+%26+G%C3%B3mez").get("apellidos")),
                () -> assertTrue(RolesHttpClient.leerCampos("").isEmpty()),
                () -> assertTrue(RolesHttpClient.leerCampos(null).isEmpty()),
                () -> assertTrue(RolesHttpClient.leerCampos("sinIgual").isEmpty()),
                () -> assertEquals("Petición rechazada.",
                        new Respuesta(500, java.util.Map.of()).error()));
    }

    @Test
    @DisplayName("C6. Si el servidor no está disponible lanza IOException")
    void servidorNoDisponible() {
        servidor.stop(0);

        assertThrows(IOException.class, () -> cliente.listarUsuarios());
    }

    @Test
    @DisplayName("C7. El constructor por defecto se crea sin conectarse")
    void constructorPorDefecto() {
        assertTrue(new RolesHttpClient() != null);
    }
}
