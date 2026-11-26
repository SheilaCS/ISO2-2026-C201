package com.sescam.radiodiagnostico.cliente.listaespera;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

@DisplayName("CDU15 · Cliente · GestionListaEsperaGUI")
class GestionListaEsperaGUITest {

    private static final String LISTA =
            "2;12;Ecografia;URGENTE;2026-11-05\n"
            + "1;11;Radiografia;ORDINARIO;2026-11-01";

    private static final String HUECOS =
            "5;2026-12-10;09:00;10:00\n"
            + "6;2026-12-11;08:00;09:00";

    private static final String PROPUESTA =
            "2;12;Ecografia;URGENTE;2026-11-05;6;2026-12-11;08:00;09:00";

    private VentanaPrueba ventana;
    private ClienteSimulado cliente;

    // ------------------------------------
    // CLIENTE HTTP SIMULADO
    // ------------------------------------

    private static class ClienteSimulado extends ListaEsperaHttpClient {

        final List<String> llamadas = new ArrayList<>();
        String usuarioRecibido;
        String claveRecibida;
        boolean errorConexion;

        RespuestaListaEspera lista = new RespuestaListaEspera(200, LISTA);
        RespuestaListaEspera huecos = new RespuestaListaEspera(200, HUECOS);
        RespuestaListaEspera propuesta =
                new RespuestaListaEspera(200, PROPUESTA);
        RespuestaListaEspera operacion =
                new RespuestaListaEspera(200, "mensaje=Hecho.");

        private void registrar(String llamada, String usuario, char[] clave)
                throws IOException {
            llamadas.add(llamada);
            usuarioRecibido = usuario;
            claveRecibida = new String(clave);

            if (errorConexion) {
                throw new IOException("Servidor no disponible");
            }
        }

        int veces(String prefijo) {
            return (int) llamadas.stream()
                    .filter(l -> l.startsWith(prefijo)).count();
        }

        @Override
        public RespuestaListaEspera consultarLista(String usuario,
                char[] clave) throws IOException {
            registrar("lista", usuario, clave);
            return lista;
        }

        @Override
        public RespuestaListaEspera consultarHuecos(String usuario,
                char[] clave, String fecha) throws IOException {
            registrar("huecos:" + fecha, usuario, clave);
            return huecos;
        }

        @Override
        public RespuestaListaEspera consultarPropuesta(String usuario,
                char[] clave) throws IOException {
            registrar("propuesta", usuario, clave);
            return propuesta;
        }

        @Override
        public RespuestaListaEspera cambiarPrioridad(String usuario,
                char[] clave, String id, String prioridad)
                throws IOException {
            registrar("prioridad:" + id + ":" + prioridad, usuario, clave);
            return operacion;
        }

        @Override
        public RespuestaListaEspera retirar(String usuario, char[] clave,
                String id) throws IOException {
            registrar("retirar:" + id, usuario, clave);
            return operacion;
        }

        @Override
        public RespuestaListaEspera asignar(String usuario, char[] clave,
                String id, String medico, String fecha, String hora)
                throws IOException {
            registrar("asignar:" + id + ":" + medico + ":" + fecha + ":"
                    + hora, usuario, clave);
            return operacion;
        }
    }

    // ------------------------------------
    // VENTANA PARA LAS PRUEBAS
    // ------------------------------------

    private static class VentanaPrueba extends GestionListaEsperaGUI {

        String ultimoError;

        VentanaPrueba(ListaEsperaHttpClient cliente) {
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

            escribir("txtUsuario", "administrativo.demo");
            escribir("txtClave", "demo");
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
            Field campo = GestionListaEsperaGUI.class
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

    private JTable tablaLista() {
        return (JTable) obtenerCampo("tablaLista");
    }

    private JTable tablaHuecos() {
        return (JTable) obtenerCampo("tablaHuecos");
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

    private void pulsar(String texto) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            JButton boton = buscarBoton(ventana.getContentPane(), texto);

            assertNotNull(boton, "No existe el boton " + texto);
            boton.doClick();
        });
    }

    private void seleccionar(JTable tabla, int fila) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> tabla.setRowSelectionInterval(fila, fila));
    }

    // Cargar las dos tablas y vaciar el registro de llamadas
    private void cargarTablas() throws Exception {
        pulsar("Actualizar lista");
        pulsar("Consultar huecos");
        cliente.llamadas.clear();
    }

    // ------------------------------------
    // VENTANA
    // ------------------------------------

    @Test
    @DisplayName("La ventana se crea con su título, tamaño y tablas vacías")
    void constructorConCliente() {
        assertAll(
                () -> assertEquals("SESCAM - Listas de espera y huecos",
                        ventana.getTitle()),
                () -> assertEquals(900, ventana.getWidth()),
                () -> assertEquals(600, ventana.getHeight()),
                () -> assertEquals(5, tablaLista().getColumnCount()),
                () -> assertEquals(0, tablaLista().getRowCount()),
                () -> assertEquals(4, tablaHuecos().getColumnCount()),
                () -> assertEquals(0, tablaHuecos().getRowCount())
        );
    }

    @Test
    @DisplayName("La ventana se crea también con el cliente normal")
    void constructorNormal() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            GestionListaEsperaGUI normal = new GestionListaEsperaGUI();

            assertEquals("SESCAM - Listas de espera y huecos",
                    normal.getTitle());
            normal.dispose();
        });
    }

    // ------------------------------------
    // LISTA DE ESPERA
    // ------------------------------------

    @Test
    @DisplayName("Actualizar la lista muestra las solicitudes y envía las credenciales")
    void actualizarLista() throws Exception {
        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals(1, cliente.veces("lista")),
                () -> assertEquals("administrativo.demo",
                        cliente.usuarioRecibido),
                () -> assertEquals("demo", cliente.claveRecibida),
                () -> assertEquals(2, tablaLista().getRowCount()),
                () -> assertEquals("2", tablaLista().getValueAt(0, 0)),
                () -> assertEquals("Ecografia", tablaLista().getValueAt(0, 2)),
                () -> assertEquals("URGENTE", tablaLista().getValueAt(0, 3)),
                () -> assertEquals("2026-11-01",
                        tablaLista().getValueAt(1, 4)),
                () -> assertEquals("2 solicitudes en la lista de espera",
                        estado()),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Con una sola solicitud el estado está en singular")
    void unaSolicitud() throws Exception {
        cliente.lista = new RespuestaListaEspera(200,
                "1;11;Radiografia;ORDINARIO;2026-11-01");

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals(1, tablaLista().getRowCount()),
                () -> assertEquals("1 solicitud en la lista de espera",
                        estado())
        );
    }

    @Test
    @DisplayName("Una lista vacía deja la tabla sin filas")
    void listaVacia() throws Exception {
        cliente.lista = new RespuestaListaEspera(200, "");

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals(0, tablaLista().getRowCount()),
                () -> assertEquals("0 solicitudes en la lista de espera",
                        estado())
        );
    }

    @Test
    @DisplayName("Si el servidor rechaza la lista se muestra el error con el código y se vacía la tabla")
    void listaRechazada() throws Exception {
        pulsar("Actualizar lista");
        cliente.lista = new RespuestaListaEspera(403,
                "error=Solo+el+personal+administrativo.");

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals(
                        "No tienes permiso para realizar esta operacion. "
                        + "Motivo: Solo el personal administrativo. "
                        + "(Codigo HTTP 403)",
                        ventana.ultimoError),
                () -> assertEquals(0, tablaLista().getRowCount()),
                () -> assertEquals("Consulta rechazada", estado())
        );
    }

    @Test
    @DisplayName("Credenciales incorrectas sin texto del servidor: se explica qué ha pasado")
    void error401() throws Exception {
        cliente.lista = new RespuestaListaEspera(401, "");

        pulsar("Actualizar lista");

        assertEquals("El usuario o la contrasena no son correctos. "
                        + "(Codigo HTTP 401)",
                ventana.ultimoError);
    }

    @Test
    @DisplayName("Una respuesta con formato incorrecto se muestra como error")
    void formatoIncorrecto() throws Exception {
        cliente.lista = new RespuestaListaEspera(200, "basura");

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals(
                        "La respuesta del servidor tiene un formato incorrecto",
                        ventana.ultimoError),
                () -> assertEquals("Operacion no realizada", estado())
        );
    }

    // ------------------------------------
    // CREDENCIALES Y CONEXION
    // ------------------------------------

    @Test
    @DisplayName("Sin usuario no se llama al servidor y se muestra el error")
    void sinUsuario() throws Exception {
        cambiarCampo("txtUsuario", "   ");

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals("El usuario es obligatorio",
                        ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty()),
                () -> assertEquals("Operacion no realizada", estado())
        );
    }

    @Test
    @DisplayName("Sin contraseña no se llama al servidor y se muestra el error")
    void sinClave() throws Exception {
        cambiarCampo("txtClave", "");

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals("La contrasena es obligatoria",
                        ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty())
        );
    }

    @Test
    @DisplayName("Si falla la conexión se muestra un error de conexión")
    void errorDeConexion() throws Exception {
        cliente.errorConexion = true;

        pulsar("Actualizar lista");

        assertAll(
                () -> assertEquals(
                        "No se ha podido conectar con el servidor. "
                        + "Comprueba que esta en marcha. "
                        + "(Servidor no disponible)",
                        ventana.ultimoError),
                () -> assertEquals("Operacion no realizada", estado())
        );
    }

    // ------------------------------------
    // HUECOS
    // ------------------------------------

    @Test
    @DisplayName("Consultar huecos sin fecha los muestra todos")
    void huecosSinFecha() throws Exception {
        pulsar("Consultar huecos");

        assertAll(
                () -> assertEquals(1, cliente.veces("huecos:")),
                () -> assertEquals(2, tablaHuecos().getRowCount()),
                () -> assertEquals("5", tablaHuecos().getValueAt(0, 0)),
                () -> assertEquals("2026-12-10",
                        tablaHuecos().getValueAt(0, 1)),
                () -> assertEquals("09:00", tablaHuecos().getValueAt(0, 2)),
                () -> assertEquals("09:00", tablaHuecos().getValueAt(1, 3)),
                () -> assertEquals("2 huecos libres", estado())
        );
    }

    @Test
    @DisplayName("Consultar huecos de una fecha envía la fecha sin espacios")
    void huecosConFecha() throws Exception {
        cambiarCampo("txtFechaHuecos", " 2026-12-10 ");
        cliente.huecos = new RespuestaListaEspera(200,
                "5;2026-12-10;09:00;10:00");

        pulsar("Consultar huecos");

        assertAll(
                () -> assertEquals(1, cliente.veces("huecos:2026-12-10")),
                () -> assertEquals(1, tablaHuecos().getRowCount()),
                () -> assertEquals("1 hueco libre", estado())
        );
    }

    @Test
    @DisplayName("Con una fecha incorrecta no se llama al servidor")
    void huecosConFechaIncorrecta() throws Exception {
        cambiarCampo("txtFechaHuecos", "ayer");

        pulsar("Consultar huecos");

        assertAll(
                () -> assertEquals("La fecha debe tener formato AAAA-MM-DD",
                        ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty())
        );
    }

    @Test
    @DisplayName("Si el servidor rechaza los huecos se muestra el error y se vacía la tabla")
    void huecosRechazados() throws Exception {
        pulsar("Consultar huecos");
        cliente.huecos = new RespuestaListaEspera(403, "");

        pulsar("Consultar huecos");

        assertAll(
                () -> assertEquals("No tienes permiso para realizar esta operacion. "
                        + "(Codigo HTTP 403)",
                        ventana.ultimoError),
                () -> assertEquals(0, tablaHuecos().getRowCount()),
                () -> assertEquals("Consulta rechazada", estado())
        );
    }

    // ------------------------------------
    // CAMBIAR PRIORIDAD Y RETIRAR
    // ------------------------------------

    @Test
    @DisplayName("Cambiar la prioridad envía la solicitud seleccionada y recarga las tablas")
    void cambiarPrioridad() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 1);
        SwingUtilities.invokeAndWait(() -> ((JComboBox<?>)
                obtenerCampo("cmbPrioridad")).setSelectedItem("PREFERENTE"));

        pulsar("Cambiar prioridad");

        assertAll(
                () -> assertEquals(1, cliente.veces("prioridad:1:PREFERENTE")),
                () -> assertEquals(1, cliente.veces("lista")),
                () -> assertEquals(1, cliente.veces("huecos:")),
                () -> assertEquals("Hecho.", estado()),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Sin solicitud seleccionada no se cambia la prioridad")
    void prioridadSinSeleccion() throws Exception {
        cargarTablas();

        pulsar("Cambiar prioridad");

        assertAll(
                () -> assertEquals(
                        "Selecciona una solicitud de la lista de espera",
                        ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty())
        );
    }

    @Test
    @DisplayName("Si el servidor rechaza el cambio de prioridad se muestra el error")
    void prioridadRechazada() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);
        cliente.operacion = new RespuestaListaEspera(409,
                "error=No+existe+esa+solicitud.");

        pulsar("Cambiar prioridad");

        assertAll(
                () -> assertEquals("La operacion no se ha podido realizar. "
                        + "Motivo: No existe esa solicitud. (Codigo HTTP 409)",
                        ventana.ultimoError),
                () -> assertEquals("Operacion rechazada", estado()),
                () -> assertEquals(0, cliente.veces("lista"))
        );
    }

    @Test
    @DisplayName("Retirar envía la solicitud seleccionada y recarga las tablas")
    void retirar() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);

        pulsar("Retirar de la lista");

        assertAll(
                () -> assertEquals(1, cliente.veces("retirar:2")),
                () -> assertEquals(1, cliente.veces("lista")),
                () -> assertEquals("Hecho.", estado())
        );
    }

    @Test
    @DisplayName("Sin solicitud seleccionada no se retira nada")
    void retirarSinSeleccion() throws Exception {
        cargarTablas();

        pulsar("Retirar de la lista");

        assertAll(
                () -> assertNotNull(ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty())
        );
    }

    @Test
    @DisplayName("Si la lista no se puede recargar tras una operación se muestra el error")
    void recargaDeListaRechazada() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);
        cliente.lista = new RespuestaListaEspera(500, "");

        pulsar("Retirar de la lista");

        assertAll(
                () -> assertEquals(
                        "El servidor ha tenido un problema interno. "
                        + "Intentalo de nuevo mas tarde. (Codigo HTTP 500)",
                        ventana.ultimoError),
                () -> assertEquals("Consulta rechazada", estado()),
                () -> assertEquals(0, cliente.veces("huecos:"))
        );
    }

    @Test
    @DisplayName("Si los huecos no se pueden recargar tras una operación se muestra el error")
    void recargaDeHuecosRechazada() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);
        cliente.huecos = new RespuestaListaEspera(500, "");

        pulsar("Retirar de la lista");

        assertAll(
                () -> assertEquals(
                        "El servidor ha tenido un problema interno. "
                        + "Intentalo de nuevo mas tarde. (Codigo HTTP 500)",
                        ventana.ultimoError),
                () -> assertEquals("Consulta rechazada", estado())
        );
    }

    // ------------------------------------
    // ASIGNAR
    // ------------------------------------

    @Test
    @DisplayName("Asignar envía la solicitud y el hueco seleccionados y recarga las tablas")
    void asignar() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);
        seleccionar(tablaHuecos(), 1);

        pulsar("Asignar hueco");

        assertAll(
                () -> assertEquals(1,
                        cliente.veces("asignar:2:6:2026-12-11:08:00")),
                () -> assertEquals(1, cliente.veces("lista")),
                () -> assertEquals("Hecho.", estado()),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Sin solicitud seleccionada no se asigna")
    void asignarSinSolicitud() throws Exception {
        cargarTablas();
        seleccionar(tablaHuecos(), 0);

        pulsar("Asignar hueco");

        assertAll(
                () -> assertEquals(
                        "Selecciona una solicitud de la lista de espera",
                        ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty())
        );
    }

    @Test
    @DisplayName("Sin hueco seleccionado no se asigna")
    void asignarSinHueco() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);

        pulsar("Asignar hueco");

        assertAll(
                () -> assertEquals("Selecciona un hueco de la tabla de huecos",
                        ventana.ultimoError),
                () -> assertTrue(cliente.llamadas.isEmpty())
        );
    }

    @Test
    @DisplayName("Si el servidor rechaza la asignación se muestra el error con el código")
    void asignarRechazada() throws Exception {
        cargarTablas();
        seleccionar(tablaLista(), 0);
        seleccionar(tablaHuecos(), 0);
        cliente.operacion = new RespuestaListaEspera(409,
                "error=El+hueco+no+existe+o+ya+esta+ocupado.");

        pulsar("Asignar hueco");

        assertAll(
                () -> assertEquals(
                        "La operacion no se ha podido realizar. "
                        + "Motivo: El hueco no existe o ya esta ocupado. "
                        + "(Codigo HTTP 409)",
                        ventana.ultimoError),
                () -> assertEquals("Operacion rechazada", estado())
        );
    }

    // ------------------------------------
    // PROPUESTA
    // ------------------------------------

    @Test
    @DisplayName("La propuesta carga las tablas y selecciona la solicitud y el hueco propuestos")
    void propuesta() throws Exception {
        cambiarCampo("txtFechaHuecos", "2026-12-10");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals(1, cliente.veces("lista")),
                () -> assertEquals(1, cliente.veces("huecos:")),
                () -> assertEquals(1, cliente.veces("propuesta")),
                () -> assertEquals("", ((JTextField)
                        obtenerCampo("txtFechaHuecos")).getText()),
                () -> assertEquals(0, tablaLista().getSelectedRow()),
                () -> assertEquals(1, tablaHuecos().getSelectedRow()),
                () -> assertTrue(estado().startsWith(
                        "Propuesta: solicitud 2 con el medico 6 el "
                        + "2026-12-11 a las 08:00")),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Una propuesta que no coincide con las tablas no selecciona filas")
    void propuestaSinCoincidencia() throws Exception {
        // Mismo medico que el segundo hueco pero otra fecha
        cliente.propuesta = new RespuestaListaEspera(200,
                "99;12;Ecografia;URGENTE;2026-11-05;6;2026-12-31;08:00;09:00");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals(-1, tablaLista().getSelectedRow()),
                () -> assertEquals(-1, tablaHuecos().getSelectedRow()),
                () -> assertTrue(estado().startsWith("Propuesta: solicitud 99"))
        );
    }

    @Test
    @DisplayName("Una propuesta con otra hora en el mismo día y médico no selecciona el hueco")
    void propuestaConOtraHora() throws Exception {
        cliente.propuesta = new RespuestaListaEspera(200,
                "2;12;Ecografia;URGENTE;2026-11-05;6;2026-12-11;12:00;13:00");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals(0, tablaLista().getSelectedRow()),
                () -> assertEquals(-1, tablaHuecos().getSelectedRow())
        );
    }

    @Test
    @DisplayName("Sin propuesta se explica el motivo")
    void sinPropuesta() throws Exception {
        cliente.propuesta = new RespuestaListaEspera(200, "");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals(
                        "No hay propuesta: faltan solicitudes o huecos libres",
                        estado()),
                () -> assertNull(ventana.ultimoError)
        );
    }

    @Test
    @DisplayName("Si el servidor rechaza la propuesta se muestra el error")
    void propuestaRechazada() throws Exception {
        cliente.propuesta = new RespuestaListaEspera(403, "");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals("No tienes permiso para realizar esta operacion. "
                        + "(Codigo HTTP 403)",
                        ventana.ultimoError),
                () -> assertEquals("Consulta rechazada", estado())
        );
    }

    @Test
    @DisplayName("Si la lista es rechazada la propuesta no sigue")
    void propuestaConListaRechazada() throws Exception {
        cliente.lista = new RespuestaListaEspera(401, "");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals("El usuario o la contrasena no son correctos. "
                        + "(Codigo HTTP 401)",
                        ventana.ultimoError),
                () -> assertEquals(0, cliente.veces("huecos:")),
                () -> assertEquals(0, cliente.veces("propuesta"))
        );
    }

    @Test
    @DisplayName("Si los huecos son rechazados la propuesta no sigue")
    void propuestaConHuecosRechazados() throws Exception {
        cliente.huecos = new RespuestaListaEspera(401, "");

        pulsar("Proponer asignacion");

        assertAll(
                () -> assertEquals("El usuario o la contrasena no son correctos. "
                        + "(Codigo HTTP 401)",
                        ventana.ultimoError),
                () -> assertEquals(0, cliente.veces("propuesta"))
        );
    }

    // ------------------------------------
    // DIALOGO Y MAIN
    // ------------------------------------

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

        GestionListaEsperaGUI.main(new String[0]);

        // Esperar a que el hilo de Swing cree la ventana
        SwingUtilities.invokeAndWait(() -> { });

        GestionListaEsperaGUI encontrada = null;

        for (Window w : Window.getWindows()) {

            if (w instanceof GestionListaEsperaGUI gui
                    && w != ventana
                    && w.isVisible()) {
                encontrada = gui;
            }
        }

        assertNotNull(encontrada);

        GestionListaEsperaGUI abierta = encontrada;
        SwingUtilities.invokeAndWait(abierta::dispose);
    }
}
