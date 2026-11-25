package com.sescam.radiodiagnostico.solicitudes;

public class SolicitudServiceImpl implements ISolicitudService {
    
    @Override
    public boolean solicitarPrueba(String idPaciente, String tipoPrueba, String prioridad) {
        
        // 1. Validar tipo de prueba
        if (tipoPrueba == null || tipoPrueba.trim().isEmpty()) {
            return false;
        }
        
        // 2. Validar prioridad (Blindado contra nulos, espacios y minúsculas)
        if (prioridad == null) {
            return false;
        }

        String prioridadLimpia = prioridad.trim().toUpperCase();
        
        if (!prioridadLimpia.equals("URGENTE") && 
            !prioridadLimpia.equals("PREFERENTE") && 
            !prioridadLimpia.equals("ORDINARIO")) {
            return false;
        }

        // Si pasa todas las validaciones, se registra
        return true;
    }
}