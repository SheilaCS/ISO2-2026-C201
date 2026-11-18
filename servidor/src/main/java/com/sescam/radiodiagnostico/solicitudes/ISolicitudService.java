package com.sescam.radiodiagnostico.solicitudes;

public interface ISolicitudService {
    boolean solicitarPrueba(String idPaciente, String tipoPrueba, String prioridad);
}