package com.sescam.radiodiagnostico.miscitas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;

/**
 * IMPLEMENTACION DEL SERVICIO DE GESTION DE MIS CITAS.
 * Iteracion 9 - RF02.
 *
 * Las citas las crea el personal administrativo (RF13, CDU3) y
 * se guardan en el modulo de citas. Este servicio valida los datos
 * que envia el paciente y consulta o modifica sus citas a traves
 * de ICitasPacienteProveedor (por defecto, el servidor de citas).
 */
public class GestionMisCitasServiceImpl
        implements IGestionMisCitasService {

    // Longitud maxima del motivo de un cambio
    private static final int LONGITUD_MAXIMA_MOTIVO = 200;

    // Mensaje cuando un ID no es un entero mayor que 0
    private static final String ERROR_ID =
            "los ID deben ser enteros mayores que 0";

    private final ICitasPacienteProveedor citas;

    // Constructor normal: consulta al servidor de citas
    public GestionMisCitasServiceImpl() {
        this(new ProveedorCitasHttp());
    }

    // Constructor para indicar de donde salen las citas
    public GestionMisCitasServiceImpl(ICitasPacienteProveedor citas) {
        this.citas = citas;
    }

    @Override
    public String consultarMisCitas(int idPaciente) {

        if (idPaciente <= 0) {
            return "ID de paciente no válido";
        }

        String listado = citas.listarCitasPaciente(idPaciente);

        if (listado.trim().isEmpty()) {
            return "No hay citas para este paciente";
        }

        return listado;
    }

    @Override
    public ResultadoOperacion confirmarCita(int idCita,
                                            int idPaciente) {

        if (idCita <= 0 || idPaciente <= 0) {
            return ResultadoOperacion.rechazada(
                    "No se pudo confirmar la cita: " + ERROR_ID);
        }

        return citas.confirmarCita(idCita, idPaciente);
    }

    @Override
    public ResultadoOperacion solicitarCambioCita(int idCita,
                                                  int idPaciente,
                                                  String motivo) {

        String error = "No se pudo solicitar el cambio de la cita: ";

        if (idCita <= 0 || idPaciente <= 0) {
            return ResultadoOperacion.rechazada(error + ERROR_ID);
        }

        if (motivo == null
                || motivo.trim().isEmpty()
                || motivo.trim().length() > LONGITUD_MAXIMA_MOTIVO) {
            return ResultadoOperacion.rechazada(error
                    + "el motivo es obligatorio y no puede superar "
                    + LONGITUD_MAXIMA_MOTIVO + " caracteres");
        }

        return citas.solicitarCambioCita(
                idCita, idPaciente, motivo.trim());
    }

    @Override
    public ResultadoOperacion cancelarCita(int idCita,
                                           int idPaciente) {

        if (idCita <= 0 || idPaciente <= 0) {
            return ResultadoOperacion.rechazada(
                    "No se pudo cancelar la cita: " + ERROR_ID);
        }

        return citas.cancelarCita(idCita, idPaciente);
    }
}
