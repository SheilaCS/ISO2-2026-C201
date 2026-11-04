
package com.sescam.radiodiagnostico.citas;

/**
 * CONTRATO DEL SERVICIO DE GESTIÓN DE CITAS.
 * Iteración 3 - RF13.
 *
 * Define las operaciones que debe ofrecer el servidor
 * para gestionar las citas de radiodiagnóstico.
 */
public interface IGestionCitasService {

    // Asignar una cita a una solicitud médica
    boolean asignarCita(int idSolicitud, int idPaciente,
                        String fecha, String hora);

    // Reprogramar una cita existente
    boolean reprogramarCita(int idCita, String nuevaFecha,
                           String nuevaHora);

    // Cancelar una cita existente
    boolean cancelarCita(int idCita);

    // Consultar el estado de una cita
    String consultarCita(int idCita);
}
