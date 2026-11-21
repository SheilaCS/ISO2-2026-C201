package com.sescam.radiodiagnostico.pacientes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RepositorioPacientesEnMemoria
        implements RepositorioPacientes {

    private final Map<UUID, Paciente> pacientes = new HashMap<>();

    @Override
    public synchronized Optional<Paciente> buscarPorId(UUID id) {
        return Optional.ofNullable(pacientes.get(id));
    }

    @Override
    public synchronized Optional<Paciente> buscarPorCip(String cip) {
        return pacientes.values().stream()
                .filter(paciente -> paciente.cip().equals(cip))
                .findFirst();
    }

    @Override
    public synchronized void guardar(Paciente paciente) {
        pacientes.put(paciente.id(), paciente);
    }
}