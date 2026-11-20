
package com.sescam.radiodiagnostico.cliente.miscitas;

import javax.swing.*;
import java.awt.*;

public class GestionMisCitasGUI extends JFrame {

    private JTextField txtIdPaciente;
    private JTextField txtIdCita;
    private JTextField txtMotivo;

    private JTextArea areaResultado;

    private MisCitasHttpClient clienteHttp;

    // Constructor normal
    public GestionMisCitasGUI() {
        this(new MisCitasHttpClient());
    }

    // Constructor para las pruebas
    public GestionMisCitasGUI(MisCitasHttpClient clienteHttp) {

        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Gestion de mis citas");
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel principal con los campos
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtIdPaciente = new JTextField();
        txtIdCita = new JTextField();
        txtMotivo = new JTextField();

        panel.add(new JLabel("ID paciente:"));
        panel.add(txtIdPaciente);

        panel.add(new JLabel("ID cita:"));
        panel.add(txtIdCita);

        panel.add(new JLabel("Motivo del cambio:"));
        panel.add(txtMotivo);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout());

        JButton btnConsultar = new JButton("Consultar mis citas");
        JButton btnConfirmar = new JButton("Confirmar cita");
        JButton btnCambio = new JButton("Solicitar cambio");
        JButton btnCancelar = new JButton("Cancelar cita");

        panelBotones.add(btnConsultar);
        panelBotones.add(btnConfirmar);
        panelBotones.add(btnCambio);
        panelBotones.add(btnCancelar);

        // Zona de resultados
        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        // Acciones
        btnConsultar.addActionListener(e -> consultarMisCitas());
        btnConfirmar.addActionListener(e -> confirmarCita());
        btnCambio.addActionListener(e -> solicitarCambioCita());
        btnCancelar.addActionListener(e -> cancelarCita());
    }

    // Validar ID
    private int leerId(String texto) {

        if (!ValidacionesMisCitas.validarId(texto)) {
            throw new IllegalArgumentException(
                    "El ID debe ser un numero entero mayor que 0");
        }

        return Integer.parseInt(texto.trim());
    }

    // Validar el motivo del cambio
    private void validarMotivo() {

        if (!ValidacionesMisCitas.validarMotivo(
                txtMotivo.getText())) {

            throw new IllegalArgumentException(
                    "El motivo es obligatorio y no puede superar "
                    + ValidacionesMisCitas.LONGITUD_MAXIMA_MOTIVO
                    + " caracteres");
        }
    }

    // Consultar mis citas
    private void consultarMisCitas() {
        try {
            int idPaciente = leerId(txtIdPaciente.getText());

            String respuesta = clienteHttp.consultarMisCitas(idPaciente);
            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: "
                    + ex.getMessage());
        }
    }

    // Confirmar cita
    private void confirmarCita() {
        try {
            int idPaciente = leerId(txtIdPaciente.getText());
            int idCita = leerId(txtIdCita.getText());

            String respuesta = clienteHttp.confirmarCita(
                    idCita, idPaciente);

            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: "
                    + ex.getMessage());
        }
    }

    // Solicitar cambio de cita
    private void solicitarCambioCita() {
        try {
            int idPaciente = leerId(txtIdPaciente.getText());
            int idCita = leerId(txtIdCita.getText());

            validarMotivo();

            String respuesta = clienteHttp.solicitarCambioCita(
                    idCita, idPaciente, txtMotivo.getText().trim());

            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: "
                    + ex.getMessage());
        }
    }

    // Cancelar cita
    private void cancelarCita() {
        try {
            int idPaciente = leerId(txtIdPaciente.getText());
            int idCita = leerId(txtIdCita.getText());

            String respuesta = clienteHttp.cancelarCita(
                    idCita, idPaciente);

            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: "
                    + ex.getMessage());
        }
    }

    // Mostrar errores
    // Protected permite comprobar los errores en las pruebas
    protected void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Ejecutar interfaz
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            GestionMisCitasGUI ventana =
                    new GestionMisCitasGUI();

            ventana.setVisible(true);
        });
    }
}
