package com.sescam.radiodiagnostico.historial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ORIGEN SIMULADO DE PRUEBAS REALIZADAS.
 * Iteracion 12 - RF04.
 *
 * Guarda las pruebas en memoria hasta que exista el registro real
 * del CDU7. Tambien se usa en las pruebas del servicio.
 */
public class ProveedorHistorialEnMemoria implements IHistorialPruebasProveedor {

    private final Map<String, List<PruebaRealizada>> pruebas = new HashMap<>();

    public synchronized void agregar(String idPaciente, PruebaRealizada prueba) {
        pruebas.computeIfAbsent(idPaciente, k -> new ArrayList<>())
                .add(prueba);
    }

    @Override
    public synchronized List<PruebaRealizada> pruebasDePaciente(
            String idPaciente) {
        return new ArrayList<>(pruebas.getOrDefault(idPaciente, List.of()));
    }
}
