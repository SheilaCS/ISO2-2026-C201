package com.sescam.radiodiagnostico.cliente.pacientes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Validaciones del lado del cliente para CDU4 (RF12 - Gestion de pacientes).
 */
public class ValidacionesPacientes {

    /**
     * Comprueba que el identificador sea un UUID valido.
     */
    public static boolean validarId(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(texto.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Normaliza el CIP eliminando espacios exteriores y pasando a mayusculas.
     */
    public static String normalizarCip(String cip) {
        if (cip == null) {
            return "";
        }
        return cip.trim().toUpperCase();
    }

    /**
     * Comprueba que el CIP sea obligatorio y no quede vacio tras normalizar.
     */
    public static boolean validarCip(String cip) {
        return !normalizarCip(cip).isEmpty();
    }

    /**
     * Comprueba que un campo de texto obligatorio (nombre o apellidos) no este vacio.
     */
    public static boolean validarTextoObligatorio(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    /**
     * Comprueba que la fecha de nacimiento tenga formato AAAA-MM-DD y no sea futura.
     */
    public static boolean validarFechaNacimiento(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return false;
        }
        try {
            LocalDate fecha = LocalDate.parse(fechaTexto.trim());
            return !fecha.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}