package com.sescam.radiodiagnostico.cdu14_estado_solicitudes;
public class EstadoServiceImpl implements IEstadoService {
    @Override
    public String consultarEstado(String idSolicitud) {
        return "Pendiente de asignación de cita";
    }
}