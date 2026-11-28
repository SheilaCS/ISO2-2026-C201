package com.sescam.radiodiagnostico.solicitudes;

import java.time.LocalDateTime;

/**
 * Solicitud de prueba diagnóstica realizada por un médico (CDU2, RF06).
 */
public record Solicitud(
        int id,
        int idPaciente,
        String medico,
        String tipoPrueba,
        String indicacionClinica,
        String justificacion,
        Prioridad prioridad,
        LocalDateTime fecha) {
}
