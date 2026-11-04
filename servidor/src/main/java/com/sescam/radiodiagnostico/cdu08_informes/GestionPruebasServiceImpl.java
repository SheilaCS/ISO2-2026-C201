package com.sescam.radiodiagnostico.cdu08_informes;
import java.util.List;
public class GestionPruebasServiceImpl implements IGestionPruebasService {
    @Override
    public boolean crearInforme(String idPrueba, String texto, String incidencias) {
        return true; // Lógica simulada
    }
    @Override
    public List<String> obtenerImagenes(String idPrueba) {
        return List.of("img1.dicom", "img2.dicom");
    }
}