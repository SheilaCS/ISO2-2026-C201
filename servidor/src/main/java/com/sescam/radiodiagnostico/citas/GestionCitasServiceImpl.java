
package com.sescam.radiodiagnostico.citas;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * IMPLEMENTACION DEL SERVICIO DE GESTION DE CITAS.
 * Iteracion 3 - RF13.
 *
 * Version provisional que almacena citas en memoria.
 */
public class GestionCitasServiceImpl implements IGestionCitasService {

    // Estados de una cita
    private static final String PROGRAMADA = "PROGRAMADA";
    private static final String CONFIRMADA = "CONFIRMADA";
    private static final String CAMBIO_SOLICITADO = "CAMBIO_SOLICITADO";
    private static final String CANCELADA = "CANCELADA";

    // Longitud maxima del motivo de un cambio (RF02)
    private static final int LONGITUD_MAXIMA_MOTIVO = 200;

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
        String motivoCambio;

        Cita(int id, int idSolicitud, int idPaciente,
             LocalDateTime fechaHora) {

            this.id = id;
            this.idSolicitud = idSolicitud;
            this.idPaciente = idPaciente;
            this.fechaHora = fechaHora;
            this.estado = PROGRAMADA;
            this.motivoCambio = "";
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

                if (!cita.estado.equals(CANCELADA)
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

        if (cita == null || cita.estado.equals(CANCELADA)) {
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
                        && !otraCita.estado.equals(CANCELADA)
                        && otraCita.fechaHora.equals(fechaHora)) {
                    return false;
                }
            }

            cita.fechaHora = fechaHora;

            // Una cita reprogramada vuelve a estar pendiente
            // de confirmar y se atiende la peticion de cambio
            cita.estado = PROGRAMADA;
            cita.motivoCambio = "";

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

        if (cita == null || cita.estado.equals(CANCELADA)) {
            return false;
        }

        cita.estado = CANCELADA;
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

        String texto = "ID cita: " + cita.id
                + "\nID solicitud: " + cita.idSolicitud
                + "\nID paciente: " + cita.idPaciente
                + "\nFecha: " + cita.fechaHora.toLocalDate()
                + "\nHora: " + cita.fechaHora.toLocalTime()
                + "\nEstado: " + cita.estado;

        if (cita.estado.equals(CAMBIO_SOLICITADO)) {
            texto += "\nMotivo del cambio: " + cita.motivoCambio;
        }

        return texto;
    }

    // -----------------------------------------
    // OPERACIONES DEL PACIENTE (RF02)
    // -----------------------------------------

    /**
     * Lista las citas de un paciente, una por linea.
     * Devuelve un texto vacio si el paciente no tiene citas.
     */
    @Override
    public String listarCitasPaciente(int idPaciente) {

        // Ordenar por ID para que la salida sea estable
        TreeMap<Integer, Cita> delPaciente = new TreeMap<>();

        for (Cita cita : citas.values()) {

            if (cita.idPaciente == idPaciente) {
                delPaciente.put(cita.id, cita);
            }
        }

        StringBuilder texto = new StringBuilder();

        for (Cita cita : delPaciente.values()) {

            if (texto.length() > 0) {
                texto.append("\n");
            }

            texto.append("Cita ").append(cita.id)
                 .append(" | Fecha: ")
                 .append(cita.fechaHora.toLocalDate())
                 .append(" | Hora: ")
                 .append(cita.fechaHora.toLocalTime())
                 .append(" | Estado: ").append(cita.estado);

            if (cita.estado.equals(CAMBIO_SOLICITADO)) {
                texto.append(" | Motivo: ")
                     .append(cita.motivoCambio);
            }
        }

        return texto.toString();
    }

    /**
     * Confirma una cita propia que aun esta programada.
     */
    @Override
    public ResultadoOperacion confirmarCitaPaciente(int idCita,
                                                    int idPaciente) {

        Cita cita = citas.get(idCita);
        String motivo = motivoNoGestionable(cita, idPaciente);

        if (motivo == null && cita.estado.equals(CONFIRMADA)) {
            motivo = "la cita ya esta confirmada";
        }

        if (motivo == null && cita.estado.equals(CAMBIO_SOLICITADO)) {
            motivo = "ya se ha solicitado un cambio para esta cita";
        }

        if (motivo != null) {
            return ResultadoOperacion.rechazada(
                    "No se pudo confirmar la cita: " + motivo);
        }

        cita.estado = CONFIRMADA;
        return ResultadoOperacion.correcta(
                "Cita confirmada correctamente");
    }

    /**
     * Registra la solicitud de cambio de una cita propia.
     */
    @Override
    public ResultadoOperacion solicitarCambioCitaPaciente(
            int idCita, int idPaciente, String motivoCambio) {

        String error = "No se pudo solicitar el cambio de la cita: ";

        if (motivoCambio == null || motivoCambio.trim().isEmpty()
                || motivoCambio.trim().length()
                        > LONGITUD_MAXIMA_MOTIVO) {
            return ResultadoOperacion.rechazada(error
                    + "el motivo es obligatorio y no puede superar "
                    + LONGITUD_MAXIMA_MOTIVO + " caracteres");
        }

        Cita cita = citas.get(idCita);
        String motivo = motivoNoGestionable(cita, idPaciente);

        if (motivo == null && cita.estado.equals(CAMBIO_SOLICITADO)) {
            motivo = "ya se ha solicitado un cambio para esta cita";
        }

        if (motivo != null) {
            return ResultadoOperacion.rechazada(error + motivo);
        }

        cita.estado = CAMBIO_SOLICITADO;
        cita.motivoCambio = motivoCambio.trim();
        return ResultadoOperacion.correcta(
                "Solicitud de cambio registrada correctamente");
    }

    /**
     * Cancela una cita propia.
     */
    @Override
    public ResultadoOperacion cancelarCitaPaciente(int idCita,
                                                   int idPaciente) {

        Cita cita = citas.get(idCita);
        String motivo = motivoNoGestionable(cita, idPaciente);

        if (motivo != null) {
            return ResultadoOperacion.rechazada(
                    "No se pudo cancelar la cita: " + motivo);
        }

        cita.estado = CANCELADA;
        return ResultadoOperacion.correcta(
                "Cita cancelada correctamente");
    }

    /**
     * Indica por que el paciente no puede gestionar una cita.
     * Una cita se puede gestionar si es suya, no esta cancelada y
     * todavia no ha pasado. Devuelve null si se puede gestionar.
     */
    private String motivoNoGestionable(Cita cita, int idPaciente) {

        if (cita == null || cita.idPaciente != idPaciente) {
            return "la cita no existe o no pertenece a este paciente";
        }

        if (cita.estado.equals(CANCELADA)) {
            return "la cita esta cancelada";
        }

        if (!cita.fechaHora.isAfter(LocalDateTime.now())) {
            return "la cita ya ha pasado";
        }

        return null;
    }
}
