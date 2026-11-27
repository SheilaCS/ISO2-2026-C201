package com.sescam.radiodiagnostico.roles;

/**
 * Error controlado de la gestión de roles y permisos.
 */
public class RolesException extends RuntimeException {

    public enum Codigo {
        DATOS_INVALIDOS,
        USUARIO_NO_ENCONTRADO,
        OPERACION_NO_PERMITIDA
    }

    private final Codigo codigo;

    public RolesException(Codigo codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public Codigo getCodigo() {
        return codigo;
    }
}
