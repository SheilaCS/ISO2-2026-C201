package com.sescam.radiodiagnostico.cliente.parametros;

public class ValidacionesParametros {
    
    // Validar que el texto no esté vacío
    public static boolean validarTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    // Validar que un parámetro numérico no sea negativo (ejemplo para pruebas)
    public static boolean validarNumeroPositivo(String texto) {
        try {
            int num = Integer.parseInt(texto.trim());
            return num >= 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }
}