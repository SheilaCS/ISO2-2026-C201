package com.sescam.radiodiagnostico.pruebas;

import java.util.List;

/**
 * CONTRATO DEL SERVICIO DE GESTIÓN DE PRUEBAS Y RESULTADOS (Yasmine)
 * Cumple con SOLID (DIP).
 */
public interface IGestionPruebasService {
    // Registrar la realización de la prueba e incidencias (RF10)
    boolean registrarRealizacion(String idPrueba, String incidencias);
    
    // Crear y consultar informes (RF09)
    boolean crearInforme(String idPrueba, String textoInforme);
    List<String> obtenerImagenes(String idPrueba);
    
    // Consultar estadísticas (RF11)
    double calcularTiempoMedioEspera(String idMedico);
}