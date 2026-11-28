package com.sescam.radiodiagnostico.parametros;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Implementación en memoria de la configuración de parámetros (CDU5, RF17).
 */
public class ParametroServiceImpl implements IParametroService {

    private final Map<CategoriaParametro, Map<String, String>> parametros =
            new EnumMap<>(CategoriaParametro.class);

    @Override
    public synchronized void guardarParametro(CategoriaParametro categoria,
            String clave, String valor) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
        if (vacio(clave)) {
            throw new IllegalArgumentException("La clave del parámetro no puede estar vacía.");
        }
        if (vacio(valor)) {
            throw new IllegalArgumentException("El valor del parámetro no puede estar vacío.");
        }
        parametros.computeIfAbsent(categoria, c -> new TreeMap<>())
                .put(clave.strip(), valor.strip());
    }

    @Override
    public synchronized Optional<String> obtenerParametro(CategoriaParametro categoria,
            String clave) {
        if (categoria == null || vacio(clave)) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                parametros.getOrDefault(categoria, Map.of()).get(clave.strip()));
    }

    @Override
    public synchronized Map<String, String> listarParametros(CategoriaParametro categoria) {
        if (categoria == null) {
            return Map.of();
        }
        return new TreeMap<>(parametros.getOrDefault(categoria, Map.of()));
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
