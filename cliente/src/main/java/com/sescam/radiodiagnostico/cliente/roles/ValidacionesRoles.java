package com.sescam.radiodiagnostico.cliente.roles;

/**
 * Comprobaciones previas de la pantalla de roles y permisos.
 * El servidor repite todas las comprobaciones.
 */
public final class ValidacionesRoles {

    private ValidacionesRoles() {
    }

    public static boolean credencialesCompletas(String usuario, char[] clave) {
        return textoObligatorio(usuario) && clave != null && clave.length > 0;
    }

    public static boolean textoObligatorio(String texto) {
        return texto != null && !texto.isBlank();
    }
}