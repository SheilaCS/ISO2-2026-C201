
package com.sescam.radiodiagnostico.agendas;

import java.time.LocalDate;
import java.time.LocalTime;

public interface IGestionAgendasService {

    // Crear un horario disponible para un médico
    boolean crearHorario(int idMedico, LocalDate fecha,
                         LocalTime horaInicio, LocalTime horaFin);

    // Consultar los horarios de un médico
    String consultarAgenda(int idMedico);

    // Modificar un horario existente
    boolean modificarHorario(int idHorario, LocalDate fecha,
                             LocalTime horaInicio, LocalTime horaFin);

    // Eliminar un horario
    boolean eliminarHorario(int idHorario);
}
