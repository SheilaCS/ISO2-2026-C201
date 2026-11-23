package com.sescam.radiodiagnostico.pacientes;

import java.time.LocalDate;
import java.util.UUID;

public record Paciente(
        UUID id,
        String cip,
        String nombre,
        String apellidos,
        LocalDate fechaNacimiento,
        boolean activo
) {
}