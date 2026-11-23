package com.sescam.radiodiagnostico.informes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class InformeServiceImpl implements IInformeService {

    @Override
    public boolean generarInforme(int idEquipo, String fecha, String descripcion, String rol) {
        // 1. SEGURIDAD: Control de acceso basado en roles (RBAC)
        if (!esRolPermitido(rol)) {
            System.out.println("SEGURIDAD: Intento de acceso denegado con rol: " + rol);
            return false;
        }

        // 2. CALIDAD: Validación de parámetros en el servidor
        if (idEquipo <= 0) return false;
        
        if (descripcion == null || descripcion.trim().isEmpty()) return false;
        
        try {
            LocalDate.parse(fecha); // Valida formato AAAA-MM-DD
        } catch (DateTimeParseException e) {
            return false;
        }

        // Aquí iría la lógica para guardar en Base de Datos...
        System.out.println("Informe generado para el equipo " + idEquipo);
        return true;
    }

    @Override
    public String consultarInforme(int idInforme, String rol) {
        // 1. SEGURIDAD
        if (!esRolPermitido(rol)) {
            return "Error de seguridad: Acceso denegado.";
        }

        if (idInforme <= 0) {
            return "Error: ID de informe inválido.";
        }

        // Simulamos la consulta a BD
        return "Informe " + idInforme + ": Equipo revisado correctamente. Sin incidencias.";
    }

    // Método auxiliar para la seguridad
    private boolean esRolPermitido(String rol) {
        if (rol == null) return false;
        String r = rol.trim().toUpperCase();
        return r.equals("TECNICO") || r.equals("ADMIN");
    }
}