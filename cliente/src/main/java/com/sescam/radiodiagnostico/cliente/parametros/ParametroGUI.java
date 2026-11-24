package com.sescam.radiodiagnostico.cliente.parametros;

import javax.swing.*;
import java.awt.*;

public class ParametroGUI extends JFrame {

    private JTextField txtClave;
    private JTextField txtValor;
    private JTextArea areaResultado;
    private ParametrosHttpClient clienteHttp;

    public ParametroGUI() {
        this(new ParametrosHttpClient());
    }

    public ParametroGUI(ParametrosHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Configuración de Parámetros (CDU5)");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtClave = new JTextField();
        txtValor = new JTextField();

        panel.add(new JLabel("Clave del parámetro:"));
        panel.add(txtClave);
        panel.add(new JLabel("Valor:"));
        panel.add(txtValor);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnGuardar = new JButton("Guardar");
        JButton btnConsultar = new JButton("Consultar");
        panelBotones.add(btnGuardar);
        panelBotones.add(btnConsultar);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnGuardar.addActionListener(e -> guardarParametro());
        btnConsultar.addActionListener(e -> consultarParametro());
    }

    private void guardarParametro() {
        try {
            if (!ValidacionesParametros.validarTexto(txtClave.getText())) {
                throw new IllegalArgumentException("La clave no puede estar vacía.");
            }
            String respuesta = clienteHttp.guardarParametro(txtClave.getText().trim(), txtValor.getText().trim());
            areaResultado.setText(respuesta);
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar: " + ex.getMessage());
        }
    }

    private void consultarParametro() {
        try {
            if (!ValidacionesParametros.validarTexto(txtClave.getText())) {
                throw new IllegalArgumentException("Introduce una clave para consultar.");
            }
            String respuesta = clienteHttp.consultarParametro(txtClave.getText().trim());
            areaResultado.setText(respuesta);
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar: " + ex.getMessage());
        }
    }

    protected void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParametroGUI().setVisible(true));
    }
}