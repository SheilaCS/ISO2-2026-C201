package com.sescam.radiodiagnostico.listasespera;

import java.util.List;

/**
 * ORIGEN DE LAS SOLICITUDES SIN CITA.
 * Iteracion 15 - RF15.
 *
 * Define lo que necesita la lista de espera del modulo de solicitudes
 * (RF06), que todavia no ofrece un listado. Mientras tanto se usa un
 * origen simulado en memoria. Si el origen no esta disponible, las
 * implementaciones lanzan IllegalStateException.
 */
public interface ISolicitudesPendientesProveedor {

    // Solicitudes que todavia no tienen cita (lista vacia si no hay)
    List<SolicitudEnEspera> solicitudesSinCita();
}
