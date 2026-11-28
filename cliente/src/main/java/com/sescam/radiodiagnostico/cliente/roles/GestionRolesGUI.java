package com.sescam.radiodiagnostico.cliente.roles;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.cliente.roles.RolesHttpClient.Respuesta;
import com.sescam.radiodiagnostico.roles.Permiso;

/**
 * Pantalla del administrador para gestionar roles y permisos (CDU10, RF18).
 */
public class GestionRolesGUI extends JFrame {

    private final RolesHttpClient cliente;

    private final JTextField campoUsuario = new JTextField();
    private final JPasswordField campoClave = new JPasswordField();
    private final JTextField campoUsuarioGestionado = new JTextField();
    private final JComboBox<Rol> rolUsuario = new JComboBox<>(Rol.values());
    private final JComboBox<Rol> rolPermisos = new JComboBox<>(Rol.values());
    private final JComboBox<Permiso> permiso = new JComboBox<>(Permiso.values());
    private final JTextArea resultado = new JTextArea(8, 40);

    @FunctionalInterface
    interface Operacion {
        Respuesta ejecutar() throws IOException, InterruptedException;
    }

    public GestionRolesGUI() {
        this(new RolesHttpClient());
    }

    GestionRolesGUI(RolesHttpClient cliente) {
        super("SESCAM - Gestión de roles y permisos");
        this.cliente = cliente;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel credenciales = new JPanel(new GridLayout(1, 4, 8, 8));
        credenciales.setBorder(BorderFactory.createTitledBorder("Administrador"));
        credenciales.add(new JLabel("Usuario:"));
        credenciales.add(campoUsuario);
        credenciales.add(new JLabel("Contraseña:"));
        credenciales.add(campoClave);

        JPanel usuarios = new JPanel(new GridLayout(3, 2, 8, 8));
        usuarios.setBorder(BorderFactory.createTitledBorder("Rol de un usuario"));
        usuarios.add(new JLabel("Usuario:"));
        usuarios.add(campoUsuarioGestionado);
        usuarios.add(new JLabel("Nuevo rol:"));
        usuarios.add(rolUsuario);
        usuarios.add(boton("Consultar rol", this::consultarRol));
        usuarios.add(boton("Cambiar rol", this::cambiarRol));

        JPanel permisos = new JPanel(new GridLayout(3, 2, 8, 8));
        permisos.setBorder(BorderFactory.createTitledBorder("Permisos de un rol"));
        permisos.add(rolPermisos);
        permisos.add(permiso);
        permisos.add(boton("Ver permisos", this::verPermisos));
        permisos.add(boton("Listar usuarios", this::listarUsuarios));
        permisos.add(boton("Conceder permiso", this::concederPermiso));
        permisos.add(boton("Retirar permiso", this::retirarPermiso));

        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 10));
        centro.add(usuarios);
        centro.add(permisos);

        resultado.setEditable(false);
        resultado.setText("Introduce tus credenciales de administrador.");

        add(credenciales, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(new JScrollPane(resultado), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private static JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(evento -> accion.run());
        return boton;
    }

    private void consultarRol() {
        if (comprobarUsuarioGestionado()) {
            String usuario = campoUsuarioGestionado.getText().strip();
            ejecutar(() -> cliente.consultarRol(usuario),
                    datos -> "El usuario " + datos.get("usuario")
                            + " tiene el rol " + datos.get("rol") + ".");
        }
    }

    private void cambiarRol() {
        if (comprobarUsuarioGestionado()) {
            String usuario = campoUsuarioGestionado.getText().strip();
            String rol = ((Rol) rolUsuario.getSelectedItem()).name();
            ejecutar(() -> cliente.cambiarRol(usuario, rol),
                    datos -> "Rol de " + datos.get("usuario")
                            + " cambiado a " + datos.get("rol") + ".");
        }
    }

    private void listarUsuarios() {
        ejecutar(cliente::listarUsuarios, datos -> {
            StringJoiner lineas = new StringJoiner("\n", "Usuarios:\n", "");
            datos.forEach((usuario, rol) -> lineas.add("- " + usuario + ": " + rol));
            return lineas.toString();
        });
    }

    private void verPermisos() {
        String rol = ((Rol) rolPermisos.getSelectedItem()).name();
        ejecutar(() -> cliente.consultarPermisos(rol), this::describirPermisos);
    }

    private void concederPermiso() {
        String rol = ((Rol) rolPermisos.getSelectedItem()).name();
        String accion = ((Permiso) permiso.getSelectedItem()).name();
        ejecutar(() -> cliente.concederPermiso(rol, accion), this::describirPermisos);
    }

    private void retirarPermiso() {
        String rol = ((Rol) rolPermisos.getSelectedItem()).name();
        String accion = ((Permiso) permiso.getSelectedItem()).name();
        ejecutar(() -> cliente.retirarPermiso(rol, accion), this::describirPermisos);
    }

    private String describirPermisos(Map<String, String> datos) {
        String lista = datos.getOrDefault("permisos", "");
        return lista.isEmpty()
                ? "El rol " + datos.get("rol") + " no tiene permisos."
                : "Permisos de " + datos.get("rol") + ": " + lista.replace(",", ", ") + ".";
    }

    private boolean comprobarUsuarioGestionado() {
        if (!ValidacionesRoles.textoObligatorio(campoUsuarioGestionado.getText())) {
            resultado.setText("Debes indicar el usuario que quieres gestionar.");
            return false;
        }
        return true;
    }

    private void ejecutar(Operacion operacion,
                          java.util.function.Function<Map<String, String>, String> mensaje) {
        char[] clave = campoClave.getPassword();
        try {
            if (!ValidacionesRoles.credencialesCompletas(campoUsuario.getText(), clave)) {
                resultado.setText("Debes introducir usuario y contraseña.");
                return;
            }
            cliente.setCredenciales(campoUsuario.getText().strip(), new String(clave));

            Respuesta respuesta = operacion.ejecutar();
            resultado.setText(respuesta.correcta()
                    ? mensaje.apply(respuesta.datos())
                    : "Error " + respuesta.codigo() + ": " + respuesta.error());
        } catch (IOException e) {
            resultado.setText("No se puede conectar con el servidor de roles (puerto 8091).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resultado.setText("La operación se ha interrumpido.");
        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestionRolesGUI().setVisible(true));
    }
}