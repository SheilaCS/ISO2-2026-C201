package com.sescam.radiodiagnostico.cliente.seguridad;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de auditoría de actividades y accesos (CDU11, RF19).
 */
public class SeguridadGUI extends JFrame {

    private final JTextField txtUsuario;
    private final JPasswordField txtClave;
    private final JTextField txtAccion;
    private final JTextField txtDetalles;
    private final JTextArea areaResultado;
    private final SeguridadHttpClient clienteHttp;

    public SeguridadGUI() {
        this(new SeguridadHttpClient());
    }

    public SeguridadGUI(SeguridadHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Auditoría de actividades y accesos (CDU11)");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        txtAccion = new JTextField();
        txtDetalles = new JTextField();

        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtClave);
        panel.add(new JLabel("Acción a registrar:"));
        panel.add(txtAccion);
        panel.add(new JLabel("Detalles:"));
        panel.add(txtDetalles);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnRegistrar = new JButton("Registrar acción");
        JButton btnVerLogs = new JButton("Ver auditoría");
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnVerLogs);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrarAccion());
        btnVerLogs.addActionListener(e -> verLogs());
    }

    private boolean prepararCredenciales() {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());
        if (usuario.isEmpty() || clave.isEmpty()) {
            areaResultado.setText("Introduce el usuario y la contraseña.");
            return false;
        }
        clienteHttp.setCredenciales(usuario, clave);
        return true;
    }

    private void registrarAccion() {
        if (!prepararCredenciales()) {
            return;
        }
        if (txtAccion.getText().trim().isEmpty()) {
            areaResultado.setText("Introduce la acción a registrar.");
            return;
        }
        try {
            mostrar(clienteHttp.registrarAccion(
                    txtAccion.getText().trim(), txtDetalles.getText().trim()));
        } catch (Exception ex) {
            areaResultado.setText("Error al conectar: " + ex.getMessage());
        }
    }

    private void verLogs() {
        if (!prepararCredenciales()) {
            return;
        }
        try {
            mostrar(clienteHttp.obtenerLogs());
        } catch (Exception ex) {
            areaResultado.setText("Error al conectar: " + ex.getMessage());
        }
    }

    private void mostrar(SeguridadHttpClient.Respuesta respuesta) {
        areaResultado.setText(respuesta.correcta()
                ? respuesta.texto()
                : "Error " + respuesta.codigo() + ": " + respuesta.texto());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SeguridadGUI().setVisible(true));
    }
}