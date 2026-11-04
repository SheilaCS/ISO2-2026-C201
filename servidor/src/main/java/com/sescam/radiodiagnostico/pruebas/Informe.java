package com.sescam.radiodiagnostico.pruebas;

import java.util.Date;

/**
 * ENTIDAD DE DOMINIO - CDU 8 (Informes)
 * Representa el informe médico que se genera tras una prueba.
 */
public class Informe {
    private String idPrueba;
    private String texto;
    private Date fechaCreacion;
    private String incidencias;

    public Informe(String idPrueba, String texto, String incidencias) {
        this.idPrueba = idPrueba;
        this.texto = texto;
        this.incidencias = incidencias;
        this.fechaCreacion = new Date();
    }

    public String getIdPrueba() { return idPrueba; }
    public String getTexto() { return texto; }
    public String getIncidencias() { return incidencias; }
    public Date getFechaCreacion() { return fechaCreacion; }
}