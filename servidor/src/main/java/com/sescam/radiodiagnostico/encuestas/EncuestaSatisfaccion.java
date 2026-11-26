package com.sescam.radiodiagnostico.encuestas;

import java.time.LocalDateTime;
public record EncuestaSatisfaccion(int idPaciente, int idCita, int puntuacion,
                                  String comentario, LocalDateTime fecha) {}
