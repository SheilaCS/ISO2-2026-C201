package com.sescam.radiodiagnostico.cliente.historial;

/**
 * Validaciones de los datos del historial antes de enviarlos.
 * Iteracion 12 - RF04.
 */
public class ValidacionesHistorial {

    // Longitud maxima del identificador de paciente
    public static final int LONGITUD_MAXIMA_ID = 50;

    // El usuario no puede estar vacio
    public static boolean validarUsuario(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    // La contrasena no puede ser nula ni vacia
    public static boolean validarClave(char[] clave) {
        return clave != null && clave.length > 0;
    }

    // El identificador del paciente solo admite letras, numeros,
    // punto, guion y guion bajo, hasta la longitud maxima
    public static boolean validarIdPaciente(String texto) {

        if (texto == null) {
            return false;
        }

        String id = texto.trim();

        return !id.isEmpty()
                && id.length() <= LONGITUD_MAXIMA_ID
                && id.matches("[A-Za-z0-9._-]+");
    }
}
