package com.sescam.radiodiagnostico.cliente.autenticacion;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.*;

public class LoginGUI extends JFrame {

    private final JTextField campoUsuario = new JTextField(20);
    private final JPasswordField campoClave = new JPasswordField(20);
    private final JLabel mensaje = new JLabel("Introduce tus datos de acceso.");
    private final ServicioAutenticacion servicioAutenticacion;
    private SesionUsuario sesionActual;

    public LoginGUI(ServicioAutenticacion servicioAutenticacion) {
    this.servicioAutenticacion = servicioAutenticacion;
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

        SesionUsuario resultado =
                servicioAutenticacion.iniciarSesion(usuario, clave);

        if (resultado == null) {
            mensaje.setText("Usuario o contraseña incorrectos.");
            return;
        }

        sesionActual = resultado;
        mensaje.setText("Acceso correcto: " + sesionActual.identificador()
                + " (" + sesionActual.rol() + ")");

    } finally {
        Arrays.fill(clave, '\0');
        campoClave.setText("");
    }
}

    public static void main(String[] args) {
    SwingUtilities.invokeLater(() ->
        new LoginGUI(new ServicioAutenticacionSimulado())
                .setVisible(true)
    );
}
}