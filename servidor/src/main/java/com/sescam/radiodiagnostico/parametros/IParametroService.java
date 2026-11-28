package com.sescam.radiodiagnostico.parametros;

import java.util.Map;
import java.util.Optional;

/**
 * Contrato del servicio de configuración de parámetros (CDU5, RF17).
 */
public interface IParametroService {

    /**
     * Guarda o actualiza un parámetro de una categoría.
     *
     * @throws IllegalArgumentException si la categoría, la clave o el valor no son válidos
     */
    void guardarParametro(CategoriaParametro categoria, String clave, String valor);

    /** Devuelve el valor del parámetro, si existe. */
    Optional<String> obtenerParametro(CategoriaParametro categoria, String clave);

    /** Devuelve todos los parámetros de una categoría, ordenados por clave. */
    Map<String, String> listarParametros(CategoriaParametro categoria);
}
