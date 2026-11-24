package com.sescam.radiodiagnostico.realizacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RealizacionServiceImpl implements IRealizacionService {

    private final Map<Integer, RealizacionPrueba> realizacionesPorCita = new ConcurrentHashMap<>();
    private final IRealizacionProveedor proveedor;

    public RealizacionServiceImpl(IRealizacionProveedor proveedor) {
        this.proveedor = java.util.Objects.requireNonNull(proveedor);
    }

    @Override
    public RealizacionPrueba registrarRealizacion(
            int idCita,
            int idSolicitud,
            LocalDate fechaRealizacion,
            String observaciones,
            String incidencia) {

        if (idCita <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException("El ID de cita y solicitud deben ser positivos.");
        }
        if (fechaRealizacion == null || fechaRealizacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de realizacion es obligatoria y no puede ser futura.");
        }
        String obsLimpia = observaciones == null ? "" : observaciones.trim();
        String incLimpia = incidencia == null ? "" : incidencia.trim();
        if (obsLimpia.isEmpty() && incLimpia.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar observaciones o una incidencia.");
        }
        if (!proveedor.existeCita(idCita)) {
            throw new IllegalArgumentException("La cita indicada no existe.");
        }
        if (!proveedor.correspondeCitaASolicitud(idCita, idSolicitud)) {
            throw new IllegalArgumentException("La cita indicada no corresponde a la solicitud.");
        }
        if (realizacionesPorCita.containsKey(idCita)) {
            throw new IllegalStateException("Ya existe una realizacion registrada para esa cita.");
        }

        String estado = incLimpia.isEmpty() ? "REALIZADA" : "REALIZADA_CON_INCIDENCIA";
        RealizacionPrueba registro = new RealizacionPrueba(
                UUID.randomUUID(),
                idCita,
                idSolicitud,
                fechaRealizacion,
                obsLimpia,
                incLimpia,
                estado
        );
        realizacionesPorCita.put(idCita, registro);
        return registro;
    }

    @Override
    public Optional<RealizacionPrueba> consultarPorCita(int idCita) {
        if (idCita <= 0) {
            throw new IllegalArgumentException("El ID de cita debe ser positivo.");
        }
        return Optional.ofNullable(realizacionesPorCita.get(idCita));
    }

    @Override
    public List<RealizacionPrueba> listarRealizaciones() {
        return new ArrayList<>(realizacionesPorCita.values());
    }
}