package com.sescam.radiodiagnostico.informes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Informe de una prueba realizada a un paciente, con sus imágenes (CDU8, RF09).
 * Las imágenes se guardan como referencias (por ejemplo, identificadores DICOM).
 */
public record Informe(
        int id,
        int idPaciente,
        int idPrueba,
        String medico,
        String tipoPrueba,
        String contenido,
        List<String> imagenes,
        LocalDateTime fecha) {

    public Informe {
        imagenes = List.copyOf(imagenes);
    }
}
