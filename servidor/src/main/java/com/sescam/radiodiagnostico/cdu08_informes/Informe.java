package com.sescam.radiodiagnostico.cdu08_informes;
public class Informe {
    private String idPrueba; private String texto; private String incidencias;
    public Informe(String idPrueba, String texto, String incidencias) {
        this.idPrueba = idPrueba; this.texto = texto; this.incidencias = incidencias;
    }
}