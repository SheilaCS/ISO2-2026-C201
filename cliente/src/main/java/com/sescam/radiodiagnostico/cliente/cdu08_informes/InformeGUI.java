package com.sescam.radiodiagnostico.cliente.cdu08_informes;

import javax.swing.*;

public class InformeGUI extends JFrame {
    public InformeGUI() {
        setTitle("SESCAM - Módulo de Informes (CDU8)");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JLabel label = new JLabel("Interfaz para crear y consultar informes", SwingConstants.CENTER);
        add(label);
    }
}