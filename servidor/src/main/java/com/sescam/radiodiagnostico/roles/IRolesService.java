package com.sescam.radiodiagnostico.roles;

import java.util.Map;
import java.util.Set;

import com.sescam.radiodiagnostico.autenticacion.Rol;

/**
 * Operaciones de la gestión de roles y permisos (CDU10, RF18).
 */
public interface IRolesService {

    Rol consultarRol(String usuario);

    Map<String, Rol> listarUsuarios();

    Rol cambiarRol(String administrador, String usuario, Rol nuevoRol);

    Set<Permiso> consultarPermisos(Rol rol);

    Set<Permiso> concederPermiso(Rol rol, Permiso permiso);

    Set<Permiso> retirarPermiso(Rol rol, Permiso permiso);

    boolean tienePermiso(String usuario, Permiso permiso);
}
