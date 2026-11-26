package com.sescam.radiodiagnostico.cliente.informes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ValidacionesInformes {

    public static boolean validarId(String texto) {
        try {
            int id = Integer.parseInt(texto.trim());
            return id > 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    public static boolean validarTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    public static boolean validarFecha(String texto) {
        try {
            LocalDate.parse(texto.trim());
            return true;
        } catch (DateTimeParseException | NullPointerException e) {
            return false;
        }
    }
    
    // Seguridad: Comprobamos que el rol sea el adecuado para ver informes
    public static boolean tienePermiso(String rolUsuario) {
        if (rolUsuario == null) return false;
        String rol = rolUsuario.trim().toUpperCase();
        return rol.equals("TECNICO") || rol.equals("ADMIN");
    }
}