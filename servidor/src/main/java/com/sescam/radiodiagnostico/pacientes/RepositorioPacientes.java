package com.sescam.radiodiagnostico.pacientes;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioPacientes {

    Optional<Paciente> buscarPorId(UUID id);

    Optional<Paciente> buscarPorCip(String cip);

    void guardar(Paciente paciente);
}