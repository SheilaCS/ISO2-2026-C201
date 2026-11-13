
package com.sescam.radiodiagnostico.agendas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;

public class GestionAgendasServiceImpl implements IGestionAgendasService {

    // Clase que representa un horario de un médico
    private static class Horario {
        int idHorario;
        int idMedico;
        LocalDate fecha;
        LocalTime horaInicio;
        LocalTime horaFin;

        Horario(int idHorario, int idMedico, LocalDate fecha,
                LocalTime horaInicio, LocalTime horaFin) {
            this.idHorario = idHorario;
            this.idMedico = idMedico;
            this.fecha = fecha;
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }
    }

    // Almacena los horarios por su identificador
    private HashMap<Integer, Horario> horarios = new HashMap<>();
    private int siguienteId = 1;

    @Override
    public boolean crearHorario(int idMedico, LocalDate fecha,
                                LocalTime horaInicio, LocalTime horaFin) {

        // Comprobar que los datos sean válidos
        if (idMedico <= 0 || fecha == null ||
                horaInicio == null || horaFin == null) {
            return false;
        }

        if (fecha.isBefore(LocalDate.now())
                || !horaInicio.isBefore(horaFin)) {
            return false;
        }

        // Evitar horarios solapados del mismo médico
        for (Horario horario : horarios.values()) {
            if (horario.idMedico == idMedico
                    && horario.fecha.equals(fecha)
                    && horaInicio.isBefore(horario.horaFin)
                    && horaFin.isAfter(horario.horaInicio)) {
                return false;
            }
        }

        // Guardar el nuevo horario
        Horario horario = new Horario(siguienteId, idMedico, fecha,
                                     horaInicio, horaFin);
        horarios.put(siguienteId, horario);
        siguienteId++;

        return true;
    }

    @Override
    public String consultarAgenda(int idMedico) {
        if (idMedico <= 0) {
            return "ID de médico no válido";
        }

        String resultado = "";

        for (Horario horario : horarios.values()) {
            if (horario.idMedico == idMedico) {
                resultado += "Horario " + horario.idHorario
                        + " | Fecha: " + horario.fecha
                        + " | Inicio: " + horario.horaInicio
                        + " | Fin: " + horario.horaFin + "\n";
            }
        }

        if (resultado.isEmpty()) {
            return "No hay horarios para este médico";
        }

        return resultado;
    }

    @Override
    public boolean modificarHorario(int idHorario, LocalDate fecha,
                                    LocalTime horaInicio, LocalTime horaFin) {

        Horario horario = horarios.get(idHorario);

        if (horario == null || fecha == null ||
                horaInicio == null || horaFin == null) {
            return false;
        }

        if (fecha.isBefore(LocalDate.now())
                || !horaInicio.isBefore(horaFin)) {
            return false;
        }

        // Comprobar solapamientos, excluyendo el horario modificado
        for (Horario otro : horarios.values()) {
            if (otro.idHorario != idHorario
                    && otro.idMedico == horario.idMedico
                    && otro.fecha.equals(fecha)
                    && horaInicio.isBefore(otro.horaFin)
                    && horaFin.isAfter(otro.horaInicio)) {
                return false;
            }
        }

        horario.fecha = fecha;
        horario.horaInicio = horaInicio;
        horario.horaFin = horaFin;

        return true;
    }

    @Override
    public boolean eliminarHorario(int idHorario) {
        return horarios.remove(idHorario) != null;
    }
}
