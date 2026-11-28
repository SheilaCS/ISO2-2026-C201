package com.sescam.radiodiagnostico.seguridad;

import java.util.List;

/**
 * Auditoría de actividades y accesos a datos clínicos (CDU11, RF19)
 */
public interface ISeguridadService {

    /**
     * Registra en la auditoría una acción realizada por un usuario.
     *
     * @return true si se ha registrado, false si los datos no son válidos
     */
    boolean registrarAuditoria(String usuario, String accion, String detalles);

    /**
     * Indica si el usuario tiene el permiso CONSULTAR_AUDITORIA (RF18).
     */
    boolean puedeConsultarAuditoria(String usuario);

    /**
     * Devuelve los registros de auditoría, del más antiguo al más reciente.
     */
    List<String> consultarLogs();
}