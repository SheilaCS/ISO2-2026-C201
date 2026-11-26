package com.sescam.radiodiagnostico.encuestas;

/** Contrato de consulta de las iteraciones 7 y 9. */
public interface IEncuestasProveedor {
    boolean citaPertenecePaciente(int idCita, int idPaciente);
    boolean pruebaRealizada(int idCita);
}
