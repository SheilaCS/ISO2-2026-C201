package com.sescam.radiodiagnostico.historial;

import java.util.Optional;

/**
 * VINCULO ENTRE UN USUARIO PACIENTE Y SU NUMERO DE PACIENTE.
 * Iteracion 12 - RF04.
 *
 * El usuario con el que se inicia sesion (por ejemplo
 * "paciente.demo") no es el identificador con el que el resto de
 * modulos conoce al paciente (por ejemplo 1). Esta interfaz dice
 * a que paciente corresponde cada usuario, de modo que el servicio
 * del historial no se fie del identificador que envia el cliente.
 */
public interface IVinculoPaciente {

    // Numero de paciente del usuario; vacio si no tiene ninguno
    Optional<String> idPacienteDe(String usuario);
}
