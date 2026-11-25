package com.sescam.radiodiagnostico.cliente.encuestas;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class GestionEncuestasGUI extends JFrame {
    private final EncuestasHttpClient cliente;
    private final JTextField usuario = new JTextField("paciente.demo");
    private final JPasswordField clave = new JPasswordField();
    private final JTextField idCita = new JTextField();
    private final JTextArea descripcion = new JTextArea(4, 30);
    private final JTextArea comentario = new JTextArea(4, 30);
    private final JComboBox<Integer> puntuacion = new JComboBox<>(new Integer[]{1,2,3,4,5});
    private final JLabel resultado = new JLabel(" ");
    private final JButton incidencia = new JButton("Comunicar incidencia");
    private final JButton encuesta = new JButton("Enviar encuesta");

    public GestionEncuestasGUI() { this(new EncuestasHttpClient()); }
    public GestionEncuestasGUI(EncuestasHttpClient cliente) {
        super("Gestion de incidencias y encuestas - RF05");
        this.cliente = cliente;
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        panel.add(new JLabel("Usuario")); panel.add(usuario);
        panel.add(new JLabel("Clave")); panel.add(clave);
        panel.add(new JLabel("ID de cita")); panel.add(idCita);
        panel.add(new JLabel("Descripcion de incidencia")); panel.add(new JScrollPane(descripcion));
        panel.add(new JLabel("Puntuacion (1-5)")); panel.add(puntuacion);
        panel.add(new JLabel("Comentario opcional")); panel.add(new JScrollPane(comentario));
        panel.add(incidencia); panel.add(encuesta);
        add(panel, BorderLayout.CENTER); add(resultado, BorderLayout.SOUTH);
        incidencia.addActionListener(e -> comunicarIncidencia());
        encuesta.addActionListener(e -> responderEncuesta());
    }

    public void comunicarIncidencia() { enviar(false); }
    public void responderEncuesta() { enviar(true); }

    private void enviar(boolean esEncuesta) {
        if (!ValidacionesEncuestas.validarId(idCita.getText())) { resultado.setText("ID de cita no valido"); return; }
        int valor = (Integer) puntuacion.getSelectedItem();
        String texto = esEncuesta ? comentario.getText() : descripcion.getText();
        if (esEncuesta ? !ValidacionesEncuestas.validarComentario(texto) || !ValidacionesEncuestas.validarPuntuacion(valor)
                : !ValidacionesEncuestas.validarDescripcion(texto)) {
            resultado.setText("Datos no validos (maximo 500 caracteres)"); return;
        }
        int id = Integer.parseInt(idCita.getText().trim());
        String nombre = usuario.getText().trim();
        char[] password = clave.getPassword();
        clave.setText("");
        incidencia.setEnabled(false); encuesta.setEnabled(false);
        new SwingWorker<String, Void>() {
            protected String doInBackground() {
                try {
                    if (esEncuesta) return cliente.responderEncuesta(id, valor, texto, nombre, password);
                    return cliente.comunicarIncidencia(id, texto, nombre, password);
                } finally { Arrays.fill(password, '\0'); }
            }
            protected void done() {
                try { resultado.setText(get()); }
                catch (Exception e) { resultado.setText("Error al enviar los datos"); }
                finally { incidencia.setEnabled(true); encuesta.setEnabled(true); }
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestionEncuestasGUI().setVisible(true));
    }
}
