package com.sescam.radiodiagnostico.cliente.solicitudes;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SolicitudGUITest {

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    // ------------------------------------
    // CLIENTE HTTP SIMULADO
    // ------------------------------------
    private static class ClienteSimulado extends SolicitudHttpClient {

        String operacion;
        int idPacienteRecibido = -1;
        int idSolicitudRecibido = -1;
        String tipoRecibido;
        String prioridadRecibida;
        boolean errorConexion = false;

        @Override
        public String crearSolicitud(int idPaciente, String tipo, String prioridad) {
            operacion = "crear";
            idPacienteRecibido = idPaciente;
            tipoRecibido = tipo;
            prioridadRecibida = prioridad;
            if (errorConexion) {
                throw new RuntimeException("Servidor no disponible");
            }
            return "Solicitud creada correctamente";
        }

        @Override
        public String consultarSolicitud(int idSolicitud) {
            operacion = "consultar";
            idSolicitudRecibido = idSolicitud;
            if (errorConexion) {
                throw new RuntimeException("Servidor no disponible");
            }
            return "Solicitud consultada correctamente";
        }
    }

    // ------------------------------------
    // VENTANA PARA LAS PRUEBAS
    // ------------------------------------
    private static class VentanaPrueba extends SolicitudGUI {

        String ultimoError;

        public VentanaPrueba(SolicitudHttpClient cliente) {
            super(cliente);
        }

        @Override
        protected void mostrarError(String mensaje) {
            ultimoError = mensaje;
        }

        public void mostrarErrorOriginal(String mensaje) {
            super.mostrarError(mensaje);
        }
    }

    // ------------------------------------
    // PREPARAR Y FINALIZAR
    // ------------------------------------
    @BeforeEach
    public void preparar() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Se necesita un entorno grafico");

        SwingUtilities.invokeAndWait(() -> {
            cliente = new ClienteSimulado();
            ventana = new VentanaPrueba(cliente);

            escribir("txtIdPaciente", "5");
            escribir("txtIdSolicitud", "3");
            escribir("txtTipoPrueba", "TAC");
            escribir("txtPrioridad", "URGENTE");
        });
    }

    @AfterEach
    public void finalizar() throws Exception {
        if (ventana != null) {
            SwingUtilities.invokeAndWait(() -> ventana.dispose());
        }
    }

    // ------------------------------------
    // AUXILIARES
    // ------------------------------------
    private Object obtenerCampo(String nombre) {
        try {
            Field campo = SolicitudGUI.class.getDeclaredField(nombre);
            campo.setAccessible(true);
            return campo.get(ventana);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void escribir(String nombre, String valor) {
        ((JTextField) obtenerCampo(nombre)).setText(valor);
    }

    private void cambiarCampo(String nombre, String valor) throws Exception {
        SwingUtilities.invokeAndWait(() -> escribir(nombre, valor));
    }

    private String obtenerResultado() {
        return ((JTextArea) obtenerCampo("areaResultado")).getText();
    }

    private JButton buscarBoton(Container contenedor, String texto) {
        for (Component c : contenedor.getComponents()) {
            if (c instanceof JButton b && b.getText().equals(texto)) return b;
            if (c instanceof Container cc) {
                JButton r = buscarBoton(cc, texto);
                if (r != null) return r;
            }
        }
        return null;
    }

    private void pulsar(String texto) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton boton = buscarBoton(ventana.getContentPane(), texto);
            assertNotNull(boton, "No se encontro el boton: " + texto);
            boton.doClick();
        });
    }

    // ------------------------------------
    // CONSTRUCTORES Y CONFIGURACION
    // ------------------------------------
    @Test
    @DisplayName("Crear la ventana con cliente simulado")
    public void testConstructorConCliente() {
        assertNotNull(ventana);
        assertEquals("SESCAM - Módulo de Solicitudes (CDU2)", ventana.getTitle());
        assertEquals(650, ventana.getWidth());
        assertEquals(500, ventana.getHeight());
    }

    @Test
    @DisplayName("Crear la ventana con constructor normal")
    public void testConstructorNormal() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SolicitudGUI otra = new SolicitudGUI();
            try {
                assertNotNull(otra);
                assertEquals("SESCAM - Módulo de Solicitudes (CDU2)", otra.getTitle());
            } finally {
                otra.dispose();
            }
        });
    }

    @Test
    @DisplayName("El area de resultados no se puede editar")
    public void testAreaResultado() {
        JTextArea area = (JTextArea) obtenerCampo("areaResultado");
        assertNotNull(area);
        assertFalse(area.isEditable());
    }

    @Test
    @DisplayName("Crear solicitud correctamente")
    public void testCrearCorrecto() throws Exception {
        pulsar("Crear Solicitud");

        assertEquals("crear", cliente.operacion);
        assertEquals(5, cliente.idPacienteRecibido);
        assertEquals("TAC", cliente.tipoRecibido);
        assertEquals("URGENTE", cliente.prioridadRecibida);
        assertEquals("Solicitud creada correctamente", obtenerResultado());
    }

    @Test
    @DisplayName("Rechazar ID paciente incorrecto al crear")
    public void testCrearIdPacienteIncorrecto() throws Exception {
        cambiarCampo("txtIdPaciente", "abc");
        pulsar("Crear Solicitud");

        assertTrue(ventana.ultimoError.contains("ID de paciente"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar tipo de prueba vacio")
    public void testCrearTipoVacio() throws Exception {
        cambiarCampo("txtTipoPrueba", "   ");
        pulsar("Crear Solicitud");

        assertTrue(ventana.ultimoError.contains("tipo de prueba"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar prioridad invalida")
    public void testCrearPrioridadInvalida() throws Exception {
        cambiarCampo("txtPrioridad", "BAJA");
        pulsar("Crear Solicitud");

        assertTrue(ventana.ultimoError.contains("Prioridad"));
        assertNull(cliente.operacion);
    }


    @Test
    @DisplayName("Consultar solicitud correctamente")
    public void testConsultarCorrecto() throws Exception {
        pulsar("Consultar Solicitud");

        assertEquals("consultar", cliente.operacion);
        assertEquals(3, cliente.idSolicitudRecibido);
        assertEquals("Solicitud consultada correctamente", obtenerResultado());
    }

    @Test
    @DisplayName("Rechazar ID solicitud incorrecto al consultar")
    public void testConsultarIdIncorrecto() throws Exception {
        cambiarCampo("txtIdSolicitud", "0");
        pulsar("Consultar Solicitud");

        assertTrue(ventana.ultimoError.contains("ID de solicitud"));
        assertNull(cliente.operacion);
    }

    // ------------------------------------
    // ERRORES DE CONEXION
    // ------------------------------------
    @Test
    @DisplayName("Error de conexion al crear")
    public void testCrearErrorConexion() throws Exception {
        cliente.errorConexion = true;
        pulsar("Crear Solicitud");

        assertTrue(ventana.ultimoError.contains("Error al conectar con el servidor"));
    }

    @Test
    @DisplayName("Error de conexion al consultar")
    public void testConsultarErrorConexion() throws Exception {
        cliente.errorConexion = true;
        pulsar("Consultar Solicitud");

        assertTrue(ventana.ultimoError.contains("Error al conectar con el servidor"));
    }

    // ------------------------------------
    // DATOS ENVIADOS AL CLIENTE
    // ------------------------------------
    @Test
    @DisplayName("Recortar espacios en IDs, tipo y prioridad")
    public void testRecorteEspacios() throws Exception {
        cambiarCampo("txtIdPaciente", " 42 ");
        cambiarCampo("txtTipoPrueba", "  RX  ");
        cambiarCampo("txtPrioridad", "  PREFERENTE  ");
        pulsar("Crear Solicitud");

        assertEquals(42, cliente.idPacienteRecibido);
        assertEquals("RX", cliente.tipoRecibido);
        assertEquals("PREFERENTE", cliente.prioridadRecibida);
    }

    @Test
    @DisplayName("Aceptar prioridad ORDINARIO")
    public void testPrioridadOrdinario() throws Exception {
        cambiarCampo("txtPrioridad", "ORDINARIO");
        pulsar("Crear Solicitud");

        assertEquals("crear", cliente.operacion);
        assertEquals("ORDINARIO", cliente.prioridadRecibida);
    }

    @Test
    @DisplayName("No borrar el resultado si hay error de validacion")
    public void testErrorNoBorraResultado() throws Exception {
        pulsar("Crear Solicitud"); // resultado OK
        cambiarCampo("txtIdPaciente", "abc");
        pulsar("Crear Solicitud"); // error
        assertEquals("Solicitud creada correctamente", obtenerResultado());
    }

    @Test
    @DisplayName("Mostrar el detalle completo del fallo de conexion")
    public void testErrorConexionIncluyeDetalle() throws Exception {
        cliente.errorConexion = true;
        pulsar("Crear Solicitud");
        assertEquals("Error al conectar con el servidor: Servidor no disponible",
                ventana.ultimoError);
        assertEquals("", obtenerResultado());
    }

    // ------------------------------------
    // METODO ORIGINAL DE ERRORES (JOptionPane)
    // ------------------------------------
    @Test
    @DisplayName("Mostrar el dialogo de error original")
    public void testMostrarErrorOriginal() throws Exception {
        AtomicBoolean dialogoEncontrado = new AtomicBoolean(false);

        Timer temporizador = new Timer(300, e -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isShowing()) {
                    dialogoEncontrado.set(true);
                    w.dispose();
                }
            }
        });
        temporizador.setRepeats(false);
        temporizador.start();

        SwingUtilities.invokeAndWait(() ->
                ventana.mostrarErrorOriginal("Error de prueba"));

        assertTrue(dialogoEncontrado.get());
    }

    // ------------------------------------
    // MAIN
    // ------------------------------------
    @Test
    @DisplayName("Ejecutar el metodo main y mostrar la ventana")
    public void testMain() throws Exception {
        SolicitudGUI.main(new String[0]);
        SwingUtilities.invokeAndWait(() -> { });

        SolicitudGUI encontrada = null;
        for (Window w : Window.getWindows()) {
            if (w instanceof SolicitudGUI && w != ventana && w.isVisible()) {
                encontrada = (SolicitudGUI) w;
            }
        }
        assertNotNull(encontrada);

        SolicitudGUI abierta = encontrada;
        SwingUtilities.invokeAndWait(abierta::dispose);
    }
}