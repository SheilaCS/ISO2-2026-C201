package com.sescam.radiodiagnostico.cliente.pacientes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

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

    private String ultimaRespuesta = "";

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
        JPanel botones = new JPanel(new GridLayout(1, 5, 10, 0));
        botones.setBackground(fondo);

        JButton btnAlta = new JButton("Alta paciente");
        JButton btnConsultar = new JButton("Consultar por CIP");
        JButton btnModificar = new JButton("Modificar");
        JButton btnBaja = new JButton("Dar de baja");
        JButton btnLimpiar = new JButton("Nuevo/Limpiar");

        JButton[] listaBotones = {btnAlta, btnConsultar, btnModificar, btnBaja, btnLimpiar};
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
        btnLimpiar.addActionListener(e -> limpiarFormulario());
    }

    private void limpiarFormulario() {
        tablaPacientes.clearSelection();
        txtId.setText("");
        txtCip.setText("");
        txtNombre.setText("");
        txtApellidos.setText("");
        txtFechaNacimiento.setText("");
        txtCip.setEditable(true);
        guardarUltimaRespuesta("");
        txtCip.requestFocusInWindow();
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
                    String respuestaLegible = decodificarTexto(respuesta);
                    if (alTerminar != null) {
                        alTerminar.run();
                    }
                    JOptionPane.showMessageDialog(
                            GestionPacientesGUI.this,
                            respuestaLegible,
                            titulo,
                            JOptionPane.INFORMATION_MESSAGE
                    );
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
                            "Error al comunicar con el servidor.\nComprueba que el servidor esté iniciado en el puerto 8087.",
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
                () -> {
                    String resp = clienteHttp.altaPaciente(cip, nombre, apellidos, fecha);
                    guardarUltimaRespuesta(resp);
                    return resp;
                },
                () -> cargarPacienteDesdeRespuesta(
                        obtenerUltimaRespuesta(),
                        cip,
                        nombre,
                        apellidos,
                        fecha,
                        "ACTIVO"
                )
        );
    }

    private synchronized void guardarUltimaRespuesta(String resp) {
        this.ultimaRespuesta = resp != null ? resp : "";
    }

    private synchronized String obtenerUltimaRespuesta() {
        return this.ultimaRespuesta;
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
                () -> {
                    String resp = clienteHttp.consultarPorCip(cip);
                    guardarUltimaRespuesta(resp);
                    return resp;
                },
                () -> cargarPacienteDesdeRespuesta(
                        obtenerUltimaRespuesta(),
                        cip,
                        txtNombre.getText().trim(),
                        txtApellidos.getText().trim(),
                        txtFechaNacimiento.getText().trim(),
                        "ACTIVO"
                )
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
        String id = txtId.getText().trim();
        String cip = ValidacionesPacientes.normalizarCip(txtCip.getText());
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String fecha = txtFechaNacimiento.getText().trim();

        ejecutarOperacion(
                "Modificar paciente",
                () -> {
                    String resp = clienteHttp.modificarPaciente(id, nombre, apellidos, fecha);
                    guardarUltimaRespuesta(resp);
                    return resp;
                },
                () -> cargarPacienteDesdeRespuesta(
                        obtenerUltimaRespuesta(),
                        cip,
                        nombre,
                        apellidos,
                        fecha,
                        "ACTIVO"
                )
        );
    }

    private void darBajaPaciente() {
        if (!ValidacionesPacientes.validarId(txtId.getText())) {
            JOptionPane.showMessageDialog(this, "Selecciona o indica un ID de paciente válido para dar de baja.");
            return;
        }
        String id = txtId.getText().trim();
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
                () -> {
                    String resp = clienteHttp.darBajaPaciente(id);
                    guardarUltimaRespuesta(resp);
                    return resp;
                },
                                () -> {
                    if (!obtenerUltimaRespuesta().startsWith("Error ")) {
                        marcarBajaEnTabla(id);
                    }
                }
        );
    }

    private String decodificarTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        try {
            return URLDecoder.decode(texto, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return texto;
        }
    }

    private void cargarPacienteDesdeRespuesta(
            String respuesta,
            String cipFallback,
            String nombreFallback,
            String apellidosFallback,
            String fechaFallback,
            String estadoFallback
    ) {
        if (respuesta == null || respuesta.startsWith("Error ")) {
            return;
        }

        Map<String, String> campos = extraerCampos(respuesta);

        String id = primerNoVacio(
                campos.get("id"),
                campos.get("uuid"),
                extraerUuid(respuesta),
                txtId.getText().trim()
        );
        String cip = primerNoVacio(
                campos.get("cip"),
                extraerValorEtiqueta(respuesta, "CIP"),
                cipFallback
        ).toUpperCase();
        String nombre = primerNoVacio(
                campos.get("nombre"),
                extraerValorEtiqueta(respuesta, "Nombre"),
                nombreFallback
        );
        String apellidos = primerNoVacio(
                campos.get("apellidos"),
                extraerValorEtiqueta(respuesta, "Apellidos"),
                apellidosFallback
        );
        String fecha = primerNoVacio(
                campos.get("fechanacimiento"),
                campos.get("fecha_nacimiento"),
                campos.get("fecha"),
                extraerValorEtiqueta(respuesta, "Fecha"),
                fechaFallback
        );
        String activoRaw = campos.get("activo");
        String estadoCalculado = "false".equalsIgnoreCase(activoRaw)
                ? "BAJA"
                : ("true".equalsIgnoreCase(activoRaw) ? "ACTIVO" : estadoFallback);
        String estado = primerNoVacio(
                campos.get("estado"),
                extraerValorEtiqueta(respuesta, "Estado"),
                estadoCalculado
        );

        if (id.isEmpty() && cip.isEmpty()) {
            return;
        }

        int filaExistente = buscarFilaPorIdOCip(id, cip);
        Object[] filaDatos = new Object[]{id, cip, nombre, apellidos, fecha, estado};
        int filaObjetivo;
        if (filaExistente >= 0) {
            for (int col = 0; col < filaDatos.length; col++) {
                modeloTabla.setValueAt(filaDatos[col], filaExistente, col);
            }
            filaObjetivo = filaExistente;
        } else {
            modeloTabla.addRow(filaDatos);
            filaObjetivo = modeloTabla.getRowCount() - 1;
        }

        if (!id.isEmpty()) {
            txtId.setText(id);
        }
        if (!cip.isEmpty()) {
            txtCip.setText(cip);
        }
        if (!nombre.isEmpty()) {
            txtNombre.setText(nombre);
        }
        if (!apellidos.isEmpty()) {
            txtApellidos.setText(apellidos);
        }
        if (!fecha.isEmpty()) {
            txtFechaNacimiento.setText(fecha);
        }
        if (filaObjetivo >= 0 && filaObjetivo < tablaPacientes.getRowCount()) {
            tablaPacientes.setRowSelectionInterval(filaObjetivo, filaObjetivo);
        }
    }

    private void marcarBajaEnTabla(String id) {
        for (int fila = 0; fila < modeloTabla.getRowCount(); fila++) {
            if (id.equals(valorCelda(fila, 0))) {
                modeloTabla.setValueAt("BAJA", fila, 5);
            }
        }
    }

    private int buscarFilaPorIdOCip(String id, String cip) {
        for (int fila = 0; fila < modeloTabla.getRowCount(); fila++) {
            String idFila = valorCelda(fila, 0);
            String cipFila = valorCelda(fila, 1);
            if (!id.isEmpty() && id.equals(idFila)) {
                return fila;
            }
            if (!cip.isEmpty() && cip.equalsIgnoreCase(cipFila)) {
                return fila;
            }
        }
        return -1;
    }

    private Map<String, String> extraerCampos(String texto) {
        Map<String, String> mapa = new LinkedHashMap<>();

        String[] partes = texto.split("[&|\\n\\r;]+");
        for (String parte : partes) {
            int eq = parte.indexOf('=');
            if (eq > 0) {
                String clave = decodificarTexto(parte.substring(0, eq)).trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
                String valor = decodificarTexto(parte.substring(eq + 1)).trim();
                mapa.putIfAbsent(clave, valor);
            }
        }

        Matcher jsonMatcher = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"").matcher(texto);
        while (jsonMatcher.find()) {
            mapa.putIfAbsent(jsonMatcher.group(1).toLowerCase(), jsonMatcher.group(2).trim());
        }

        return mapa;
    }

    private String extraerValorEtiqueta(String texto, String etiqueta) {
        Matcher m = Pattern.compile("(?i)\\b" + Pattern.quote(etiqueta) + "\\s*[:=]\\s*([^|\\n\\r,;]+)").matcher(texto);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private String extraerUuid(String texto) {
        Matcher m = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").matcher(texto);
        if (m.find()) {
            return m.group();
        }
        return "";
    }

    private String primerNoVacio(String... candidatos) {
        for (String c : candidatos) {
            if (c != null && !c.trim().isEmpty()) {
                return c.trim();
            }
        }
        return "";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GestionPacientesGUI ventana = new GestionPacientesGUI();
            ventana.setVisible(true);
        });
    }
}