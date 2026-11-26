package com.sescam.radiodiagnostico.listasespera;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SERVICIO DE LISTAS DE ESPERA Y HUECOS.
 * Iteracion 15 - RF15.
 *
 * Los datos de solicitudes y huecos vienen de los origenes
 * (ISolicitudesPendientesProveedor e IHuecosProveedor). El servicio
 * guarda en memoria lo que gestiona el RF15: las prioridades
 * cambiadas, las solicitudes retiradas o ya asignadas y los huecos
 * que ya se han ocupado.
 */
public class ListaEsperaServiceImpl implements IListaEsperaService {

    private final ISolicitudesPendientesProveedor solicitudes;
    private final IHuecosProveedor huecos;
    private final IAsignadorCitas asignador;

    private final Map<Integer, Prioridad> prioridades = new HashMap<>();
    private final Set<Integer> retiradas = new HashSet<>();
    private final Set<Hueco> ocupados = new HashSet<>();

    public ListaEsperaServiceImpl(ISolicitudesPendientesProveedor solicitudes,
                                  IHuecosProveedor huecos,
                                  IAsignadorCitas asignador) {
        this.solicitudes = solicitudes;
        this.huecos = huecos;
        this.asignador = asignador;
    }

    @Override
    public synchronized List<SolicitudEnEspera> consultarListaEspera() {

        return solicitudes.solicitudesSinCita().stream()
                .filter(s -> !retiradas.contains(s.idSolicitud()))
                .map(s -> prioridades.containsKey(s.idSolicitud())
                        ? s.conPrioridad(prioridades.get(s.idSolicitud()))
                        : s)
                .sorted(Comparator
                        .comparing(SolicitudEnEspera::prioridad)
                        .thenComparing(SolicitudEnEspera::fechaSolicitud)
                        .thenComparingInt(SolicitudEnEspera::idSolicitud))
                .toList();
    }

    @Override
    public synchronized ResultadoOperacion cambiarPrioridad(
            int idSolicitud, Prioridad prioridad) {

        if (prioridad == null) {
            return ResultadoOperacion.rechazada("Prioridad incorrecta.");
        }

        if (buscarSolicitud(idSolicitud).isEmpty()) {
            return ResultadoOperacion.rechazada(
                    "No existe esa solicitud en la lista de espera.");
        }

        prioridades.put(idSolicitud, prioridad);
        return ResultadoOperacion.correcta("Prioridad actualizada.");
    }

    @Override
    public synchronized ResultadoOperacion retirarDeLista(int idSolicitud) {

        if (buscarSolicitud(idSolicitud).isEmpty()) {
            return ResultadoOperacion.rechazada(
                    "No existe esa solicitud en la lista de espera.");
        }

        retiradas.add(idSolicitud);
        return ResultadoOperacion.correcta(
                "Solicitud retirada de la lista de espera.");
    }

    @Override
    public synchronized List<Hueco> consultarHuecos(LocalDate fecha) {

        return huecos.huecosLibres().stream()
                .filter(h -> !ocupados.contains(h))
                .filter(h -> fecha == null || h.fecha().equals(fecha))
                .sorted(Comparator
                        .comparing(Hueco::fecha)
                        .thenComparing(Hueco::inicio)
                        .thenComparingInt(Hueco::idMedico))
                .toList();
    }

    @Override
    public synchronized Optional<Propuesta> proponerAsignacion() {

        Optional<SolicitudEnEspera> solicitud =
                consultarListaEspera().stream().findFirst();
        Optional<Hueco> hueco =
                consultarHuecos(null).stream().findFirst();

        if (solicitud.isPresent() && hueco.isPresent()) {
            return Optional.of(new Propuesta(solicitud.get(), hueco.get()));
        }

        return Optional.empty();
    }

    @Override
    public synchronized ResultadoOperacion asignarHueco(int idSolicitud,
            int idMedico, LocalDate fecha, LocalTime inicio) {

        if (fecha == null || inicio == null) {
            return ResultadoOperacion.rechazada("Datos incorrectos.");
        }

        Optional<SolicitudEnEspera> solicitud = buscarSolicitud(idSolicitud);

        if (solicitud.isEmpty()) {
            return ResultadoOperacion.rechazada(
                    "No existe esa solicitud en la lista de espera.");
        }

        Optional<Hueco> hueco = consultarHuecos(fecha).stream()
                .filter(h -> h.idMedico() == idMedico
                        && h.inicio().equals(inicio))
                .findFirst();

        if (hueco.isEmpty()) {
            return ResultadoOperacion.rechazada(
                    "El hueco no existe o ya esta ocupado.");
        }

        try {
            boolean asignada = asignador.asignarCita(
                    idSolicitud, solicitud.get().idPaciente(), fecha, inicio);

            if (!asignada) {
                return ResultadoOperacion.rechazada(
                        "No se ha podido asignar la cita.");
            }

        } catch (IllegalStateException e) {
            return ResultadoOperacion.rechazada(
                    "El servicio de citas no esta disponible.");
        }

        retiradas.add(idSolicitud);
        ocupados.add(hueco.get());
        return ResultadoOperacion.correcta("Cita asignada correctamente.");
    }

    // Buscar una solicitud que siga en la lista de espera
    private Optional<SolicitudEnEspera> buscarSolicitud(int idSolicitud) {
        return consultarListaEspera().stream()
                .filter(s -> s.idSolicitud() == idSolicitud)
                .findFirst();
    }
}
