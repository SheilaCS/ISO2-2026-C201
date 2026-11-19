
package com.sescam.radiodiagnostico.cliente.citas;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del cliente GUI.
 * Iteracion 3 - RF13: Gestion de Citas.
 *
 * Tecnicas:
 * - Clases de equivalencia.
 * - Valores limite.
 * - Conjetura de errores.
 * - Pruebas de interfaz.
 * - Simulacion de errores HTTP.
 */
public class GestionCitasGUITest {

    private GestionCitasGUI ventana;
    private ClienteSimulado clienteSimulado;
    private Timer temporizador;

    private boolean confirmarCancelacion;

    private final List<String> mensajesMostrados =
            new ArrayList<>();

    // -----------------------------------------
    // CLIENTE HTTP SIMULADO
    // -----------------------------------------

    private static class ClienteSimulado
            extends CitasHttpClient {

        volatile int asignaciones;
        volatile int reprogramaciones;
        volatile int cancelaciones;
        volatile int consultas;

        volatile int ultimoId;
        volatile int ultimaSolicitud;
        volatile int ultimoPaciente;
        volatile String ultimaFecha;
        volatile String ultimaHora;

        volatile boolean provocarError;

        volatile String respuestaAsignar =
                "Cita asignada correctamente";

        volatile String respuestaReprogramar =
                "Cita reprogramada correctamente";

        volatile String respuestaCancelar =
                "Cita cancelada correctamente";

        volatile String respuestaConsultar =
                "ID cita: 1\n"
                + "Solicitud: 2\n"
                + "Paciente: 3\n"
                + "Fecha: 2027-01-15\n"
                + "Hora: 10:00\n"
                + "Estado: ASIGNADA";

        ClienteSimulado() {
            super("http://localhost:0/citas");
        }

        @Override
        public String asignarCita(
                int solicitud,
                int paciente,
                String fecha,
                String hora) throws IOException {

            ultimaSolicitud = solicitud;
            ultimoPaciente = paciente;
            ultimaFecha = fecha;
            ultimaHora = hora;

            asignaciones++;

            if (provocarError) {
                throw new IOException("Error simulado");
            }

            return respuestaAsignar;
        }

        @Override
        public String reprogramarCita(
                int idCita,
                String fecha,
                String hora) throws IOException {

            ultimoId = idCita;
            ultimaFecha = fecha;
            ultimaHora = hora;

            reprogramaciones++;

            if (provocarError) {
                throw new IOException("Error simulado");
            }

            return respuestaReprogramar;
        }

        @Override
        public String cancelarCita(int idCita)
                throws IOException {

            ultimoId = idCita;
            cancelaciones++;

            if (provocarError) {
                throw new IOException("Error simulado");
            }

            return respuestaCancelar;
        }

        @Override
        public String consultarCita(int idCita)
                throws IOException {

            ultimoId = idCita;
            consultas++;

            if (provocarError) {
                throw new IOException("Error simulado");
            }

            return respuestaConsultar;
        }
    }

    // -----------------------------------------
    // PREPARAR Y FINALIZAR
    // -----------------------------------------

    @BeforeEach
    public void preparar() throws Exception {

        // Las pruebas Swing necesitan entorno grafico
        assumeFalse(GraphicsEnvironment.isHeadless());

        clienteSimulado = new ClienteSimulado();
        confirmarCancelacion = false;

        SwingUtilities.invokeAndWait(() -> {

            ventana = new GestionCitasGUI(clienteSimulado);

            // Cerrar los mensajes emergentes automaticamente
            temporizador = new Timer(50, e -> {

                for (Window w : Window.getWindows()) {

                    if (w instanceof JDialog
                            && w.isVisible()
                            && w.getOwner() == ventana) {

                        JDialog dialogo = (JDialog) w;
                        String titulo = dialogo.getTitle();

                        mensajesMostrados.add(titulo);

                        if ("Confirmar cancelacion".equals(titulo)) {

                            JOptionPane panel =
                                    buscarPanelOpciones(dialogo);

                            if (panel != null) {

                                int opcion = confirmarCancelacion
                                        ? JOptionPane.YES_OPTION
                                        : JOptionPane.NO_OPTION;

                                panel.setValue(opcion);

                            } else {
                                dialogo.dispose();
                            }

                        } else {
                            dialogo.dispose();
                        }
                    }
                }
            });

            temporizador.start();
        });
    }

    @AfterEach
    public void finalizar() throws Exception {

        if (ventana != null) {

            SwingUtilities.invokeAndWait(() -> {

                if (temporizador != null) {
                    temporizador.stop();
                }

                for (Window w : Window.getWindows()) {

                    if (w instanceof JDialog
                            && w.getOwner() == ventana) {

                        w.dispose();
                    }
                }

                ventana.dispose();
            });
        }
    }

    // -----------------------------------------
    // METODOS AUXILIARES
    // -----------------------------------------

    private JOptionPane buscarPanelOpciones(
            Container contenedor) {

        for (Component componente :
                contenedor.getComponents()) {

            if (componente instanceof JOptionPane) {
                return (JOptionPane) componente;
            }

            if (componente instanceof Container) {

                JOptionPane encontrado =
                        buscarPanelOpciones(
                                (Container) componente);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    private Object obtenerCampo(String nombre)
            throws Exception {

        Field campo = GestionCitasGUI.class
                .getDeclaredField(nombre);

        campo.setAccessible(true);

        return campo.get(ventana);
    }

    private void escribir(String nombre, String valor)
            throws Exception {

        JTextField campo =
                (JTextField) obtenerCampo(nombre);

        SwingUtilities.invokeAndWait(() -> {
            campo.setText(valor);
        });
    }

    private JButton buscarBoton(
            Container contenedor, String texto) {

        for (Component componente :
                contenedor.getComponents()) {

            if (componente instanceof JButton) {

                JButton boton = (JButton) componente;

                if (texto.equals(boton.getText())) {
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

        JButton boton = buscarBoton(
                ventana.getContentPane(), texto);

        assertNotNull(boton);

        SwingUtilities.invokeAndWait(() -> {
            boton.doClick();
        });
    }

    private void actualizarTabla(String respuesta)
            throws Exception {

        Method metodo = GestionCitasGUI.class
                .getDeclaredMethod(
                        "actualizarTablaConRespuesta",
                        String.class);

        metodo.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {

            try {
                metodo.invoke(ventana, respuesta);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private DefaultTableModel obtenerModelo()
            throws Exception {

        return (DefaultTableModel)
                obtenerCampo("modeloTabla");
    }

    private String respuestaCorrecta() {

        return "ID cita: 1\n"
                + "Solicitud: 2\n"
                + "Paciente: 3\n"
                + "Fecha: 2027-01-15\n"
                + "Hora: 10:00\n"
                + "Estado: ASIGNADA";
    }

    private void prepararDatosValidos() throws Exception {

        escribir("txtIdCita", "1");
        escribir("txtIdSolicitud", "2");
        escribir("txtIdPaciente", "3");

        escribir(
                "txtFecha",
                LocalDate.now().plusDays(10).toString());

        escribir("txtHora", "10:00");
    }

    // Esperar a que finalice una operacion asincrona
    private void esperar(BooleanSupplier condicion)
            throws Exception {

        long limite =
                System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < limite) {

            if (condicion.getAsBoolean()) {
                return;
            }

            Thread.sleep(25);
        }

        fail("La operacion no termino dentro del plazo");
    }

    private boolean mensajeMostrado(String titulo) {

        AtomicInteger encontrado = new AtomicInteger();

        try {

            SwingUtilities.invokeAndWait(() -> {

                if (mensajesMostrados.contains(titulo)) {
                    encontrado.set(1);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return encontrado.get() == 1;
    }

    private int obtenerFilas() {

        AtomicInteger filas = new AtomicInteger();

        try {

            SwingUtilities.invokeAndWait(() -> {

                try {
                    filas.set(obtenerModelo().getRowCount());

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return filas.get();
    }

    // -----------------------------------------
    // PRUEBAS DE LA VENTANA
    // -----------------------------------------

    @Test
    @DisplayName("Crear correctamente la ventana")
    public void testConstructor() {

        assertNotNull(ventana);

        assertEquals(
                "SESCAM - Gestion de Citas",
                ventana.getTitle());

        assertEquals(
                JFrame.DISPOSE_ON_CLOSE,
                ventana.getDefaultCloseOperation());
    }

    @Test
    @DisplayName("Comprobar los cuatro botones")
    public void testBotones() {

        assertNotNull(buscarBoton(
                ventana.getContentPane(), "Asignar cita"));

        assertNotNull(buscarBoton(
                ventana.getContentPane(), "Reprogramar"));

        assertNotNull(buscarBoton(
                ventana.getContentPane(), "Cancelar"));

        assertNotNull(buscarBoton(
                ventana.getContentPane(), "Consultar"));
    }

    @Test
    @DisplayName("Impedir editar directamente la tabla")
    public void testTablaNoEditable() throws Exception {

        DefaultTableModel modelo = obtenerModelo();

        assertFalse(modelo.isCellEditable(0, 0));
    }

    // -----------------------------------------
    // PRUEBAS DE LA TABLA
    // -----------------------------------------

    @Test
    @DisplayName("Añadir una cita a la tabla")
    public void testAgregarCitaTabla() throws Exception {

        actualizarTabla(respuestaCorrecta());

        DefaultTableModel modelo = obtenerModelo();

        assertEquals(1, modelo.getRowCount());
        assertEquals("1", modelo.getValueAt(0, 0));
        assertEquals("2", modelo.getValueAt(0, 1));
        assertEquals("3", modelo.getValueAt(0, 2));
        assertEquals("2027-01-15", modelo.getValueAt(0, 3));
        assertEquals("10:00", modelo.getValueAt(0, 4));
        assertEquals("ASIGNADA", modelo.getValueAt(0, 5));
    }

    @Test
    @DisplayName("Actualizar una cita existente")
    public void testActualizarCitaTabla() throws Exception {

        actualizarTabla(respuestaCorrecta());

        String nuevaRespuesta =
                "ID cita: 1\n"
                + "Solicitud: 2\n"
                + "Paciente: 3\n"
                + "Fecha: 2027-01-20\n"
                + "Hora: 12:00\n"
                + "Estado: REPROGRAMADA";

        actualizarTabla(nuevaRespuesta);

        DefaultTableModel modelo = obtenerModelo();

        assertEquals(1, modelo.getRowCount());
        assertEquals("2027-01-20", modelo.getValueAt(0, 3));
        assertEquals("12:00", modelo.getValueAt(0, 4));
    }

    @Test
    @DisplayName("Rechazar una respuesta incompleta")
    public void testRespuestaIncompleta() throws Exception {

        actualizarTabla(
                "ID cita: 1\n"
                + "Solicitud: 2\n"
                + "Paciente: 3");

        assertEquals(0, obtenerModelo().getRowCount());
    }

    @Test
    @DisplayName("Rechazar una respuesta sin separador")
    public void testRespuestaSinSeparador() throws Exception {

        String respuesta =
                "ID cita: 1\n"
                + "Solicitud: 2\n"
                + "Paciente: 3\n"
                + "Fecha incorrecta\n"
                + "Hora: 10:00\n"
                + "Estado: ASIGNADA";

        actualizarTabla(respuesta);

        assertEquals(0, obtenerModelo().getRowCount());
    }

    @Test
    @DisplayName("Copiar el ID al seleccionar una cita")
    public void testSeleccionarCita() throws Exception {

        actualizarTabla(respuestaCorrecta());

        JTable tabla =
                (JTable) obtenerCampo("tablaCitas");

        JTextField campo =
                (JTextField) obtenerCampo("txtIdCita");

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(0, 0);
        });

        assertEquals("1", campo.getText());
    }

    // -----------------------------------------
    // VALIDACIONES DE LOS BOTONES
    // -----------------------------------------

    @Test
    @DisplayName("Rechazar solicitud incorrecta")
    public void testAsignarSolicitudIncorrecta()
            throws Exception {

        escribir("txtIdSolicitud", "abc");

        pulsar("Asignar cita");

        assertEquals(0, clienteSimulado.asignaciones);
        assertTrue(mensajeMostrado("Message"));
    }

    @Test
    @DisplayName("Rechazar paciente incorrecto")
    public void testAsignarPacienteIncorrecto()
            throws Exception {

        escribir("txtIdSolicitud", "1");
        escribir("txtIdPaciente", "0");

        pulsar("Asignar cita");

        assertEquals(0, clienteSimulado.asignaciones);
    }

    @Test
    @DisplayName("Rechazar fecha pasada al asignar")
    public void testAsignarFechaIncorrecta()
            throws Exception {

        escribir("txtIdSolicitud", "1");
        escribir("txtIdPaciente", "2");

        escribir(
                "txtFecha",
                LocalDate.now().minusDays(2).toString());

        escribir("txtHora", "10:00");

        pulsar("Asignar cita");

        assertEquals(0, clienteSimulado.asignaciones);
    }

    @Test
    @DisplayName("Rechazar ID incorrecto al reprogramar")
    public void testReprogramarIdIncorrecto()
            throws Exception {

        escribir("txtIdCita", "-1");

        pulsar("Reprogramar");

        assertEquals(0, clienteSimulado.reprogramaciones);
    }

    @Test
    @DisplayName("Rechazar fecha incorrecta al reprogramar")
    public void testReprogramarFechaIncorrecta()
            throws Exception {

        escribir("txtIdCita", "1");
        escribir("txtFecha", "fecha-invalida");
        escribir("txtHora", "10:00");

        pulsar("Reprogramar");

        assertEquals(0, clienteSimulado.reprogramaciones);
    }

    @Test
    @DisplayName("Rechazar ID incorrecto al cancelar")
    public void testCancelarIdIncorrecto()
            throws Exception {

        escribir("txtIdCita", "0");

        pulsar("Cancelar");

        assertEquals(0, clienteSimulado.cancelaciones);
    }

    @Test
    @DisplayName("Rechazar ID incorrecto al consultar")
    public void testConsultarIdIncorrecto()
            throws Exception {

        escribir("txtIdCita", "abc");

        pulsar("Consultar");

        assertEquals(0, clienteSimulado.consultas);
    }

    // -----------------------------------------
    // ASIGNAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Asignar una cita correctamente")
    public void testAsignarCorrectamente()
            throws Exception {

        prepararDatosValidos();

        pulsar("Asignar cita");

        esperar(() -> clienteSimulado.asignaciones == 1);

        esperar(() -> mensajeMostrado("Asignar cita"));

        assertEquals(2, clienteSimulado.ultimaSolicitud);
        assertEquals(3, clienteSimulado.ultimoPaciente);
        assertEquals("10:00", clienteSimulado.ultimaHora);
    }

    @Test
    @DisplayName("Mostrar error al fallar la asignacion")
    public void testAsignarErrorConexion()
            throws Exception {

        prepararDatosValidos();

        clienteSimulado.provocarError = true;

        pulsar("Asignar cita");

        esperar(() -> clienteSimulado.asignaciones == 1);

        esperar(() -> mensajeMostrado("Error de conexion"));

        assertEquals(1, clienteSimulado.asignaciones);
    }

    // -----------------------------------------
    // REPROGRAMAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Reprogramar una cita correctamente")
    public void testReprogramarCorrectamente()
            throws Exception {

        prepararDatosValidos();

        pulsar("Reprogramar");

        esperar(() ->
                clienteSimulado.reprogramaciones == 1);

        esperar(() ->
                clienteSimulado.consultas == 1);

        esperar(() -> obtenerFilas() == 1);

        assertEquals(1, clienteSimulado.ultimoId);
        assertEquals(1, obtenerFilas());
    }

    @Test
    @DisplayName("No actualizar cuando falla la reprogramacion")
    public void testReprogramarRespuestaIncorrecta()
            throws Exception {

        prepararDatosValidos();

        clienteSimulado.respuestaReprogramar =
                "No se pudo reprogramar la cita";

        pulsar("Reprogramar");

        esperar(() ->
                clienteSimulado.reprogramaciones == 1);

        esperar(() ->
                mensajeMostrado("Reprogramar cita"));

        assertEquals(0, clienteSimulado.consultas);
    }

    // -----------------------------------------
    // CANCELAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("No cancelar si el usuario responde NO")
    public void testCancelarNoConfirmada()
            throws Exception {

        escribir("txtIdCita", "1");

        confirmarCancelacion = false;

        pulsar("Cancelar");

        assertEquals(0, clienteSimulado.cancelaciones);

        assertTrue(mensajeMostrado(
                "Confirmar cancelacion"));
    }

    @Test
    @DisplayName("Cancelar correctamente al confirmar")
    public void testCancelarConfirmada()
            throws Exception {

        escribir("txtIdCita", "1");

        confirmarCancelacion = true;

        pulsar("Cancelar");

        esperar(() -> clienteSimulado.cancelaciones == 1);
        esperar(() -> clienteSimulado.consultas == 1);

        assertEquals(1, clienteSimulado.ultimoId);
    }

    @Test
    @DisplayName("No actualizar si la cancelacion falla")
    public void testCancelarRespuestaIncorrecta()
            throws Exception {

        escribir("txtIdCita", "1");

        confirmarCancelacion = true;

        clienteSimulado.respuestaCancelar =
                "No se pudo cancelar la cita";

        pulsar("Cancelar");

        esperar(() -> clienteSimulado.cancelaciones == 1);

        esperar(() -> mensajeMostrado("Cancelar cita"));

        assertEquals(0, clienteSimulado.consultas);
    }

    // -----------------------------------------
    // CONSULTAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Consultar una cita correctamente")
    public void testConsultarCorrectamente()
            throws Exception {

        escribir("txtIdCita", "1");

        pulsar("Consultar");

        esperar(() -> clienteSimulado.consultas == 1);
        esperar(() -> obtenerFilas() == 1);

        assertEquals(1, clienteSimulado.ultimoId);
        assertEquals(1, obtenerFilas());
    }

    @Test
    @DisplayName("Consultar una cita inexistente")
    public void testConsultarInexistente()
            throws Exception {

        escribir("txtIdCita", "99");

        clienteSimulado.respuestaConsultar =
                "No existe la cita";

        pulsar("Consultar");

        esperar(() -> clienteSimulado.consultas == 1);

        esperar(() -> mensajeMostrado("Consultar cita"));

        assertEquals(0, obtenerFilas());
    }

    @Test
    @DisplayName("Mostrar error de conexion al consultar")
    public void testConsultarErrorConexion()
            throws Exception {

        escribir("txtIdCita", "1");

        clienteSimulado.provocarError = true;

        pulsar("Consultar");

        esperar(() -> clienteSimulado.consultas == 1);

        esperar(() -> mensajeMostrado("Error de conexion"));

        assertEquals(0, obtenerFilas());
    }

    // -----------------------------------------
    // NUEVAS PRUEBAS PARA COMPLETAR JACOCO
    // -----------------------------------------

    @Test
    @DisplayName("Ejecutar el constructor normal")
    public void testConstructorNormal() throws Exception {

        final GestionCitasGUI[] otraVentana =
                new GestionCitasGUI[1];

        try {

            // Ejecutar el constructor sin parametros
            SwingUtilities.invokeAndWait(() -> {
                otraVentana[0] = new GestionCitasGUI();
            });

            // Comprobar
            assertNotNull(otraVentana[0]);

            assertEquals(
                    "SESCAM - Gestion de Citas",
                    otraVentana[0].getTitle());

        } finally {

            // Cerrar la ventana creada
            SwingUtilities.invokeAndWait(() -> {

                if (otraVentana[0] != null) {
                    otraVentana[0].dispose();
                }
            });
        }
    }

    @Test
    @DisplayName("Añadir dos citas con distintos identificadores")
    public void testDosCitasDiferentes() throws Exception {

        // Preparar la primera cita
        actualizarTabla(respuestaCorrecta());

        // Preparar otra cita con un ID diferente
        String segundaCita =
                "ID cita: 2\n"
                + "Solicitud: 4\n"
                + "Paciente: 5\n"
                + "Fecha: 2027-02-20\n"
                + "Hora: 12:00\n"
                + "Estado: ASIGNADA";

        // Ejecutar
        actualizarTabla(segundaCita);

        // Comprobar
        DefaultTableModel modelo = obtenerModelo();

        assertEquals(2, modelo.getRowCount());
        assertEquals("1", modelo.getValueAt(0, 0));
        assertEquals("2", modelo.getValueAt(1, 0));
    }

    @Test
    @DisplayName("Deseleccionar una cita de la tabla")
    public void testDeseleccionarCita() throws Exception {

        // Preparar
        actualizarTabla(respuestaCorrecta());

        JTable tabla =
                (JTable) obtenerCampo("tablaCitas");

        JTextField campo =
                (JTextField) obtenerCampo("txtIdCita");

        // Ejecutar
        SwingUtilities.invokeAndWait(() -> {

            tabla.setRowSelectionInterval(0, 0);

            // Quitar la seleccion
            tabla.clearSelection();
        });

        // Comprobar
        assertEquals(-1, tabla.getSelectedRow());

        // El campo conserva el ultimo ID seleccionado
        assertEquals("1", campo.getText());
    }

    @Test
    @DisplayName("Comprobar eventos durante el ajuste de seleccion")
    public void testSeleccionEnAjuste() throws Exception {

        // Preparar
        actualizarTabla(respuestaCorrecta());

        JTable tabla =
                (JTable) obtenerCampo("tablaCitas");

        JTextField campo =
                (JTextField) obtenerCampo("txtIdCita");

        // Ejecutar
        SwingUtilities.invokeAndWait(() -> {

            tabla.getSelectionModel()
                    .setValueIsAdjusting(true);

            tabla.setRowSelectionInterval(0, 0);

            // Mientras cambia la seleccion,
            // el ID no debe copiarse todavia
            assertEquals("", campo.getText());

            tabla.getSelectionModel()
                    .setValueIsAdjusting(false);
        });

        // Comprobar que al finalizar se copia el ID
        assertEquals("1", campo.getText());
    }

    @Test
    @DisplayName("Ejecutar el metodo main de GestionCitasGUI")
    public void testMain() throws Exception {

        // Guardar las ventanas existentes
        Set<Window> anteriores = new HashSet<>(
                Arrays.asList(Window.getWindows()));

        final GestionCitasGUI[] principal =
                new GestionCitasGUI[1];

        try {

            // Ejecutar main
            GestionCitasGUI.main(new String[0]);

            // Esperar a que Swing cree la ventana
            SwingUtilities.invokeAndWait(() -> {

                for (Window w : Window.getWindows()) {

                    if (!anteriores.contains(w)
                            && w instanceof GestionCitasGUI) {

                        principal[0] = (GestionCitasGUI) w;
                    }
                }
            });

            // Comprobar
            assertNotNull(principal[0]);
            assertTrue(principal[0].isVisible());

        } finally {

            // Cerrar la ventana creada por main
            SwingUtilities.invokeAndWait(() -> {

                if (principal[0] != null) {
                    principal[0].dispose();
                }
            });
        }
    }
}
