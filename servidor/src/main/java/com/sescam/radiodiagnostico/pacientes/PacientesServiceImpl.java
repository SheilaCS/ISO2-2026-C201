package com.sescam.radiodiagnostico.pacientes;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PacientesServiceImpl implements IPacientesService {

    private final RepositorioPacientes repositorio;

    public PacientesServiceImpl(RepositorioPacientes repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    @Override
    public Paciente altaPaciente(
            String cip,
            String nombre,
            String apellidos,
            LocalDate fechaNacimiento) {

        String cipNormalizado = normalizarCip(cip);
        String nombreValidado = validarTexto(nombre, "El nombre");
        String apellidosValidados = validarTexto(apellidos, "Los apellidos");
        validarFechaNacimiento(fechaNacimiento);

        // La comprobación del duplicado y el guardado son una sola operación.
        synchronized (repositorio) {
            if (repositorio.buscarPorCip(cipNormalizado).isPresent()) {
                throw new PacienteException(
                        PacienteException.Codigo.CIP_DUPLICADO,
                        "Ya existe un paciente con ese CIP."
                );
            }

            Paciente paciente = new Paciente(
                    UUID.randomUUID(),
                    cipNormalizado,
                    nombreValidado,
                    apellidosValidados,
                    fechaNacimiento,
                    true
            );

            repositorio.guardar(paciente);
            return paciente;
        }
    }

    @Override
    public Optional<Paciente> consultarPorCip(String cip) {
        String cipNormalizado = normalizarCip(cip);

        synchronized (repositorio) {
            return repositorio.buscarPorCip(cipNormalizado);
        }
    }

    @Override
    public Paciente modificarPaciente(
            UUID id,
            String nombre,
            String apellidos,
            LocalDate fechaNacimiento) {

        validarId(id);
        String nombreValidado = validarTexto(nombre, "El nombre");
        String apellidosValidados = validarTexto(apellidos, "Los apellidos");
        validarFechaNacimiento(fechaNacimiento);

        synchronized (repositorio) {
            Paciente actual = obtenerPacienteActivo(id);

            Paciente actualizado = new Paciente(
                    actual.id(),
                    actual.cip(),
                    nombreValidado,
                    apellidosValidados,
                    fechaNacimiento,
                    actual.activo()
            );

            repositorio.guardar(actualizado);
            return actualizado;
        }
    }

    @Override
    public void bajaPaciente(UUID id) {
        validarId(id);

        synchronized (repositorio) {
            Paciente actual = obtenerPacienteActivo(id);

            Paciente inactivo = new Paciente(
                    actual.id(),
                    actual.cip(),
                    actual.nombre(),
                    actual.apellidos(),
                    actual.fechaNacimiento(),
                    false
            );

            repositorio.guardar(inactivo);
        }
    }

    private Paciente obtenerPacienteActivo(UUID id) {
        Paciente paciente = repositorio.buscarPorId(id)
                .orElseThrow(() -> new PacienteException(
                        PacienteException.Codigo.PACIENTE_NO_ENCONTRADO,
                        "No existe un paciente con ese identificador."
                ));

        if (!paciente.activo()) {
            throw new PacienteException(
                    PacienteException.Codigo.PACIENTE_INACTIVO,
                    "El paciente está inactivo."
            );
        }

        return paciente;
    }

    private String normalizarCip(String cip) {
        return validarTexto(cip, "El CIP").toUpperCase(Locale.ROOT);
    }

    private String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new PacienteException(
                    PacienteException.Codigo.DATOS_INVALIDOS,
                    campo + " es obligatorio."
            );
        }

        return valor.strip();
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null
                || fechaNacimiento.isAfter(LocalDate.now())) {

            throw new PacienteException(
                    PacienteException.Codigo.DATOS_INVALIDOS,
                    "La fecha de nacimiento es obligatoria y no puede ser futura."
            );
        }
    }

    private void validarId(UUID id) {
        if (id == null) {
            throw new PacienteException(
                    PacienteException.Codigo.DATOS_INVALIDOS,
                    "El identificador del paciente es obligatorio."
            );
        }
    }
}