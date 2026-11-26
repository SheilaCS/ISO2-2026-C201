package com.sescam.radiodiagnostico.encuestas;

import java.time.LocalDateTime;
public record IncidenciaPaciente(int id, int idPaciente, int idCita,
                                String descripcion, LocalDateTime fecha) {}
