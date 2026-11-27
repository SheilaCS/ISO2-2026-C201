package com.sescam.radiodiagnostico.roles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sescam.radiodiagnostico.autenticacion.Rol;

/**
 * Repositorio en memoria. Empieza con los usuarios de demostración
 * y los permisos que el sistema ya aplica.
 */
public class RepositorioRolesEnMemoria implements RepositorioRoles {

    private final Map<String, Rol> usuarios = new HashMap<>();
    private final Map<Rol, Set<Permiso>> permisos = new EnumMap<>(Rol.class);

    public RepositorioRolesEnMemoria() {
        usuarios.put("paciente.demo", Rol.PACIENTE);
        usuarios.put("medico.demo", Rol.MEDICO);
        usuarios.put("administrativo.demo", Rol.ADMINISTRATIVO);
        usuarios.put("admin.demo", Rol.ADMINISTRADOR);

        permisos.put(Rol.PACIENTE, EnumSet.of(
                Permiso.CONSULTAR_MIS_CITAS,
                Permiso.CONSULTAR_HISTORIAL));
        permisos.put(Rol.MEDICO, EnumSet.of(
                Permiso.SOLICITAR_PRUEBAS,
                Permiso.REGISTRAR_REALIZACION,
                Permiso.CONSULTAR_HISTORIAL));
        permisos.put(Rol.ADMINISTRATIVO, EnumSet.of(
                Permiso.GESTIONAR_PACIENTES,
                Permiso.GESTIONAR_CITAS,
                Permiso.GESTIONAR_AGENDAS));
        permisos.put(Rol.ADMINISTRADOR, EnumSet.of(
                Permiso.GESTIONAR_ROLES,
                Permiso.CONFIGURAR_PARAMETROS,
                Permiso.CONSULTAR_AUDITORIA));
    }

    @Override
    public synchronized Optional<Rol> buscarRol(String usuario) {
        return Optional.ofNullable(usuarios.get(usuario));
    }

    @Override
    public synchronized void guardarRol(String usuario, Rol rol) {
        usuarios.put(usuario, rol);
    }

    @Override
    public synchronized Map<String, Rol> listarUsuarios() {
        return Map.copyOf(usuarios);
    }

    @Override
    public synchronized Set<Permiso> buscarPermisos(Rol rol) {
        Set<Permiso> actuales = permisos.get(rol);
        return actuales == null || actuales.isEmpty()
                ? EnumSet.noneOf(Permiso.class)
                : EnumSet.copyOf(actuales);
    }

    @Override
    public synchronized void guardarPermisos(Rol rol, Set<Permiso> nuevos) {
        permisos.put(rol, nuevos.isEmpty()
                ? EnumSet.noneOf(Permiso.class)
                : EnumSet.copyOf(nuevos));
    }
}
