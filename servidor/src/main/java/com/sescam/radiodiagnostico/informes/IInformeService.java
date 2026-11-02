package com.sescam.radiodiagnostico.informes;

import java.util.List;

/**
 * 📝 CONTRATO DEL SERVICIO DE INFORMES Y RESULTADOS
 * Cumple con el Principio de Inversión de Dependencias (DIP - SOLID).
 */
public interface IInformeService {
    // 🔍 RF08
    String consultarEstadoSolicitud(String idSolicitud);
    // 🖼️ RF09
    boolean crearInforme(String idSolicitud, String textoInforme);
    List<String> obtenerImagenes(String idSolicitud);
    // 🏥 RF10
    boolean registrarRealizacion(String idSolicitud, String incidencias);
    // 📊 RF11
    double calcularTiempoMedioEspera(String idMedico);
    // 💬 RF22
    List<String> consultarHistorialSeguimiento(String idPaciente);
}