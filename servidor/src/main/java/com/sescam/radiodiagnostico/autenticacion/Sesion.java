package com.sescam.radiodiagnostico.autenticacion;

/**
 * Sesión abierta tras una autenticación correcta.
 */
public record Sesion(String identificador, Rol rol) {
}
