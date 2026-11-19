
package com.sescam.radiodiagnostico.cliente.agendas;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
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

public class GestionAgendasGUITest {

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    // ------------------------------------
    // CLIENTE HTTP SIMULADO
    // ------------------------------------

    private static class ClienteSimulado extends AgendasHttpClient {

        String operacion;
        boolean errorConexion = false;

        private String responder(String nombre) throws IOException {

            operacion = nombre;

            if (errorConexion) {
                throw new IOException("Servidor no disponible");
            }

            return "Operacion correcta";
        }

        @Override
        public String crearHorario(int idMedico, String fecha,
                String inicio, String fin) throws IOException {

            return responder("crear");
        }

        @Override
        public String consultarAgenda(int idMedico) throws IOException {

            return responder("consultar");
        }

        @Override
        public String modificarHorario(int idHorario, String fecha,
                String inicio, String fin) throws IOException {

            return responder("modificar");
        }

        @Override
        public String eliminarHorario(int idHorario) throws IOException {

            return responder("eliminar");
        }
    }

    // ------------------------------------
    // VENTANA PARA LAS PRUEBAS
    // ------------------------------------

    private static class VentanaPrueba extends GestionAgendasGUI {

        String ultimoError;

        public VentanaPrueba(AgendasHttpClient cliente) {
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

            escribir("txtIdMedico", "5");
            escribir("txtIdHorario", "3");
            escribir("txtFecha",
                    LocalDate.now().plusDays(5).toString());
            escribir("txtInicio", "09:00");
            escribir("txtFin", "10:00");
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
            Field campo = GestionAgendasGUI.class
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
        assertEquals("SESCAM - Gestion de agendas",
                ventana.getTitle());

        assertEquals(650, ventana.getWidth());
        assertEquals(500, ventana.getHeight());
    }

    @Test
    @DisplayName("Crear la ventana con constructor normal")
    public void testConstructorNormal() throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            GestionAgendasGUI otra =
                    new GestionAgendasGUI();

            try {
                assertNotNull(otra);
                assertEquals("SESCAM - Gestion de agendas",
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
    @DisplayName("Crear un horario correctamente")
    public void testCrearHorarioCorrecto() throws Exception {

        pulsar("Crear horario");

        assertEquals("crear", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Consultar una agenda correctamente")
    public void testConsultarAgendaCorrecta() throws Exception {

        pulsar("Consultar agenda");

        assertEquals("consultar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Modificar un horario correctamente")
    public void testModificarHorarioCorrecto() throws Exception {

        pulsar("Modificar horario");

        assertEquals("modificar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    @Test
    @DisplayName("Eliminar un horario correctamente")
    public void testEliminarHorarioCorrecto() throws Exception {

        pulsar("Eliminar horario");

        assertEquals("eliminar", cliente.operacion);
        assertEquals("Operacion correcta", obtenerResultado());
    }

    // ------------------------------------
    // PRUEBAS DE ID INCORRECTO
    // ------------------------------------

    @Test
    @DisplayName("Rechazar un ID incorrecto al crear")
    public void testCrearIdIncorrecto() throws Exception {

        cambiarCampo("txtIdMedico", "abc");

        pulsar("Crear horario");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID incorrecto al consultar")
    public void testConsultarIdIncorrecto() throws Exception {

        cambiarCampo("txtIdMedico", "0");

        pulsar("Consultar agenda");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID incorrecto al modificar")
    public void testModificarIdIncorrecto() throws Exception {

        cambiarCampo("txtIdHorario", "-1");

        pulsar("Modificar horario");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar un ID incorrecto al eliminar")
    public void testEliminarIdIncorrecto() throws Exception {

        cambiarCampo("txtIdHorario", "abc");

        pulsar("Eliminar horario");

        assertTrue(ventana.ultimoError.contains("ID"));
        assertNull(cliente.operacion);
    }

    // ------------------------------------
    // PRUEBAS DE FECHAS INCORRECTAS
    // ------------------------------------

    @Test
    @DisplayName("Rechazar una fecha pasada al crear")
    public void testCrearFechaIncorrecta() throws Exception {

        cambiarCampo("txtFecha",
                LocalDate.now().minusDays(5).toString());

        pulsar("Crear horario");

        assertTrue(ventana.ultimoError.contains("fecha"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar una fecha incorrecta al modificar")
    public void testModificarFechaIncorrecta() throws Exception {

        cambiarCampo("txtFecha", "fecha incorrecta");

        pulsar("Modificar horario");

        assertTrue(ventana.ultimoError.contains("fecha"));
        assertNull(cliente.operacion);
    }

    // ------------------------------------
    // PRUEBAS DE HORAS INCORRECTAS
    // ------------------------------------

    @Test
    @DisplayName("Rechazar horas invertidas al crear")
    public void testCrearHorasIncorrectas() throws Exception {

        cambiarCampo("txtInicio", "12:00");
        cambiarCampo("txtFin", "10:00");

        pulsar("Crear horario");

        assertTrue(ventana.ultimoError.contains("horas"));
        assertNull(cliente.operacion);
    }

    @Test
    @DisplayName("Rechazar horas incorrectas al modificar")
    public void testModificarHorasIncorrectas() throws Exception {

        cambiarCampo("txtInicio", "25:00");

        pulsar("Modificar horario");

        assertTrue(ventana.ultimoError.contains("horas"));
        assertNull(cliente.operacion);
    }

    // ------------------------------------
    // PRUEBAS DE ERRORES DE CONEXION
    // ------------------------------------

    @Test
    @DisplayName("Error de conexion al crear")
    public void testErrorConexionCrear() throws Exception {

        cliente.errorConexion = true;

        pulsar("Crear horario");

        assertEquals(
                "Error al conectar con el servidor: Servidor no disponible",
                ventana.ultimoError);
    }

    @Test
    @DisplayName("Error de conexion al consultar")
    public void testErrorConexionConsultar() throws Exception {

        cliente.errorConexion = true;

        pulsar("Consultar agenda");

        assertEquals(
                "Error al conectar con el servidor: Servidor no disponible",
                ventana.ultimoError);
    }

    @Test
    @DisplayName("Error de conexion al modificar")
    public void testErrorConexionModificar() throws Exception {

        cliente.errorConexion = true;

        pulsar("Modificar horario");

        assertEquals(
                "Error al conectar con el servidor: Servidor no disponible",
                ventana.ultimoError);
    }

    @Test
    @DisplayName("Error de conexion al eliminar")
    public void testErrorConexionEliminar() throws Exception {

        cliente.errorConexion = true;

        pulsar("Eliminar horario");

        assertEquals(
                "Error al conectar con el servidor: Servidor no disponible",
                ventana.ultimoError);
    }

    // ------------------------------------
    // PROBAR EL MENSAJE ORIGINAL DE ERROR
    // ------------------------------------

    @Test
    @DisplayName("Mostrar y cerrar el mensaje de error")
    public void testMostrarErrorOriginal() throws Exception {

        AtomicBoolean dialogoEncontrado = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> {

            // Cerrar automaticamente el dialogo
            Timer temporizador = new Timer(50, e -> {

                for (Window w : ventana.getOwnedWindows()) {

                    if (w instanceof JDialog
                            && w.isShowing()) {

                        assertEquals("Error",
                                ((Dialog) w).getTitle());

                        dialogoEncontrado.set(true);
                        w.dispose();
                    }
                }
            });

            temporizador.start();

            try {
                ventana.mostrarErrorOriginal("Error de prueba");
            } finally {
                temporizador.stop();
            }
        });

        assertTrue(dialogoEncontrado.get());
    }

    // ------------------------------------
    // PRUEBA DEL METODO MAIN
    // ------------------------------------

    @Test
    @DisplayName("Ejecutar el metodo main de la ventana")
    public void testMain() throws Exception {

        // Ejecutar
        GestionAgendasGUI.main(new String[0]);

        // Esperar a que Swing cree la ventana
        SwingUtilities.invokeAndWait(() -> {

            boolean encontrada = false;

            for (Window w : Window.getWindows()) {

                if (w instanceof GestionAgendasGUI
                        && w.isVisible()
                        && w != ventana) {

                    encontrada = true;
                    w.dispose();
                }
            }

            assertTrue(encontrada);
        });
    }
}
