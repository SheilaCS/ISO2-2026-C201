package com.sescam.radiodiagnostico.cdu02_solicitudes;

public class SolicitudServiceImpl implements ISolicitudService {
    
    @Override
    public boolean solicitarPrueba(String idPaciente, String tipoPrueba, String prioridad) {
        // 1. Validar que el paciente y el tipo no sean nulos o vacíos
        if (idPaciente == null || idPaciente.trim().isEmpty()) {
            return false;
        }
        if (tipoPrueba == null || tipoPrueba.trim().isEmpty()) {
            return false;
        }
        
        // 2. Validar que la prioridad sea correcta (RF06)
        if (prioridad == null || 
            (!prioridad.equals("URGENTE") && 
             !prioridad.equals("PREFERENTE") && 
             !prioridad.equals("ORDINARIO"))) {
            return false;
        }

        // Si pasa todas las validaciones, se registra
        return true;
    }
}