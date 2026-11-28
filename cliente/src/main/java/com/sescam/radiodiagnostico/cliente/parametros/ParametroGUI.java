package com.sescam.radiodiagnostico.cliente.parametros;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de configuración de parámetros del sistema (CDU5, RF17).
 */
public class ParametroGUI extends JFrame {

    private final JTextField txtUsuario;
    private final JPasswordField txtClave;
    private final JComboBox<String> cmbCategoria;
    private final JTextField txtClaveParametro;
    private final JTextField txtValor;
    private final JTextArea areaResultado;

    private final ParametrosHttpClient clienteHttp;

    public ParametroGUI() {
        this(new ParametrosHttpClient());
    }

    public ParametroGUI(ParametrosHttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Configuración de Parámetros (CDU5)");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        cmbCategoria = new JComboBox<>(new String[] {
            "TIPO_PRUEBA", "PROTOCOLO", "HORARIO", "POLITICA_CITACION", "PRIORIDAD"});
        txtClaveParametro = new JTextField();
        txtValor = new JTextField();

        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtClave);
        panel.add(new JLabel("Categoría:"));
        panel.add(cmbCategoria);
        panel.add(new JLabel("Clave del parámetro:"));
        panel.add(txtClaveParametro);
        panel.add(new JLabel("Valor:"));
        panel.add(txtValor);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnGuardar = new JButton("Guardar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnListar = new JButton("Listar categoría");
        panelBotones.add(btnGuardar);
        panelBotones.add(btnConsultar);
        panelBotones.add(btnListar);

        areaResultado = new JTextArea();
        areaResultado.setEditable(false);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panel, BorderLayout.NORTH);
        panelCentro.add(panelBotones, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.NORTH);
        add(new JScrollPane(areaResultado), BorderLayout.CENTER);

        btnGuardar.addActionListener(e -> guardar());
        btnConsultar.addActionListener(e -> consultar());
        btnListar.addActionListener(e -> listar());
    }

    private String prepararCredencialesYCategoria() {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());
        if (usuario.isEmpty() || clave.isEmpty()) {
            throw new IllegalArgumentException("Introduce el usuario y la contraseña.");
        }
        String categoria = (String) cmbCategoria.getSelectedItem();
        if (!ValidacionesParametros.validarCategoria(categoria)) {
            throw new IllegalArgumentException("Selecciona una categoría válida.");
        }
        clienteHttp.setCredenciales(usuario, clave);
        return categoria;
    }

    private void guardar() {
        try {
            String categoria = prepararCredencialesYCategoria();
            if (!ValidacionesParametros.validarTexto(txtClaveParametro.getText())) {
                throw new IllegalArgumentException("La clave del parámetro no puede estar vacía.");
            }
            if (!ValidacionesParametros.validarTexto(txtValor.getText())) {
                throw new IllegalArgumentException("El valor no puede estar vacío.");
            }
            mostrar(clienteHttp.guardarParametro(categoria,
                    txtClaveParametro.getText().trim(), txtValor.getText().trim()));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void consultar() {
        try {
            String categoria = prepararCredencialesYCategoria();
            if (!ValidacionesParametros.validarTexto(txtClaveParametro.getText())) {
                throw new IllegalArgumentException("La clave del parámetro no puede estar vacía.");
            }
            mostrar(clienteHttp.consultarParametro(categoria,
                    txtClaveParametro.getText().trim()));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void listar() {
        try {
            mostrar(clienteHttp.listarParametros(prepararCredencialesYCategoria()));
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error al conectar con el servidor: " + ex.getMessage());
        }
    }

    private void mostrar(ParametrosHttpClient.Respuesta respuesta) {
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
        SwingUtilities.invokeLater(() -> new ParametroGUI().setVisible(true));
    }
}
