
package com.sescam.radiodiagnostico.citas;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;

/**
 * IMPLEMENTACION DEL SERVICIO DE GESTION DE CITAS.
 * Iteracion 3 - RF13.
 *
 * Version provisional que almacena citas en memoria.
 */
public class GestionCitasServiceImpl implements IGestionCitasService {

    // Almacenamiento temporal de las citas
    private HashMap<Integer, Cita> citas = new HashMap<>();

    // Identificador de la siguiente cita
    private int siguienteId = 1;

    // Clase que representa una cita
    private static class Cita {

        int id;
        int idSolicitud;
        int idPaciente;
        LocalDateTime fechaHora;
        String estado;

        Cita(int id, int idSolicitud, int idPaciente,
             LocalDateTime fechaHora) {

            this.id = id;
            this.idSolicitud = idSolicitud;
            this.idPaciente = idPaciente;
            this.fechaHora = fechaHora;
            this.estado = "PROGRAMADA";
        }
    }

    /**
     * Asigna una cita nueva.
     */
    @Override
    public boolean asignarCita(int idSolicitud, int idPaciente,
                               String fecha, String hora) {

        if (idSolicitud <= 0 || idPaciente <= 0) {
            return false;
        }

        try {
            LocalDateTime fechaHora = LocalDateTime.of(
                LocalDate.parse(fecha),
                LocalTime.parse(hora)
            );

            if (!fechaHora.isAfter(LocalDateTime.now())) {
                return false;
            }

            // Comprobar si el horario ya esta ocupado
            for (Cita cita : citas.values()) {

                if (cita.estado.equals("PROGRAMADA")
                        && cita.fechaHora.equals(fechaHora)) {
                    return false;
                }
            }

            // Registrar cita
            Cita nuevaCita = new Cita(
                siguienteId, idSolicitud, idPaciente, fechaHora
            );

            citas.put(siguienteId, nuevaCita);
            siguienteId++;

            return true;

        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Reprograma una cita existente.
     */
    @Override
    public boolean reprogramarCita(int idCita, String nuevaFecha,
                                   String nuevaHora) {

        Cita cita = citas.get(idCita);

        if (cita == null || cita.estado.equals("CANCELADA")) {
            return false;
        }

        try {
            LocalDateTime fechaHora = LocalDateTime.of(
                LocalDate.parse(nuevaFecha),
                LocalTime.parse(nuevaHora)
            );

            if (!fechaHora.isAfter(LocalDateTime.now())) {
                return false;
            }

            // Comprobar disponibilidad
            for (Cita otraCita : citas.values()) {

                if (otraCita.id != idCita
                        && otraCita.estado.equals("PROGRAMADA")
                        && otraCita.fechaHora.equals(fechaHora)) {
                    return false;
                }
            }

            cita.fechaHora = fechaHora;
            return true;

        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Cancela una cita existente.
     */
    @Override
    public boolean cancelarCita(int idCita) {

        Cita cita = citas.get(idCita);

        if (cita == null || cita.estado.equals("CANCELADA")) {
            return false;
        }

        cita.estado = "CANCELADA";
        return true;
    }

    /**
     * Consulta una cita por su identificador.
     */
    @Override
    public String consultarCita(int idCita) {

        Cita cita = citas.get(idCita);

        if (cita == null) {
            return "No existe una cita con ese ID.";
        }

        return "ID cita: " + cita.id
                + "\nID solicitud: " + cita.idSolicitud
                + "\nID paciente: " + cita.idPaciente
                + "\nFecha: " + cita.fechaHora.toLocalDate()
                + "\nHora: " + cita.fechaHora.toLocalTime()
                + "\nEstado: " + cita.estado;
    }
}
