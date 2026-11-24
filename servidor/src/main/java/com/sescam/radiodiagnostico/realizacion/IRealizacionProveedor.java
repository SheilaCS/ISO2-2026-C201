package com.sescam.radiodiagnostico.realizacion;

/**
 * Proveedor desacoplado para verificar citas/solicitudes sin acoplar
 * el modulo de realizacion a CDU2 ni CDU3.
 */
public interface IRealizacionProveedor {

    /**
     * Comprueba si existe una cita valida para registrar su realizacion.
     *
     * @param idCita identificador de la cita
     * @return true si la cita existe y es valida
     * @throws IllegalStateException si el servicio externo no esta disponible
     */
    boolean existeCita(int idCita);
}