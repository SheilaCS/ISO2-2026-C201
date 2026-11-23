package com.sescam.radiodiagnostico.historial;

import java.time.LocalDate;

/**
 * UNA PRUEBA DIAGNOSTICA YA REALIZADA A UN PACIENTE.
 * Iteracion 12 - RF04.
 *
 * Datos minimos que muestra el historial. Cuando exista el CDU7
 * (registro de la realizacion) se ajustaran a sus datos reales.
 */
public record PruebaRealizada(
        String id,
        LocalDate fecha,
        String tipo,
        String estado,
        String informe) {
}
