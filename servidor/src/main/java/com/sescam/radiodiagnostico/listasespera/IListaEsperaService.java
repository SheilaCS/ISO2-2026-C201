package com.sescam.radiodiagnostico.listasespera;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * CONTRATO DEL SERVICIO DE LISTAS DE ESPERA Y HUECOS.
 * Iteracion 15 - RF15.
 *
 * Permite al personal administrativo ver las solicitudes sin cita
 * ordenadas por prioridad, cambiar su prioridad, retirarlas de la
 * lista, consultar los huecos libres y asignar un hueco a una solicitud.
 */
public interface IListaEsperaService {

    // Solicitudes sin cita, de mas a menos prioritaria y, a igual
    // prioridad, de mas antigua a mas reciente
    List<SolicitudEnEspera> consultarListaEspera();

    // Cambiar la prioridad de una solicitud de la lista
    ResultadoOperacion cambiarPrioridad(int idSolicitud, Prioridad prioridad);

    // Retirar una solicitud de la lista de espera
    ResultadoOperacion retirarDeLista(int idSolicitud);

    // Huecos libres de una fecha (todos si la fecha es null)
    List<Hueco> consultarHuecos(LocalDate fecha);

    // Solicitud mas prioritaria con el primer hueco libre, si hay ambos
    Optional<Propuesta> proponerAsignacion();

    // Asignar a una solicitud de la lista un hueco libre
    ResultadoOperacion asignarHueco(int idSolicitud, int idMedico,
                                    LocalDate fecha, LocalTime inicio);
}
