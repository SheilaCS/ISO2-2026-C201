package com.sescam.radiodiagnostico.cliente.solicitudes;

public class ValidacionesSolicitudes {

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
    
    public static boolean validarPrioridad(String prioridad) {
        if (prioridad == null) return false;
        String p = prioridad.trim().toUpperCase();
        return p.equals("URGENTE") || p.equals("PREFERENTE") || p.equals("ORDINARIO");
    }
}