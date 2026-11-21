
package com.sescam.radiodiagnostico.cliente.miscitas;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GestionMisCitasGUITest {

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    // ------------------------------------
    // CLIENTE HTTP SIMULADO
    // ------------------------------------

    private static class ClienteSimulado extends MisCitasHttpClient {

        String operacion;
        String motivoRecibido;
        int idCitaRecibido = -1;
        int idPacienteRecibido = -1;
        boolean errorConexion = false;

        private String responder(String nombre) throws IOException {

            operacion = nombre;

            if (errorConexion) {
                throw new IOException("Servidor no disponible");
            }

            return "Operacion correcta";
        }

        @Override
        public String consultarMisCitas(int idPaciente)
                throws IOException {

            idPacienteRecibido = idPaciente;
            return responder("consultar");
        }

        @Override
        public String confirmarCita(int idCita, int idPaciente)
                throws IOException {

            idCitaRecibido = idCita;
            idPacienteRecibido = idPaciente;
            return responder("confirmar");
        }

        @Override
        public String solicitarCambioCita(int idCita, int idPaciente,
                String motivo) throws IOException {

            motivoRecibido = motivo;
            idCitaRecibido = idCita;
            idPacienteRecibido = idPaciente;
            return responder("cambio");
        }

        @Override
        public String cancelarCita(int idCita, int idPaciente)
                throws IOException {

            idCitaRecibido = idCita;
            idPacienteRecibido = idPaciente;
            return responder("cancelar");
        }
    }

    // ------------------------------------
    // VENTANA PARA LAS PRUEBAS
    // ------------------------------------

    private static class VentanaPrueba extends GestionMisCitasGUI {

        String ultimoError;

        public VentanaPrueba(MisCitasHttpClient cliente) {
            super(cliente);
        }

        @Override
        protected void mostrarError(String mensaje) {
            ultimoError = mensaje;
        }

        // Permite probar tambien el metodo original
        public void mostrarErrorOriginal(String mensaje) {
            super.mostrarError(mensaje);
        }
    }

    // ------------------------------------
    // PREPARAR Y FINALIZAR
    // ------------------------------------

    @BeforeEach
    public void preparar() throws Exception {

        // Las ventanas Swing necesitan una pantalla
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Se necesita un entorno grafico");

        SwingUtilities.invokeAndWait(() -> {

            cliente = new ClienteSimulado();
            ventana = new VentanaPrueba(cliente);

            escribir("txtIdPaciente", "5");
            escribir("txtIdCita", "3");
            escribir("txtMotivo", "No puedo asistir");
        });
    }

    @AfterEach
    public void finalizar() throws Exception {

        if (ventana != null) {
            SwingUtilities.invokeAndWait(() -> {
                ventana.dispose();
            });
        }
    }

    // ------------------------------------
    // METODOS AUXILIARES
    // ------------------------------------

    private Object obtenerCampo(String nombre) {

        try {
            Field campo = GestionMisCitasGUI.class
                    .getDeclaredField(nombre);

            campo.setAccessible(true);

            return campo.get(ventana);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void escribir(String nombre, String valor) {

        JTextField campo = (JTextField) obtenerCampo(nombre);
        campo.setText(valor);
    }

    private void cambiarCampo(String nombre, String valor)
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            escribir(nombre, valor);
        });
    }

    private String obtenerResultado() {

        JTextArea area = (JTextArea) obtenerCampo("areaResultado");
        return area.getText();
    }

    private JButton buscarBoton(Container contenedor,
            String texto) {

        for (Component componente : contenedor.getComponents()) {

            if (componente instanceof JButton) {

                JButton boton = (JButton) componente;

                if (boton.getText().equals(texto)) {
                    return boton;
                }
            }

            if (componente instanceof Container) {

                JButton encontrado = buscarBoton(
                        (Container) componente, texto);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    private void pulsar(String texto) throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            JButton boton = buscarBoton(
                    ventana.getContentPane(), texto);

            assertNotNull(boton);
            boton.doClick();
        });
    }

    // ------------------------------------
    // PRUEBAS DE CONSTRUCTORES
    // ------------------------------------

    @Test
    @DisplayName("Crear la ventana con cliente simulado")
    public void testConstructorConCliente() {

        assertNotNull(ventana);
        assertEquals("SESCAM - Gestion de mis citas",
                ventana.getTitle());

        assertEquals(650, ventana.getWidth());
        assertEquals(450, ventana.getHeight());
    }

    @Test
    @DisplayName("Crear la ventana con constructor normal")
    public void testConstructorNormal() throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            GestionMisCitasGUI otra =
                    new GestionMisCitasGUI();

            try {
                assertNotNull(otra);
                assertEquals("SESCAM - Gestion de mis citas",
                        otra.getTitle());
            } finally {
                otra.dispose();
            }
        });
    }

    @Test
    @DisplayName("El area de resultados no se puede editar")
    public void testAreaResultado() {

        JTextArea area =
                (JTextArea) obtenerCampo("areaResultado");

        assertNotNull(area);
        assertFalse(area.isEditable());
    }

    // ------------------------------------
    // PRUEBAS CORRECTAS
    // ------------------------------------

    @Test
    @DisplayName("Consultar mis citas correctamente")
    public void testConsultarCorrecto() throws Exception {

        pulsar("Consultar mis citas");

        assertEquals("consultar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Confirmar una cita correctamente")
    public void testConfirmarCorrecto() throws Exception {

        pulsar("Confirmar cita");

        assertEquals("confirmar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Solicitar un cambio correctamente")
    public void testCambioCorrecto() throws Exception {

        pulsar("Solicitar cambio");

        assertEquals("cambio", cliente.operacion);
        assertEquals("No puedo asistir", cliente.motivoRecibido);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Cancelar una cita correctamente")
    public void testCancelarCorrecto() throws Exception {

        pulsar("Cancelar cita");

        assertEquals("cancelar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    // ------------------------------------
    // PRUEBAS DE ID INCORRECTO
    // ------------------------------------

    @Test
    @DisplayName("Rechazar un ID de paciente incorrecto al consultar")
    public void testConsultarIdIncorrecto() throws Exception {

        cambiarCampo("txtIdPaciente", "abc");

        pulsar("Consultar mis citas");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID de paciente incorrecto al confirmar")
    public void testConfirmarIdPacienteIncorrecto() throws Exception {

        cambiarCampo("txtIdPaciente", "0");

        pulsar("Confirmar cita");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID de cita incorrecto al confirmar")
    public void testConfirmarIdCitaIncorrecto() throws Exception {

        cambiarCampo("txtIdCita", "-1");

        pulsar("Confirmar cita");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID de cita incorrecto al solicitar un cambio")
    public void testCambioIdIncorrecto() throws Exception {

        cambiarCampo("txtIdCita", "");

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID de cita incorrecto al cancelar")
    public void testCancelarIdIncorrecto() throws Exception {

        cambiarCampo("txtIdCita", "abc");

        pulsar("Cancelar cita");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    // ------------------------------------
    // PRUEBAS DEL MOTIVO
    // ------------------------------------

    @Test
    @DisplayName("Rechazar un motivo vacio al solicitar un cambio")
    public void testCambioMotivoVacio() throws Exception {

        cambiarCampo("txtMotivo", "   ");

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains("motivo"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un motivo demasiado largo")
    public void testCambioMotivoLargo() throws Exception {

        cambiarCampo("txtMotivo", "a".repeat(201));

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains("motivo"));
        assertNull(cliente.operacion);
    }

    // ------------------------------------
    // PRUEBAS DE ERROR DE CONEXION
    // ------------------------------------

    @Test
    @DisplayName("Mostrar un error si falla la conexion al consultar")
    public void testConsultarErrorConexion() throws Exception {

        cliente.errorConexion = true;

        pulsar("Consultar mis citas");

        assertTrue(ventana.ultimoError.contains(
                "Error al conectar con el servidor"));
    }

    @Test
    @DisplayName("Mostrar un error si falla la conexion al confirmar")
    public void testConfirmarErrorConexion() throws Exception {

        cliente.errorConexion = true;

        pulsar("Confirmar cita");

        assertTrue(ventana.ultimoError.contains(
                "Error al conectar con el servidor"));
    }

    @Test
    @DisplayName("Mostrar un error si falla la conexion al solicitar un cambio")
    public void testCambioErrorConexion() throws Exception {

        cliente.errorConexion = true;

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains(
                "Error al conectar con el servidor"));
    }

    @Test
    @DisplayName("Mostrar un error si falla la conexion al cancelar")
    public void testCancelarErrorConexion() throws Exception {

        cliente.errorConexion = true;

        pulsar("Cancelar cita");

        assertTrue(ventana.ultimoError.contains(
                "Error al conectar con el servidor"));
    }

    // ------------------------------------
    // PRUEBA DEL METODO ORIGINAL DE ERRORES
    // ------------------------------------

    @Test
    @DisplayName("Mostrar el dialogo de error original")
    public void testMostrarErrorOriginal() throws Exception {

        AtomicBoolean dialogoEncontrado = new AtomicBoolean(false);

        // Cerrar el dialogo automaticamente cuando aparezca
        Timer temporizador = new Timer(300, e -> {

            for (Window ventanaAbierta : Window.getWindows()) {

                if (ventanaAbierta instanceof JDialog
                        && ventanaAbierta.isShowing()) {

                    dialogoEncontrado.set(true);
                    ventanaAbierta.dispose();
                }
            }
        });

        temporizador.setRepeats(false);
        temporizador.start();

        SwingUtilities.invokeAndWait(() -> {
            ventana.mostrarErrorOriginal("Error de prueba");
        });

        assertTrue(dialogoEncontrado.get());
    }

    // ------------------------------------
    // PRUEBAS DE LOS DATOS ENVIADOS AL CLIENTE
    // ------------------------------------

    @Test
    @DisplayName("Enviar el ID de paciente al consultar")
    public void testConsultarEnviaIdPaciente() throws Exception {

        pulsar("Consultar mis citas");

        assertEquals(5, cliente.idPacienteRecibido);
        assertEquals(-1, cliente.idCitaRecibido);
    }

    @Test
    @DisplayName("Consultar sin necesitar ID de cita ni motivo")
    public void testConsultarIgnoraOtrosCampos() throws Exception {

        cambiarCampo("txtIdCita", "abc");
        cambiarCampo("txtMotivo", "");

        pulsar("Consultar mis citas");

        assertEquals("consultar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Enviar cada ID en su posicion al confirmar")
    public void testConfirmarOrdenArgumentos() throws Exception {

        pulsar("Confirmar cita");

        assertEquals(3, cliente.idCitaRecibido);
        assertEquals(5, cliente.idPacienteRecibido);
    }

    @Test
    @DisplayName("Enviar cada ID en su posicion al solicitar un cambio")
    public void testCambioOrdenArgumentos() throws Exception {

        pulsar("Solicitar cambio");

        assertEquals(3, cliente.idCitaRecibido);
        assertEquals(5, cliente.idPacienteRecibido);
    }

    @Test
    @DisplayName("Enviar cada ID en su posicion al cancelar")
    public void testCancelarOrdenArgumentos() throws Exception {

        pulsar("Cancelar cita");

        assertEquals(3, cliente.idCitaRecibido);
        assertEquals(5, cliente.idPacienteRecibido);
    }

    @Test
    @DisplayName("Quitar los espacios de los IDs")
    public void testIdsConEspacios() throws Exception {

        cambiarCampo("txtIdPaciente", " 7 ");
        cambiarCampo("txtIdCita", "  9");

        pulsar("Confirmar cita");

        assertEquals(9, cliente.idCitaRecibido);
        assertEquals(7, cliente.idPacienteRecibido);
    }

    @Test
    @DisplayName("Quitar los espacios del motivo antes de enviarlo")
    public void testMotivoSeRecorta() throws Exception {

        cambiarCampo("txtMotivo", "   Cambio de turno  ");

        pulsar("Solicitar cambio");

        assertEquals("Cambio de turno", cliente.motivoRecibido);
    }

    @Test
    @DisplayName("Aceptar un motivo de 200 caracteres")
    public void testMotivoLongitudMaxima() throws Exception {

        cambiarCampo("txtMotivo", "a".repeat(200));

        pulsar("Solicitar cambio");

        assertEquals("cambio", cliente.operacion);
        assertEquals(200, cliente.motivoRecibido.length());
    }

    // ------------------------------------
    // PRUEBAS DE ID DE PACIENTE INCORRECTO
    // ------------------------------------

    @Test
    @DisplayName("Rechazar un ID de paciente incorrecto al solicitar un cambio")
    public void testCambioIdPacienteIncorrecto() throws Exception {

        cambiarCampo("txtIdPaciente", "-3");

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID de paciente incorrecto al cancelar")
    public void testCancelarIdPacienteIncorrecto() throws Exception {

        cambiarCampo("txtIdPaciente", "");

        pulsar("Cancelar cita");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Validar el ID antes que el motivo al solicitar un cambio")
    public void testCambioIdAntesQueMotivo() throws Exception {

        cambiarCampo("txtIdCita", "x");
        cambiarCampo("txtMotivo", "");

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains("ID"));
    }

    @Test
    @DisplayName("Mostrar en el mensaje el limite del motivo")
    public void testMensajeMotivoIncluyeLimite() throws Exception {

        cambiarCampo("txtMotivo", "");

        pulsar("Solicitar cambio");

        assertTrue(ventana.ultimoError.contains("200"));
    }

    @Test
    @DisplayName("No cambiar el resultado si hay un error de validacion")
    public void testErrorNoBorraResultado() throws Exception {

        pulsar("Consultar mis citas");
        cambiarCampo("txtIdPaciente", "abc");
        pulsar("Consultar mis citas");

        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Mostrar el detalle del fallo de conexion")
    public void testErrorConexionIncluyeDetalle() throws Exception {

        cliente.errorConexion = true;

        pulsar("Consultar mis citas");

        assertEquals(
                "Error al conectar con el servidor: Servidor no disponible",
                ventana.ultimoError);
        assertEquals("", obtenerResultado());
    }

    @Test
    @DisplayName("Mostrar el mensaje de ID incorrecto completo")
    public void testMensajeIdIncorrecto() throws Exception {

        cambiarCampo("txtIdPaciente", "abc");

        pulsar("Consultar mis citas");

        assertEquals("El ID debe ser un numero entero mayor que 0",
                ventana.ultimoError);
    }

    // ------------------------------------
    // PRUEBA DEL METODO MAIN
    // ------------------------------------

    @Test
    @DisplayName("Ejecutar el metodo main y mostrar la ventana")
    public void testMain() throws Exception {

        GestionMisCitasGUI.main(new String[0]);

        // Esperar a que el hilo de Swing cree la ventana
        SwingUtilities.invokeAndWait(() -> { });

        GestionMisCitasGUI encontrada = null;

        for (Window w : Window.getWindows()) {

            if (w instanceof GestionMisCitasGUI
                    && w != ventana
                    && w.isVisible()) {

                encontrada = (GestionMisCitasGUI) w;
            }
        }

        assertNotNull(encontrada);

        GestionMisCitasGUI abierta = encontrada;
        SwingUtilities.invokeAndWait(abierta::dispose);
    }
}
