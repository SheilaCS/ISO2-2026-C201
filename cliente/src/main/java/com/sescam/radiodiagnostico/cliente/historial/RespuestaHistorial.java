package com.sescam.radiodiagnostico.cliente.historial;

/**
 * Respuesta HTTP del servidor del historial: codigo y cuerpo.
 * Iteracion 12 - RF04.
 */
public record RespuestaHistorial(int codigo, String cuerpo) {

    // La consulta se ha realizado correctamente (HTTP 200)
    public boolean correcta() {
        return codigo == 200;
    }
}
