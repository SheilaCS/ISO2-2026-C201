package com.sescam.radiodiagnostico.cliente.autenticacion;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.*;

public class LoginGUI extends JFrame {

    private final JTextField campoUsuario = new JTextField(20);
    private final JPasswordField campoClave = new JPasswordField(20);
    private final JLabel mensaje = new JLabel("Introduce tus datos de acceso.");

    public LoginGUI() {
        setTitle("SESCAM - Inicio de sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel formulario = new JPanel(new GridLayout(2, 2, 10, 10));
        formulario.add(new JLabel("Usuario:"));
        formulario.add(campoUsuario);
        formulario.add(new JLabel("Contraseña:"));
        formulario.add(campoClave);

        JButton botonEntrar = new JButton("Iniciar sesión");
        botonEntrar.addActionListener(evento -> iniciarSesion());

        JPanel parteInferior = new JPanel(new BorderLayout(10, 10));
        parteInferior.add(mensaje, BorderLayout.CENTER);
        parteInferior.add(botonEntrar, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout(10, 15));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contenido.add(
            new JLabel("Acceso a Radiodiagnóstico", SwingConstants.CENTER),
            BorderLayout.NORTH
        );
        contenido.add(formulario, BorderLayout.CENTER);
        contenido.add(parteInferior, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(botonEntrar);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void iniciarSesion() {
        String usuario = campoUsuario.getText().trim();
        char[] clave = campoClave.getPassword();

        try {
            if (usuario.isEmpty() || clave.length == 0) {
                mensaje.setText("Debes introducir usuario y contraseña.");
                return;
            }

            // La autenticación se conectará al servicio acordado con el servidor.
            mensaje.setText("Autenticación pendiente de conectar al servidor.");
        } finally {
            Arrays.fill(clave, '\0');
            campoClave.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginGUI().setVisible(true));
    }
}