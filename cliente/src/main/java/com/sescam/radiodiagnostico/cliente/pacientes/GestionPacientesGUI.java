package com.sescam.radiodiagnostico.cliente.pacientes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * CLIENTE - Iteracion 4 (RF12).
 * Interfaz de gestion de pacientes del SESCAM para rol ADMINISTRATIVO.
 */
public class GestionPacientesGUI extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JTextField txtId;
    private JTextField txtCip;
    private JTextField txtNombre;
    private JTextField txtApellidos;
    private JTextField txtFechaNacimiento;

    private DefaultTableModel modeloTabla;
    private JTable tablaPacientes;

    private final PacientesHttpClient clienteHttp;

    private final Color azul = new Color(20, 75, 115);
    private final Color fondo = new Color(245, 248, 252);

    public GestionPacientesGUI() {
        this(new PacientesHttpClient());
    }

    GestionPacientesGUI(PacientesHttpClient clienteHttp) {
        this.clienteHttp = java.util.Objects.requireNonNull(clienteHttp);

        setTitle("SESCAM - Gestion de Pacientes");
        setSize(900, 650);
        setMinimumSize(new Dimension(780, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel principal = new JPanel(new BorderLayout(15, 15));
        principal.setBackground(fondo);
        principal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // CABECERA
        JPanel cabecera = new JPanel(new GridLayout(2, 1, 0, 5));
        cabecera.setBackground(azul);
        cabecera.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titulo = new JLabel("SESCAM | GESTIÓN DE PACIENTES");
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel("Sistema Integral de Radiodiagnóstico e Imagen Médica (CDU4 / RF12)");
        subtitulo.setForeground(Color.WHITE);

        cabecera.add(titulo);
        cabecera.add(subtitulo);
        principal.add(cabecera, BorderLayout.NORTH);

        // AUTENTICACION (HTTP BASIC LOCAL)
        JPanel panelAuth = new JPanel(new GridLayout(1, 4, 10, 10));
        panelAuth.setBackground(Color.WHITE);
        panelAuth.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Credenciales (HTTP Basic - Rol ADMINISTRATIVO)"),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        txtUsuario = new JTextField();
        txtPassword = new JPasswordField();

        panelAuth.add(new JLabel("Usuario:"));
        panelAuth.add(txtUsuario);
        panelAuth.add(new JLabel("Contraseña:"));
        panelAuth.add(txtPassword);

        // FORMULARIO PACIENTE
        JPanel formulario = new JPanel(new GridLayout(5, 2, 10, 10));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Datos del paciente"),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        txtId = new JTextField();
        txtId.setEditable(false);
        txtId.setToolTipText("Generado por el servidor (no editable)");

        txtCip = new JTextField();
        txtNombre = new JTextField();
        txtApellidos = new JTextField();
        txtFechaNacimiento = new JTextField();

        formulario.add(new JLabel("ID interno (servidor):"));
        formulario.add(txtId);

        formulario.add(new JLabel("CIP (obligatorio, único e inmutable):"));
        formulario.add(txtCip);

        formulario.add(new JLabel("Nombre:"));
        formulario.add(txtNombre);

        formulario.add(new JLabel("Apellidos:"));
        formulario.add(txtApellidos);

        formulario.add(new JLabel("Fecha de nacimiento (AAAA-MM-DD):"));
        formulario.add(txtFechaNacimiento);

        // BOTONES
        JPanel botones = new JPanel(new GridLayout(1, 4, 10, 0));
        botones.setBackground(fondo);

        JButton btnAlta = new JButton("Alta paciente");
        JButton btnConsultar = new JButton("Consultar por CIP");
        JButton btnModificar = new JButton("Modificar");
        JButton btnBaja = new JButton("Dar de baja");

        JButton[] listaBotones = {btnAlta, btnConsultar, btnModificar, btnBaja};
        for (JButton boton : listaBotones) {
            boton.setFont(new Font("Arial", Font.BOLD, 13));
            boton.setFocusPainted(false);
            boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botones.add(boton);
        }

        btnAlta.setBackground(azul);
        btnAlta.setForeground(Color.WHITE);

        // TABLA
        String[] columnas = {"ID", "CIP", "Nombre", "Apellidos", "Fecha nacimiento", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tablaPacientes = new JTable(modeloTabla);
        tablaPacientes.setRowHeight(28);
        tablaPacientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tablaPacientes);
        scroll.setBorder(BorderFactory.createTitledBorder("Pacientes consultados"));

        tablaPacientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = tablaPacientes.getSelectedRow();
                if (fila >= 0) {
                    txtId.setText(valorCelda(fila, 0));
                    txtCip.setText(valorCelda(fila, 1));
                    txtCip.setEditable(false);
                    txtNombre.setText(valorCelda(fila, 2));
                    txtApellidos.setText(valorCelda(fila, 3));
                    txtFechaNacimiento.setText(valorCelda(fila, 4));
                }
            }
        });

        // ORGANIZACION CENTRAL
        JPanel zonaFormularios = new JPanel(new BorderLayout(10, 10));
        zonaFormularios.setBackground(fondo);
        zonaFormularios.add(panelAuth, BorderLayout.NORTH);
        zonaFormularios.add(formulario, BorderLayout.CENTER);

        JPanel zonaSuperior = new JPanel(new BorderLayout(10, 10));
        zonaSuperior.setBackground(fondo);
        zonaSuperior.add(zonaFormularios, BorderLayout.CENTER);
        zonaSuperior.add(botones, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout(15, 15));
        contenido.setBackground(fondo);
        contenido.add(zonaSuperior, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

        principal.add(contenido, BorderLayout.CENTER);
        add(principal);

        // ACCIONES
        btnAlta.addActionListener(e -> altaPaciente());
        btnConsultar.addActionListener(e -> consultarPorCip());
        btnModificar.addActionListener(e -> modificarPaciente());
        btnBaja.addActionListener(e -> darBajaPaciente());
    }

    private String valorCelda(int fila, int col) {
        Object val = modeloTabla.getValueAt(fila, col);
        return val != null ? val.toString() : "";
    }

    private void sincronizarCredenciales() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());
        clienteHttp.setCredenciales(user, pass);
    }

    private boolean validarDatosAltaOEdicion(boolean exigirCip) {
        if (exigirCip && !ValidacionesPacientes.validarCip(txtCip.getText())) {
            JOptionPane.showMessageDialog(this, "El CIP es obligatorio.");
            return false;
        }
        if (!ValidacionesPacientes.validarTextoObligatorio(txtNombre.getText())) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return false;
        }
        if (!ValidacionesPacientes.validarTextoObligatorio(txtApellidos.getText())) {
            JOptionPane.showMessageDialog(this, "Los apellidos son obligatorios.");
            return false;
        }
        if (!ValidacionesPacientes.validarFechaNacimiento(txtFechaNacimiento.getText())) {
            JOptionPane.showMessageDialog(this,
                    "Introduce una fecha de nacimiento válida (AAAA-MM-DD) que no sea futura.");
            return false;
        }
        return true;
    }

    private void ejecutarOperacion(String titulo, OperacionHttp operacion, Runnable alTerminar) {
        sincronizarCredenciales();

        SwingWorker<String, Void> trabajador = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return operacion.ejecutar();
            }

            @Override
            protected void done() {
                try {
                    String respuesta = get();
                    JOptionPane.showMessageDialog(
                            GestionPacientesGUI.this,
                            respuesta,
                            titulo,
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    if (alTerminar != null) {
                        alTerminar.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(
                            GestionPacientesGUI.this,
                            "La operación se ha interrumpido.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(
                            GestionPacientesGUI.this,
                            "Error al comunicar con el servidor.\nComprueba que el servidor esté iniciado en el puerto 8080.",
                            "Error de conexión",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        trabajador.execute();
    }

    @FunctionalInterface
    private interface OperacionHttp {
        String ejecutar() throws Exception;
    }

    private void altaPaciente() {
        if (!validarDatosAltaOEdicion(true)) {
            return;
        }
        String cip = ValidacionesPacientes.normalizarCip(txtCip.getText());
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String fecha = txtFechaNacimiento.getText().trim();

        ejecutarOperacion(
                "Alta de paciente",
                () -> clienteHttp.altaPaciente(cip, nombre, apellidos, fecha),
                null
        );
    }

    private void consultarPorCip() {
        if (!ValidacionesPacientes.validarCip(txtCip.getText())) {
            JOptionPane.showMessageDialog(this, "Introduce un CIP válido para consultar.");
            return;
        }
        String cip = ValidacionesPacientes.normalizarCip(txtCip.getText());
        txtCip.setText(cip);

        ejecutarOperacion(
                "Consulta por CIP",
                () -> clienteHttp.consultarPorCip(cip),
                null
        );
    }

    private void modificarPaciente() {
        if (!ValidacionesPacientes.validarId(txtId.getText())) {
            JOptionPane.showMessageDialog(this, "Selecciona o indica un ID de paciente válido para modificar.");
            return;
        }
        if (!validarDatosAltaOEdicion(false)) {
            return;
        }
        int id = Integer.parseInt(txtId.getText().trim());
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String fecha = txtFechaNacimiento.getText().trim();

        ejecutarOperacion(
                "Modificar paciente",
                () -> clienteHttp.modificarPaciente(id, nombre, apellidos, fecha),
                null
        );
    }

    private void darBajaPaciente() {
        if (!ValidacionesPacientes.validarId(txtId.getText())) {
            JOptionPane.showMessageDialog(this, "Selecciona o indica un ID de paciente válido para dar de baja.");
            return;
        }
        int id = Integer.parseInt(txtId.getText().trim());
        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Quieres dar de baja lógica al paciente con ID " + id + "?",
                "Confirmar baja lógica",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        ejecutarOperacion(
                "Baja lógica de paciente",
                () -> clienteHttp.darBajaPaciente(id),
                null
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GestionPacientesGUI ventana = new GestionPacientesGUI();
            ventana.setVisible(true);
        });
    }
}