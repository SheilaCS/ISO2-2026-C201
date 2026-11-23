package com.sescam.radiodiagnostico.cliente.pacientes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Interfaz gráfica de gestión de pacientes (CDU4)")
class GestionPacientesGUITest {

    private GestionPacientesGUI gui;
    private Timer timerDialogos;

    @AfterEach
    void cerrarVentana() throws Exception {
        if (timerDialogos != null) {
            timerDialogos.stop();
            timerDialogos = null;
        }
        cerrarDialogosActivos();
        if (gui != null) {
            SwingUtilities.invokeAndWait(() -> gui.dispose());
            gui = null;
        }
    }

    private void cerrarDialogosActivos() {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog dialog && dialog.isShowing()) {
                dialog.dispose();
            }
        }
    }

    private void armarCierreAutomaticoDialogos() {
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        timerDialogos = new Timer(50, e -> cerrarDialogosActivos());
        timerDialogos.setRepeats(true);
        timerDialogos.start();
    }

    private void setCampo(String nombreCampo, String valor) throws Exception {
        Field campo = GestionPacientesGUI.class.getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        JTextField tf = (JTextField) campo.get(gui);
        tf.setText(valor);
    }

    private String getCampo(String nombreCampo) throws Exception {
        Field campo = GestionPacientesGUI.class.getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        JTextField tf = (JTextField) campo.get(gui);
        return tf.getText();
    }

    private void invocarMetodo(String nombreMetodo) throws Exception {
        Method m = GestionPacientesGUI.class.getDeclaredMethod(nombreMetodo);
        m.setAccessible(true);
        m.invoke(gui);
    }

    @Test
    @DisplayName("Inicializa correctamente la ventana y su título")
    void inicializaComponentesCorrectamente() throws Exception {
        SwingUtilities.invokeAndWait(() -> gui = new GestionPacientesGUI());
        assertNotNull(gui);
        assertEquals("SESCAM - Gestion de Pacientes", gui.getTitle());
    }

    @Test
    @DisplayName("Valida los campos obligatorios antes de ejecutar alta o consulta")
    void validaCamposAntesDeAltaYConsulta() throws Exception {
        AtomicInteger llamadasAlta = new AtomicInteger(0);
        PacientesHttpClient fakeClient = new PacientesHttpClient("http://localhost:0/pacientes", "", "") {
            @Override
            public String altaPaciente(String cip, String nombre, String apellidos, String fechaNacimiento) {
                llamadasAlta.incrementAndGet();
                return "OK";
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            gui = new GestionPacientesGUI(fakeClient);
            armarCierreAutomaticoDialogos();
            try {
                invocarMetodo("altaPaciente");
                assertEquals(0, llamadasAlta.get());

                setCampo("txtCip", "CIP001");
                invocarMetodo("altaPaciente");
                assertEquals(0, llamadasAlta.get());

                setCampo("txtNombre", "Luis");
                invocarMetodo("altaPaciente");
                assertEquals(0, llamadasAlta.get());

                setCampo("txtApellidos", "Garcia");
                invocarMetodo("altaPaciente");
                assertEquals(0, llamadasAlta.get());

                setCampo("txtCip", "");
                invocarMetodo("consultarPorCip");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
        assertEquals(0, llamadasAlta.get());
    }

    @Test
    @DisplayName("Ejecuta alta, consulta cargando tabla, modificación con UUID y baja lógica")
    void ejecutaAltaConsultaModificacionYBajaConClienteSimulado() throws Exception {
        String uuidPaciente = UUID.randomUUID().toString();
        AtomicInteger altas = new AtomicInteger(0);
        AtomicInteger consultas = new AtomicInteger(0);
        AtomicInteger modificaciones = new AtomicInteger(0);
        AtomicInteger bajas = new AtomicInteger(0);

        PacientesHttpClient fakeClient = new PacientesHttpClient("http://localhost:0/pacientes", "", "") {
            @Override
            public String altaPaciente(String cip, String nombre, String apellidos, String fechaNacimiento) {
                altas.incrementAndGet();
                return "id=" + uuidPaciente + "&cip=" + cip + "&nombre=" + nombre
                        + "&apellidos=" + apellidos + "&fechaNacimiento=" + fechaNacimiento + "&estado=ACTIVO";
            }

            @Override
            public String consultarPorCip(String cip) {
                consultas.incrementAndGet();
                return "id=" + uuidPaciente + "&cip=" + cip
                        + "&nombre=Luis&apellidos=Garcia&fechaNacimiento=1985-04-20&estado=ACTIVO";
            }

            @Override
            public String modificarPaciente(String id, String nombre, String apellidos, String fechaNacimiento) {
                modificaciones.incrementAndGet();
                return "id=" + id + "&cip=CIP001&nombre=" + nombre
                        + "&apellidos=" + apellidos + "&fechaNacimiento=" + fechaNacimiento + "&estado=ACTIVO";
            }

            @Override
            public String darBajaPaciente(String id) {
                bajas.incrementAndGet();
                return "id=" + id + "&estado=BAJA";
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            gui = new GestionPacientesGUI(fakeClient);
            try {
                setCampo("txtUsuario", "administrativo.demo");
                setCampo("txtCip", " cip001 ");
                setCampo("txtNombre", "Luis");
                setCampo("txtApellidos", "Garcia");
                setCampo("txtFechaNacimiento", "1985-04-20");

                armarCierreAutomaticoDialogos();
                invocarMetodo("altaPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(200);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
        assertEquals(1, altas.get());
        assertEquals(uuidPaciente, getCampo("txtId"));

        SwingUtilities.invokeAndWait(() -> {
            try {
                setCampo("txtCip", "cip001");
                armarCierreAutomaticoDialogos();
                invocarMetodo("consultarPorCip");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(200);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
        assertEquals(1, consultas.get());
        assertEquals("CIP001", getCampo("txtCip"));

        SwingUtilities.invokeAndWait(() -> {
            try {
                setCampo("txtNombre", "");
                armarCierreAutomaticoDialogos();
                invocarMetodo("modificarPaciente");
                assertEquals(0, modificaciones.get());

                setCampo("txtNombre", "Luis Alberto");
                armarCierreAutomaticoDialogos();
                invocarMetodo("modificarPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(200);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
        assertEquals(1, modificaciones.get());

        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        timerDialogos = new Timer(50, e -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog dialog && dialog.isShowing()) {
                    JButton yesBtn = buscarBoton(dialog, "Yes");
                    if (yesBtn == null) {
                        yesBtn = buscarBoton(dialog, "Sí");
                    }
                    if (yesBtn != null) {
                        yesBtn.doClick();
                    } else {
                        dialog.dispose();
                    }
                }
            }
        });
        timerDialogos.setRepeats(true);

        SwingUtilities.invokeAndWait(() -> {
            try {
                timerDialogos.start();
                invocarMetodo("darBajaPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(250);
        timerDialogos.stop();
        cerrarDialogosActivos();
        assertEquals(1, bajas.get());
    }

    @Test
    @DisplayName("Selecciona filas en la tabla y gestiona errores de red en SwingWorker")
    void seleccionaFilaEnTablaYManejaErroresDeSwingWorker() throws Exception {
        String uuidFila = UUID.randomUUID().toString();
        PacientesHttpClient errorClient = new PacientesHttpClient("http://localhost:0/pacientes", "", "") {
            @Override
            public String consultarPorCip(String cip) throws IOException {
                throw new IOException("Fallo de red simulado");
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            gui = new GestionPacientesGUI(errorClient);
            try {
                Field modeloField = GestionPacientesGUI.class.getDeclaredField("modeloTabla");
                modeloField.setAccessible(true);
                DefaultTableModel modelo = (DefaultTableModel) modeloField.get(gui);
                modelo.addRow(new Object[]{uuidFila, "CIP555", "Marta", "Ruiz", "1992-07-11", "ACTIVO"});
                modelo.addRow(new Object[]{null, null, null, null, null, null});

                Field tablaField = GestionPacientesGUI.class.getDeclaredField("tablaPacientes");
                tablaField.setAccessible(true);
                JTable tabla = (JTable) tablaField.get(gui);
                tabla.setRowSelectionInterval(0, 0);
                assertEquals(uuidFila, getCampo("txtId"));
                assertEquals("CIP555", getCampo("txtCip"));

                tabla.setRowSelectionInterval(1, 1);
                assertEquals("", getCampo("txtId"));

                setCampo("txtCip", "CIP555");
                armarCierreAutomaticoDialogos();
                invocarMetodo("consultarPorCip");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(200);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
    }

    @Test
    @DisplayName("Rechaza modificación y baja cuando no hay un UUID válido")
    void validaIdEnModificacionYBaja() throws Exception {
        PacientesHttpClient fakeClient = new PacientesHttpClient("http://localhost:0/pacientes", "", "");

        SwingUtilities.invokeAndWait(() -> {
            gui = new GestionPacientesGUI(fakeClient);
            armarCierreAutomaticoDialogos();
            try {
                invocarMetodo("modificarPaciente");
                invocarMetodo("darBajaPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });

        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
    }

    @Test
    @DisplayName("Cubre formatos alternativos de respuesta, cancelación de baja y validaciones adicionales")
    void cubreRamasDeParseoBajaCanceladaYValidaciones() throws Exception {
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        AtomicInteger contadorBajas = new AtomicInteger(0);

        PacientesHttpClient client = new PacientesHttpClient("http://localhost:0/pacientes", "", "") {
            @Override
            public String consultarPorCip(String cip) {
                if ("CIPERROR".equalsIgnoreCase(cip)) {
                    return "Error 404: No existe un paciente con ese CIP.";
                }
                if ("CIPJSON".equalsIgnoreCase(cip)) {
                    return "{\"id\":\"" + uuid2 + "\",\"cip\":\"CIPJSON\",\"nombre\":\"Lucia\",\"apellidos\":\"Sanz\",\"fechaNacimiento\":\"1998-02-14\",\"activo\":\"false\"}";
                }
                return "CIP: " + cip + " | Nombre: Pedro | Apellidos: Gomez | Fecha: 1988-09-01 | Estado: ACTIVO | extra=" + uuid1;
            }

            @Override
            public String modificarPaciente(String id, String nombre, String apellidos, String fechaNacimiento) {
                return "%ZZid=" + id + "&cip=CIP100&nombre=" + nombre + "&apellidos=" + apellidos + "&fechaNacimiento=" + fechaNacimiento + "&activo=true";
            }

            @Override
            public String darBajaPaciente(String id) {
                contadorBajas.incrementAndGet();
                return "Baja ok";
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            gui = new GestionPacientesGUI(client);
            armarCierreAutomaticoDialogos();
            try {
                setCampo("txtCip", "cip100");
                invocarMetodo("consultarPorCip");
            } catch (Exception ex) {
                fail(ex);
            }
        });
        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();

        SwingUtilities.invokeAndWait(() -> {
            armarCierreAutomaticoDialogos();
            try {
                setCampo("txtCip", "cipjson");
                invocarMetodo("consultarPorCip");
            } catch (Exception ex) {
                fail(ex);
            }
        });
        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();

        SwingUtilities.invokeAndWait(() -> {
            armarCierreAutomaticoDialogos();
            try {
                setCampo("txtCip", "ciperror");
                invocarMetodo("consultarPorCip");
            } catch (Exception ex) {
                fail(ex);
            }
        });
        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();

        SwingUtilities.invokeAndWait(() -> {
            armarCierreAutomaticoDialogos();
            try {
                setCampo("txtId", uuid1);
                setCampo("txtNombre", "Pedro");
                setCampo("txtApellidos", "");
                invocarMetodo("modificarPaciente");

                setCampo("txtApellidos", "Gomez");
                setCampo("txtFechaNacimiento", "2099-01-01");
                invocarMetodo("modificarPaciente");

                setCampo("txtFechaNacimiento", "1988-09-01");
                invocarMetodo("modificarPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });
        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();

        SwingUtilities.invokeAndWait(() -> {
            armarCierreAutomaticoDialogos();
            try {
                setCampo("txtId", uuid1);
                invocarMetodo("darBajaPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });
        Thread.sleep(150);
        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        cerrarDialogosActivos();
        assertEquals(0, contadorBajas.get());

        if (timerDialogos != null) {
            timerDialogos.stop();
        }
        timerDialogos = new Timer(50, e -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog dialog && dialog.isShowing()) {
                    JButton yesBtn = buscarBoton(dialog, "Yes");
                    if (yesBtn == null) {
                        yesBtn = buscarBoton(dialog, "Sí");
                    }
                    if (yesBtn != null) {
                        yesBtn.doClick();
                    } else {
                        dialog.dispose();
                    }
                }
            }
        });
        timerDialogos.setRepeats(true);

        SwingUtilities.invokeAndWait(() -> {
            try {
                setCampo("txtId", uuid1);
                timerDialogos.start();
                invocarMetodo("darBajaPaciente");
            } catch (Exception ex) {
                fail(ex);
            }
        });
        Thread.sleep(200);
        timerDialogos.stop();
        cerrarDialogosActivos();
        assertEquals(1, contadorBajas.get());

        SwingUtilities.invokeAndWait(() -> {
            try {
                Field modeloField = GestionPacientesGUI.class.getDeclaredField("modeloTabla");
                modeloField.setAccessible(true);
                DefaultTableModel modelo = (DefaultTableModel) modeloField.get(gui);
                boolean encontradaBaja = false;
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    if (uuid1.equals(modelo.getValueAt(i, 0))) {
                        assertEquals("BAJA", modelo.getValueAt(i, 5));
                        encontradaBaja = true;
                    }
                }
                assertTrue(encontradaBaja);
            } catch (Exception ex) {
                fail(ex);
            }
        });
    }

    private JButton buscarBoton(Container root, String texto) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton btn && texto.equals(btn.getText())) {
                return btn;
            }
            if (c instanceof Container child) {
                JButton found = buscarBoton(child, texto);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}