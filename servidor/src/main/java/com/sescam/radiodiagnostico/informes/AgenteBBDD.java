package com.sescam.radiodiagnostico.informes;

import java.util.ArrayList;
import java.util.List;

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