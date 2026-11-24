package com.sescam.radiodiagnostico.cliente.realizacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RealizacionGUI extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JTextField txtIdCita;
    private JTextField txtIdSolicitud;
    private JTextField txtFechaRealizacion;
    private JTextField txtObservaciones;
    private JTextField txtIncidencia;

    private DefaultTableModel modeloTabla;
    private JTable tablaRealizaciones;

    private final RealizacionHttpClient clienteHttp;

    public RealizacionGUI() {
        this(new RealizacionHttpClient());
    }

    public RealizacionGUI(RealizacionHttpClient clienteHttp) {
        this.clienteHttp = java.util.Objects.requireNonNull(clienteHttp);

        setTitle("SESCAM - Realizacion de Pruebas e Incidencias");
        setSize(900, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel principal = new JPanel(new BorderLayout(12, 12));
        principal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel panelAuth = new JPanel(new GridLayout(1, 4, 8, 8));
        panelAuth.setBorder(BorderFactory.createTitledBorder("Credenciales (HTTP Basic - Rol MEDICO)"));
        txtUsuario = new JTextField();
        txtPassword = new JPasswordField();
        panelAuth.add(new JLabel("Usuario:"));
        panelAuth.add(txtUsuario);
        panelAuth.add(new JLabel("Contraseña:"));
        panelAuth.add(txtPassword);

        JPanel formulario = new JPanel(new GridLayout(5, 2, 8, 8));
        formulario.setBorder(BorderFactory.createTitledBorder("Datos de realizacion"));
        txtIdCita = new JTextField();
        txtIdSolicitud = new JTextField();
        txtFechaRealizacion = new JTextField();
        txtObservaciones = new JTextField();
        txtIncidencia = new JTextField();

        formulario.add(new JLabel("ID Cita:"));
        formulario.add(txtIdCita);
        formulario.add(new JLabel("ID Solicitud:"));
        formulario.add(txtIdSolicitud);
        formulario.add(new JLabel("Fecha realizacion (AAAA-MM-DD):"));
        formulario.add(txtFechaRealizacion);
        formulario.add(new JLabel("Observaciones:"));
        formulario.add(txtObservaciones);
        formulario.add(new JLabel("Incidencia (opcional):"));
        formulario.add(txtIncidencia);

        JPanel botones = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton btnRegistrar = new JButton("Registrar realizacion");
        JButton btnConsultar = new JButton("Consultar por cita");
        JButton btnListar = new JButton("Listar todas");
        botones.add(btnRegistrar);
        botones.add(btnConsultar);
        botones.add(btnListar);

        String[] columnas = {"ID", "ID Cita", "ID Solicitud", "Fecha", "Observaciones", "Incidencia", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaRealizaciones = new JTable(modeloTabla);

        JPanel superior = new JPanel(new BorderLayout(10, 10));
        superior.add(panelAuth, BorderLayout.NORTH);
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        principal.add(superior, BorderLayout.NORTH);
        principal.add(new JScrollPane(tablaRealizaciones), BorderLayout.CENTER);
        add(principal);

        btnRegistrar.addActionListener(e -> registrarRealizacion());
        btnConsultar.addActionListener(e -> consultarPorCita());
        btnListar.addActionListener(e -> listarRealizaciones());
    }

    protected void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void mostrarInfo(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    private void sincronizarCredenciales() {
        clienteHttp.setCredenciales(txtUsuario.getText().trim(), new String(txtPassword.getPassword()));
    }

    private void registrarRealizacion() {
        if (!ValidacionesRealizacion.validarIdPositivo(txtIdCita.getText())
                || !ValidacionesRealizacion.validarIdPositivo(txtIdSolicitud.getText())) {
            mostrarError("Los IDs de cita y solicitud deben ser enteros positivos.");
            return;
        }
        if (!ValidacionesRealizacion.validarFechaRealizacion(txtFechaRealizacion.getText())) {
            mostrarError("Introduce una fecha valida (AAAA-MM-DD) que no sea futura.");
            return;
        }
        if (!ValidacionesRealizacion.validarObservacionesOIncidencia(
                txtObservaciones.getText(), txtIncidencia.getText())) {
            mostrarError("Debes indicar observaciones o una incidencia.");
            return;
        }

        int idCita = Integer.parseInt(txtIdCita.getText().trim());
        int idSolicitud = Integer.parseInt(txtIdSolicitud.getText().trim());
        String fecha = txtFechaRealizacion.getText().trim();
        String obs = txtObservaciones.getText().trim();
        String inc = txtIncidencia.getText().trim();

        ejecutar("Registro de realizacion",
                () -> clienteHttp.registrarRealizacion(idCita, idSolicitud, fecha, obs, inc),
                false);
    }

    private void consultarPorCita() {
        if (!ValidacionesRealizacion.validarIdPositivo(txtIdCita.getText())) {
            mostrarError("Introduce un ID de cita valido.");
            return;
        }
        int idCita = Integer.parseInt(txtIdCita.getText().trim());
        ejecutar("Consulta por cita", () -> clienteHttp.consultarPorCita(idCita), false);
    }

    private void listarRealizaciones() {
        ejecutar("Listado de realizaciones", clienteHttp::listarRealizaciones, true);
    }

    private void ejecutar(String titulo, OperacionHttp op, boolean limpiarTabla) {
        sincronizarCredenciales();
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return op.ejecutar();
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp.startsWith("Error ")) {
                        mostrarError(decodificar(resp));
                        return;
                    }
                    cargarTabla(resp, limpiarTabla);
                    mostrarInfo(titulo, decodificar(resp));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("Operacion interrumpida.");
                } catch (ExecutionException e) {
                    mostrarError("Error de comunicacion con el servidor en el puerto 8087.");
                }
            }
        }.execute();
    }

    private void cargarTabla(String respuesta, boolean limpiar) {
        if (limpiar) {
            modeloTabla.setRowCount(0);
        }
        if (respuesta == null || respuesta.isBlank()) {
            return;
        }
        for (String linea : respuesta.split("\\r?\\n")) {
            Map<String, String> campos = parsearLinea(linea);
            if (!campos.isEmpty() && campos.containsKey("id")) {
                modeloTabla.addRow(new Object[]{
                        campos.getOrDefault("id", ""),
                        campos.getOrDefault("idCita", ""),
                        campos.getOrDefault("idSolicitud", ""),
                        campos.getOrDefault("fechaRealizacion", ""),
                        campos.getOrDefault("observaciones", ""),
                        campos.getOrDefault("incidencia", ""),
                        campos.getOrDefault("estado", "")
                });
            }
        }
    }

    private Map<String, String> parsearLinea(String linea) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (String parte : linea.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length == 2) {
                mapa.put(decodificar(par[0]), decodificar(par[1]));
            }
        }
        return mapa;
    }

    private String decodificar(String texto) {
        try {
            return URLDecoder.decode(texto, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return texto;
        }
    }

    @FunctionalInterface
    private interface OperacionHttp {
        String ejecutar() throws Exception;
    }
}