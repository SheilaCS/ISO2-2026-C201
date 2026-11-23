package com.sescam.radiodiagnostico.listasespera;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * HORARIO LIBRE DE LA AGENDA DE UN MEDICO.
 * Iteracion 15 - RF15.
 */
public record Hueco(
        int idMedico,
        LocalDate fecha,
        LocalTime inicio,
        LocalTime fin) {
}
