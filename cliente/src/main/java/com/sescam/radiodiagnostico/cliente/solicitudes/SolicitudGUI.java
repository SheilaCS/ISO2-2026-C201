package com.sescam.radiodiagnostico.cliente.solicitudes;

import javax.swing.*;
import java.awt.*;

public class SolicitudGUI extends JFrame {

    private JTextField txtIdPaciente;
    private JTextField txtIdSolicitud;
    private JTextField txtTipoPrueba;
    private JTextField txtPrioridad; 
    private JTextArea areaResultado;

    private SolicitudHttpClient clienteHttp;

    public SolicitudGUI() {
        this(new SolicitudHttpClient());
    }

    public SolicitudGUI(SolicitudHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Módulo de Solicitudes (CDU2)");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtIdPaciente = new JTextField();
        txtIdSolicitud = new JTextField();
        txtTipoPrueba = new JTextField();
        txtPrioridad = new JTextField(); 

        panel.add(new JLabel("ID Paciente:"));
        panel.add(txtIdPaciente);
        panel.add(new JLabel("ID Solicitud (para consultar):"));
        panel.add(txtIdSolicitud);
        panel.add(new JLabel("Tipo de Prueba:"));
        panel.add(txtTipoPrueba);
        panel.add(new JLabel("Prioridad (URGENTE/PREFERENTE/ORDINARIO):")); 
        panel.add(txtPrioridad);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnCrear = new JButton("Crear Solicitud");
        JButton btnConsultar = new JButton("Consultar Solicitud");
        panelBotones.add(btnCrear);
        panelBotones.add(btnConsultar);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnCrear.addActionListener(e -> crearSolicitud());
        btnConsultar.addActionListener(e -> consultarSolicitud());
    }

    private void crearSolicitud() {
        try {
            if (!ValidacionesSolicitudes.validarId(txtIdPaciente.getText())) {
                throw new IllegalArgumentException("El ID de paciente debe ser un entero positivo.");
            }
            if (!ValidacionesSolicitudes.validarTexto(txtTipoPrueba.getText())) {
                throw new IllegalArgumentException("El tipo de prueba no puede estar vacío.");
            }
            if (!ValidacionesSolicitudes.validarPrioridad(txtPrioridad.getText())) { 
                throw new IllegalArgumentException("Prioridad inválida. Use URGENTE, PREFERENTE u ORDINARIO.");
            }

            int idPaciente = Integer.parseInt(txtIdPaciente.getText().trim());
            String respuesta = clienteHttp.crearSolicitud(
                    idPaciente, 
                    txtTipoPrueba.getText().trim(), 
                    txtPrioridad.getText().trim() 
            );
            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void consultarSolicitud() {
        try {
            if (!ValidacionesSolicitudes.validarId(txtIdSolicitud.getText())) {
                throw new IllegalArgumentException("El ID de solicitud debe ser un entero positivo.");
            }

            int idSolicitud = Integer.parseInt(txtIdSolicitud.getText().trim());
            String respuesta = clienteHttp.consultarSolicitud(idSolicitud);
            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    protected void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SolicitudGUI().setVisible(true));
    }
}