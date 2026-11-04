
package com.sescam.radiodiagnostico.cliente.citas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;

/**
 * CLIENTE - Iteracion 3 (RF13)
 * Interfaz de gestion de citas del SESCAM.
 *
 * Las operaciones se realizan mediante HTTP.
 */
public class GestionCitasGUI extends JFrame {

    // Campos del formulario
    private JTextField txtIdCita;
    private JTextField txtIdSolicitud;
    private JTextField txtIdPaciente;
    private JTextField txtFecha;
    private JTextField txtHora;

    // Tabla de resultados consultados al servidor
    private DefaultTableModel modeloTabla;
    private JTable tablaCitas;

    // Cliente que envia peticiones al servidor
    private CitasHttpClient clienteHttp = new CitasHttpClient();

    // Colores
    private Color azul = new Color(20, 75, 115);
    private Color fondo = new Color(245, 248, 252);

    public GestionCitasGUI() {

        // Configuracion de la ventana
        setTitle("SESCAM - Gestion de Citas");
        setSize(850, 600);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel principal = new JPanel(new BorderLayout(15, 15));
        principal.setBackground(fondo);
        principal.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        // CABECERA
        JPanel cabecera = new JPanel(new GridLayout(2, 1, 0, 5));
        cabecera.setBackground(azul);
        cabecera.setBorder(
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        );

        JLabel titulo = new JLabel("SESCAM | GESTIÓN DE CITAS");
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel(
            "Sistema Integral de Radiodiagnóstico e Imagen Médica"
        );
        subtitulo.setForeground(Color.WHITE);

        cabecera.add(titulo);
        cabecera.add(subtitulo);
        principal.add(cabecera, BorderLayout.NORTH);

        // FORMULARIO
        JPanel formulario = new JPanel(new GridLayout(5, 2, 10, 10));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Datos de la cita"),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            )
        );

        txtIdCita = new JTextField();
        txtIdSolicitud = new JTextField();
        txtIdPaciente = new JTextField();
        txtFecha = new JTextField();
        txtHora = new JTextField();

        formulario.add(new JLabel("ID de cita:"));
        formulario.add(txtIdCita);

        formulario.add(new JLabel("ID de solicitud:"));
        formulario.add(txtIdSolicitud);

        formulario.add(new JLabel("ID de paciente:"));
        formulario.add(txtIdPaciente);

        formulario.add(new JLabel("Fecha (AAAA-MM-DD):"));
        formulario.add(txtFecha);

        formulario.add(new JLabel("Hora (HH:MM):"));
        formulario.add(txtHora);

        // BOTONES
        JPanel botones = new JPanel(new GridLayout(1, 4, 10, 0));
        botones.setBackground(fondo);

        JButton btnAsignar = new JButton("Asignar cita");
        JButton btnReprogramar = new JButton("Reprogramar");
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnConsultar = new JButton("Consultar");

        JButton[] listaBotones = {
            btnAsignar, btnReprogramar,
            btnCancelar, btnConsultar
        };

        for (JButton boton : listaBotones) {
            boton.setFont(new Font("Arial", Font.BOLD, 13));
            boton.setFocusPainted(false);
            boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botones.add(boton);
        }

        btnAsignar.setBackground(azul);
        btnAsignar.setForeground(Color.WHITE);

        // TABLA DE CITAS
        String[] columnas = {
            "ID cita", "Solicitud", "Paciente",
            "Fecha", "Hora", "Estado"
        };

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tablaCitas = new JTable(modeloTabla);
        tablaCitas.setRowHeight(28);
        tablaCitas.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        JScrollPane scroll = new JScrollPane(tablaCitas);
        scroll.setBorder(
            BorderFactory.createTitledBorder("Citas consultadas")
        );

        // Seleccionar una cita de la tabla
        tablaCitas.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {
                int fila = tablaCitas.getSelectedRow();

                if (fila >= 0) {
                    txtIdCita.setText(
                        modeloTabla.getValueAt(fila, 0).toString()
                    );
                }
            }
        });

        // ORGANIZACION DEL CONTENIDO
        JPanel contenido = new JPanel(new BorderLayout(15, 15));
        contenido.setBackground(fondo);

        JPanel zonaSuperior = new JPanel(new BorderLayout(10, 10));
        zonaSuperior.setBackground(fondo);
        zonaSuperior.add(formulario, BorderLayout.CENTER);
        zonaSuperior.add(botones, BorderLayout.SOUTH);

        contenido.add(zonaSuperior, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

        principal.add(contenido, BorderLayout.CENTER);
        add(principal);

        // ACCIONES DE LOS BOTONES
        btnAsignar.addActionListener(e -> asignarCita());
        btnReprogramar.addActionListener(e -> reprogramarCita());
        btnCancelar.addActionListener(e -> cancelarCita());
        btnConsultar.addActionListener(e -> consultarCita());
    }

    /**
     * Comprobar que un ID sea un entero positivo.
     */
    private boolean validarId(String texto, String nombre) {

        try {
            int id = Integer.parseInt(texto.trim());

            if (id <= 0) {
                JOptionPane.showMessageDialog(this,
                    nombre + " debe ser mayor que cero.");
                return false;
            }

            return true;

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                nombre + " debe ser un numero entero valido.");
            return false;
        }
    }

    /**
     * Comprobar que la fecha y hora sean validas y futuras.
     */
    private boolean validarFechaHora(String fecha, String hora) {

        try {
            LocalDate fechaCita = LocalDate.parse(fecha.trim());
            LocalTime horaCita = LocalTime.parse(hora.trim());

            LocalDateTime fechaHoraCita =
                LocalDateTime.of(fechaCita, horaCita);

            if (!fechaHoraCita.isAfter(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this,
                    "La fecha y hora deben ser futuras.");
                return false;
            }

            return true;

        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                "Introduce fecha AAAA-MM-DD y hora HH:MM validas.");
            return false;
        }
    }

    /**
     * Ejecutar una peticion HTTP sin bloquear Swing.
     */
    private void ejecutarOperacion(
            String titulo, OperacionHttp operacion,
            Runnable despuesDeExito) {

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
                        GestionCitasGUI.this,
                        respuesta,
                        titulo,
                        JOptionPane.INFORMATION_MESSAGE
                    );

                    if (despuesDeExito != null
                            && respuesta.toLowerCase()
                                .contains("correctamente")) {
                        despuesDeExito.run();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    JOptionPane.showMessageDialog(
                        GestionCitasGUI.this,
                        "La operacion se ha interrumpido.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );

                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(
                        GestionCitasGUI.this,
                        "Error al comunicar con el servidor.\n"
                        + "Comprueba que esta iniciado en el puerto 8080.",
                        "Error de conexion",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        trabajador.execute();
    }

    /**
     * Interfaz para ejecutar una operacion HTTP.
     */
    @FunctionalInterface
    private interface OperacionHttp {
        String ejecutar() throws Exception;
    }

    /**
     * ASIGNAR UNA CITA EN EL SERVIDOR
     */
    private void asignarCita() {

        if (!validarId(txtIdSolicitud.getText(),
                "El ID de solicitud")) {
            return;
        }

        if (!validarId(txtIdPaciente.getText(),
                "El ID de paciente")) {
            return;
        }

        if (!validarFechaHora(
                txtFecha.getText(), txtHora.getText())) {
            return;
        }

        int solicitud =
            Integer.parseInt(txtIdSolicitud.getText().trim());

        int paciente =
            Integer.parseInt(txtIdPaciente.getText().trim());

        String fecha = txtFecha.getText().trim();
        String hora = txtHora.getText().trim();

        ejecutarOperacion(
            "Asignar cita",
            () -> clienteHttp.asignarCita(
                solicitud, paciente, fecha, hora
            ),
            null
        );
    }

    /**
     * REPROGRAMAR UNA CITA EN EL SERVIDOR
     */
    private void reprogramarCita() {

        if (!validarId(txtIdCita.getText(), "El ID de cita")) {
            return;
        }

        if (!validarFechaHora(
                txtFecha.getText(), txtHora.getText())) {
            return;
        }

        int id = Integer.parseInt(txtIdCita.getText().trim());
        String fecha = txtFecha.getText().trim();
        String hora = txtHora.getText().trim();

        ejecutarOperacion(
            "Reprogramar cita",
            () -> clienteHttp.reprogramarCita(id, fecha, hora),
            () -> consultarCitaPorId(id, false)
        );
    }

    /**
     * CANCELAR UNA CITA EN EL SERVIDOR
     */
    private void cancelarCita() {

        if (!validarId(txtIdCita.getText(), "El ID de cita")) {
            return;
        }

        int id = Integer.parseInt(txtIdCita.getText().trim());

        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Quieres cancelar la cita " + id + "?",
            "Confirmar cancelacion",
            JOptionPane.YES_NO_OPTION
        );

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        ejecutarOperacion(
            "Cancelar cita",
            () -> clienteHttp.cancelarCita(id),
            () -> consultarCitaPorId(id, false)
        );
    }

    /**
     * CONSULTAR UNA CITA EN EL SERVIDOR
     */
    private void consultarCita() {

        if (!validarId(txtIdCita.getText(), "El ID de cita")) {
            return;
        }

        int id = Integer.parseInt(txtIdCita.getText().trim());
        consultarCitaPorId(id, true);
    }

    /**
     * Consultar por ID y actualizar la tabla.
     */
    private void consultarCitaPorId(int id, boolean mostrarMensaje) {

        SwingWorker<String, Void> trabajador = new SwingWorker<>() {

            @Override
            protected String doInBackground() throws Exception {
                return clienteHttp.consultarCita(id);
            }

            @Override
            protected void done() {
                try {
                    String respuesta = get();

                    if (mostrarMensaje) {
                        JOptionPane.showMessageDialog(
                            GestionCitasGUI.this,
                            respuesta,
                            "Consultar cita",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }

                    if (respuesta.startsWith("ID cita:")) {
                        actualizarTablaConRespuesta(respuesta);
                    }

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        GestionCitasGUI.this,
                        "No se ha podido consultar el servidor.",
                        "Error de conexion",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        trabajador.execute();
    }

    /**
     * Mostrar en la tabla una cita devuelta por el servidor.
     */
    private void actualizarTablaConRespuesta(String respuesta) {

        String[] lineas = respuesta.split("\\R");

        if (lineas.length != 6) {
            return;
        }

        String[] valores = new String[6];

        for (int i = 0; i < lineas.length; i++) {
            int posicion = lineas[i].indexOf(":");

            if (posicion < 0) {
                return;
            }

            valores[i] = lineas[i].substring(posicion + 1).trim();
        }

        // Actualizar la fila si la cita ya estaba consultada
        for (int fila = 0; fila < modeloTabla.getRowCount(); fila++) {

            if (modeloTabla.getValueAt(fila, 0)
                    .toString().equals(valores[0])) {

                for (int columna = 0; columna < 6; columna++) {
                    modeloTabla.setValueAt(
                        valores[columna], fila, columna
                    );
                }

                return;
            }
        }

        // Anadir la cita consultada
        modeloTabla.addRow(valores);
    }

    /**
     * METODO PRINCIPAL
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            GestionCitasGUI ventana = new GestionCitasGUI();
            ventana.setVisible(true);
        });
    }
}
