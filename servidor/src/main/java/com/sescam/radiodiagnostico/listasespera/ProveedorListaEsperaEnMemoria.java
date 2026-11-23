package com.sescam.radiodiagnostico.listasespera;

import java.util.ArrayList;
import java.util.List;

/**
 * ORIGEN SIMULADO DE SOLICITUDES SIN CITA Y DE HUECOS LIBRES.
 * Iteracion 15 - RF15.
 *
 * Guarda los datos en memoria hasta que los modulos de solicitudes,
 * agendas y citas ofrezcan un listado. Tambien se usa en las pruebas.
 */
public class ProveedorListaEsperaEnMemoria
        implements ISolicitudesPendientesProveedor, IHuecosProveedor {

    private final List<SolicitudEnEspera> solicitudes = new ArrayList<>();
    private final List<Hueco> huecos = new ArrayList<>();

    public synchronized void agregarSolicitud(SolicitudEnEspera solicitud) {
        solicitudes.add(solicitud);
    }

    public synchronized void agregarHueco(Hueco hueco) {
        huecos.add(hueco);
    }

    @Override
    public synchronized List<SolicitudEnEspera> solicitudesSinCita() {
        return new ArrayList<>(solicitudes);
    }

    @Override
    public synchronized List<Hueco> huecosLibres() {
        return new ArrayList<>(huecos);
    }
}
