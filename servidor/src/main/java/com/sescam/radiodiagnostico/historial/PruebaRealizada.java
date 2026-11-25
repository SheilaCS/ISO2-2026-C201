package com.sescam.radiodiagnostico.historial;

import java.time.LocalDate;

/**
 * UNA PRUEBA DIAGNOSTICA YA REALIZADA A UN PACIENTE.
 * Iteracion 12 - RF04.
 *
 * Datos minimos que muestra el historial. Se obtienen del registro
 * de la realizacion (CDU7): fecha, estado y observaciones.
 */
public record PruebaRealizada(
        String id,
        LocalDate fecha,
        String tipo,
        String estado,
        String informe) {
}
