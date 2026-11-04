package com.sescam.radiodiagnostico.pruebas;

import java.util.List;

/**
 * CAPA DE DOMINIO - CDU 8
 * Implementa la lógica de negocio y se comunica con la persistencia.
 */
public class GestionPruebasServiceImpl implements IGestionPruebasService {
    
    private AgenteBBDD agente = new AgenteBBDD();

    @Override
    public boolean crearInforme(String idPrueba, String textoInforme) {
        // 1. Validación
        if (idPrueba == null || textoInforme == null || idPrueba.isEmpty()) {
            return false; 
        }
        
        // 2. Creamos la entidad Informe
        Informe nuevoInforme = new Informe(idPrueba, textoInforme, "");
        
        // 3. Lo guardamos en el agente
        return agente.guardarInforme(nuevoInforme);
    }

    @Override
    public boolean registrarRealizacion(String idPrueba, String incidencias) {
        return true; 
    }

    @Override
    public List<String> obtenerImagenes(String idPrueba) {
        return List.of("imagen1.dicom", "imagen2.dicom");
    }

    @Override
    public double calcularTiempoMedioEspera(String idMedico) {
        return 3.5; 
    }
}