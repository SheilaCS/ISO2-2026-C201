
package com.sescam.radiodiagnostico.cliente.citas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

/**
 * Pruebas unitarias del cliente.
 * Iteracion 3 - RF13.
 */
public class ValidacionesCitasTest {

    @Test
    @DisplayName("Aceptar un ID positivo")
    public void testIdCorrecto() {

        // Un ID positivo debe ser valido.
        assertTrue(ValidacionesCitas.validarId("5"));
    }

    @Test
    @DisplayName("Rechazar un ID negativo")
    public void testIdNegativo() {

        // No se permiten identificadores negativos.
        assertFalse(ValidacionesCitas.validarId("-3"));
    }

    @Test
    @DisplayName("Rechazar un ID igual a cero")
    public void testIdCero() {

        // Valor limite: cero no es valido.
        assertFalse(ValidacionesCitas.validarId("0"));
    }

    @Test
    @DisplayName("Rechazar un ID no numerico")
    public void testIdNoNumerico() {

        // No se pueden introducir letras como ID.
        assertFalse(ValidacionesCitas.validarId("abc"));
    }

    @Test
    @DisplayName("Aceptar una fecha y hora futuras")
    public void testFechaFutura() {

        // Generamos una fecha futura.
        String fecha = LocalDate.now()
                .plusDays(10).toString();

        // Debe aceptar la fecha y la hora.
        assertTrue(ValidacionesCitas.validarFechaHora(
                fecha, "10:00"
        ));
    }

    @Test
    @DisplayName("Rechazar una fecha pasada")
    public void testFechaPasada() {

        // Generamos una fecha pasada.
        String fecha = LocalDate.now()
                .minusDays(10).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, "10:00"
        ));
    }

    @Test
    @DisplayName("Rechazar una fecha incorrecta")
    public void testFechaIncorrecta() {

        assertFalse(ValidacionesCitas.validarFechaHora(
                "fecha-incorrecta", "10:00"
        ));
    }

    @Test
    @DisplayName("Rechazar una hora incorrecta")
    public void testHoraIncorrecta() {

        String fecha = LocalDate.now()
                .plusDays(10).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, "25:00"
        ));
    }
}
