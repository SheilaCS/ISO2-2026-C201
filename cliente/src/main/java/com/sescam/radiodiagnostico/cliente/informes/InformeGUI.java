package com.sescam.radiodiagnostico.cliente.informes;

import javax.swing.*;
import java.awt.*;

public class InformeGUI extends JFrame {
    public InformeGUI() {
        setTitle("SESCAM - Gestión de Informes Médicos (CDU8)");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(5, 1, 10, 10));

        JTextField txtIdPrueba = new JTextField("ID de la Prueba");
        JTextArea txtInforme = new JTextArea("Escriba el informe...");
        JCheckBox chkIncidencias = new JCheckBox("¿Hubo incidencias?");
        JButton btnGuardar = new JButton("Guardar Informe");

        add(txtIdPrueba);
        add(new JScrollPane(txtInforme));
        add(chkIncidencias);
        add(btnGuardar);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InformeGUI().setVisible(true));
    }
}