package com.sescam.radiodiagnostico.cliente.informes;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de creación y consulta de informes e imágenes de pacientes (CDU8, RF09).
 */
public class InformeGUI extends JFrame {

    private final JTextField txtUsuario;
    private final JPasswordField txtClave;
    private final JTextField txtIdPaciente;
    private final JTextField txtIdPrueba;
    private final JTextField txtTipoPrueba;
    private final JTextField txtContenido;
    private final JTextField txtImagenes;
    private final JTextField txtIdInforme;
    private final JTextArea areaResultado;

    private final InformesHttpClient clienteHttp;

    public InformeGUI() {
        this(new InformesHttpClient());
    }

    public InformeGUI(InformesHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Informes e imágenes de pacientes (CDU8)");
        setSize(750, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        txtIdPaciente = new JTextField();
        txtIdPrueba = new JTextField();
        txtTipoPrueba = new JTextField();
        txtContenido = new JTextField();
        txtImagenes = new JTextField();
        txtIdInforme = new JTextField();

        panel.add(new JLabel("Usuario (médico):"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtClave);
        panel.add(new JLabel("ID Paciente:"));
        panel.add(txtIdPaciente);
        panel.add(new JLabel("ID Prueba realizada:"));
        panel.add(txtIdPrueba);
        panel.add(new JLabel("Tipo de prueba:"));
        panel.add(txtTipoPrueba);
        panel.add(new JLabel("Informe (hallazgos y conclusión):"));
        panel.add(txtContenido);
        panel.add(new JLabel("Imágenes (referencias separadas por comas):"));
        panel.add(txtImagenes);
        panel.add(new JLabel("ID Informe (para consultar):"));
        panel.add(txtIdInforme);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnCrear = new JButton("Crear Informe");
        JButton btnConsultar = new JButton("Consultar Informe");
        JButton btnPaciente = new JButton("Informes del Paciente");
        panelBotones.add(btnCrear);
        panelBotones.add(btnConsultar);
        panelBotones.add(btnPaciente);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnCrear.addActionListener(e -> crearInforme());
        btnConsultar.addActionListener(e -> consultarInforme());
        btnPaciente.addActionListener(e -> consultarInformesPaciente());
    }

    private void prepararCredenciales() {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());
        if (usuario.isEmpty() || clave.isEmpty()) {
            throw new IllegalArgumentException("Introduce el usuario y la contraseña.");
        }
        clienteHttp.setCredenciales(usuario, clave);
    }

    private void crearInforme() {
        try {
            prepararCredenciales();
            if (!ValidacionesInformes.validarId(txtIdPaciente.getText())) {
                throw new IllegalArgumentException("El ID de paciente debe ser un entero positivo.");
            }
            if (!ValidacionesInformes.validarId(txtIdPrueba.getText())) {
                throw new IllegalArgumentException("El ID de prueba debe ser un entero positivo.");
            }
            if (!ValidacionesInformes.validarTexto(txtTipoPrueba.getText())) {
                throw new IllegalArgumentException("El tipo de prueba no puede estar vacío.");
            }
            if (!ValidacionesInformes.validarTexto(txtContenido.getText())) {
                throw new IllegalArgumentException("El informe no puede estar vacío.");
            }
            if (!ValidacionesInformes.validarImagenes(txtImagenes.getText())) {
                throw new IllegalArgumentException("Hay una referencia de imagen vacía.");
            }
            mostrar(clienteHttp.crearInforme(
                    Integer.parseInt(txtIdPaciente.getText().trim()),
                    Integer.parseInt(txtIdPrueba.getText().trim()),
                    txtTipoPrueba.getText().trim(),
                    txtContenido.getText().trim(),
                    txtImagenes.getText().trim()));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void consultarInforme() {
        try {
            prepararCredenciales();
            if (!ValidacionesInformes.validarId(txtIdInforme.getText())) {
                throw new IllegalArgumentException("El ID de informe debe ser un entero positivo.");
            }
            mostrar(clienteHttp.consultarInforme(
                    Integer.parseInt(txtIdInforme.getText().trim())));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void consultarInformesPaciente() {
        try {
            prepararCredenciales();
            if (!ValidacionesInformes.validarId(txtIdPaciente.getText())) {
                throw new IllegalArgumentException("El ID de paciente debe ser un entero positivo.");
            }
            mostrar(clienteHttp.consultarInformesPaciente(
                    Integer.parseInt(txtIdPaciente.getText().trim())));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void mostrar(InformesHttpClient.Respuesta respuesta) {
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
        SwingUtilities.invokeLater(() -> new InformeGUI().setVisible(true));
    }
}
