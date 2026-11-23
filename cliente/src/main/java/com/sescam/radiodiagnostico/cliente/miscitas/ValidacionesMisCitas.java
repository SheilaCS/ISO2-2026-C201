
package com.sescam.radiodiagnostico.cliente.miscitas;

public class ValidacionesMisCitas {

    // Longitud maxima del motivo de un cambio
    public static final int LONGITUD_MAXIMA_MOTIVO = 200;

    // Comprobar que el ID sea un entero positivo
    public static boolean validarId(String texto) {
        try {
            int id = Integer.parseInt(texto.trim());
            return id > 0;

        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    // Comprobar que el motivo no este vacio
    // y que no supere la longitud maxima
    public static boolean validarMotivo(String texto) {

        if (texto == null) {
            return false;
        }

        String motivo = texto.trim();

        return !motivo.isEmpty()
                && motivo.length() <= LONGITUD_MAXIMA_MOTIVO;
    }
}
