package com.sescam.radiodiagnostico.cliente.seguridad;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeguridadGUITest {

    private SeguridadGUI ventana;
    private ClienteSimulado cliente;

    private static class ClienteSimulado extends SeguridadHttpClient {

        boolean errorConexion;

        String usuarioRecibido;
        String rolRecibido;
        String accionRecibida;

        @Override
        public String verificarPermiso(
                String usuario,
                String rol,
                String accion) {

            usuarioRecibido = usuario;
            rolRecibido = rol;
            accionRecibida = accion;

            if (errorConexion) {
                throw new RuntimeException(
                        "Servidor no disponible");
            }

            return "PERMITIDO";
        }

        @Override
        public String obtenerLogs(
                String rol) {

            rolRecibido = rol;

            if (errorConexion) {
                throw new RuntimeException(
                        "Servidor no disponible");
            }

            return "LOGS DEL SISTEMA";
        }
    }

    @BeforeEach
    void preparar() throws Exception {

        assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "Se necesita entorno gráfico");

        SwingUtilities.invokeAndWait(() -> {

            cliente = new ClienteSimulado();

            ventana = new SeguridadGUI(cliente);

            escribir("txtUsuario", "juan");
            escribir("txtRol", "ADMINISTRADOR");
            escribir("txtAccion", "CONSULTAR");
        });
    }

    @AfterEach
    void finalizar() throws Exception {

        if (ventana != null) {

            SwingUtilities.invokeAndWait(
                    () -> ventana.dispose());
        }
    }

    private Object obtenerCampo(
            String nombre) {

        try {

            Field campo =
                    SeguridadGUI.class
                            .getDeclaredField(nombre);

            campo.setAccessible(true);

            return campo.get(ventana);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private void escribir(
            String campo,
            String valor) {

        ((JTextField)
                obtenerCampo(campo))
                .setText(valor);
    }

    private void cambiarCampo(
            String campo,
            String valor)
            throws Exception {

        SwingUtilities.invokeAndWait(
                () -> escribir(campo, valor));
    }

    private JTextArea areaResultado() {

        return (JTextArea)
                obtenerCampo("areaResultado");
    }

    private JButton buscarBoton(
            Container contenedor,
            String texto) {

        for (Component c :
                contenedor.getComponents()) {

            if (c instanceof JButton b
                    && texto.equals(
                    b.getText())) {

                return b;
            }

            if (c instanceof Container hijo) {

                JButton resultado =
                        buscarBoton(
                                hijo,
                                texto);

                if (resultado != null) {
                    return resultado;
                }
            }
        }

        return null;
    }

    private void pulsar(
            String texto)
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
    @DisplayName("Constructor correcto")
    void testConstructor() {

        assertEquals(
                "SESCAM - Gestión de Seguridad y Calidad (CDU11)",
                ventana.getTitle());

        assertEquals(
                600,
                ventana.getWidth());

        assertEquals(
                500,
                ventana.getHeight());
    }

    @Test
    @DisplayName("Area resultado no editable")
    void testAreaResultado() {

        assertFalse(
                areaResultado()
                        .isEditable());
    }

    @Test
    @DisplayName("Verificar permiso correctamente")
    void testVerificarPermiso()
            throws Exception {

        pulsar(
                "Verificar Permiso");

        assertEquals(
                "juan",
                cliente.usuarioRecibido);

        assertEquals(
                "ADMINISTRADOR",
                cliente.rolRecibido);

        assertEquals(
                "CONSULTAR",
                cliente.accionRecibida);

        assertEquals(
                "PERMITIDO",
                areaResultado().getText());
    }

    @Test
    @DisplayName("Ver logs correctamente")
    void testVerLogs()
            throws Exception {

        pulsar(
                "Ver Logs (Solo Admin)");

        assertEquals(
                "ADMINISTRADOR",
                cliente.rolRecibido);

        assertEquals(
                "LOGS DEL SISTEMA",
                areaResultado().getText());
    }

    @Test
    @DisplayName("Error de conexión al verificar")
    void testErrorVerificar()
            throws Exception {

        cliente.errorConexion = true;

        pulsar(
                "Verificar Permiso");

        assertTrue(
                areaResultado()
                        .getText()
                        .contains(
                                "Error al conectar"));
    }

    @Test
    @DisplayName("Error de conexión al obtener logs")
    void testErrorLogs()
            throws Exception {

        cliente.errorConexion = true;

        pulsar(
                "Ver Logs (Solo Admin)");

        assertTrue(
                areaResultado()
                        .getText()
                        .contains(
                                "Error al conectar"));
    }

    @Test
    @DisplayName("Recortar espacios de los campos")
    void testTrim()
            throws Exception {

        cambiarCampo(
                "txtUsuario",
                " juan ");

        cambiarCampo(
                "txtRol",
                " ADMINISTRADOR ");

        cambiarCampo(
                "txtAccion",
                " CONSULTAR ");

        pulsar(
                "Verificar Permiso");

        assertEquals(
                "juan",
                cliente.usuarioRecibido);

        assertEquals(
                "ADMINISTRADOR",
                cliente.rolRecibido);

        assertEquals(
                "CONSULTAR",
                cliente.accionRecibida);
    }

    @Test
    @DisplayName("Ejecutar main")
    void testMain()
            throws Exception {

        SeguridadGUI.main(
                new String[0]);

        SwingUtilities.invokeAndWait(
                () -> {
                });

        SeguridadGUI abierta = null;

        for (Window w :
                Window.getWindows()) {

            if (w instanceof SeguridadGUI
                    && w != ventana
                    && w.isVisible()) {

                abierta =
                        (SeguridadGUI) w;
            }
        }

        assertNotNull(abierta);

        SeguridadGUI finalAbierta =
                abierta;

        SwingUtilities.invokeAndWait(
                finalAbierta::dispose);
    }
}