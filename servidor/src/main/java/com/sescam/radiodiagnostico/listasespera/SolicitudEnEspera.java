package com.sescam.radiodiagnostico.listasespera;

import java.time.LocalDate;

/**
 * SOLICITUD DE PRUEBA QUE TODAVIA NO TIENE CITA.
 * Iteracion 15 - RF15.
 */
public record SolicitudEnEspera(
        int idSolicitud,
        int idPaciente,
        String tipoPrueba,
        Prioridad prioridad,
        LocalDate fechaSolicitud) {

    // Misma solicitud con otra prioridad
    public SolicitudEnEspera conPrioridad(Prioridad nueva) {
        return new SolicitudEnEspera(
                idSolicitud, idPaciente, tipoPrueba, nueva, fechaSolicitud);
    }
}
