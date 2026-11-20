
package com.sescam.radiodiagnostico.citas;

/**
 * CONTRATO DEL SERVICIO DE GESTIÓN DE CITAS.
 * Iteración 3 - RF13.
 *
 * Define las operaciones que debe ofrecer el servidor
 * para gestionar las citas de radiodiagnóstico.
 *
 * Iteración 9 - RF02.
 * Se añaden las operaciones que usa el paciente para gestionar
 * sus propias citas. Se definen como métodos por defecto para no
 * obligar a las implementaciones anteriores (por ejemplo, los
 * servicios simulados de las pruebas) a implementarlas.
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

    // -----------------------------------------
    // OPERACIONES DEL PACIENTE (RF02)
    // -----------------------------------------

    // Listar las citas de un paciente (una por línea)
    default String listarCitasPaciente(int idPaciente) {
        return "";
    }

    // Confirmar una cita propia
    default ResultadoOperacion confirmarCitaPaciente(int idCita,
                                                     int idPaciente) {
        return ResultadoOperacion.rechazada(
                "Operacion no disponible");
    }

    // Solicitar el cambio de una cita propia
    default ResultadoOperacion solicitarCambioCitaPaciente(
            int idCita, int idPaciente, String motivo) {
        return ResultadoOperacion.rechazada(
                "Operacion no disponible");
    }

    // Cancelar una cita propia
    default ResultadoOperacion cancelarCitaPaciente(int idCita,
                                                    int idPaciente) {
        return ResultadoOperacion.rechazada(
                "Operacion no disponible");
    }
}
