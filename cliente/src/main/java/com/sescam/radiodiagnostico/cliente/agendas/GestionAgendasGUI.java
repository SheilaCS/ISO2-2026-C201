
package com.sescam.radiodiagnostico.cliente.agendas;

import javax.swing.*;
import java.awt.*;

public class GestionAgendasGUI extends JFrame {

    private JTextField txtIdMedico;
    private JTextField txtIdHorario;
    private JTextField txtFecha;
    private JTextField txtInicio;
    private JTextField txtFin;

    private JTextArea areaResultado;

    private AgendasHttpClient clienteHttp = new AgendasHttpClient();

    public GestionAgendasGUI() {

        setTitle("SESCAM - Gestion de agendas");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel principal con los campos del formulario
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtIdMedico = new JTextField();
        txtIdHorario = new JTextField();
        txtFecha = new JTextField();
        txtInicio = new JTextField();
        txtFin = new JTextField();

        panel.add(new JLabel("ID medico:"));
        panel.add(txtIdMedico);

        panel.add(new JLabel("ID horario:"));
        panel.add(txtIdHorario);

        panel.add(new JLabel("Fecha (AAAA-MM-DD):"));
        panel.add(txtFecha);

        panel.add(new JLabel("Hora inicio (HH:MM):"));
        panel.add(txtInicio);

        panel.add(new JLabel("Hora fin (HH:MM):"));
        panel.add(txtFin);

        // Botones de las operaciones
        JPanel panelBotones = new JPanel(new FlowLayout());

        JButton btnCrear = new JButton("Crear horario");
        JButton btnConsultar = new JButton("Consultar agenda");
        JButton btnModificar = new JButton("Modificar horario");
        JButton btnEliminar = new JButton("Eliminar horario");

        panelBotones.add(btnCrear);
        panelBotones.add(btnConsultar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnEliminar);

        // Zona donde se muestran las respuestas del servidor
        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        // Acciones de los botones
        btnCrear.addActionListener(e -> crearHorario());
        btnConsultar.addActionListener(e -> consultarAgenda());
        btnModificar.addActionListener(e -> modificarHorario());
        btnEliminar.addActionListener(e -> eliminarHorario());
    }

    // Validar que el identificador sea un entero positivo
    private int leerId(String texto) {

        if (!ValidacionesAgendas.validarId(texto)) {
            throw new IllegalArgumentException(
                    "El ID debe ser un numero entero mayor que 0");
        }

        return Integer.parseInt(texto.trim());
    }

    // Validar la fecha y las horas antes de enviarlas al servidor
    private void validarDatosHorario() {

        // Comprobar que la fecha sea correcta
        if (!ValidacionesAgendas.validarFecha(txtFecha.getText().trim())) {
            throw new IllegalArgumentException(
                    "La fecha debe ser valida y no estar en el pasado (AAAA-MM-DD)");
        }

        // Comprobar las horas y que el inicio sea anterior al fin
        if (!ValidacionesAgendas.validarRangoHorario(
                txtInicio.getText().trim(), txtFin.getText().trim())) {
            throw new IllegalArgumentException(
                    "Las horas deben tener formato HH:MM y la hora inicial debe ser anterior a la final");
        }
    }

    // Crear un nuevo horario
    private void crearHorario() {
        try {
            int idMedico = leerId(txtIdMedico.getText());

            // Validar los datos antes de enviar la peticion HTTP
            validarDatosHorario();

            String respuesta = clienteHttp.crearHorario(
                    idMedico,
                    txtFecha.getText().trim(),
                    txtInicio.getText().trim(),
                    txtFin.getText().trim());

            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    // Consultar la agenda de un medico
    private void consultarAgenda() {
        try {
            int idMedico = leerId(txtIdMedico.getText());

            String respuesta = clienteHttp.consultarAgenda(idMedico);
            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    // Modificar un horario existente
    private void modificarHorario() {
        try {
            int idHorario = leerId(txtIdHorario.getText());

            // Validar los datos antes de enviar la peticion HTTP
            validarDatosHorario();

            String respuesta = clienteHttp.modificarHorario(
                    idHorario,
                    txtFecha.getText().trim(),
                    txtInicio.getText().trim(),
                    txtFin.getText().trim());

            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    // Eliminar un horario existente
    private void eliminarHorario() {
        try {
            int idHorario = leerId(txtIdHorario.getText());

            String respuesta = clienteHttp.eliminarHorario(idHorario);
            areaResultado.setText(respuesta);

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    // Mostrar un mensaje de error al usuario
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Ejecutar la ventana Swing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GestionAgendasGUI ventana = new GestionAgendasGUI();
            ventana.setVisible(true);
        });
    }
}
