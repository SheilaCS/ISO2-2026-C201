package com.sescam.radiodiagnostico.pacientes;

public class PacienteException extends RuntimeException {

    public enum Codigo {
        DATOS_INVALIDOS,
        CIP_DUPLICADO,
        PACIENTE_NO_ENCONTRADO,
        PACIENTE_INACTIVO
    }

    private final Codigo codigo;

    public PacienteException(Codigo codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public Codigo getCodigo() {
        return codigo;
    }
}