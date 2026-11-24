package com.sescam.radiodiagnostico.cliente.realizacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Interfaz grafica de realizacion de pruebas (CDU7)")
class RealizacionGUITest {

    private RealizacionGUI gui;

    @AfterEach
    void cerrar() throws Exception {
        if (gui != null) {
            SwingUtilities.invokeAndWait(() -> gui.dispose());
        }
    }

    @Test
    @DisplayName("Inicializa constructor por defecto y tabla no editable")
    void inicializaConstructorPorDefectoYTablaNoEditable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            gui = new RealizacionGUI();
            try {
                Field f = RealizacionGUI.class.getDeclaredField("modeloTabla");
                f.setAccessible(true);
                DefaultTableModel modelo = (DefaultTableModel) f.get(gui);
                assertFalse(modelo.isCellEditable(0, 0));
            } catch (Exception e) {
                fail(e);
            }
        });
        assertEquals("SESCAM - Realizacion de Pruebas e Incidencias", gui.getTitle());
    }

    @Test
    @DisplayName("Valida campos y muestra errores en cada caso invalido")
    void validaCamposYMuestraErrores() throws Exception {
        List<String> errores = new ArrayList<>();
        RealizacionHttpClient fakeClient = new RealizacionHttpClient("http://127.0.0.1:0/realizaciones");

        SwingUtilities.invokeAndWait(() -> {
            gui = new RealizacionGUI(fakeClient) {
                @Override
                protected void mostrarError(String mensaje) {
                    errores.add(mensaje);
                }

                @Override
                protected void mostrarInfo(String titulo, String mensaje) {
                }
            };
            try {
                Method m = RealizacionGUI.class.getDeclaredMethod("registrarRealizacion");
                m.setAccessible(true);

                // IDs invalidos
                m.invoke(gui);

                // Fecha invalida
                setCampo("txtIdCita", "10");
                setCampo("txtIdSolicitud", "2");
                setCampo("txtFechaRealizacion", "2099-01-01");
                m.invoke(gui);

                // Observaciones e incidencia vacias
                setCampo("txtFechaRealizacion", "2026-11-20");
                m.invoke(gui);
            } catch (Exception e) {
                fail(e);
            }
        });

        assertEquals(3, errores.size());
    }

    @Test
    @DisplayName("Registra, consulta, lista, decodifica textos y maneja errores de red")
    void cubreFlujosYErroresEnGUI() throws Exception {
        List<String> errores = new ArrayList<>();
        RealizacionHttpClient client = new RealizacionHttpClient("http://127.0.0.1:0/realizaciones") {
            @Override
            public String registrarRealizacion(int idCita, int idSolicitud, String fecha, String obs, String inc) {
                return "%ZZid=uuid-1&idCita=" + idCita + "&idSolicitud=" + idSolicitud
                        + "&fechaRealizacion=" + fecha + "&observaciones=" + obs + "&incidencia=" + inc + "&estado=REALIZADA";
            }

            @Override
            public String consultarPorCita(int idCita) throws IOException {
                if (idCita == 99) {
                    return "Error 404: No existe";
                }
                if (idCita == 88) {
                    throw new IOException("Red caida");
                }
                return "sin_id=1";
            }

            @Override
            public String listarRealizaciones() {
                return "";
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            gui = new RealizacionGUI(client) {
                @Override
                protected void mostrarError(String mensaje) {
                    errores.add(mensaje);
                }

                @Override
                protected void mostrarInfo(String titulo, String mensaje) {
                }
            };
            try {
                setCampo("txtIdCita", "10");
                setCampo("txtIdSolicitud", "2");
                setCampo("txtFechaRealizacion", "2026-11-20");
                setCampo("txtObservaciones", "OK");

                Method mRegistrar = RealizacionGUI.class.getDeclaredMethod("registrarRealizacion");
                mRegistrar.setAccessible(true);
                mRegistrar.invoke(gui);

                Method mConsultar = RealizacionGUI.class.getDeclaredMethod("consultarPorCita");
                mConsultar.setAccessible(true);
                Method mListar = RealizacionGUI.class.getDeclaredMethod("listarRealizaciones");
                mListar.setAccessible(true);

                setCampo("txtIdCita", "0");
                mConsultar.invoke(gui);

                setCampo("txtIdCita", "10");
                mConsultar.invoke(gui);
                setCampo("txtIdCita", "99");
                mConsultar.invoke(gui);
                setCampo("txtIdCita", "88");
                mConsultar.invoke(gui);
                mListar.invoke(gui);
            } catch (Exception e) {
                fail(e);
            }
        });

        Thread.sleep(250);
        assertFalse(errores.isEmpty());
    }

    private void setCampo(String nombre, String valor) throws Exception {
        Field f = RealizacionGUI.class.getDeclaredField(nombre);
        f.setAccessible(true);
        ((JTextField) f.get(gui)).setText(valor);
    }
}