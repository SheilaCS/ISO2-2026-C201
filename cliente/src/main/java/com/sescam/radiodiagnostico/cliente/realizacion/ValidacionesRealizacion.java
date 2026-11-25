package com.sescam.radiodiagnostico.cliente.realizacion;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class ValidacionesRealizacion {

    private ValidacionesRealizacion() {
    }

    public static boolean validarIdPositivo(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(texto.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean validarFechaRealizacion(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        try {
            LocalDate fecha = LocalDate.parse(texto.trim());
            return !fecha.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean validarObservacionesOIncidencia(String observaciones, String incidencia) {
        boolean tieneObs = observaciones != null && !observaciones.isBlank();
        boolean tieneInc = incidencia != null && !incidencia.isBlank();
        return tieneObs || tieneInc;
    }
}