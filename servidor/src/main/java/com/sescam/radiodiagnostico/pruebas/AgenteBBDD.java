package com.sescam.radiodiagnostico.pruebas;

import java.util.ArrayList;
import java.util.List;

/**
 * CAPA DE PERSISTENCIA - CDU 8
 * Simula el acceso a la base de datos guardando los informes en memoria.
 */
public class AgenteBBDD {
    private List<Informe> listaInformes = new ArrayList<>();

    public boolean guardarInforme(Informe informe) {
        if (informe != null) {
            listaInformes.add(informe);
            return true;
        }
        return false;
    }

    public int obtenerCantidadInformes() {
        return listaInformes.size();
    }
}