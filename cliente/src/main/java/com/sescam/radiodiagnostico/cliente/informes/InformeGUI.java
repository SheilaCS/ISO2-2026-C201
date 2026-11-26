package com.sescam.radiodiagnostico.cliente.informes;

import javax.swing.*;
import java.awt.*;

public class InformeGUI extends JFrame {

    private JTextField txtIdEquipo;
    private JTextField txtFecha;
    private JTextField txtDescripcion;
    private JTextField txtRol;
    private JTextField txtIdInforme;
    private JTextArea areaResultado;
    private InformesHttpClient clienteHttp;

    public InformeGUI() {
        this(new InformesHttpClient());
    }

    public InformeGUI(InformesHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Informes de Mantenimiento (CDU8)");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtIdEquipo = new JTextField();
        txtFecha = new JTextField();
        txtDescripcion = new JTextField();
        txtRol = new JTextField("TECNICO"); // Por defecto ponemos TECNICO
        txtIdInforme = new JTextField();

        panel.add(new JLabel("Rol del usuario (TECNICO/ADMIN):"));
        panel.add(txtRol);
        panel.add(new JLabel("ID Equipo (para generar):"));
        panel.add(txtIdEquipo);
        panel.add(new JLabel("Fecha (AAAA-MM-DD):"));
        panel.add(txtFecha);
        panel.add(new JLabel("Descripción de la avería:"));
        panel.add(txtDescripcion);
        panel.add(new JLabel("ID Informe (para consultar):"));
        panel.add(txtIdInforme);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnGenerar = new JButton("Generar Informe");
        JButton btnConsultar = new JButton("Consultar Informe");
        panelBotones.add(btnGenerar);
        panelBotones.add(btnConsultar);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnGenerar.addActionListener(e -> generarInforme());
        btnConsultar.addActionListener(e -> consultarInforme());
    }

    private void generarInforme() {
        try {
            // 1. Seguridad
            if (!ValidacionesInformes.tienePermiso(txtRol.getText())) {
                throw new SecurityException("Acceso denegado: Se requiere rol TECNICO o ADMIN.");
            }
            // 2. Calidad y validaciones
            if (!ValidacionesInformes.validarId(txtIdEquipo.getText())) {
                throw new IllegalArgumentException("El ID de equipo debe ser un entero positivo.");
            }
            if (!ValidacionesInformes.validarFecha(txtFecha.getText())) {
                throw new IllegalArgumentException("La fecha debe tener formato válido AAAA-MM-DD.");
            }
            if (!ValidacionesInformes.validarTexto(txtDescripcion.getText())) {
                throw new IllegalArgumentException("La descripción no puede estar vacía.");
            }

            int idEquipo = Integer.parseInt(txtIdEquipo.getText().trim());
            String respuesta = clienteHttp.generarInforme(
                    idEquipo, 
                    txtFecha.getText().trim(), 
                    txtDescripcion.getText().trim(),
                    txtRol.getText().trim()
            );
            areaResultado.setText(respuesta);

        } catch (SecurityException | IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void consultarInforme() {
        try {
            if (!ValidacionesInformes.tienePermiso(txtRol.getText())) {
                throw new SecurityException("Acceso denegado: Se requiere rol TECNICO o ADMIN.");
            }
            if (!ValidacionesInformes.validarId(txtIdInforme.getText())) {
                throw new IllegalArgumentException("El ID de informe debe ser un entero positivo.");
            }

            int idInforme = Integer.parseInt(txtIdInforme.getText().trim());
            String respuesta = clienteHttp.consultarInforme(idInforme, txtRol.getText().trim());
            areaResultado.setText(respuesta);

        } catch (SecurityException | IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    protected void mostrarError(String mensaje) {
        // Si es un error de seguridad, lo mostramos con un icono distinto
        if (mensaje.contains("Acceso denegado")) {
            JOptionPane.showMessageDialog(this, mensaje, "Seguridad", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InformeGUI().setVisible(true));
    }
}