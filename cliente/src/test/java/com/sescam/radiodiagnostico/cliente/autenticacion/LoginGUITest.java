package com.sescam.radiodiagnostico.cliente.autenticacion;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la pantalla de inicio de sesión (CDU1, RF01).
 * Se usa un servicio de autenticación falso para probar solo la pantalla.
 */
@DisplayName("CDU1 · Cliente · LoginGUI (pantalla)")
class LoginGUITest {

    private LoginGUI ventana;
    private JTextField campoUsuario;
    private JPasswordField campoClave;
    private JButton botonEntrar;
    private JLabel mensaje;

    private int llamadasAlServicio;

    @BeforeEach
    void setUp() throws Exception {
        // Servicio falso: solo acepta medico.demo / demo
        ServicioAutenticacion servicioFalso = (usuario, clave) -> {
            llamadasAlServicio++;
            if ("medico.demo".equals(usuario) && "demo".equals(new String(clave))) {
                return new SesionUsuario("medico.demo", "MEDICO");
            }
            return null;
        };

        SwingUtilities.invokeAndWait(() -> {
            ventana = new LoginGUI(servicioFalso);
            campoClave = buscar(ventana, JPasswordField.class, c -> true);
            campoUsuario = buscar(ventana, JTextField.class, c -> !(c instanceof JPasswordField));
            botonEntrar = buscar(ventana, JButton.class, b -> "Iniciar sesión".equals(b.getText()));
            mensaje = buscar(ventana, JLabel.class,
                    l -> "Introduce tus datos de acceso.".equals(l.getText()));
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> ventana.dispose());
    }

    @Test
    @DisplayName("L1. Con credenciales correctas muestra el acceso y el rol")
    void credencialesCorrectasMuestranAcceso() throws Exception {
        // Preparar
        escribir("medico.demo", "demo");

        // Ejecutar
        pulsarEntrar();

        // Comprobar
        assertEquals("Acceso correcto: medico.demo (MEDICO)", mensaje.getText());
    }

    @Test
    @DisplayName("L2. Con una clave incorrecta muestra que los datos no son válidos")
    void claveIncorrectaMuestraError() throws Exception {
        // Preparar
        escribir("medico.demo", "mal");

        // Ejecutar
        pulsarEntrar();

        // Comprobar
        assertEquals("Usuario o contraseña incorrectos.", mensaje.getText());
    }

    @Test
    @DisplayName("L3. Con el usuario vacío pide rellenar los campos y no llama al servicio")
    void usuarioVacioPideRellenarCampos() throws Exception {
        // Preparar
        escribir("", "demo");

        // Ejecutar
        pulsarEntrar();

        // Comprobar
        assertAll(
                () -> assertEquals("Debes introducir usuario y contraseña.", mensaje.getText()),
                () -> assertEquals(0, llamadasAlServicio, "No debe llamar al servicio"));
    }

    @Test
    @DisplayName("L4. Con la clave vacía pide rellenar los campos y no llama al servicio")
    void claveVaciaPideRellenarCampos() throws Exception {
        // Preparar
        escribir("medico.demo", "");

        // Ejecutar
        pulsarEntrar();

        // Comprobar
        assertAll(
                () -> assertEquals("Debes introducir usuario y contraseña.", mensaje.getText()),
                () -> assertEquals(0, llamadasAlServicio, "No debe llamar al servicio"));
    }

    @Test
    @DisplayName("L5. Los espacios alrededor del usuario se ignoran")
    void espaciosAlrededorDelUsuarioSeIgnoran() throws Exception {
        // Preparar
        escribir("  medico.demo  ", "demo");

        // Ejecutar
        pulsarEntrar();

        // Comprobar
        assertEquals("Acceso correcto: medico.demo (MEDICO)", mensaje.getText());
    }

    @Test
    @DisplayName("L6. Tras pulsar el botón, el campo de contraseña queda vacío")
    void campoClaveQuedaVacioTrasPulsar() throws Exception {
        // Preparar
        escribir("medico.demo", "mal");

        // Ejecutar
        pulsarEntrar();

        // Comprobar
        assertEquals(0, campoClave.getPassword().length, "La contraseña no debe quedar en pantalla");
    }

    @Test
    @DisplayName("L7. La ventana muestra el título del inicio de sesión")
    void ventanaTieneElTituloCorrecto() {
        // Comprobar
        assertEquals("SESCAM - Inicio de sesión", ventana.getTitle());
    }

    @Test
    @DisplayName("L8. El método main abre una pantalla de inicio de sesión visible")
    void mainAbreLaPantalla() throws Exception {
        // Ejecutar: main pide a Swing que cree la ventana más tarde (invokeLater)
        LoginGUI.main(new String[] {});

        // Esperar a que Swing termine esa tarea pendiente
        SwingUtilities.invokeAndWait(() -> { });

        // Comprobar: hay otra LoginGUI visible además de la del setUp
        boolean encontrada = false;
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof LoginGUI gui && gui != ventana && gui.isVisible()) {
                encontrada = true;
                SwingUtilities.invokeAndWait(gui::dispose);
            }
        }
        assertTrue(encontrada, "main debe abrir la ventana de inicio de sesión");
    }

    // ---------- Métodos de ayuda ----------

    private void escribir(String usuario, String clave) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            campoUsuario.setText(usuario);
            campoClave.setText(clave);
        });
    }

    private void pulsarEntrar() throws Exception {
        SwingUtilities.invokeAndWait(() -> botonEntrar.doClick());
    }

    /** Busca dentro de la ventana el primer componente de un tipo que cumpla una condición. */
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