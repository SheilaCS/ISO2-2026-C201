package com.sescam.radiodiagnostico.cliente.encuestas;

public class ValidacionesEncuestas {
    public static boolean validarId(String texto) {
        try { return Integer.parseInt(texto.trim()) > 0; }
        catch (NumberFormatException | NullPointerException e) { return false; }
    }
    public static boolean validarDescripcion(String texto) {
        return texto != null && !texto.isBlank() && texto.trim().length() <= 500;
    }
    public static boolean validarPuntuacion(int valor) { return valor >= 1 && valor <= 5; }
    public static boolean validarComentario(String texto) { return texto == null || texto.trim().length() <= 500; }
}
