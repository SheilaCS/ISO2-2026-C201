package com.sescam.radiodiagnostico.seguridad;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SeguridadServiceImpl implements ISeguridadService {

    private final List<String> logsAuditoria = new ArrayList<>();

    @Override
    public boolean verificarPermiso(String usuario, String rol, String accion) {
        if (usuario == null || rol == null || accion == null) return false;
        
        if (accion.equals("GESTIONAR_USUARIOS") && !rol.equalsIgnoreCase("ADMINISTRADOR")) {
            return false;
        }
        
        if (accion.equals("VER_HISTORIAL") && rol.equalsIgnoreCase("PACIENTE")) {
            return true; 
        }

        return true; 
    }

    @Override
    public boolean registrarAuditoria(String usuario, String accion, String detalles) {
        if (usuario == null || accion == null) return false;
        
        String log = LocalDateTime.now() + " | Usuario: " + usuario + " | Acción: " + accion + " | Detalles: " + detalles;
        logsAuditoria.add(log);
        
        System.out.println("AUDITORÍA REGISTRADA: " + log);
        return true;
    }

    public String obtenerLogs() {
        if (logsAuditoria.isEmpty()) return "No hay registros de auditoría.";
        return String.join("\n", logsAuditoria);
    }
}