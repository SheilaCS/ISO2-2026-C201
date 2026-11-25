package com.sescam.radiodiagnostico.informes;

public interface IInformeService {
    // Devuelve true si el informe se genera correctamente
    boolean generarInforme(int idEquipo, String fecha, String descripcion, String rol);
    
    // Devuelve el texto del informe si existe
    String consultarInforme(int idInforme, String rol);
}