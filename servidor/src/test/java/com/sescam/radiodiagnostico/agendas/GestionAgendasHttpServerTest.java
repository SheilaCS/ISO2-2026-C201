
package com.sescam.radiodiagnostico.agendas;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GestionAgendasHttpServerTest {

    private HttpServer servidor;
    private HttpClient cliente;
    private String urlBase;
    private String fecha;

    // -----------------------------------------
    // PREPARAR Y FINALIZAR
    // -----------------------------------------

    @BeforeEach
    public void preparar() throws Exception {

        // Crear un servicio nuevo para cada prueba
        Field campo = GestionAgendasHttpServer.class
                .getDeclaredField("servicio");

        campo.setAccessible(true);
        campo.set(null, new GestionAgendasServiceImpl());

        // Crear servidor con un puerto libre
        servidor = GestionAgendasHttpServer.crearServidor(0);
        servidor.start();

        int puerto = servidor.getAddress().getPort();

        urlBase = "http://localhost:" + puerto + "/agendas";

        cliente = HttpClient.newHttpClient();

        fecha = LocalDate.now().plusDays(5).toString();
    }

    @AfterEach
    public void finalizar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // -----------------------------------------
    // METODOS AUXILIARES
    // -----------------------------------------

    // Enviar peticiones GET y POST
    private HttpResponse<String> enviar(
            String metodo, String ruta, String datos)
            throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + ruta));

        if (metodo.equals("POST")) {

            peticion.header(
                    "Content-Type",
                    "application/x-www-form-urlencoded");

            peticion.POST(
                    HttpRequest.BodyPublishers.ofString(
                            datos, StandardCharsets.UTF_8));

        } else if (metodo.equals("GET")) {

            peticion.GET();

        } else {

            peticion.method(
                    metodo,
                    HttpRequest.BodyPublishers.noBody());
        }

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));
    }

    // Datos correctos para crear un horario
    private String datosCrear() {

        return "idMedico=1&fecha=" + fecha
                + "&inicio=09%3A00&fin=10%3A00";
    }

    // -----------------------------------------
    // PRUEBAS DE CREAR HORARIO
    // -----------------------------------------

    @Test
    @DisplayName("Crear un horario correctamente")
    public void testCrearCorrecto() throws Exception {

        // Preparar y ejecutar
        HttpResponse<String> respuesta =
                enviar("POST", "/crear", datosCrear());

        // Comprobar
        assertEquals(200, respuesta.statusCode());
        assertEquals(
                "Horario creado correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar un ID de medico incorrecto")
    public void testCrearIdIncorrecto() throws Exception {

        String datos = "idMedico=0&fecha=" + fecha
                + "&inicio=09:00&fin=10:00";

        HttpResponse<String> respuesta =
                enviar("POST", "/crear", datos);

        assertEquals(400, respuesta.statusCode());

        assertEquals(
                "No se pudo crear el horario",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al crear")
    public void testCrearDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/crear", "idMedico=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET en la ruta crear")
    public void testCrearMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/crear", "");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar POST sin parametros")
    public void testPostSinParametros() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/crear", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE CONSULTAR AGENDA
    // -----------------------------------------

    @Test
    @DisplayName("Consultar una agenda con horarios")
    public void testConsultarCorrecto() throws Exception {

        // Preparar
        enviar("POST", "/crear", datosCrear());

        // Ejecutar
        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idMedico=1", "");

        // Comprobar
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Horario 1"));
        assertTrue(respuesta.body().contains("09:00"));
    }

    @Test
    @DisplayName("Consultar una agenda vacia")
    public void testConsultarAgendaVacia() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idMedico=5", "");

        assertEquals(200, respuesta.statusCode());

        assertEquals(
                "No hay horarios para este médico",
                respuesta.body());
    }

    @Test
    @DisplayName("Consultar un ID de medico incorrecto")
    public void testConsultarIdIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idMedico=0", "");

        assertEquals(200, respuesta.statusCode());

        assertEquals(
                "ID de médico no válido",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al consultar")
    public void testConsultarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idMedico=abc", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Consultar sin parametros")
    public void testConsultarSinParametros() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Consultar con parametros vacios")
    public void testConsultarParametrosVacios() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?", "");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar POST al consultar")
    public void testConsultarMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/consultar", "idMedico=1");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE MODIFICAR HORARIO
    // -----------------------------------------

    @Test
    @DisplayName("Modificar un horario correctamente")
    public void testModificarCorrecto() throws Exception {

        // Preparar
        enviar("POST", "/crear", datosCrear());

        String datos = "idHorario=1&fecha=" + fecha
                + "&inicio=11:00&fin=12:00";

        // Ejecutar
        HttpResponse<String> respuesta =
                enviar("POST", "/modificar", datos);

        // Comprobar
        assertEquals(200, respuesta.statusCode());

        assertEquals(
                "Horario modificado correctamente",
                respuesta.body());

        HttpResponse<String> consulta =
                enviar("GET", "/consultar?idMedico=1", "");

        assertTrue(consulta.body().contains("11:00"));
    }

    @Test
    @DisplayName("Rechazar un horario inexistente al modificar")
    public void testModificarInexistente() throws Exception {

        String datos = "idHorario=99&fecha=" + fecha
                + "&inicio=11:00&fin=12:00";

        HttpResponse<String> respuesta =
                enviar("POST", "/modificar", datos);

        assertEquals(400, respuesta.statusCode());

        assertEquals(
                "No se pudo modificar el horario",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al modificar")
    public void testModificarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/modificar", "idHorario=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET al modificar")
    public void testModificarMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/modificar", "");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE ELIMINAR HORARIO
    // -----------------------------------------

    @Test
    @DisplayName("Eliminar un horario correctamente")
    public void testEliminarCorrecto() throws Exception {

        // Preparar
        enviar("POST", "/crear", datosCrear());

        // Ejecutar
        HttpResponse<String> respuesta =
                enviar("POST", "/eliminar", "idHorario=1");

        // Comprobar
        assertEquals(200, respuesta.statusCode());

        assertEquals(
                "Horario eliminado correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar un horario inexistente al eliminar")
    public void testEliminarInexistente() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/eliminar", "idHorario=99");

        assertEquals(400, respuesta.statusCode());

        assertEquals(
                "No se pudo eliminar el horario",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar datos incorrectos al eliminar")
    public void testEliminarDatosIncorrectos() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/eliminar", "idHorario=abc");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Rechazar GET al eliminar")
    public void testEliminarMetodoIncorrecto() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/eliminar", "");

        assertEquals(405, respuesta.statusCode());
        assertEquals("Metodo no permitido", respuesta.body());
    }

    // -----------------------------------------
    // PRUEBAS DE PARAMETROS HTTP
    // -----------------------------------------

    @Test
    @DisplayName("Decodificar parametros URL")
    public void testParametrosCodificados() throws Exception {

        String datos = "idMedico=%31&fecha=" + fecha
                + "&inicio=09%3A00&fin=10%3A00";

        HttpResponse<String> respuesta =
                enviar("POST", "/crear", datos);

        assertEquals(200, respuesta.statusCode());

        assertEquals(
                "Horario creado correctamente",
                respuesta.body());
    }

    @Test
    @DisplayName("Rechazar parametros sin signo igual")
    public void testParametroSinIgual() throws Exception {

        HttpResponse<String> respuesta =
                enviar("POST", "/crear", "idMedico");

        assertEquals(400, respuesta.statusCode());
        assertEquals("Datos incorrectos", respuesta.body());
    }

    @Test
    @DisplayName("Comprobar cabecera HTTP")
    public void testCabeceraRespuesta() throws Exception {

        HttpResponse<String> respuesta =
                enviar("GET", "/consultar?idMedico=1", "");

        assertEquals(200, respuesta.statusCode());

        assertTrue(respuesta.headers()
                .firstValue("Content-Type")
                .orElse("")
                .contains("text/plain"));
    }

    // -----------------------------------------
    // PRUEBAS DEL CONSTRUCTOR Y MAIN
    // -----------------------------------------

    @Test
    @DisplayName("Comprobar el constructor del servidor")
    public void testConstructor() {

        // Ejecutar
        GestionAgendasHttpServer objeto =
                new GestionAgendasHttpServer();

        // Comprobar
        assertNotNull(objeto);
    }

    @Test
    @DisplayName("Ejecutar el metodo main del servidor")
    public void testMainServidor() throws Exception {

        // Guardar la configuracion anterior
        String puertoAnterior =
                System.getProperty("agendas.puerto");

        // Usar un puerto libre en vez del 8081
        System.setProperty("agendas.puerto", "0");

        try {

            // Ejecutar
            GestionAgendasHttpServer.main(new String[0]);

            // Comprobar
            assertNotNull(
                    GestionAgendasHttpServer.servidorPrincipal);

            assertTrue(
                    GestionAgendasHttpServer.servidorPrincipal
                            .getAddress().getPort() > 0);

        } finally {

            // Detener el servidor creado por main
            if (GestionAgendasHttpServer.servidorPrincipal != null) {

                GestionAgendasHttpServer.servidorPrincipal.stop(0);

                GestionAgendasHttpServer.servidorPrincipal = null;
            }

            // Restaurar la configuracion original
            if (puertoAnterior == null) {
                System.clearProperty("agendas.puerto");
            } else {
                System.setProperty("agendas.puerto", puertoAnterior);
            }
        }
    }
}
