package com.sescam.radiodiagnostico.solicitudes;

import java.util.Optional;

/**
 * Contrato del servicio de solicitudes de pruebas (CDU2, RF06).
 */
public interface ISolicitudService {

    /**
     * Registra una nueva solicitud.
     *
     * @throws IllegalArgumentException si algún dato no es válido
     */
    Solicitud solicitarPrueba(String medico, String idPaciente, String tipoPrueba,
            String indicacionClinica, String justificacion, String prioridad);

    /** Devuelve la solicitud con ese identificador, si existe. */
    Optional<Solicitud> consultarSolicitud(int id);
}
