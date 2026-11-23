package com.sescam.radiodiagnostico.cliente.listaespera;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * VENTANA DE LISTAS DE ESPERA Y HUECOS.
 * Iteracion 15 - RF15.
 *
 * Pide usuario y contrasena de un administrativo y permite consultar la
 * lista de espera, cambiar prioridades, retirar solicitudes, consultar
 * huecos libres, ver la propuesta de asignacion y asignar un hueco.
 * Los errores (datos incorrectos, rechazos HTTP o fallo de conexion)
 * se muestran en una ventana.
 */
public class GestionListaEsperaGUI extends JFrame {

    private static final String[] COLUMNAS_LISTA =
            {"ID", "Paciente", "Tipo de prueba", "Prioridad", "Fecha solicitud"};

    private static final String[] COLUMNAS_HUECOS =
            {"Medico", "Fecha", "Inicio", "Fin"};

    private static final String[] PRIORIDADES =
            {"URGENTE", "PREFERENTE", "ORDINARIO"};

    // Operacion que necesita usuario y contrasena ya validados
    private interface Operacion {
        void ejecutar(String usuario, char[] clave) throws Exception;
    }

    private JTextField txtUsuario;
    private JPasswordField txtClave;
    private JTextField txtFechaHuecos;
    private JComboBox<String> cmbPrioridad;

    private DefaultTableModel modeloLista;
    private JTable tablaLista;
    private DefaultTableModel modeloHuecos;
    private JTable tablaHuecos;
    private JLabel lblEstado;

    private ListaEsperaHttpClient clienteHttp;

    // Constructor normal
    public GestionListaEsperaGUI() {
        this(new ListaEsperaHttpClient());
    }

    // Constructor para las pruebas
    public GestionListaEsperaGUI(ListaEsperaHttpClient clienteHttp) {

        this.clienteHttp = clienteHttp;

        setTitle("SESCAM - Listas de espera y huecos");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Credenciales
        JPanel panelCredenciales = new JPanel(new GridLayout(2, 2, 10, 10));
        panelCredenciales.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 5, 15));

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();

        panelCredenciales.add(new JLabel("Usuario:"));
        panelCredenciales.add(txtUsuario);
        panelCredenciales.add(new JLabel("Contrasena:"));
        panelCredenciales.add(txtClave);

        // Lista de espera y sus botones
        modeloLista = new DefaultTableModel(COLUMNAS_LISTA, 0);
        tablaLista = new JTable(modeloLista);
        tablaLista.setDefaultEditor(Object.class, null);
        tablaLista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cmbPrioridad = new JComboBox<>(PRIORIDADES);

        JButton btnActualizar = new JButton("Actualizar lista");
        JButton btnPrioridad = new JButton("Cambiar prioridad");
        JButton btnRetirar = new JButton("Retirar de la lista");

        JPanel botonesLista = new JPanel(new FlowLayout());
        botonesLista.add(btnActualizar);
        botonesLista.add(new JLabel("Nueva prioridad:"));
        botonesLista.add(cmbPrioridad);
        botonesLista.add(btnPrioridad);
        botonesLista.add(btnRetirar);

        JPanel panelLista = new JPanel(new BorderLayout());
        panelLista.setBorder(BorderFactory.createTitledBorder(
                "Lista de espera"));
        panelLista.add(new JScrollPane(tablaLista), BorderLayout.CENTER);
        panelLista.add(botonesLista, BorderLayout.SOUTH);

        // Huecos y sus botones
        modeloHuecos = new DefaultTableModel(COLUMNAS_HUECOS, 0);
        tablaHuecos = new JTable(modeloHuecos);
        tablaHuecos.setDefaultEditor(Object.class, null);
        tablaHuecos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        txtFechaHuecos = new JTextField(10);

        JButton btnHuecos = new JButton("Consultar huecos");
        JButton btnPropuesta = new JButton("Proponer asignacion");
        JButton btnAsignar = new JButton("Asignar hueco");

        JPanel botonesHuecos = new JPanel(new FlowLayout());
        botonesHuecos.add(new JLabel("Fecha (AAAA-MM-DD, opcional):"));
        botonesHuecos.add(txtFechaHuecos);
        botonesHuecos.add(btnHuecos);
        botonesHuecos.add(btnPropuesta);
        botonesHuecos.add(btnAsignar);

        JPanel panelHuecos = new JPanel(new BorderLayout());
        panelHuecos.setBorder(BorderFactory.createTitledBorder(
                "Huecos libres"));
        panelHuecos.add(new JScrollPane(tablaHuecos), BorderLayout.CENTER);
        panelHuecos.add(botonesHuecos, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new GridLayout(2, 1));
        centro.add(panelLista);
        centro.add(panelHuecos);

        lblEstado = new JLabel(
                "Introduce usuario y contrasena y pulsa Actualizar lista");
        lblEstado.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        add(panelCredenciales, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);

        btnActualizar.addActionListener(
                e -> ejecutar(this::cargarLista));
        btnPrioridad.addActionListener(
                e -> ejecutar(this::cambiarPrioridad));
        btnRetirar.addActionListener(
                e -> ejecutar(this::retirar));
        btnHuecos.addActionListener(
                e -> ejecutar(this::cargarHuecos));
        btnPropuesta.addActionListener(
                e -> ejecutar(this::proponer));
        btnAsignar.addActionListener(
                e -> ejecutar(this::asignar));
    }

    // Validar usuario y contrasena, ejecutar la operacion y mostrar
    // los errores en una ventana
    private void ejecutar(Operacion operacion) {

        char[] clave = txtClave.getPassword();

        try {
            String usuario = txtUsuario.getText().trim();

            if (!ValidacionesListaEspera.validarUsuario(usuario)) {
                throw new IllegalArgumentException(
                        "El usuario es obligatorio");
            }

            if (!ValidacionesListaEspera.validarClave(clave)) {
                throw new IllegalArgumentException(
                        "La contrasena es obligatoria");
            }

            operacion.ejecutar(usuario, clave);

        } catch (IllegalArgumentException ex) {
            lblEstado.setText("Operacion no realizada");
            mostrarError(ex.getMessage());

        } catch (Exception ex) {
            lblEstado.setText("Operacion no realizada");
            mostrarError("No se ha podido conectar con el servidor. "
                    + "Comprueba que esta en marcha. ("
                    + ex.getMessage() + ")");

        } finally {
            // No conservar copias de la contrasena en memoria
            Arrays.fill(clave, '\0');
        }
    }

    // ------------------------------------
    // OPERACIONES
    // ------------------------------------

    // Rellenar la tabla de la lista de espera (false si se rechaza)
    private boolean cargarLista(String usuario, char[] clave)
            throws Exception {

        RespuestaListaEspera respuesta =
                clienteHttp.consultarLista(usuario, clave);

        if (!respuesta.correcta()) {
            modeloLista.setRowCount(0);
            lblEstado.setText("Consulta rechazada");
            mostrarError(ListaEsperaHttpClient.mensajeError(respuesta));
            return false;
        }

        List<SolicitudLista> solicitudes =
                ListaEsperaHttpClient.parsearSolicitudes(respuesta.cuerpo());

        modeloLista.setRowCount(0);
        for (SolicitudLista s : solicitudes) {
            modeloLista.addRow(new Object[] {
                    s.id(), s.paciente(), s.tipo(), s.prioridad(), s.fecha()});
        }

        lblEstado.setText(solicitudes.size() == 1
                ? "1 solicitud en la lista de espera"
                : solicitudes.size() + " solicitudes en la lista de espera");
        return true;
    }

    private void cargarHuecos(String usuario, char[] clave)
            throws Exception {
        cargarHuecos(usuario, clave, txtFechaHuecos.getText().trim());
    }

    // Rellenar la tabla de huecos (false si se rechaza)
    private boolean cargarHuecos(String usuario, char[] clave, String fecha)
            throws Exception {

        if (!fecha.isEmpty() && !ValidacionesListaEspera.validarFecha(fecha)) {
            throw new IllegalArgumentException(
                    "La fecha debe tener formato AAAA-MM-DD");
        }

        RespuestaListaEspera respuesta =
                clienteHttp.consultarHuecos(usuario, clave, fecha);

        if (!respuesta.correcta()) {
            modeloHuecos.setRowCount(0);
            lblEstado.setText("Consulta rechazada");
            mostrarError(ListaEsperaHttpClient.mensajeError(respuesta));
            return false;
        }

        List<HuecoLista> huecos =
                ListaEsperaHttpClient.parsearHuecos(respuesta.cuerpo());

        modeloHuecos.setRowCount(0);
        for (HuecoLista h : huecos) {
            modeloHuecos.addRow(new Object[] {
                    h.medico(), h.fecha(), h.inicio(), h.fin()});
        }

        lblEstado.setText(huecos.size() == 1
                ? "1 hueco libre"
                : huecos.size() + " huecos libres");
        return true;
    }

    private void cambiarPrioridad(String usuario, char[] clave)
            throws Exception {

        String id = solicitudSeleccionada();

        RespuestaListaEspera respuesta = clienteHttp.cambiarPrioridad(
                usuario, clave, id, (String) cmbPrioridad.getSelectedItem());

        terminarOperacion(usuario, clave, respuesta);
    }

    private void retirar(String usuario, char[] clave) throws Exception {

        String id = solicitudSeleccionada();

        terminarOperacion(usuario, clave,
                clienteHttp.retirar(usuario, clave, id));
    }

    private void asignar(String usuario, char[] clave) throws Exception {

        String id = solicitudSeleccionada();

        int fila = tablaHuecos.getSelectedRow();
        if (fila < 0) {
            throw new IllegalArgumentException(
                    "Selecciona un hueco de la tabla de huecos");
        }

        RespuestaListaEspera respuesta = clienteHttp.asignar(
                usuario, clave, id,
                modeloHuecos.getValueAt(fila, 0).toString(),
                modeloHuecos.getValueAt(fila, 1).toString(),
                modeloHuecos.getValueAt(fila, 2).toString());

        terminarOperacion(usuario, clave, respuesta);
    }

    // Cargar la lista y los huecos y seleccionar la propuesta del servidor
    private void proponer(String usuario, char[] clave) throws Exception {

        txtFechaHuecos.setText("");

        if (!cargarLista(usuario, clave)
                || !cargarHuecos(usuario, clave, "")) {
            return;
        }

        RespuestaListaEspera respuesta =
                clienteHttp.consultarPropuesta(usuario, clave);

        if (!respuesta.correcta()) {
            lblEstado.setText("Consulta rechazada");
            mostrarError(ListaEsperaHttpClient.mensajeError(respuesta));
            return;
        }

        Optional<PropuestaLista> propuesta =
                ListaEsperaHttpClient.parsearPropuesta(respuesta.cuerpo());

        if (propuesta.isEmpty()) {
            lblEstado.setText(
                    "No hay propuesta: faltan solicitudes o huecos libres");
            return;
        }

        SolicitudLista s = propuesta.get().solicitud();
        HuecoLista h = propuesta.get().hueco();

        seleccionar(tablaLista, modeloLista, s.id(), null, null);
        seleccionar(tablaHuecos, modeloHuecos, h.medico(), h.fecha(),
                h.inicio());

        lblEstado.setText("Propuesta: solicitud " + s.id()
                + " con el medico " + h.medico() + " el " + h.fecha()
                + " a las " + h.inicio()
                + ". Pulsa Asignar hueco para confirmarla");
    }

    // Seleccionar la fila cuyas primeras columnas coinciden
    private void seleccionar(JTable tabla, DefaultTableModel modelo,
            String col0, String col1, String col2) {

        for (int i = 0; i < modelo.getRowCount(); i++) {
            boolean coincide = modelo.getValueAt(i, 0).equals(col0)
                    && (col1 == null || modelo.getValueAt(i, 1).equals(col1))
                    && (col2 == null || modelo.getValueAt(i, 2).equals(col2));

            if (coincide) {
                tabla.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    // Mostrar el resultado de una operacion y, si ha ido bien, volver a
    // cargar las tablas
    private void terminarOperacion(String usuario, char[] clave,
            RespuestaListaEspera respuesta) throws Exception {

        if (!respuesta.correcta()) {
            lblEstado.setText("Operacion rechazada");
            mostrarError(ListaEsperaHttpClient.mensajeError(respuesta));
            return;
        }

        String mensaje = ListaEsperaHttpClient.mensajeCorrecto(respuesta);

        if (cargarLista(usuario, clave)
                && cargarHuecos(usuario, clave,
                        txtFechaHuecos.getText().trim())) {
            lblEstado.setText(mensaje);
        }
    }

    // Id de la solicitud seleccionada en la lista de espera
    private String solicitudSeleccionada() {

        int fila = tablaLista.getSelectedRow();

        if (fila < 0) {
            throw new IllegalArgumentException(
                    "Selecciona una solicitud de la lista de espera");
        }

        return modeloLista.getValueAt(fila, 0).toString();
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

            GestionListaEsperaGUI ventana = new GestionListaEsperaGUI();

            ventana.setVisible(true);
        });
    }
}
