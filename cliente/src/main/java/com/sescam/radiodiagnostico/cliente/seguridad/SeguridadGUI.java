package com.sescam.radiodiagnostico.cliente.seguridad;

import javax.swing.*;
import java.awt.*;

public class SeguridadGUI extends JFrame {

    private JTextField txtUsuario;
    private JTextField txtRol;
    private JTextField txtAccion;
    private JTextArea areaResultado;
    private SeguridadHttpClient clienteHttp;

    public SeguridadGUI() {
        this(new SeguridadHttpClient());
    }

    public SeguridadGUI(SeguridadHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Gestión de Seguridad y Calidad (CDU11)");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsuario = new JTextField();
        txtRol = new JTextField("ADMINISTRADOR"); // Por defecto
        txtAccion = new JTextField();

        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Rol (PACIENTE/MEDICO/ADMINISTRATIVO/ADMINISTRADOR):"));
        panel.add(txtRol);
        panel.add(new JLabel("Acción a realizar:"));
        panel.add(txtAccion);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnVerificar = new JButton("Verificar Permiso");
        JButton btnVerLogs = new JButton("Ver Logs (Solo Admin)");
        panelBotones.add(btnVerificar);
        panelBotones.add(btnVerLogs);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnVerificar.addActionListener(e -> verificarPermiso());
        btnVerLogs.addActionListener(e -> verLogs());
    }

    private void verificarPermiso() {
        try {
            String respuesta = clienteHttp.verificarPermiso(
                    txtUsuario.getText().trim(),
                    txtRol.getText().trim(),
                    txtAccion.getText().trim()
            );
            areaResultado.setText(respuesta);
        } catch (Exception ex) {
            areaResultado.setText("Error al conectar: " + ex.getMessage());
        }
    }

    private void verLogs() {
        try {
            String respuesta = clienteHttp.obtenerLogs(txtRol.getText().trim());
            areaResultado.setText(respuesta);
        } catch (Exception ex) {
            areaResultado.setText("Error al conectar: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SeguridadGUI().setVisible(true));
    }
}