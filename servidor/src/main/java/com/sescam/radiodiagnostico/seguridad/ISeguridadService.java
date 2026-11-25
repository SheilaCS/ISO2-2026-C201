package com.sescam.radiodiagnostico.seguridad;

public interface ISeguridadService {
    // Verifica si un usuario tiene permiso para realizar una acción
    boolean verificarPermiso(String usuario, String rol, String accion);
    
    // Registra una acción en el log de auditoría (Calidad/Trazabilidad)
    boolean registrarAuditoria(String usuario, String accion, String detalles);
}