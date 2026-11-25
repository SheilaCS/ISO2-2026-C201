package com.sescam.radiodiagnostico.historial;

import java.util.List;

/**
 * ORIGEN DE LAS PRUEBAS REALIZADAS DE UN PACIENTE.
 * Iteracion 12 - RF04.
 *
 * Define lo que necesita el historial del registro de pruebas
 * realizadas (CDU7). El origen real es ProveedorHistorialRealizacion;
 * ProveedorHistorialEnMemoria solo se usa en las pruebas. Si el
 * origen no esta disponible, las implementaciones lanzan
 * IllegalStateException.
 */
public interface IHistorialPruebasProveedor {

    // Pruebas realizadas del paciente (lista vacia si no tiene ninguna)
    List<PruebaRealizada> pruebasDePaciente(String idPaciente);
}
