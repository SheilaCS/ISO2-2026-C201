package com.sescam.radiodiagnostico.informes;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚙️ IMPLEMENTACIÓN DEL SERVICIO DE INFORMES
 */
public class InformeServiceImpl implements IInformeService {

    @Override
    public String consultarEstadoSolicitud(String idSolicitud) {
        return "🟢 Estado de la solicitud " + idSolicitud + ": PENDIENTE DE REALIZACIÓN";
    }

    @Override
    public boolean crearInforme(String idSolicitud, String textoInforme) {
        if (idSolicitud == null || textoInforme == null) return false;
        return true;
    }

    @Override
    public List<String> obtenerImagenes(String idSolicitud) {
        List<String> imagenes = new ArrayList<>();
        imagenes.add("imagen_" + idSolicitud + "_frontal.dicom");
        imagenes.add("imagen_" + idSolicitud + "_lateral.dicom");
        return imagenes;
    }

    @Override
    public boolean registrarRealizacion(String idSolicitud, String incidencias) {
        return true;
    }

    @Override
    public double calcularTiempoMedioEspera(String idMedico) {
        return 3.5; // Simulación
    }

    @Override
    public List<String> consultarHistorialSeguimiento(String idPaciente) {
        List<String> seguimiento = new ArrayList<>();
        seguimiento.add("Revisión de nódulos - 12/10/2026");
        return seguimiento;
    }
}