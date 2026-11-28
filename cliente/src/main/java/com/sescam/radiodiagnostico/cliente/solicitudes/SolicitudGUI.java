package com.sescam.radiodiagnostico.cliente.solicitudes;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de solicitud de pruebas diagnósticas (CDU2, RF06).
 */
public class SolicitudGUI extends JFrame {

    private final JTextField txtUsuario;
    private final JPasswordField txtClave;
    private final JTextField txtIdPaciente;
    private final JTextField txtTipoPrueba;
    private final JTextField txtIndicacion;
    private final JTextField txtJustificacion;
    private final JComboBox<String> cmbPrioridad;
    private final JTextField txtIdSolicitud;
    private final JTextArea areaResultado;

    private final SolicitudHttpClient clienteHttp;

    public SolicitudGUI() {
        this(new SolicitudHttpClient());
    }

    public SolicitudGUI(SolicitudHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Solicitud de pruebas diagnósticas (CDU2)");
        setSize(700, 560);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        txtIdPaciente = new JTextField();
        txtTipoPrueba = new JTextField();
        txtIndicacion = new JTextField();
        txtJustificacion = new JTextField();
        cmbPrioridad = new JComboBox<>(new String[] {"ORDINARIO", "PREFERENTE", "URGENTE"});
        txtIdSolicitud = new JTextField();

        panel.add(new JLabel("Usuario (médico):"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtClave);
        panel.add(new JLabel("ID Paciente:"));
        panel.add(txtIdPaciente);
        panel.add(new JLabel("Tipo de prueba:"));
        panel.add(txtTipoPrueba);
        panel.add(new JLabel("Indicación clínica:"));
        panel.add(txtIndicacion);
        panel.add(new JLabel("Justificación:"));
        panel.add(txtJustificacion);
        panel.add(new JLabel("Prioridad:"));
        panel.add(cmbPrioridad);
        panel.add(new JLabel("ID Solicitud (para consultar):"));
        panel.add(txtIdSolicitud);

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

    private void prepararCredenciales() {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());
        if (usuario.isEmpty() || clave.isEmpty()) {
            throw new IllegalArgumentException("Introduce el usuario y la contraseña.");
        }
        clienteHttp.setCredenciales(usuario, clave);
    }

    private void crearSolicitud() {
        try {
            prepararCredenciales();
            if (!ValidacionesSolicitudes.validarId(txtIdPaciente.getText())) {
                throw new IllegalArgumentException("El ID de paciente debe ser un entero positivo.");
            }
            if (!ValidacionesSolicitudes.validarTexto(txtTipoPrueba.getText())) {
                throw new IllegalArgumentException("El tipo de prueba no puede estar vacío.");
            }
            if (!ValidacionesSolicitudes.validarTexto(txtIndicacion.getText())) {
                throw new IllegalArgumentException("La indicación clínica no puede estar vacía.");
            }
            if (!ValidacionesSolicitudes.validarTexto(txtJustificacion.getText())) {
                throw new IllegalArgumentException("La justificación no puede estar vacía.");
            }
            String prioridad = (String) cmbPrioridad.getSelectedItem();
            if (!ValidacionesSolicitudes.validarPrioridad(prioridad)) {
                throw new IllegalArgumentException("Prioridad inválida. Use URGENTE, PREFERENTE u ORDINARIO.");
            }

            mostrar(clienteHttp.crearSolicitud(
                    Integer.parseInt(txtIdPaciente.getText().trim()),
                    txtTipoPrueba.getText().trim(),
                    txtIndicacion.getText().trim(),
                    txtJustificacion.getText().trim(),
                    prioridad));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void consultarSolicitud() {
        try {
            prepararCredenciales();
            if (!ValidacionesSolicitudes.validarId(txtIdSolicitud.getText())) {
                throw new IllegalArgumentException("El ID de solicitud debe ser un entero positivo.");
            }
            mostrar(clienteHttp.consultarSolicitud(
                    Integer.parseInt(txtIdSolicitud.getText().trim())));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void mostrar(SolicitudHttpClient.Respuesta respuesta) {
        if (respuesta.correcta()) {
            areaResultado.setText(respuesta.texto());
        } else {
            mostrarError("Error " + respuesta.codigo() + ": " + respuesta.texto());
        }
    }

    protected void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SolicitudGUI().setVisible(true));
    }
}
