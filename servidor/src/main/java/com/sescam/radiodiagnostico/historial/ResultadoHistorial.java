package com.sescam.radiodiagnostico.historial;

import java.util.List;

/**
 * RESULTADO DE CONSULTAR UN HISTORIAL.
 * Iteracion 12 - RF04.
 *
 * El estado indica si se ha podido consultar o por que se rechaza.
 * Las pruebas solo se rellenan cuando el estado es CORRECTO.
 */
public record ResultadoHistorial(
        Estado estado,
        String mensaje,
        List<PruebaRealizada> pruebas) {

    public enum Estado {
        CORRECTO,
        NO_AUTENTICADO,
        PROHIBIDO,
        DATOS_INVALIDOS,
        NO_DISPONIBLE
    }

    public static ResultadoHistorial correcto(List<PruebaRealizada> pruebas) {
        return new ResultadoHistorial(Estado.CORRECTO, "", pruebas);
    }

    public static ResultadoHistorial rechazado(Estado estado, String mensaje) {
        return new ResultadoHistorial(estado, mensaje, List.of());
    }
}
