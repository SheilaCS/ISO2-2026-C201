package com.sescam.radiodiagnostico.historial;

import java.util.List;

/**
 * ORIGEN DE LAS PRUEBAS REALIZADAS DE UN PACIENTE.
 * Iteracion 12 - RF04.
 *
 * Define lo que necesita el historial del registro de pruebas
 * realizadas (RF10, CDU7). Mientras ese caso de uso no exista, se usa
 * un origen simulado en memoria. Si el origen no esta disponible,
 * las implementaciones lanzan IllegalStateException.
 */
public interface IHistorialPruebasProveedor {

    // Pruebas realizadas del paciente (lista vacia si no tiene ninguna)
    List<PruebaRealizada> pruebasDePaciente(String idPaciente);
}
