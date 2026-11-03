package com.sescam.radiodiagnostico.cliente.informes;

import javax.swing.*;
import java.awt.*;

/**
 * VISTA (CLIENTE) - Módulo de Informes y Pruebas (Yasmine)
 * Interfaz gráfica en Swing para que el médico gestione los informes.
 */
public class InformeGUI extends JFrame {

    public InformeGUI() {
        // Configuración básica de la ventana
        setTitle("🏥 SESCAM - Gestión de Informes Médicos");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(6, 1, 10, 10)); // Filas, Columnas, Espaciado

        // Componentes visuales
        JLabel titulo = new JLabel("Registro de Informe de Prueba", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 14));

        JTextField txtIdPrueba = new JTextField("ID de la Prueba (ej. PRB-001)");
        
        JTextArea txtInforme = new JTextArea("Escriba aquí el informe médico...");
        txtInforme.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JCheckBox chkIncidencias = new JCheckBox("¿Hubo incidencias en la realización?");
        
        JButton btnGuardar = new JButton("💾 Guardar Informe y Registrar Realización");
        btnGuardar.setBackground(new Color(46, 204, 113)); // Verde
        btnGuardar.setForeground(Color.WHITE);

        // Añadir elementos a la ventana
        add(titulo);
        add(txtIdPrueba);
        add(new JScrollPane(txtInforme));
        add(chkIncidencias);
        add(new JLabel("")); // Espacio en blanco
        add(btnGuardar);
    }

    public static void main(String[] args) {
        // Lanzar la ventana
        SwingUtilities.invokeLater(() -> {
            new InformeGUI().setVisible(true);
        });
    }
}