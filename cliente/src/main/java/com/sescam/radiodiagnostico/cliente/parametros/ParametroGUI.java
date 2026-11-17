package com.sescam.radiodiagnostico.cliente.parametros;

import javax.swing.*;

public class ParametroGUI extends JFrame {
    public ParametroGUI() {
        setTitle("SESCAM - Configuración de Parámetros (CDU5)");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JLabel label = new JLabel("Interfaz para configurar parámetros del sistema", SwingConstants.CENTER);
        add(label);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParametroGUI().setVisible(true));
    }
}