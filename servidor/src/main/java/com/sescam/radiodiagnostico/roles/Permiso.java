package com.sescam.radiodiagnostico.roles;

/**
 * Acciones del sistema que se pueden conceder a un rol (CDU10, RF18).
 */
public enum Permiso {
    GESTIONAR_ROLES,
    GESTIONAR_PACIENTES,
    GESTIONAR_CITAS,
    GESTIONAR_AGENDAS,
    SOLICITAR_PRUEBAS,
    REGISTRAR_REALIZACION,
    CONSULTAR_HISTORIAL,
    CONSULTAR_MIS_CITAS,
    CONFIGURAR_PARAMETROS,
    CONSULTAR_AUDITORIA
}