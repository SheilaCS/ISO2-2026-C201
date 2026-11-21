package com.sescam.radiodiagnostico.miscitas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;

/**
 * CONTRATO DEL SERVICIO DE GESTION DE MIS CITAS.
 * Iteracion 9 - RF02.
 *
 * Define las operaciones que el paciente puede realizar
 * sobre sus citas programadas. Cada operacion devuelve si se ha
 * realizado y un mensaje que explica el motivo si se rechaza.
 */
public interface IGestionMisCitasService {

    // Consultar las citas de un paciente
    String consultarMisCitas(int idPaciente);

    // Confirmar la asistencia a una cita
    ResultadoOperacion confirmarCita(int idCita, int idPaciente);

    // Solicitar el cambio de una cita
    ResultadoOperacion solicitarCambioCita(int idCita, int idPaciente,
                                           String motivo);

    // Cancelar una cita
    ResultadoOperacion cancelarCita(int idCita, int idPaciente);
}
