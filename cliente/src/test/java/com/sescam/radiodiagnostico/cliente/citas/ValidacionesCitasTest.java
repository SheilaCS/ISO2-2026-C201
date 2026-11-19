
package com.sescam.radiodiagnostico.cliente.citas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pruebas unitarias de las validaciones del cliente.
 * Iteracion 3 - RF13: Gestion de Citas.
 *
 * Tecnicas utilizadas:
 * - Clases de equivalencia.
 * - Analisis de valores limite.
 * - Conjetura de errores.
 */
public class ValidacionesCitasTest {

    // -----------------------------------------
    // PRUEBAS DE VALIDACION DE ID
    // -----------------------------------------

    @Test
    @DisplayName("Aceptar un ID positivo")
    public void testIdCorrecto() {

        // Preparar y ejecutar
        boolean resultado = ValidacionesCitas.validarId("5");

        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Aceptar el menor ID positivo")
    public void testIdUno() {

        // Valor limite inferior valido
        assertTrue(ValidacionesCitas.validarId("1"));
    }

    @Test
    @DisplayName("Aceptar el mayor ID entero positivo")
    public void testIdMaximo() {

        // Valor limite superior valido
        assertTrue(ValidacionesCitas.validarId(
                String.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    @DisplayName("Rechazar un ID negativo")
    public void testIdNegativo() {

        assertFalse(ValidacionesCitas.validarId("-3"));
    }

    @Test
    @DisplayName("Rechazar un ID igual a cero")
    public void testIdCero() {

        // Valor limite inmediatamente inferior a 1
        assertFalse(ValidacionesCitas.validarId("0"));
    }

    @Test
    @DisplayName("Rechazar un ID no numerico")
    public void testIdNoNumerico() {

        assertFalse(ValidacionesCitas.validarId("abc"));
    }

    @Test
    @DisplayName("Rechazar un ID decimal")
    public void testIdDecimal() {

        assertFalse(ValidacionesCitas.validarId("2.5"));
    }

    @Test
    @DisplayName("Rechazar un ID superior al maximo entero")
    public void testIdFueraDeRango() {

        assertFalse(ValidacionesCitas.validarId(
                "2147483648"));
    }

    @Test
    @DisplayName("Aceptar un ID con espacios")
    public void testIdConEspacios() {

        // El metodo debe eliminar los espacios
        assertTrue(ValidacionesCitas.validarId("  5  "));
    }

    @Test
    @DisplayName("Rechazar un ID vacio")
    public void testIdVacio() {

        assertFalse(ValidacionesCitas.validarId(""));
    }

    @Test
    @DisplayName("Rechazar un ID con solo espacios")
    public void testIdSoloEspacios() {

        assertFalse(ValidacionesCitas.validarId("   "));
    }

    @Test
    @DisplayName("Rechazar un ID nulo")
    public void testIdNulo() {

        assertFalse(ValidacionesCitas.validarId(null));
    }

    // -----------------------------------------
    // PRUEBAS DE FECHA Y HORA
    // -----------------------------------------

    @Test
    @DisplayName("Aceptar una fecha y hora futuras")
    public void testFechaFutura() {

        // Preparar
        String fecha = LocalDate.now()
                .plusDays(10).toString();

        // Ejecutar
        boolean resultado = ValidacionesCitas.validarFechaHora(
                fecha, "10:00");

        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Rechazar una fecha pasada")
    public void testFechaPasada() {

        String fecha = LocalDate.now()
                .minusDays(10).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, "10:00"));
    }

    @Test
    @DisplayName("Rechazar una fecha incorrecta")
    public void testFechaIncorrecta() {

        assertFalse(ValidacionesCitas.validarFechaHora(
                "fecha-incorrecta", "10:00"));
    }

    @Test
    @DisplayName("Rechazar una hora incorrecta")
    public void testHoraIncorrecta() {

        String fecha = LocalDate.now()
                .plusDays(10).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, "25:00"));
    }

    @Test
    @DisplayName("Aceptar fecha y hora con espacios")
    public void testFechaHoraConEspacios() {

        String fecha = LocalDate.now()
                .plusDays(5).toString();

        assertTrue(ValidacionesCitas.validarFechaHora(
                "  " + fecha + "  ",
                "  12:30  "));
    }

    @Test
    @DisplayName("Rechazar una fecha nula")
    public void testFechaNula() {

        assertFalse(ValidacionesCitas.validarFechaHora(
                null, "12:00"));
    }

    @Test
    @DisplayName("Rechazar una hora nula")
    public void testHoraNula() {

        String fecha = LocalDate.now()
                .plusDays(5).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, null));
    }

    @Test
    @DisplayName("Rechazar una fecha vacia")
    public void testFechaVacia() {

        assertFalse(ValidacionesCitas.validarFechaHora(
                "", "12:00"));
    }

    @Test
    @DisplayName("Rechazar una hora vacia")
    public void testHoraVacia() {

        String fecha = LocalDate.now()
                .plusDays(5).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, ""));
    }

    @Test
    @DisplayName("Rechazar una fecha y hora pasadas")
    public void testFechaHoraPasadas() {

        String fecha = LocalDate.now()
                .minusDays(1).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, "23:59"));
    }

    @Test
    @DisplayName("Aceptar una cita posterior a la hora actual")
    public void testHoraFuturaHoy() {

        // Elegir una fecha y hora 2 horas posteriores
        // Funciona tambien si cambia el dia
        LocalDateTime futuro =
                LocalDateTime.now().plusHours(2);

        String fecha = futuro.toLocalDate().toString();

        String hora = futuro.toLocalTime()
                .withSecond(0)
                .withNano(0)
                .toString();

        assertTrue(ValidacionesCitas.validarFechaHora(
                fecha, hora));
    }

    @Test
    @DisplayName("Rechazar una cita anterior a la hora actual")
    public void testHoraPasadaHoy() {

        // Elegir una fecha y hora 2 horas anteriores
        LocalDateTime pasado =
                LocalDateTime.now().minusHours(2);

        String fecha = pasado.toLocalDate().toString();

        String hora = pasado.toLocalTime()
                .withSecond(0)
                .withNano(0)
                .toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, hora));
    }

    @Test
    @DisplayName("Rechazar una fecha imposible")
    public void testFechaImposible() {

        assertFalse(ValidacionesCitas.validarFechaHora(
                "2027-02-30", "12:00"));
    }

    @Test
    @DisplayName("Rechazar una hora negativa")
    public void testHoraNegativa() {

        String fecha = LocalDate.now()
                .plusDays(5).toString();

        assertFalse(ValidacionesCitas.validarFechaHora(
                fecha, "-1:00"));
    }

    // -----------------------------------------
    // PRUEBA DEL CONSTRUCTOR
    // -----------------------------------------

    @Test
    @DisplayName("Comprobar el constructor de ValidacionesCitas")
    public void testConstructor() {

        // Ejecutar
        ValidacionesCitas objeto = new ValidacionesCitas();

        // Comprobar
        assertNotNull(objeto);
    }
}
