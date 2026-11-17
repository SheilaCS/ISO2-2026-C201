package com.sescam.radiodiagnostico.informes;

import java.util.ArrayList;
import java.util.List;

public class InformeServiceImpl implements IInformeService {
    private AgenteBBDD agente = new AgenteBBDD();
    private List<String> logs = new ArrayList<>();

    @Override
    public boolean crearInforme(String idPrueba, String texto, String incidencias) {
        if (idPrueba == null || texto == null) return false;
        Informe informe = new Informe(idPrueba, texto, incidencias);
        logs.add("Informe creado: " + idPrueba);
        return agente.guardarInforme(informe);
    }

    @Override
    public List<String> obtenerImagenes(String idPrueba) {
        return List.of("img1.dicom", "img2.dicom");
    }

    @Override
    public boolean registrarRealizacion(String idPrueba, String incidencias) {
        return true; 
    }

    @Override
    public List<String> obtenerAuditoria() {
        return logs;
    }

    @Override
    public int obtenerTotalInformes() {
        return agente.obtenerCantidadInformes();
    }
}