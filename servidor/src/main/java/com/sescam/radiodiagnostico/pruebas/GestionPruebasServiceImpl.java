package com.sescam.radiodiagnostico.pruebas;

import java.util.ArrayList;
import java.util.List;

/**
 * IMPLEMENTACIÓN DEL SERVICIO DE PRUEBAS (Miriam y Yasmine)
 */
public class GestionPruebasServiceImpl implements IGestionPruebasService {

    @Override
    public boolean registrarRealizacion(String idPrueba, String incidencias) {
        return true; // Lógica simulada
    }

    @Override
    public boolean crearInforme(String idPrueba, String textoInforme) {
        if (idPrueba == null || textoInforme == null) return false;
        return true;
    }

    @Override
    public List<String> obtenerImagenes(String idPrueba) {
        List<String> imagenes = new ArrayList<>();
        imagenes.add("imagen_" + idPrueba + ".dicom");
        return imagenes;
    }

    @Override
    public double calcularTiempoMedioEspera(String idMedico) {
        return 3.5; // Simulación
    }
}