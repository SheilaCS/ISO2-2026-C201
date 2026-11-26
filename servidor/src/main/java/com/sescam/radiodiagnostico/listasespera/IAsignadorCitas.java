package com.sescam.radiodiagnostico.listasespera;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ASIGNACION DE UNA CITA EN EL MODULO DE CITAS.
 * Iteracion 15 - RF15.
 *
 * Define lo que necesita la lista de espera del modulo de citas
 * (RF13) para convertir una solicitud en una cita. Si el modulo de
 * citas no esta disponible, las implementaciones lanzan
 * IllegalStateException.
 */
public interface IAsignadorCitas {

    // true si la cita se ha asignado
    boolean asignarCita(int idSolicitud, int idPaciente,
                        LocalDate fecha, LocalTime hora);
}
