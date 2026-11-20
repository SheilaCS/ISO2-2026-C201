package com.sescam.radiodiagnostico.citas;

/**
 * RESULTADO DE UNA OPERACION DEL PACIENTE SOBRE SUS CITAS.
 * Iteracion 9 - RF02.
 *
 * Indica si la operacion se ha realizado y el mensaje que se
 * muestra al paciente, que explica el motivo cuando se rechaza.
 */
public final class ResultadoOperacion {

    private final boolean correcto;
    private final String mensaje;

    private ResultadoOperacion(boolean correcto, String mensaje) {
        this.correcto = correcto;
        this.mensaje = mensaje;
    }

    // Operacion realizada
    public static ResultadoOperacion correcta(String mensaje) {
        return new ResultadoOperacion(true, mensaje);
    }

    // Operacion rechazada
    public static ResultadoOperacion rechazada(String mensaje) {
        return new ResultadoOperacion(false, mensaje);
    }

    public boolean isCorrecto() {
        return correcto;
    }

    public String getMensaje() {
        return mensaje;
    }
}
