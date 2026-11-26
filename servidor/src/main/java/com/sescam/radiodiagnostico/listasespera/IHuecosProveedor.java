package com.sescam.radiodiagnostico.listasespera;

import java.util.List;

/**
 * ORIGEN DE LOS HUECOS LIBRES.
 * Iteracion 15 - RF15.
 *
 * Define lo que necesita la gestion de huecos de las agendas (RF14)
 * y de las citas (RF13): los horarios creados que no tienen cita.
 * Mientras esos modulos no ofrezcan un listado se usa un origen
 * simulado en memoria. Si no esta disponible, las implementaciones
 * lanzan IllegalStateException.
 */
public interface IHuecosProveedor {

    // Huecos libres de todos los medicos (lista vacia si no hay)
    List<Hueco> huecosLibres();
}
