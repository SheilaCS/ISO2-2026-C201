package com.sescam.radiodiagnostico.cliente.historial;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * VENTANA DEL HISTORIAL DE PRUEBAS.
 * Iteracion 12 - RF04.
 *
 * Pide usuario, contrasena e ID del paciente, consulta el servidor
 * y muestra las pruebas en una tabla. Los errores (datos incorrectos,
 * rechazos HTTP o fallo de conexion) se muestran en una ventana.
 */
public class GestionHistorialGUI extends JFrame {

    private static final String[] COLUMNAS =
            {"ID", "Fecha", "Tipo", "Estado", "Informe"};

    private JTextField txtUsuario;
    private JPasswordField txtClave;
    private JTextField txtIdPaciente;

    private DefaultTableModel modeloTabla;
    private JTable tablaPruebas;
    private JLabel lblEstado;

    private HistorialHttpClient clienteHttp;

    // Constructor normal
    public GestionHistorialGUI() {
        this(new HistorialHttpClient());
    }

    // Constructor para las pruebas
    public GestionHistorialGUI(HistorialHttpClient clienteHttp) {

        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Historial de pruebas");
        setSize(750, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel con los campos
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        txtIdPaciente = new JTextField();

        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);

        panel.add(new JLabel("Contrasena:"));
        panel.add(txtClave);

        panel.add(new JLabel("ID paciente:"));
        panel.add(txtIdPaciente);

        // Boton
        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnConsultar = new JButton("Consultar historial");
        panelBotones.add(btnConsultar);

        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.add(panel, BorderLayout.NORTH);
        panelNorte.add(panelBotones, BorderLayout.CENTER);

        // Tabla de resultados
        modeloTabla = new DefaultTableModel(COLUMNAS, 0);
        tablaPruebas = new JTable(modeloTabla);
        tablaPruebas.setDefaultEditor(Object.class, null);

        lblEstado = new JLabel(
                "Introduce los datos y pulsa Consultar historial");
        lblEstado.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        add(panelNorte, BorderLayout.NORTH);
        add(new JScrollPane(tablaPruebas), BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);

        btnConsultar.addActionListener(e -> consultarHistorial());
    }

    // Validar los datos, consultar y mostrar el resultado
    private void consultarHistorial() {

        char[] clave = txtClave.getPassword();

        try {
            String usuario = txtUsuario.getText().trim();

            if (!ValidacionesHistorial.validarUsuario(usuario)) {
                throw new IllegalArgumentException(
                        "El usuario es obligatorio");
            }

            if (!ValidacionesHistorial.validarClave(clave)) {
                throw new IllegalArgumentException(
                        "La contrasena es obligatoria");
            }

            String idPaciente = txtIdPaciente.getText().trim();

            if (!ValidacionesHistorial.validarIdPaciente(idPaciente)) {
                throw new IllegalArgumentException(
                        "El ID de paciente es obligatorio y solo admite letras,"
                        + " numeros, punto, guion y guion bajo (maximo "
                        + ValidacionesHistorial.LONGITUD_MAXIMA_ID
                        + " caracteres)");
            }

            RespuestaHistorial respuesta = clienteHttp.consultarHistorial(
                    usuario, clave, idPaciente);

            if (respuesta.correcta()) {
                mostrarPruebas(
                        HistorialHttpClient.parsearPruebas(respuesta.cuerpo()));
            } else {
                limpiarTabla("Consulta rechazada");
                mostrarError(HistorialHttpClient.mensajeError(respuesta));
            }

        } catch (IllegalArgumentException ex) {
            limpiarTabla("Consulta no realizada");
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            limpiarTabla("Consulta no realizada");
            mostrarError("Error al conectar con el servidor: "
                    + ex.getMessage());

        } finally {
            // No conservar la contrasena en memoria ni en pantalla
            Arrays.fill(clave, '\0');
            txtClave.setText("");
        }
    }

    // Rellenar la tabla con las pruebas recibidas
    private void mostrarPruebas(List<PruebaHistorial> pruebas) {

        modeloTabla.setRowCount(0);

        for (PruebaHistorial p : pruebas) {
            modeloTabla.addRow(new Object[] {
                    p.id(), p.fecha(), p.tipo(), p.estado(), p.informe()});
        }

        if (pruebas.isEmpty()) {
            lblEstado.setText("El paciente no tiene pruebas realizadas");
        } else if (pruebas.size() == 1) {
            lblEstado.setText("1 prueba realizada");
        } else {
            lblEstado.setText(pruebas.size() + " pruebas realizadas");
        }
    }

    private void limpiarTabla(String estado) {
        modeloTabla.setRowCount(0);
        lblEstado.setText(estado);
    }

    // Mostrar errores en una ventana
    // Protected permite comprobar los errores en las pruebas
    protected void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Ejecutar interfaz
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            GestionHistorialGUI ventana = new GestionHistorialGUI();

            ventana.setVisible(true);
        });
    }
}
