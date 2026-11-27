package com.sescam.radiodiagnostico.roles;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sescam.radiodiagnostico.autenticacion.Rol;

/**
 * Reglas de negocio de la gestión de roles y permisos.
 */
public class RolesServiceImpl implements IRolesService {

    private final RepositorioRoles repositorio;

    public RolesServiceImpl(RepositorioRoles repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    @Override
    public Rol consultarRol(String usuario) {
        String nombre = validarUsuario(usuario);
        return repositorio.buscarRol(nombre)
                .orElseThrow(() -> new RolesException(
                        RolesException.Codigo.USUARIO_NO_ENCONTRADO,
                        "No existe el usuario indicado."));
    }

    @Override
    public Map<String, Rol> listarUsuarios() {
        return repositorio.listarUsuarios();
    }

    @Override
    public Rol cambiarRol(String administrador, String usuario, Rol nuevoRol) {
        String nombre = validarUsuario(usuario);
        validarRol(nuevoRol);

        if (administrador != null && nombre.equals(administrador.strip())) {
            throw new RolesException(
                    RolesException.Codigo.OPERACION_NO_PERMITIDA,
                    "Un administrador no puede cambiar su propio rol.");
        }

        // La comprobación y el cambio son una sola operación.
        synchronized (repositorio) {
            Rol actual = consultarRol(nombre);

            if (actual == Rol.ADMINISTRADOR
                    && nuevoRol != Rol.ADMINISTRADOR
                    && contarAdministradores() == 1) {
                throw new RolesException(
                        RolesException.Codigo.OPERACION_NO_PERMITIDA,
                        "El sistema debe tener al menos un administrador.");
            }

            repositorio.guardarRol(nombre, nuevoRol);
            return nuevoRol;
        }
    }

    @Override
    public Set<Permiso> consultarPermisos(Rol rol) {
        validarRol(rol);
        return repositorio.buscarPermisos(rol);
    }

    @Override
    public Set<Permiso> concederPermiso(Rol rol, Permiso permiso) {
        validarRol(rol);
        validarPermiso(permiso);

        synchronized (repositorio) {
            Set<Permiso> permisos = repositorio.buscarPermisos(rol);
            permisos.add(permiso);
            repositorio.guardarPermisos(rol, permisos);
            return repositorio.buscarPermisos(rol);
        }
    }

    @Override
    public Set<Permiso> retirarPermiso(Rol rol, Permiso permiso) {
        validarRol(rol);
        validarPermiso(permiso);

        if (rol == Rol.ADMINISTRADOR && permiso == Permiso.GESTIONAR_ROLES) {
            throw new RolesException(
                    RolesException.Codigo.OPERACION_NO_PERMITIDA,
                    "No se puede retirar al administrador la gestión de roles.");
        }

        synchronized (repositorio) {
            Set<Permiso> permisos = repositorio.buscarPermisos(rol);
            permisos.remove(permiso);
            repositorio.guardarPermisos(rol, permisos);
            return repositorio.buscarPermisos(rol);
        }
    }

    @Override
    public boolean tienePermiso(String usuario, Permiso permiso) {
        if (usuario == null || usuario.isBlank() || permiso == null) {
            return false;
        }
        return repositorio.buscarRol(usuario.strip())
                .map(rol -> repositorio.buscarPermisos(rol).contains(permiso))
                .orElse(false);
    }

    private long contarAdministradores() {
        return repositorio.listarUsuarios().values().stream()
                .filter(rol -> rol == Rol.ADMINISTRADOR)
                .count();
    }

    private String validarUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new RolesException(
                    RolesException.Codigo.DATOS_INVALIDOS,
                    "El usuario es obligatorio.");
        }
        return usuario.strip();
    }

    private void validarRol(Rol rol) {
        if (rol == null) {
            throw new RolesException(
                    RolesException.Codigo.DATOS_INVALIDOS,
                    "El rol es obligatorio.");
        }
    }

    private void validarPermiso(Permiso permiso) {
        if (permiso == null) {
            throw new RolesException(
                    RolesException.Codigo.DATOS_INVALIDOS,
                    "El permiso es obligatorio.");
        }
    }
}