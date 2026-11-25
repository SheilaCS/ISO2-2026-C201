package com.sescam.radiodiagnostico.cliente.parametros;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParametroGUITest {

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    private static class ClienteSimulado extends ParametrosHttpClient {

        String operacion;
        String claveRecibida;
        String valorRecibido;
        boolean errorConexion = false;

        @Override
        public String guardarParametro(String clave, String valor) {
            operacion = "guardar";
            claveRecibida = clave;
            valorRecibido = valor;

            if (errorConexion) {
                throw new RuntimeException("Servidor no disponible");
            }

            return "Parametro guardado";
        }

        @Override
        public String consultarParametro(String clave) {
            operacion = "consultar";
            claveRecibida = clave;

            if (errorConexion) {
                throw new RuntimeException("Servidor no disponible");
            }

            return "Parametro encontrado";
        }
    }

    private static class VentanaPrueba extends ParametroGUI {

        String ultimoError;

        public VentanaPrueba(ParametrosHttpClient cliente) {
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

    @BeforeEach
    void preparar() throws Exception {

        assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "Se necesita entorno grafico");

        SwingUtilities.invokeAndWait(() -> {

            cliente = new ClienteSimulado();
            ventana = new VentanaPrueba(cliente);

            escribir("txtClave", "MAX_PACIENTES");
            escribir("txtValor", "100");
        });
    }

    @AfterEach
    void finalizar() throws Exception {

        if (ventana != null) {
            SwingUtilities.invokeAndWait(
                    () -> ventana.dispose());
        }
    }

    private Object obtenerCampo(String nombre) {

        try {

            Field campo =
                    ParametroGUI.class.getDeclaredField(nombre);

            campo.setAccessible(true);

            return campo.get(ventana);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void escribir(String nombre, String valor) {

        ((JTextField) obtenerCampo(nombre))
                .setText(valor);
    }

    private void cambiarCampo(
            String nombre,
            String valor) throws Exception {

        SwingUtilities.invokeAndWait(
                () -> escribir(nombre, valor));
    }

    private String obtenerResultado() {

        return ((JTextArea)
                obtenerCampo("areaResultado"))
                .getText();
    }

    private JButton buscarBoton(
            Container contenedor,
            String texto) {

        for (Component c : contenedor.getComponents()) {

            if (c instanceof JButton boton
                    && texto.equals(boton.getText())) {

                return boton;
            }

            if (c instanceof Container hijo) {

                JButton resultado =
                        buscarBoton(hijo, texto);

                if (resultado != null) {
                    return resultado;
                }
            }
        }

        return null;
    }

    private void pulsar(String texto) throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            JButton boton =
                    buscarBoton(
                            ventana.getContentPane(),
                            texto);

            assertNotNull(boton);

            boton.doClick();
        });
    }

    @Test
    @DisplayName("Constructor con cliente simulado")
    void testConstructorConCliente() {

        assertNotNull(ventana);

        assertEquals(
                "SESCAM - Configuración de Parámetros (CDU5)",
                ventana.getTitle());

        assertEquals(500, ventana.getWidth());
        assertEquals(400, ventana.getHeight());
    }

    @Test
    @DisplayName("Constructor normal")
    void testConstructorNormal() throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            ParametroGUI otra =
                    new ParametroGUI();

            try {
                assertNotNull(otra);
            } finally {
                otra.dispose();
            }
        });
    }

    @Test
    @DisplayName("Area resultado no editable")
    void testAreaResultado() {

        JTextArea area =
                (JTextArea)
                        obtenerCampo("areaResultado");

        assertFalse(area.isEditable());
    }

    @Test
    @DisplayName("Guardar parametro correctamente")
    void testGuardarCorrecto() throws Exception {

        pulsar("Guardar");

        assertEquals(
                "guardar",
                cliente.operacion);

        assertEquals(
                "MAX_PACIENTES",
                cliente.claveRecibida);

        assertEquals(
                "100",
                cliente.valorRecibido);

        assertEquals(
                "Parametro guardado",
                obtenerResultado());
    }

    @Test
    @DisplayName("Consultar parametro correctamente")
    void testConsultarCorrecto() throws Exception {

        pulsar("Consultar");

        assertEquals(
                "consultar",
                cliente.operacion);

        assertEquals(
                "MAX_PACIENTES",
                cliente.claveRecibida);

        assertEquals(
                "Parametro encontrado",
                obtenerResultado());
    }

    @Test
    @DisplayName("Guardar con clave vacia")
    void testGuardarClaveVacia() throws Exception {

        cambiarCampo("txtClave", "   ");

        pulsar("Guardar");

        assertTrue(
                ventana.ultimoError
                        .contains("clave"));

        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Consultar con clave vacia")
    void testConsultarClaveVacia() throws Exception {

        cambiarCampo("txtClave", "");

        pulsar("Consultar");

        assertTrue(
                ventana.ultimoError
                        .contains("consultar"));

        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Error de conexion al guardar")
    void testGuardarErrorConexion() throws Exception {

        cliente.errorConexion = true;

        pulsar("Guardar");

        assertTrue(
                ventana.ultimoError
                        .contains("Error al conectar"));
    }

    @Test
    @DisplayName("Error de conexion al consultar")
    void testConsultarErrorConexion() throws Exception {

        cliente.errorConexion = true;

        pulsar("Consultar");

        assertTrue(
                ventana.ultimoError
                        .contains("Error al conectar"));
    }

    @Test
    @DisplayName("Recortar espacios")
    void testRecorteEspacios() throws Exception {

        cambiarCampo(
                "txtClave",
                "  MAX_PACIENTES  ");

        cambiarCampo(
                "txtValor",
                "  200  ");

        pulsar("Guardar");

        assertEquals(
                "MAX_PACIENTES",
                cliente.claveRecibida);

        assertEquals(
                "200",
                cliente.valorRecibido);
    }

    @Test
    @DisplayName("No borrar resultado tras error")
    void testErrorNoBorraResultado()
            throws Exception {

        pulsar("Guardar");

        cambiarCampo("txtClave", "");

        pulsar("Guardar");

        assertEquals(
                "Parametro guardado",
                obtenerResultado());
    }

    @Test
    @DisplayName("Mostrar dialogo de error")
    void testMostrarErrorOriginal()
            throws Exception {

        AtomicBoolean encontrado =
                new AtomicBoolean(false);

        Timer timer = new Timer(
                300,
                e -> {

                    for (Window w
                            : Window.getWindows()) {

                        if (w instanceof JDialog
                                && w.isShowing()) {

                            encontrado.set(true);
                            w.dispose();
                        }
                    }
                });

        timer.setRepeats(false);
        timer.start();

        SwingUtilities.invokeAndWait(
                () -> ventana.mostrarErrorOriginal(
                        "Error prueba"));

        assertTrue(encontrado.get());
    }

    @Test
    @DisplayName("Ejecutar main")
    void testMain() throws Exception {

        ParametroGUI.main(new String[0]);

        SwingUtilities.invokeAndWait(() -> { });

        ParametroGUI abierta = null;

        for (Window w : Window.getWindows()) {

            if (w instanceof ParametroGUI
                    && w != ventana
                    && w.isVisible()) {

                abierta = (ParametroGUI) w;
            }
        }

        assertNotNull(abierta);

        ParametroGUI finalAbierta = abierta;

        SwingUtilities.invokeAndWait(
                finalAbierta::dispose);
    }
}