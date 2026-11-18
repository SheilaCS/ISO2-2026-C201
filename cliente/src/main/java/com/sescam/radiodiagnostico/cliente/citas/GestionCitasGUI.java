
package com.sescam.radiodiagnostico.cliente.citas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * CLIENTE - Iteracion 3 (RF13).
 * Interfaz de gestion de citas del SESCAM.
 *
 * Permite asignar, reprogramar, cancelar
 * y consultar citas mediante HTTP.
 *
 * Las validaciones se realizan utilizando
 * la clase ValidacionesCitas.
 */
public class GestionCitasGUI extends JFrame {

    // Campos del formulario
    private JTextField txtIdCita;
    private JTextField txtIdSolicitud;
    private JTextField txtIdPaciente;
    private JTextField txtFecha;
    private JTextField txtHora;

    // Tabla de citas consultadas
    private DefaultTableModel modeloTabla;
    private JTable tablaCitas;

    // Cliente que envia las peticiones HTTP
    private CitasHttpClient clienteHttp = new CitasHttpClient();

    // Colores de la interfaz
    private Color azul = new Color(20, 75, 115);
    private Color fondo = new Color(245, 248, 252);

    /**
     * Constructor de la ventana de gestion de citas.
     */
    public GestionCitasGUI() {

        // CONFIGURACION DE LA VENTANA

        setTitle("SESCAM - Gestion de Citas");
        setSize(850, 600);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel principal = new JPanel(
                new BorderLayout(15, 15));

        principal.setBackground(fondo);
        principal.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 15, 15, 15)
        );

        // CABECERA

        JPanel cabecera = new JPanel(
                new GridLayout(2, 1, 0, 5));

        cabecera.setBackground(azul);
        cabecera.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 20, 15, 20)
        );

        JLabel titulo = new JLabel(
                "SESCAM | GESTIÓN DE CITAS");

        titulo.setFont(
                new Font("Arial", Font.BOLD, 22));

        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel(
                "Sistema Integral de Radiodiagnóstico e Imagen Médica"
        );

        subtitulo.setForeground(Color.WHITE);

        cabecera.add(titulo);
        cabecera.add(subtitulo);

        principal.add(cabecera, BorderLayout.NORTH);

        // FORMULARIO

        JPanel formulario = new JPanel(
                new GridLayout(5, 2, 10, 10));

        formulario.setBackground(Color.WHITE);

        formulario.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(
                                "Datos de la cita"),
                        BorderFactory.createEmptyBorder(
                                15, 15, 15, 15)
                )
        );

        // Crear los campos de texto
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

        JPanel botones = new JPanel(
                new GridLayout(1, 4, 10, 0));

        botones.setBackground(fondo);

        JButton btnAsignar = new JButton("Asignar cita");
        JButton btnReprogramar = new JButton("Reprogramar");
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnConsultar = new JButton("Consultar");

        JButton[] listaBotones = {
                btnAsignar,
                btnReprogramar,
                btnCancelar,
                btnConsultar
        };

        // Aplicar el mismo estilo a los botones
        for (JButton boton : listaBotones) {

            boton.setFont(
                    new Font("Arial", Font.BOLD, 13));

            boton.setFocusPainted(false);

            boton.setCursor(
                    new Cursor(Cursor.HAND_CURSOR));

            botones.add(boton);
        }

        btnAsignar.setBackground(azul);
        btnAsignar.setForeground(Color.WHITE);

        // TABLA DE CITAS

        String[] columnas = {
                "ID cita",
                "Solicitud",
                "Paciente",
                "Fecha",
                "Hora",
                "Estado"
        };

        modeloTabla = new DefaultTableModel(
                columnas, 0) {

            @Override
            public boolean isCellEditable(
                    int fila, int columna) {

                // Impedir modificar directamente las celdas
                return false;
            }
        };

        tablaCitas = new JTable(modeloTabla);

        tablaCitas.setRowHeight(28);

        tablaCitas.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tablaCitas);

        scroll.setBorder(
                BorderFactory.createTitledBorder(
                        "Citas consultadas")
        );

        // Al seleccionar una cita de la tabla,
        // se copia su ID al formulario.
        tablaCitas.getSelectionModel()
                .addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {

                int fila = tablaCitas.getSelectedRow();

                if (fila >= 0) {

                    txtIdCita.setText(
                            modeloTabla.getValueAt(
                                    fila, 0).toString()
                    );
                }
            }
        });

        // ORGANIZACION DE LA VENTANA

        JPanel contenido = new JPanel(
                new BorderLayout(15, 15));

        contenido.setBackground(fondo);

        JPanel zonaSuperior = new JPanel(
                new BorderLayout(10, 10));

        zonaSuperior.setBackground(fondo);

        zonaSuperior.add(
                formulario, BorderLayout.CENTER);

        zonaSuperior.add(
                botones, BorderLayout.SOUTH);

        contenido.add(
                zonaSuperior, BorderLayout.NORTH);

        contenido.add(
                scroll, BorderLayout.CENTER);

        principal.add(
                contenido, BorderLayout.CENTER);

        add(principal);

        // ACCIONES DE LOS BOTONES

        btnAsignar.addActionListener(
                e -> asignarCita());

        btnReprogramar.addActionListener(
                e -> reprogramarCita());

        btnCancelar.addActionListener(
                e -> cancelarCita());

        btnConsultar.addActionListener(
                e -> consultarCita());
    }

    /**
     * Comprueba que un ID sea un entero positivo.
     *
     * La comprobacion se realiza utilizando
     * ValidacionesCitas, que tambien se prueba con JUnit.
     */
    private boolean validarId(
            String texto, String nombre) {

        if (!ValidacionesCitas.validarId(texto)) {

            JOptionPane.showMessageDialog(
                    this,
                    nombre + " debe ser un entero mayor que cero."
            );

            return false;
        }

        return true;
    }

    /**
     * Comprueba que la fecha y la hora sean
     * validas y futuras.
     */
    private boolean validarFechaHora(
            String fecha, String hora) {

        if (!ValidacionesCitas.validarFechaHora(
                fecha, hora)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Introduce una fecha y hora validas y futuras."
            );

            return false;
        }

        return true;
    }

    /**
     * Ejecuta una operacion HTTP sin bloquear
     * la interfaz grafica de Swing.
     */
    private void ejecutarOperacion(
            String titulo,
            OperacionHttp operacion,
            Runnable despuesDeExito) {

        SwingWorker<String, Void> trabajador =
                new SwingWorker<>() {

            @Override
            protected String doInBackground()
                    throws Exception {

                // Ejecutar la peticion en segundo plano
                return operacion.ejecutar();
            }

            @Override
            protected void done() {

                try {

                    String respuesta = get();

                    // Mostrar la respuesta del servidor
                    JOptionPane.showMessageDialog(
                            GestionCitasGUI.this,
                            respuesta,
                            titulo,
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    // Actualizar si la operacion fue correcta
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
     * Interfaz para ejecutar operaciones HTTP.
     */
    @FunctionalInterface
    private interface OperacionHttp {

        String ejecutar() throws Exception;
    }

    /**
     * ASIGNAR UNA CITA EN EL SERVIDOR.
     */
    private void asignarCita() {

        // Comprobar el ID de la solicitud
        if (!validarId(
                txtIdSolicitud.getText(),
                "El ID de solicitud")) {

            return;
        }

        // Comprobar el ID del paciente
        if (!validarId(
                txtIdPaciente.getText(),
                "El ID de paciente")) {

            return;
        }

        // Comprobar la fecha y hora
        if (!validarFechaHora(
                txtFecha.getText(),
                txtHora.getText())) {

            return;
        }

        // Obtener los valores introducidos
        int solicitud = Integer.parseInt(
                txtIdSolicitud.getText().trim());

        int paciente = Integer.parseInt(
                txtIdPaciente.getText().trim());

        String fecha = txtFecha.getText().trim();
        String hora = txtHora.getText().trim();

        // Enviar la peticion HTTP al servidor
        ejecutarOperacion(
                "Asignar cita",
                () -> clienteHttp.asignarCita(
                        solicitud,
                        paciente,
                        fecha,
                        hora
                ),
                null
        );
    }

    /**
     * REPROGRAMAR UNA CITA EN EL SERVIDOR.
     */
    private void reprogramarCita() {

        // Comprobar el identificador de la cita
        if (!validarId(
                txtIdCita.getText(),
                "El ID de cita")) {

            return;
        }

        // Comprobar la nueva fecha y hora
        if (!validarFechaHora(
                txtFecha.getText(),
                txtHora.getText())) {

            return;
        }

        int id = Integer.parseInt(
                txtIdCita.getText().trim());

        String fecha = txtFecha.getText().trim();
        String hora = txtHora.getText().trim();

        // Enviar peticion HTTP y actualizar la tabla
        ejecutarOperacion(
                "Reprogramar cita",
                () -> clienteHttp.reprogramarCita(
                        id, fecha, hora),
                () -> consultarCitaPorId(id, false)
        );
    }

    /**
     * CANCELAR UNA CITA EN EL SERVIDOR.
     */
    private void cancelarCita() {

        // Comprobar que el ID sea valido
        if (!validarId(
                txtIdCita.getText(),
                "El ID de cita")) {

            return;
        }

        int id = Integer.parseInt(
                txtIdCita.getText().trim());

        // Solicitar confirmacion al usuario
        int confirmacion =
                JOptionPane.showConfirmDialog(
                        this,
                        "¿Quieres cancelar la cita " + id + "?",
                        "Confirmar cancelacion",
                        JOptionPane.YES_NO_OPTION
                );

        // Si el usuario no confirma, no cancelar
        if (confirmacion != JOptionPane.YES_OPTION) {

            return;
        }

        // Enviar peticion HTTP para cancelar
        ejecutarOperacion(
                "Cancelar cita",
                () -> clienteHttp.cancelarCita(id),
                () -> consultarCitaPorId(id, false)
        );
    }

    /**
     * CONSULTAR UNA CITA EN EL SERVIDOR.
     */
    private void consultarCita() {

        // Comprobar el identificador
        if (!validarId(
                txtIdCita.getText(),
                "El ID de cita")) {

            return;
        }

        int id = Integer.parseInt(
                txtIdCita.getText().trim());

        // Consultar y mostrar el resultado
        consultarCitaPorId(id, true);
    }

    /**
     * Consulta una cita por ID y actualiza
     * la tabla de resultados.
     */
    private void consultarCitaPorId(
            int id, boolean mostrarMensaje) {

        SwingWorker<String, Void> trabajador =
                new SwingWorker<>() {

            @Override
            protected String doInBackground()
                    throws Exception {

                // Solicitar la cita al servidor
                return clienteHttp.consultarCita(id);
            }

            @Override
            protected void done() {

                try {

                    String respuesta = get();

                    // Mostrar la respuesta si corresponde
                    if (mostrarMensaje) {

                        JOptionPane.showMessageDialog(
                                GestionCitasGUI.this,
                                respuesta,
                                "Consultar cita",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }

                    // Actualizar la tabla si existe la cita
                    if (respuesta.startsWith("ID cita:")) {

                        actualizarTablaConRespuesta(
                                respuesta);
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
     * Muestra en la tabla una cita recibida
     * como respuesta del servidor.
     */
    private void actualizarTablaConRespuesta(
            String respuesta) {

        // Separar las lineas de la respuesta
        String[] lineas = respuesta.split("\\R");

        // La respuesta debe contener seis campos
        if (lineas.length != 6) {
            return;
        }

        String[] valores = new String[6];

        // Extraer el valor de cada campo
        for (int i = 0; i < lineas.length; i++) {

            int posicion = lineas[i].indexOf(":");

            if (posicion < 0) {
                return;
            }

            valores[i] = lineas[i]
                    .substring(posicion + 1).trim();
        }

        // Comprobar si la cita ya esta en la tabla
        for (int fila = 0;
                fila < modeloTabla.getRowCount();
                fila++) {

            if (modeloTabla.getValueAt(fila, 0)
                    .toString().equals(valores[0])) {

                // Actualizar los datos de la fila
                for (int columna = 0;
                        columna < 6;
                        columna++) {

                    modeloTabla.setValueAt(
                            valores[columna],
                            fila,
                            columna
                    );
                }

                return;
            }
        }

        // Si no existia, añadir la cita a la tabla
        modeloTabla.addRow(valores);
    }

    /**
     * METODO PRINCIPAL.
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            GestionCitasGUI ventana =
                    new GestionCitasGUI();

            ventana.setVisible(true);
        });
    }
}
