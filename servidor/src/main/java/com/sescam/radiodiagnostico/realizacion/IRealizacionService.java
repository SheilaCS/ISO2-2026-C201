package com.sescam.radiodiagnostico.realizacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IRealizacionService {

    RealizacionPrueba registrarRealizacion(
            int idCita,
            int idSolicitud,
            LocalDate fechaRealizacion,
            String observaciones,
            String incidencia
    );

    Optional<RealizacionPrueba> consultarPorCita(int idCita);

    List<RealizacionPrueba> listarRealizaciones();
}