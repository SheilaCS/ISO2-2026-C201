package com.sescam.radiodiagnostico.cdu08_informes;
import java.util.List;
public interface IGestionPruebasService {
    boolean crearInforme(String idPrueba, String texto, String incidencias);
    List<String> obtenerImagenes(String idPrueba);
}