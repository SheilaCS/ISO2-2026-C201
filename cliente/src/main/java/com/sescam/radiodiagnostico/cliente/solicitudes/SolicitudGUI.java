package com.sescam.radiodiagnostico.cliente.solicitudes;

import javax.swing.*;

public class SolicitudGUI extends JFrame {
    public SolicitudGUI() {
        setTitle("SESCAM - Módulo de Solicitudes (CDU2)");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JLabel label = new JLabel("Interfaz para solicitar pruebas médicas", SwingConstants.CENTER);
        add(label);
    }
}
