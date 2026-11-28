package com.sescam.radiodiagnostico.seguridad;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.roles.IRolesService;
import com.sescam.radiodiagnostico.roles.Permiso;
import com.sescam.radiodiagnostico.roles.RolesException;

/**
 * Implementación en memoria de la auditoría (CDU11, RF19).
 * Se apoya en IRolesService (CDU10, RF18) para saber el rol de cada
 * usuario y si puede consultar la auditoría.
 */
public class SeguridadServiceImpl implements ISeguridadService {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IRolesService roles;
    private final List<String> logsAuditoria = new ArrayList<>();

    public SeguridadServiceImpl(IRolesService roles) {
        if (roles == null) {
            throw new IllegalArgumentException("El servicio de roles es obligatorio.");
        }
        this.roles = roles;
    }

    @Override
    public synchronized boolean registrarAuditoria(
            String usuario, String accion, String detalles) {

        if (vacio(usuario) || vacio(accion)) {
            return false;
        }

        String registro = LocalDateTime.now().format(FORMATO)
                + " | Usuario: " + usuario.strip()
                + " | Rol: " + rolDe(usuario.strip())
                + " | Acción: " + accion.strip()
                + " | Detalles: " + (detalles == null ? "" : detalles.strip());

        logsAuditoria.add(registro);
        return true;
    }

    @Override
    public boolean puedeConsultarAuditoria(String usuario) {
        if (vacio(usuario)) {
            return false;
        }
        return roles.tienePermiso(usuario.strip(), Permiso.CONSULTAR_AUDITORIA);
    }

    @Override
    public synchronized List<String> consultarLogs() {
        return List.copyOf(logsAuditoria);
    }

    /** Pregunta el rol al servicio de roles (RF18). */
    private String rolDe(String usuario) {
        try {
            Rol rol = roles.consultarRol(usuario);
            return rol == null ? "DESCONOCIDO" : rol.name();
        } catch (RolesException e) {
            return "DESCONOCIDO";
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}