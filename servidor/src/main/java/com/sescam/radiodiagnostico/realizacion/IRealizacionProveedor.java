package com.sescam.radiodiagnostico.realizacion;

public interface IRealizacionProveedor {

    boolean existeCita(int idCita);

    default boolean correspondeCitaASolicitud(int idCita, int idSolicitud) {
        return true;
    }
}