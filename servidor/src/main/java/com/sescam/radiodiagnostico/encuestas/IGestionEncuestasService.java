package com.sescam.radiodiagnostico.encuestas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
public interface IGestionEncuestasService {
    ResultadoOperacion comunicarIncidencia(int idPaciente, int idCita, String descripcion);
    ResultadoOperacion responderEncuesta(int idPaciente, int idCita, int puntuacion, String comentario);
}
