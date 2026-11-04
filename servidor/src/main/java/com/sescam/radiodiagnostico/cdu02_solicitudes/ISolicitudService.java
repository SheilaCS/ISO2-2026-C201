package com.sescam.radiodiagnostico.cdu02_solicitudes;
public interface ISolicitudService {
    boolean solicitarPrueba(String idPaciente, String tipoPrueba);
}