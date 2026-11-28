package com.sescam.radiodiagnostico.cliente.seguridad;

import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SeguridadGUITest {

    private Object campo(
            SeguridadGUI gui,
            String nombre)
            throws Exception {

        Field f =
                SeguridadGUI.class
                        .getDeclaredField(nombre);

        f.setAccessible(true);

        return f.get(gui);
    }

    @Test
    void constructor()
            throws Exception {

        SeguridadGUI gui =
                new SeguridadGUI(
                        new SeguridadHttpClient(
                                "http://localhost"));

        assertEquals(
                "SESCAM - Auditoría de actividades y accesos (CDU11)",
                gui.getTitle());

        gui.dispose();
    }

    @Test
    void areaNoEditable()
            throws Exception {

        SeguridadGUI gui =
                new SeguridadGUI(
                        new SeguridadHttpClient(
                                "http://localhost"));

        JTextArea area =
                (JTextArea)
                        campo(
                                gui,
                                "areaResultado");

        assertFalse(
                area.isEditable());

        gui.dispose();
    }

    @Test
    void camposExisten()
            throws Exception {

        SeguridadGUI gui =
                new SeguridadGUI(
                        new SeguridadHttpClient(
                                "http://localhost"));

        assertNotNull(
                campo(gui,
                        "txtUsuario"));

        assertNotNull(
                campo(gui,
                        "txtClave"));

        assertNotNull(
                campo(gui,
                        "txtAccion"));

        assertNotNull(
                campo(gui,
                        "txtDetalles"));

        gui.dispose();
    }

    @Test
    void metodoMain()
            throws Exception {

        SeguridadGUI.main(
                new String[0]);

        boolean encontrada =
                false;

        for (Window w :
                Window.getWindows()) {

            if (w instanceof SeguridadGUI) {

                encontrada = true;
                w.dispose();
            }
        }

        assertTrue(
                encontrada);
    }

    @Test
    void usuarioVacio()
            throws Exception {

        SeguridadGUI gui =
                new SeguridadGUI(
                        new SeguridadHttpClient(
                                "http://localhost"));

        JTextField usuario =
                (JTextField)
                        campo(
                                gui,
                                "txtUsuario");

        usuario.setText("");

        assertEquals(
                "",
                usuario.getText());

        gui.dispose();
    }

    @Test
        void credencialesVacias()
                throws Exception {

        SeguridadGUI gui =
                new SeguridadGUI(
                        new SeguridadHttpClient(
                                "http://localhost"));

        Method metodo =
                SeguridadGUI.class.getDeclaredMethod(
                        "registrarAccion");

        metodo.setAccessible(true);

        metodo.invoke(gui);

        Field campo =
                SeguridadGUI.class.getDeclaredField(
                        "areaResultado");

        campo.setAccessible(true);

        JTextArea area =
                (JTextArea) campo.get(gui);

        assertEquals(
                "Introduce el usuario y la contraseña.",
                area.getText());

        gui.dispose();
        }

        @Test
void accionVacia()
        throws Exception {

    SeguridadGUI gui =
            new SeguridadGUI(
                    new SeguridadHttpClient(
                            "http://localhost"));

    ((JTextField) obtenerCampo(
            gui,
            "txtUsuario"))
            .setText("admin");

    ((JPasswordField) obtenerCampo(
            gui,
            "txtClave"))
            .setText("demo");

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "registrarAccion");

    metodo.setAccessible(true);

    metodo.invoke(gui);

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertEquals(
            "Introduce la acción a registrar.",
            area.getText());

    gui.dispose();
}

@Test
void errorConexionRegistrar()
        throws Exception {

    SeguridadHttpClient cliente =
            new SeguridadHttpClient(
                    "http://127.0.0.1:9999/seguridad");

    SeguridadGUI gui =
            new SeguridadGUI(cliente);

    ((JTextField) obtenerCampo(
            gui,
            "txtUsuario"))
            .setText("admin");

    ((JPasswordField) obtenerCampo(
            gui,
            "txtClave"))
            .setText("demo");

    ((JTextField) obtenerCampo(
            gui,
            "txtAccion"))
            .setText("LOGIN");

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "registrarAccion");

    metodo.setAccessible(true);

    metodo.invoke(gui);

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertTrue(
            area.getText()
                    .contains(
                            "Error al conectar"));

    gui.dispose();
}

        @Test
        void errorConexionLogs()
                throws Exception {

        SeguridadHttpClient cliente =
                new SeguridadHttpClient(
                        "http://127.0.0.1:9999/seguridad");

        SeguridadGUI gui =
                new SeguridadGUI(cliente);

        ((JTextField) obtenerCampo(
                gui,
                "txtUsuario"))
                .setText("admin");

        ((JPasswordField) obtenerCampo(
                gui,
                "txtClave"))
                .setText("demo");

        Method metodo =
                SeguridadGUI.class.getDeclaredMethod(
                        "verLogs");

        metodo.setAccessible(true);

        metodo.invoke(gui);

        JTextArea area =
                (JTextArea) obtenerCampo(
                        gui,
                        "areaResultado");

        assertTrue(
                area.getText()
                        .contains(
                                "Error al conectar"));

        gui.dispose();
        }

        private Object obtenerCampo(
                SeguridadGUI gui,
                String nombre)
                throws Exception {

        Field field =
                SeguridadGUI.class
                        .getDeclaredField(nombre);

        field.setAccessible(true);

        return field.get(gui);
        }

        @Test
void mostrarRespuestaCorrecta()
        throws Exception {

    SeguridadGUI gui =
            new SeguridadGUI(
                    new SeguridadHttpClient(
                            "http://localhost"));

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "mostrar",
                    SeguridadHttpClient.Respuesta.class);

    metodo.setAccessible(true);

    metodo.invoke(
            gui,
            new SeguridadHttpClient.Respuesta(
                    200,
                    "OK"));

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertEquals(
            "OK",
            area.getText());

    gui.dispose();
}

@Test
void mostrarRespuestaError()
        throws Exception {

    SeguridadGUI gui =
            new SeguridadGUI(
                    new SeguridadHttpClient(
                            "http://localhost"));

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "mostrar",
                    SeguridadHttpClient.Respuesta.class);

    metodo.setAccessible(true);

    metodo.invoke(
            gui,
            new SeguridadHttpClient.Respuesta(
                    403,
                    "Denegado"));

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertEquals(
            "Error 403: Denegado",
            area.getText());

    gui.dispose();
}

@Test
void claveVacia()
        throws Exception {

    SeguridadGUI gui =
            new SeguridadGUI(
                    new SeguridadHttpClient(
                            "http://localhost"));

    ((JTextField) obtenerCampo(
            gui,
            "txtUsuario"))
            .setText("admin");

    ((JPasswordField) obtenerCampo(
            gui,
            "txtClave"))
            .setText("");

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "verLogs");

    metodo.setAccessible(true);

    metodo.invoke(gui);

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertEquals(
            "Introduce el usuario y la contraseña.",
            area.getText());

    gui.dispose();
}
private static class ClienteOK
        extends SeguridadHttpClient {

    @Override
    public Respuesta obtenerLogs() {

        return new Respuesta(
                200,
                "LOGS");
    }

    @Override
    public Respuesta registrarAccion(
            String accion,
            String detalles) {

        return new Respuesta(
                200,
                "REGISTRADO");
    }
}

@Test
void verLogsCorrectamente()
        throws Exception {

    SeguridadGUI gui =
            new SeguridadGUI(
                    new ClienteOK());

    ((JTextField) obtenerCampo(
            gui,
            "txtUsuario"))
            .setText("admin");

    ((JPasswordField) obtenerCampo(
            gui,
            "txtClave"))
            .setText("demo");

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "verLogs");

    metodo.setAccessible(true);

    metodo.invoke(gui);

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertEquals(
            "LOGS",
            area.getText());

    gui.dispose();
}

@Test
void registrarCorrectamente()
        throws Exception {

    SeguridadGUI gui =
            new SeguridadGUI(
                    new ClienteOK());

    ((JTextField) obtenerCampo(
            gui,
            "txtUsuario"))
            .setText("admin");

    ((JPasswordField) obtenerCampo(
            gui,
            "txtClave"))
            .setText("demo");

    ((JTextField) obtenerCampo(
            gui,
            "txtAccion"))
            .setText("LOGIN");

    Method metodo =
            SeguridadGUI.class.getDeclaredMethod(
                    "registrarAccion");

    metodo.setAccessible(true);

    metodo.invoke(gui);

    JTextArea area =
            (JTextArea) obtenerCampo(
                    gui,
                    "areaResultado");

    assertEquals(
            "REGISTRADO",
            area.getText());

    gui.dispose();
}
}