package com.sescam.radiodiagnostico.cliente.historial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CDU12 · Cliente · GestionHistorialGUI")
class GestionHistorialGUITest {

    private static final String DOS_PRUEBAS =
            "P2;2026-03-05;Ecografia;REALIZADA;Control\n"
            + "P1;2026-01-10;Radiografia;REALIZADA;Sin hallazgos";

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    // ------------------------------------
    // CLIENTE HTTP SIMULADO
    // ------------------------------------

    private static class ClienteSimulado extends HistorialHttpClient {

        int llamadas;
        String usuarioRecibido;
        String claveRecibida;
        String idRecibido;
        boolean errorConexion;
        RespuestaHistorial respuesta = new RespuestaHistorial(200, "");

        @Override
        public RespuestaHistorial consultarHistorial(String usuario,
                char[] clave, String idPaciente) throws IOException {

            llamadas++;
            usuarioRecibido = usuario;
            claveRecibida = new String(clave);
            idRecibido = idPaciente;

            if (errorConexion) {
                throw new IOException("Servidor no disponible");
            }

            return respuesta;
        }
    }

    // ------------------------------------
    // VENTANA PARA LAS PRUEBAS
    // ------------------------------------

    private static class VentanaPrueba extends GestionHistorialGUI {

        String ultimoError;

        VentanaPrueba(HistorialHttpClient cliente) {
            super(cliente);
        }

        @Override
        protected void mostrarError(String mensaje) {
            ultimoError = mensaje;
        }

        // Permite probar tambien el metodo original
        void mostrarErrorOriginal(String mensaje) {
            super.mostrarError(mensaje);
        }
    }

    // ------------------------------------
    // PREPARAR Y FINALIZAR
    // ------------------------------------

    @BeforeEach
    void preparar() throws Exception {

        // Las ventanas Swing necesitan una pantalla
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Se necesita un entorno grafico");

        SwingUtilities.invokeAndWait(() -> {
            cliente = new ClienteSimulado();
            ventana = new VentanaPrueba(cliente);

            escribir("txtUsuario", "medico.demo");
            escribir("txtClave", "demo");
            escribir("txtIdPaciente", "paciente.demo");
        });
    }

    @AfterEach
    void finalizar() throws Exception {

        if (ventana != null) {
            SwingUtilities.invokeAndWait(ventana::dispose);
        }
    }

    // ------------------------------------
    // METODOS AUXILIARES
    // ------------------------------------

    private Object obtenerCampo(String nombre) {

        try {
            Field campo = GestionHistorialGUI.class
                    .getDeclaredField(nombre);
            campo.setAccessible(true);
            return campo.get(ventana);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void escribir(String nombre, String valor) {
        ((JTextField) obtenerCampo(nombre)).setText(valor);
    }

    private void cambiarCampo(String nombre, String valor)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> escribir(nombre, valor));
    }

    private JTable tabla() {
        return (JTable) obtenerCampo("tablaPruebas");
    }

    private String estado() {
        return ((JLabel) obtenerCampo("lblEstado")).getText();
    }

    private JButton buscarBoton(Container contenedor, String texto) {

        for (Component componente : contenedor.getComponents()) {

            if (componente instanceof JButton boton
                    && boton.getText().equals(texto)) {
                return boton;
            }

            if (componente instanceof Container hijo) {
                JButton encontrado = buscarBoton(hijo, texto);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    private void consultar() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            JButton boton = buscarBoton(
                    ventana.getContentPane(), "Consultar historial");

            assertNotNull(boton);
            boton.doClick();
        });
    }

    // ------------------------------------
    // PRUEBAS DE LA VENTANA
    // ------------------------------------

    @Test
    @DisplayName("La ventana se crea con su título y tamaño")
    void constructorConCliente() {
        assertAll(
                () -> assertEquals("SESCAM - Historial de pruebas",
                        ventana.getTitle()),
                () -> assertEquals(750, ventana.getWidth()),
                () -> assertEquals(450, ventana.getHeight()),
                () -> assertEquals(5, tabla().getColumnCount()),
                () -> assertEquals(0, tabla().getRowCount())
        );
    }

    @Test
    @DisplayName("La ventana se crea también con el cliente normal")
    void constructorNormal() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            GestionHistorialGUI normal = new GestionHistorialGUI();

            assertEquals("SESCAM - Historial de pruebas", normal.getTitle());
            normal.dispose();
        });
    }

    @Test
    @DisplayName("Consulta correcta: muestra las pruebas en la tabla y envía los datos")
    void consultaCorrecta() throws Exception {
        // Preparar
        cliente.respuesta = new RespuestaHistorial(200, DOS_PRUEBAS);

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals(1, cliente.llamadas),
                () -> assertEquals("medico.demo", cliente.usuarioRecibido),
                () -> assertEquals("demo", cliente.claveRecibida),
                () -> assertEquals("paciente.demo", cliente.idRecibido),
                () -> assertEquals(2, tabla().getRowCount()),
                () -> assertEquals("P2", tabla().getValueAt(0, 0)),
                () -> assertEquals("2026-03-05", tabla().getValueAt(0, 1)),
                () -> assertEquals("Radiografia", tabla().getValueAt(1, 2)),
                () -> assertEquals("REALIZADA", tabla().getValueAt(1, 3)),
                () -> assertEquals("Sin hallazgos", tabla().getValueAt(1, 4)),
                () -> assertEquals("2 pruebas realizadas", estado()),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Con una sola prueba el estado está en singular")
    void unaSolaPrueba() throws Exception {
        // Preparar
        cliente.respuesta = new RespuestaHistorial(200,
                "P1;2026-01-10;Radiografia;REALIZADA;Sin hallazgos");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals(1, tabla().getRowCount()),
                () -> assertEquals("1 prueba realizada", estado())
        );
    }

    @Test
    @DisplayName("Un historial vacío se indica y deja la tabla sin filas")
    void historialVacio() throws Exception {
        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals(0, tabla().getRowCount()),
                () -> assertEquals(
                        "El paciente no tiene pruebas realizadas", estado()),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Se eliminan los espacios del usuario y del identificador")
    void quitaEspacios() throws Exception {
        // Preparar
        cambiarCampo("txtUsuario", "  medico.demo ");
        cambiarCampo("txtIdPaciente", " paciente.demo  ");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals("medico.demo", cliente.usuarioRecibido),
                () -> assertEquals("paciente.demo", cliente.idRecibido)
        );
    }

    @Test
    @DisplayName("Tras consultar se borra la contraseña de la ventana")
    void borraLaClave() throws Exception {
        // Ejecutar
        consultar();

        // Comprobar
        assertEquals("", ((JTextField) obtenerCampo("txtClave")).getText());
    }

    @Test
    @DisplayName("Error HTTP 403: se muestra el mensaje con el código y se vacía la tabla")
    void errorHttp403() throws Exception {
        // Preparar: primero una consulta correcta para llenar la tabla
        cliente.respuesta = new RespuestaHistorial(200, DOS_PRUEBAS);
        consultar();
        assertEquals(2, tabla().getRowCount());

        cliente.respuesta = new RespuestaHistorial(403,
                "error=Un+paciente+solo+puede+consultar+su+historial.");
        cambiarCampo("txtClave", "demo");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals(
                        "Error HTTP 403: Un paciente solo puede consultar su historial.",
                        ventana.ultimoError),
                () -> assertEquals(0, tabla().getRowCount()),
                () -> assertEquals("Consulta rechazada", estado())
        );
    }

    @Test
    @DisplayName("Error HTTP 401 sin texto: se muestra el mensaje por defecto")
    void errorHttp401() throws Exception {
        // Preparar
        cliente.respuesta = new RespuestaHistorial(401, "");

        // Ejecutar
        consultar();

        // Comprobar
        assertEquals("Error HTTP 401: Credenciales incorrectas.",
                ventana.ultimoError);
    }

    @Test
    @DisplayName("Error HTTP 503: se avisa de que el servicio no está disponible")
    void errorHttp503() throws Exception {
        // Preparar
        cliente.respuesta = new RespuestaHistorial(503,
                "error=El+servicio+de+pruebas+no+esta+disponible.");

        // Ejecutar
        consultar();

        // Comprobar
        assertEquals(
                "Error HTTP 503: El servicio de pruebas no esta disponible.",
                ventana.ultimoError);
    }

    @Test
    @DisplayName("Sin usuario no se llama al servidor y se muestra el error")
    void sinUsuario() throws Exception {
        // Preparar
        cambiarCampo("txtUsuario", "   ");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals("El usuario es obligatorio",
                        ventana.ultimoError),
                () -> assertEquals(0, cliente.llamadas),
                () -> assertEquals("Consulta no realizada", estado())
        );
    }

    @Test
    @DisplayName("Sin contraseña no se llama al servidor y se muestra el error")
    void sinClave() throws Exception {
        // Preparar
        cambiarCampo("txtClave", "");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals("La contrasena es obligatoria",
                        ventana.ultimoError),
                () -> assertEquals(0, cliente.llamadas)
        );
    }

    @Test
    @DisplayName("Con un identificador incorrecto no se llama al servidor")
    void identificadorIncorrecto() throws Exception {
        // Preparar
        cambiarCampo("txtIdPaciente", "a b");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertTrue(ventana.ultimoError
                        .startsWith("El ID de paciente es obligatorio")),
                () -> assertEquals(0, cliente.llamadas)
        );
    }

    @Test
    @DisplayName("Con el identificador vacío no se llama al servidor")
    void identificadorVacio() throws Exception {
        // Preparar
        cambiarCampo("txtIdPaciente", "");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertNotNull(ventana.ultimoError),
                () -> assertEquals(0, cliente.llamadas)
        );
    }

    @Test
    @DisplayName("Si falla la conexión se muestra un error de conexión")
    void errorDeConexion() throws Exception {
        // Preparar
        cliente.errorConexion = true;

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals(
                        "Error al conectar con el servidor: Servidor no disponible",
                        ventana.ultimoError),
                () -> assertEquals("Consulta no realizada", estado())
        );
    }

    @Test
    @DisplayName("Una respuesta con formato incorrecto se muestra como error")
    void respuestaConFormatoIncorrecto() throws Exception {
        // Preparar
        cliente.respuesta = new RespuestaHistorial(200, "basura");

        // Ejecutar
        consultar();

        // Comprobar
        assertAll(
                () -> assertEquals(
                        "La respuesta del servidor tiene un formato incorrecto",
                        ventana.ultimoError),
                () -> assertEquals(0, tabla().getRowCount())
        );
    }

    @Test
    @DisplayName("Mostrar el diálogo de error original")
    void mostrarErrorOriginal() throws Exception {

        AtomicBoolean dialogoEncontrado = new AtomicBoolean(false);

        // Cerrar el dialogo automaticamente cuando aparezca
        Timer temporizador = new Timer(300, e -> {

            for (Window abierta : Window.getWindows()) {

                if (abierta instanceof JDialog && abierta.isShowing()) {
                    dialogoEncontrado.set(true);
                    abierta.dispose();
                }
            }
        });

        temporizador.setRepeats(false);
        temporizador.start();

        SwingUtilities.invokeAndWait(
                () -> ventana.mostrarErrorOriginal("Error de prueba"));

        assertTrue(dialogoEncontrado.get());
    }

    @Test
    @DisplayName("El método main abre la ventana")
    void ejecutarMain() throws Exception {

        GestionHistorialGUI.main(new String[0]);

        // Esperar a que el hilo de Swing cree la ventana
        SwingUtilities.invokeAndWait(() -> { });

        GestionHistorialGUI encontrada = null;

        for (Window w : Window.getWindows()) {

            if (w instanceof GestionHistorialGUI gui
                    && w != ventana
                    && w.isVisible()) {
                encontrada = gui;
            }
        }

        assertNotNull(encontrada);

        GestionHistorialGUI abierta = encontrada;
        SwingUtilities.invokeAndWait(abierta::dispose);
    }
}
