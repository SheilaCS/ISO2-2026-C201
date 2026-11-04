package com.sescam.radiodiagnostico.cdu02_solicitudes;
public class SolicitudServiceImpl implements ISolicitudService {
    @Override
    public boolean solicitarPrueba(String idPaciente, String tipoPrueba) {
        return idPaciente != null && tipoPrueba != null;
    }
}