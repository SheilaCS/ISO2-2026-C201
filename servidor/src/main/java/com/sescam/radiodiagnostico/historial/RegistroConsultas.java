package com.sescam.radiodiagnostico.historial;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REGISTRO SIMPLE DE CONSULTAS DE HISTORIAL.
 * Iteracion 12 - RF04.
 *
 * Anota quien consulta, a que paciente y con que resultado, tanto
 * si la consulta se acepta como si se rechaza. Se guarda en memoria.
 * El CDU11 de auditoria lo unificara mas adelante.
 */
public class RegistroConsultas {

    public record Entrada(
            Instant momento,
            String usuario,
            Rol rol,
            String idPaciente,
            ResultadoHistorial.Estado resultado) {
    }

    private final List<Entrada> entradas = new ArrayList<>();

    public synchronized void registrar(String usuario, Rol rol,
            String idPaciente, ResultadoHistorial.Estado resultado) {
        entradas.add(new Entrada(
                Instant.now(), usuario, rol, idPaciente, resultado));
    }

    public synchronized List<Entrada> consultas() {
        return Collections.unmodifiableList(new ArrayList<>(entradas));
    }
}
