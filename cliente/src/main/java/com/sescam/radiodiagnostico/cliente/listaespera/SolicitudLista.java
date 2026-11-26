package com.sescam.radiodiagnostico.cliente.listaespera;

/**
 * Solicitud de la lista de espera tal y como la recibe el cliente.
 * Iteracion 15 - RF15.
 */
public record SolicitudLista(
        String id,
        String paciente,
        String tipo,
        String prioridad,
        String fecha) {
}
