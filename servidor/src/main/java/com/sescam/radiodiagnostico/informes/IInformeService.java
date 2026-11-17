package com.sescam.radiodiagnostico.informes;

import java.util.List;

public interface IInformeService {
    boolean crearInforme(String idPrueba, String texto, String incidencias);
    List<String> obtenerImagenes(String idPrueba);
    boolean registrarRealizacion(String idPrueba, String incidencias);
    List<String> obtenerAuditoria(); 
    int obtenerTotalInformes(); 
}