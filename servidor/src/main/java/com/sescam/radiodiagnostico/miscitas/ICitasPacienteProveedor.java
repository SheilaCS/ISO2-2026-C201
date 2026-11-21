package com.sescam.radiodiagnostico.miscitas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;

/**
 * ORIGEN DE LAS CITAS DEL PACIENTE.
 * Iteracion 9 - RF02.
 *
 * Define lo que necesita "Mis citas" del modulo de citas (RF13).
 * La implementacion normal consulta al servidor de citas por HTTP.
 * En las pruebas se puede sustituir por un origen simulado.
 *
 * Si el servidor de citas no esta disponible, las implementaciones
 * lanzan IllegalStateException.
 */
public interface ICitasPacienteProveedor {

    // Obtener las citas del paciente, una por linea
    // (texto vacio si no tiene ninguna)
    String listarCitasPaciente(int idPaciente);

    // Confirmar una cita propia
    ResultadoOperacion confirmarCita(int idCita, int idPaciente);

    // Solicitar el cambio de una cita propia
    ResultadoOperacion solicitarCambioCita(int idCita, int idPaciente,
                                           String motivo);

    // Cancelar una cita propia
    ResultadoOperacion cancelarCita(int idCita, int idPaciente);
}
