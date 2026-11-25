package com.sescam.radiodiagnostico.historial;

import com.sescam.radiodiagnostico.autenticacion.Sesion;

/**
 * CONTRATO DEL SERVICIO DE HISTORIAL DE PRUEBAS.
 * Iteracion 12 - RF04.
 *
 * Reglas de acceso:
 * - Sin sesion: se rechaza (NO_AUTENTICADO).
 * - Paciente: solo puede consultar su propio historial, el del
 *   numero de paciente vinculado a su usuario (IVinculoPaciente).
 * - Medico: puede consultar el de cualquier paciente.
 * - Resto de roles: se rechaza (PROHIBIDO).
 * Cada consulta aceptada o rechazada queda registrada.
 */
public interface IHistorialService {

    ResultadoHistorial consultarHistorial(Sesion sesion, String idPaciente);
}
