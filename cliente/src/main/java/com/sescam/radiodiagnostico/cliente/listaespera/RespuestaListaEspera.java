package com.sescam.radiodiagnostico.cliente.listaespera;

/**
 * Respuesta HTTP del servidor de listas de espera: codigo y cuerpo.
 * Iteracion 15 - RF15.
 */
public record RespuestaListaEspera(int codigo, String cuerpo) {

    // La operacion se ha realizado correctamente (HTTP 200)
    public boolean correcta() {
        return codigo == 200;
    }
}
