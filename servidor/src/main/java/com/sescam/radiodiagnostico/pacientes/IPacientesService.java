package com.sescam.radiodiagnostico.pacientes;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IPacientesService {

    Paciente altaPaciente(
            String cip,
            String nombre,
            String apellidos,
            LocalDate fechaNacimiento
    );

    Optional<Paciente> consultarPorCip(String cip);

    Paciente modificarPaciente(
            UUID id,
            String nombre,
            String apellidos,
            LocalDate fechaNacimiento
    );

    void bajaPaciente(UUID id);
}