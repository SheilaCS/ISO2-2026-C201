
package com.sescam.radiodiagnostico.cliente.citas;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class ValidacionesCitas {

    // Comprobar que el ID sea un entero positivo.
    public static boolean validarId(String texto) {
        try {
            int id = Integer.parseInt(texto.trim());
            return id > 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    // Comprobar que la fecha y hora sean validas y futuras.
    public static boolean validarFechaHora(String fecha, String hora) {
        try {
            LocalDate fechaCita = LocalDate.parse(fecha.trim());
            LocalTime horaCita = LocalTime.parse(hora.trim());

            LocalDateTime fechaHoraCita =
                    LocalDateTime.of(fechaCita, horaCita);

            return fechaHoraCita.isAfter(LocalDateTime.now());

        } catch (DateTimeParseException | NullPointerException e) {
            return false;
        }
    }
}
