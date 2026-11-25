package com.sescam.radiodiagnostico.cliente.informes;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InformeGUITest {

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    private static class ClienteSimulado extends InformesHttpClient {

        String operacion;
        int idRecibido;
        String fechaRecibida;
        String descripcionRecibida;
        String rolRecibido;
        boolean errorConexion = false;

        @Override
        public String generarInforme(int idEquipo,
                                     String fecha,
                                     String descripcion,
                                     String rol) {

            operacion = "generar";
            idRecibido = idEquipo;
            fechaRecibida = fecha;
            descripcionRecibida = descripcion;
            rolRecibido = rol;

            if (errorConexion) {
                throw new RuntimeException("Servidor no disponible");
            }

            return "Informe generado";
        }

        @Override
        public String consultarInforme(int idInforme,
                                       String rol) {

            operacion = "consultar";
            idRecibido = idInforme;
            rolRecibido = rol;

            if (errorConexion) {
                throw new RuntimeException("Servidor no disponible");
            }

            return "Informe encontrado";
        }
    }

    private static class VentanaPrueba extends InformeGUI {

        String ultimoError;

        public VentanaPrueba(InformesHttpClient cliente) {
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
                "Se necesita entorno gráfico");

        SwingUtilities.invokeAndWait(() -> {

            cliente = new ClienteSimulado();
            ventana = new VentanaPrueba(cliente);

            escribir("txtRol", "TECNICO");
            escribir("txtIdEquipo", "10");
            escribir("txtFecha", "2026-12-01");
            escribir("txtDescripcion", "Error de calibracion");
            escribir("txtIdInforme", "20");
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
                    InformeGUI.class.getDeclaredField(nombre);

            campo.setAccessible(true);

            return campo.get(ventana);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void escribir(String campo,
                          String valor) {

        ((JTextField) obtenerCampo(campo))
                .setText(valor);
    }

    private void cambiarCampo(
            String campo,
            String valor) throws Exception {

        SwingUtilities.invokeAndWait(
                () -> escribir(campo, valor));
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

            if (c instanceof JButton b &&
                    texto.equals(b.getText())) {

                return b;
            }

            if (c instanceof Container cc) {

                JButton resultado =
                        buscarBoton(cc, texto);

                if (resultado != null) {
                    return resultado;
                }
            }
        }

        return null;
    }

    private void pulsar(String texto)
            throws Exception {

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
    void testConstructor() {

        assertEquals(
                "SESCAM - Informes de Mantenimiento (CDU8)",
                ventana.getTitle());

        assertEquals(650, ventana.getWidth());
        assertEquals(500, ventana.getHeight());
    }

    @Test
    void testAreaResultadoNoEditable() {

        JTextArea area =
                (JTextArea)
                        obtenerCampo("areaResultado");

        assertFalse(area.isEditable());
    }

    @Test
    void testGenerarInformeCorrecto()
            throws Exception {

        pulsar("Generar Informe");

        assertEquals(
                "generar",
                cliente.operacion);

        assertEquals(
                "Informe generado",
                obtenerResultado());
    }

    @Test
    void testConsultarInformeCorrecto()
            throws Exception {

        pulsar("Consultar Informe");

        assertEquals(
                "consultar",
                cliente.operacion);

        assertEquals(
                "Informe encontrado",
                obtenerResultado());
    }

    @Test
    void testRolSinPermisosGenerar()
            throws Exception {

        cambiarCampo("txtRol", "USUARIO");

        pulsar("Generar Informe");

        assertTrue(
                ventana.ultimoError
                        .contains("Acceso denegado"));
    }

    @Test
    void testRolSinPermisosConsultar()
            throws Exception {

        cambiarCampo("txtRol", "USUARIO");

        pulsar("Consultar Informe");

        assertTrue(
                ventana.ultimoError
                        .contains("Acceso denegado"));
    }

    @Test
    void testIdEquipoIncorrecto()
            throws Exception {

        cambiarCampo("txtIdEquipo", "abc");

        pulsar("Generar Informe");

        assertTrue(
                ventana.ultimoError
                        .contains("ID de equipo"));
    }

    @Test
    void testFechaIncorrecta()
            throws Exception {

        cambiarCampo("txtFecha", "01/01/2026");

        pulsar("Generar Informe");

        assertTrue(
                ventana.ultimoError
                        .contains("formato"));
    }

    @Test
    void testDescripcionVacia()
            throws Exception {

        cambiarCampo("txtDescripcion", " ");

        pulsar("Generar Informe");

        assertTrue(
                ventana.ultimoError
                        .contains("descripción"));
    }

    @Test
    void testIdInformeIncorrecto()
            throws Exception {

        cambiarCampo("txtIdInforme", "-1");

        pulsar("Consultar Informe");

        assertTrue(
                ventana.ultimoError
                        .contains("ID de informe"));
    }

    @Test
    void testErrorConexionGenerar()
            throws Exception {

        cliente.errorConexion = true;

        pulsar("Generar Informe");

        assertTrue(
                ventana.ultimoError
                .contains(
                    "Error al conectar con el servidor"));
    }

    @Test
    void testErrorConexionConsultar()
            throws Exception {

        cliente.errorConexion = true;

        pulsar("Consultar Informe");

        assertTrue(
                ventana.ultimoError
                .contains(
                    "Error al conectar con el servidor"));
    }

    @Test
    void testTrimDatos()
            throws Exception {

        cambiarCampo("txtIdEquipo", " 10 ");
        cambiarCampo("txtFecha",
                " 2026-12-01 ");
        cambiarCampo("txtDescripcion",
                " Averia ");
        cambiarCampo("txtRol",
                " TECNICO ");

        pulsar("Generar Informe");

        assertEquals(10,
                cliente.idRecibido);

        assertEquals("2026-12-01",
                cliente.fechaRecibida);

        assertEquals("Averia",
                cliente.descripcionRecibida);

        assertEquals("TECNICO",
                cliente.rolRecibido);
    }

    @Test
    void testMain()
            throws Exception {

        InformeGUI.main(new String[0]);

        SwingUtilities.invokeAndWait(() -> {});

        InformeGUI encontrada = null;

        for (Window w : Window.getWindows()) {

            if (w instanceof InformeGUI
                    && w != ventana
                    && w.isVisible()) {

                encontrada =
                        (InformeGUI) w;
            }
        }

        assertNotNull(encontrada);

        InformeGUI abierta = encontrada;

        SwingUtilities.invokeAndWait(
                abierta::dispose);
    }

    @Test
    void testMostrarErrorOriginal()
            throws Exception {

        AtomicBoolean mostrada =
                new AtomicBoolean(false);

        Timer timer = new Timer(300, e -> {

            for (Window w :
                    Window.getWindows()) {

                if (w instanceof JDialog
                        && w.isShowing()) {

                    mostrada.set(true);
                    w.dispose();
                }
            }
        });

        timer.setRepeats(false);
        timer.start();

        SwingUtilities.invokeAndWait(
                () -> ventana.mostrarErrorOriginal(
                        "Error de prueba"));

        assertTrue(mostrada.get());
    }
}