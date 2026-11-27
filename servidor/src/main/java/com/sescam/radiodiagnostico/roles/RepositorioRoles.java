package com.sescam.radiodiagnostico.roles;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sescam.radiodiagnostico.autenticacion.Rol;

/**
 * Almacenamiento de los roles de los usuarios y de los permisos de cada rol.
 */
public interface RepositorioRoles {

    Optional<Rol> buscarRol(String usuario);

    void guardarRol(String usuario, Rol rol);

    Map<String, Rol> listarUsuarios();

    Set<Permiso> buscarPermisos(Rol rol);

    void guardarPermisos(Rol rol, Set<Permiso> permisos);
}
