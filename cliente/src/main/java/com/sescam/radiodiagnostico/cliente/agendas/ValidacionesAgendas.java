
package com.sescam.radiodiagnostico.cliente.agendas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class ValidacionesAgendas {

    // Comprobar que el ID sea un entero positivo
    public static boolean validarId(String texto) {
        try {
            int id = Integer.parseInt(texto.trim());
            return id > 0;

        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    // Comprobar que la fecha tenga un formato correcto
    // y que no sea anterior a hoy
    public static boolean validarFecha(String texto) {
        try {
            LocalDate fecha = LocalDate.parse(texto);
            return !fecha.isBefore(LocalDate.now());

        } catch (DateTimeParseException | NullPointerException e) {
            return false;
        }
    }

    // Comprobar el formato de la hora (HH:MM)
    public static boolean validarHora(String texto) {
        try {
            if (texto == null ||
                    !texto.matches("\\d{2}:\\d{2}")) {
                return false;
            }

            LocalTime.parse(texto);
            return true;

        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // Comprobar que la hora inicial sea anterior a la final
    public static boolean validarRangoHorario(
            String inicio, String fin) {

        if (!validarHora(inicio) || !validarHora(fin)) {
            return false;
        }

        LocalTime horaInicio = LocalTime.parse(inicio);
        LocalTime horaFin = LocalTime.parse(fin);

        return horaInicio.isBefore(horaFin);
    }
}
