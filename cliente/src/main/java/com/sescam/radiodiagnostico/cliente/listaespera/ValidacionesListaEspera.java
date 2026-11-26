package com.sescam.radiodiagnostico.cliente.listaespera;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Validaciones de los datos de la lista de espera antes de enviarlos.
 * Iteracion 15 - RF15.
 */
public class ValidacionesListaEspera {

    // El usuario no puede estar vacio
    public static boolean validarUsuario(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    // La contrasena no puede ser nula ni vacia
    public static boolean validarClave(char[] clave) {
        return clave != null && clave.length > 0;
    }

    // La fecha debe tener formato AAAA-MM-DD y existir en el calendario
    public static boolean validarFecha(String texto) {

        if (texto == null) {
            return false;
        }

        try {
            LocalDate.parse(texto.trim());
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
