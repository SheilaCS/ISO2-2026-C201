package com.sescam.radiodiagnostico.cliente.roles;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.roles.GestionRolesHttpServer;
import com.sescam.radiodiagnostico.roles.Permiso;
import com.sun.net.httpserver.HttpServer;

/**
 * Pruebas de la pantalla contra el servidor real de roles en un puerto libre.
 */
@DisplayName("CDU10 · Cliente · GestionRolesGUI (pantalla)")
class GestionRolesGUITest {

    private HttpServer servidor;
    private GestionRolesGUI ventana;
    private JTextField campoUsuario;
    private JPasswordField campoClave;
    private JTextField campoUsuarioGestionado;
    private JComboBox<?> rolUsuario;
    private JComboBox<?> rolPermisos;
    private JComboBox<?> permiso;
    private JTextArea resultado;

    @BeforeEach
    void setUp() throws Exception {
        servidor = GestionRolesHttpServer.crearServidor(0);
        servidor.start();
        RolesHttpClient cliente = new RolesHttpClient(
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/roles");

        SwingUtilities.invokeAndWait(() -> {
            ventana = new GestionRolesGUI(cliente);
            campoClave = buscar(ventana, JPasswordField.class, c -> true);
            campoUsuario = buscar(ventana, JTextField.class, c -> !(c instanceof JPasswordField));
            campoUsuarioGestionado = buscar(ventana, JTextField.class,
                    c -> !(c instanceof JPasswordField) && c != campoUsuario);
            rolUsuario = buscar(ventana, JComboBox.class, c -> true);
            rolPermisos = buscar(ventana, JComboBox.class, c -> c != rolUsuario
                    && c.getItemAt(0) instanceof Rol);
            permiso = buscar(ventana, JComboBox.class, c -> c.getItemAt(0) instanceof Permiso);
            resultado = buscar(ventana, JTextArea.class, c -> true);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> ventana.dispose());
        servidor.stop(0);
    }

    @Test
    @DisplayName("P1. La ventana muestra su título y el mensaje inicial")
    void tituloYMensajeInicial() {
        assertEquals("SESCAM - Gestión de roles y permisos", ventana.getTitle());
        assertEquals("Introduce tus credenciales de administrador.", resultado.getText());
    }

    @Test
    @DisplayName("P2. Sin credenciales pide usuario y contraseña")
    void sinCredenciales() throws Exception {
        pulsar("Listar usuarios");

        assertEquals("Debes introducir usuario y contraseña.", resultado.getText());
    }

    @Test
    @DisplayName("P3. Sin usuario a gestionar pide indicarlo")
    void sinUsuarioGestionado() throws Exception {
        credenciales("admin.demo", "demo");

        pulsar("Consultar rol");
        String consultar = resultado.getText();
        pulsar("Cambiar rol");

        assertEquals("Debes indicar el usuario que quieres gestionar.", consultar);
        assertEquals("Debes indicar el usuario que quieres gestionar.", resultado.getText());
    }

    @Test
    @DisplayName("P4. El administrador consulta y cambia el rol de un usuario")
    void consultaYCambiaRol() throws Exception {
        credenciales("admin.demo", "demo");
        escribir(campoUsuarioGestionado, "medico.demo");

        pulsar("Consultar rol");
        String consulta = resultado.getText();
        seleccionar(rolUsuario, Rol.ADMINISTRATIVO);
        pulsar("Cambiar rol");

        assertEquals("El usuario medico.demo tiene el rol MEDICO.", consulta);
        assertEquals("Rol de medico.demo cambiado a ADMINISTRATIVO.", resultado.getText());
    }

    @Test
    @DisplayName("P5. El administrador lista usuarios y gestiona permisos")
    void listaYGestionaPermisos() throws Exception {
        credenciales("admin.demo", "demo");

        pulsar("Listar usuarios");
        String lista = resultado.getText();
        seleccionar(rolPermisos, Rol.PACIENTE);
        pulsar("Ver permisos");
        String ver = resultado.getText();
        seleccionar(permiso, Permiso.SOLICITAR_PRUEBAS);
        pulsar("Conceder permiso");
        String concedido = resultado.getText();
        pulsar("Retirar permiso");

        assertTrue(lista.startsWith("Usuarios:") && lista.contains("- medico.demo: MEDICO"));
        assertEquals("Permisos de PACIENTE: CONSULTAR_HISTORIAL, CONSULTAR_MIS_CITAS.", ver);
        assertTrue(concedido.contains("SOLICITAR_PRUEBAS"));
        assertEquals("Permisos de PACIENTE: CONSULTAR_HISTORIAL, CONSULTAR_MIS_CITAS.", resultado.getText());
    }

    @Test
    @DisplayName("P6. Un rol sin permisos se indica con un mensaje")
    void rolSinPermisos() throws Exception {
        credenciales("admin.demo", "demo");
        seleccionar(rolPermisos, Rol.PACIENTE);
        seleccionar(permiso, Permiso.CONSULTAR_HISTORIAL);
        pulsar("Retirar permiso");
        seleccionar(permiso, Permiso.CONSULTAR_MIS_CITAS);
        pulsar("Retirar permiso");

        assertEquals("El rol PACIENTE no tiene permisos.", resultado.getText());
    }

    @Test
    @DisplayName("P7. Un rol sin permiso de gestión de roles ve el error 403")
    void rolNoAutorizado() throws Exception {
        credenciales("medico.demo", "demo");

        pulsar("Listar usuarios");

        assertEquals("Error 403: Acceso reservado a la gestión de roles.", resultado.getText());
    }

    @Test
    @DisplayName("P8. Si el servidor no está disponible lo indica")
    void servidorNoDisponible() throws Exception {
        servidor.stop(0);
        credenciales("admin.demo", "demo");

        pulsar("Listar usuarios");

        assertEquals("No se puede conectar con el servidor de roles (puerto 8091).", resultado.getText());
    }

    @Test
    @DisplayName("P9. El método main abre la pantalla de roles")
    void mainAbreLaPantalla() throws Exception {
        GestionRolesGUI.main(new String[] {});
        SwingUtilities.invokeAndWait(() -> { });

        boolean encontrada = false;
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof GestionRolesGUI gui && gui != ventana && gui.isVisible()) {
                encontrada = true;
                SwingUtilities.invokeAndWait(gui::dispose);
            }
        }
        assertTrue(encontrada, "main debe abrir la pantalla de roles");
    }

    // ---------- Métodos de ayuda ----------

    private void credenciales(String usuario, String clave) throws Exception {
        escribir(campoUsuario, usuario);
        SwingUtilities.invokeAndWait(() -> campoClave.setText(clave));
    }

    private void escribir(JTextField campo, String texto) throws Exception {
        SwingUtilities.invokeAndWait(() -> campo.setText(texto));
    }

    private void seleccionar(JComboBox<?> lista, Object valor) throws Exception {
        SwingUtilities.invokeAndWait(() -> lista.setSelectedItem(valor));
    }

    private void pulsar(String texto) throws Exception {
        SwingUtilities.invokeAndWait(() ->
                buscar(ventana, JButton.class, b -> texto.equals(b.getText())).doClick());
    }

    /** Busca dentro de la ventana el primer componente de un tipo que cumpla una condición. */
    @SuppressWarnings("unchecked")
    private static <T extends Component> T buscar(Container contenedor, Class<T> tipo,
                                                  Predicate<T> condicion) {
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo) && condicion.test(tipo.cast(hijo))) {
                return tipo.cast(hijo);
            }
            if (hijo instanceof Container c) {
                T encontrado = buscar(c, tipo, condicion);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }
}
