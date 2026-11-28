package com.sescam.radiodiagnostico.cliente.informes;

/**
 * Validaciones del cliente de informes e imágenes (CDU8, RF09).
 */
public class ValidacionesInformes {

    private ValidacionesInformes() {
    }

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

    /** Las imágenes son opcionales; si se indican, ninguna referencia puede estar vacía. */
    public static boolean validarImagenes(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return true;
        }
        for (String imagen : texto.split(",", -1)) {
            if (imagen.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
