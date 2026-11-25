package com.sescam.radiodiagnostico.realizacion;

import java.time.LocalDate;
import java.util.UUID;

public record RealizacionPrueba(
        UUID id,
        int idCita,
        int idSolicitud,
        LocalDate fechaRealizacion,
        String observaciones,
        String incidencia,
        String estado
) {
}