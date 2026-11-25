package com.sescam.radiodiagnostico.cliente.encuestas;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@DisplayName("CDU18 · Cliente · GestionEncuestasGUI")
class GestionEncuestasGUITest {
    private GestionEncuestasGUI gui;
    private ClienteSimulado cliente;

    private static class ClienteSimulado extends EncuestasHttpClient {
        volatile String operacion, texto, usuario;
        volatile int id, nota;
        volatile char[] claveRecibida;
        volatile String copiaClave;
        volatile boolean fallo;
        final CountDownLatch liberar = new CountDownLatch(1);
        private String registrar(String op, int id, int nota, String texto, String usuario, char[] clave) {
            this.operacion = op; this.id = id; this.nota = nota;
            this.texto = texto; this.usuario = usuario;
            this.claveRecibida = clave; this.copiaClave = new String(clave);
            try {
                if (!liberar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo agotado");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); throw new IllegalStateException(e);
            }
            if (fallo) throw new IllegalStateException("Fallo simulado");
            return "Registrado";
        }
        @Override public String comunicarIncidencia(int id, String texto, String usuario, char[] clave) {
            return registrar("incidencia", id, 0, texto, usuario, clave);
        }
        @Override public String responderEncuesta(int id, int nota, String texto, String usuario, char[] clave) {
            return registrar("encuesta", id, nota, texto, usuario, clave);
        }
    }

    private <T> T edt(Callable<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion);
        SwingUtilities.invokeAndWait(tarea);
        return tarea.get();
    }
    private <T> T campo(String nombre, Class<T> tipo) throws Exception {
        Field f = GestionEncuestasGUI.class.getDeclaredField(nombre);
        f.setAccessible(true);
        return tipo.cast(f.get(gui));
    }
    private void texto(String nombre, String valor) throws Exception {
        edt(() -> { campo(nombre, javax.swing.text.JTextComponent.class).setText(valor); return null; });
    }
    private void pulsar(String nombre) throws Exception {
        edt(() -> { campo(nombre, JButton.class).doClick(0); return null; });
    }
    private String resultado() throws Exception {
        return edt(() -> campo("resultado", JLabel.class).getText());
    }
    private void nota(int valor) throws Exception {
        edt(() -> { campo("puntuacion", JComboBox.class).getModel().setSelectedItem(valor); return null; });
    }
    private void esperarResultado(String esperado) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < limite) {
            if (edt(() -> campo("incidencia", JButton.class).isEnabled() && campo("encuesta", JButton.class).isEnabled())) {
                assertEquals(esperado, resultado()); return;
            }
            Thread.sleep(20);
        }
        fail("Los botones no se reactivaron");
    }
    @BeforeEach void preparar() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requiere entorno gráfico");
        cliente = new ClienteSimulado();
        gui = edt(() -> new GestionEncuestasGUI(cliente));
        texto("idCita", " 1 "); texto("usuario", " paciente.demo "); texto("clave", "demo");
        texto("descripcion", "Incidencia de prueba");
    }
    @AfterEach void limpiar() throws Exception {
        if (cliente != null) cliente.liberar.countDown();
        if (gui != null) edt(() -> { gui.dispose(); return null; });
    }
    @Test @DisplayName("ID cero: se rechaza la incidencia")
    void idIncorrecto() throws Exception {
        // Preparar
        texto("idCita", "0");
        // Ejecutar
        pulsar("incidencia");
        // Comprobar
        assertEquals("ID de cita no valido", resultado()); assertNull(cliente.operacion);
    }
    @Test @DisplayName("Descripción vacía: no se envía la incidencia")
    void descripcionVacia() throws Exception {
        // Preparar
        texto("descripcion", "");
        // Ejecutar
        pulsar("incidencia");
        // Comprobar
        assertEquals("Datos no validos (maximo 500 caracteres)", resultado()); assertNull(cliente.operacion);
    }
    @Test @DisplayName("Descripción de 501 caracteres: se rechaza")
    void descripcionLarga() throws Exception {
        // Preparar
        texto("descripcion", "a".repeat(501));
        // Ejecutar
        pulsar("incidencia");
        // Comprobar
        assertEquals("Datos no validos (maximo 500 caracteres)", resultado()); assertNull(cliente.operacion);
    }
    @Test @DisplayName("Comentario de 501 caracteres: se rechaza")
    void comentarioLargo() throws Exception {
        // Preparar
        texto("comentario", "a".repeat(501));
        // Ejecutar
        pulsar("encuesta");
        // Comprobar
        assertEquals("Datos no validos (maximo 500 caracteres)", resultado()); assertNull(cliente.operacion);
    }
    @Test @DisplayName("Puntuación 6 introducida en el modelo: se rechaza")
    void puntuacionIncorrecta() throws Exception {
        // Preparar
        nota(6);
        // Ejecutar
        pulsar("encuesta");
        // Comprobar
        assertEquals("Datos no validos (maximo 500 caracteres)", resultado()); assertNull(cliente.operacion);
    }
    @Test @DisplayName("Incidencia válida: se envía y se limpian las credenciales")
    void incidenciaCorrecta() throws Exception {
        // Preparar: datos válidos en setUp
        // Ejecutar
        pulsar("incidencia");
        // Comprobar mientras está pendiente
        assertTrue(edt(() -> !campo("incidencia", JButton.class).isEnabled() && !campo("encuesta", JButton.class).isEnabled()));
        assertEquals(0, edt(() -> campo("clave", JPasswordField.class).getPassword().length).intValue());
        cliente.liberar.countDown(); esperarResultado("Registrado");
        assertAll(() -> assertEquals("incidencia", cliente.operacion), () -> assertEquals(1, cliente.id),
            () -> assertEquals("Incidencia de prueba", cliente.texto), () -> assertEquals("paciente.demo", cliente.usuario),
            () -> assertEquals("demo", cliente.copiaClave), () -> assertArrayEquals(new char[4], cliente.claveRecibida));
    }
    @Test @DisplayName("Encuesta válida con puntuación 5 y comentario opcional vacío")
    void encuestaCorrecta() throws Exception {
        // Preparar
        nota(5); texto("comentario", "");
        // Ejecutar
        pulsar("encuesta"); cliente.liberar.countDown();
        // Comprobar
        esperarResultado("Registrado");
        assertAll(() -> assertEquals("encuesta", cliente.operacion), () -> assertEquals(5, cliente.nota),
            () -> assertEquals("", cliente.texto), () -> assertEquals(1, cliente.id));
    }
    @Test @DisplayName("Fallo del cliente: se muestra el error y se reactivan los botones")
    void falloComunicacion() throws Exception {
        // Preparar
        cliente.fallo = true;
        // Ejecutar
        pulsar("incidencia"); cliente.liberar.countDown();
        // Comprobar
        esperarResultado("Error al enviar los datos");
        assertArrayEquals(new char[4], cliente.claveRecibida);
    }
    @Test @DisplayName("Constructor por defecto: la ventana presenta los controles esperados")
    void constructorPorDefecto() throws Exception {
        // Ejecutar
        GestionEncuestasGUI ventana = edt(GestionEncuestasGUI::new);
        try {
            // Comprobar
            assertEquals("Gestion de incidencias y encuestas - RF05", edt(ventana::getTitle));
            assertEquals(new Dimension(650, 500), edt(ventana::getSize));
        } finally { edt(() -> { ventana.dispose(); return null; }); }
    }
    @Test @DisplayName("Main abre una ventana visible")
    void mainAbreVentana() throws Exception {
        // Preparar
        Window[] anteriores = edt(Window::getWindows);
        // Ejecutar
        GestionEncuestasGUI.main(new String[0]);
        // Comprobar y limpiar
        edt(() -> {
            boolean visible = false;
            for (Window w : Window.getWindows()) {
                if (w instanceof GestionEncuestasGUI && !java.util.Arrays.asList(anteriores).contains(w)) {
                    visible |= w.isVisible(); w.dispose();
                }
            }
            assertTrue(visible); return null;
        });
    }
}
