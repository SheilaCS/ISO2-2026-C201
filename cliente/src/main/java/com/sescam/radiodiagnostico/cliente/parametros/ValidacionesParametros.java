package com.sescam.radiodiagnostico.cliente.parametros;

import java.util.Set;

/**
 * Validaciones del cliente de configuración de parámetros (CDU5, RF17).
 */
public class ValidacionesParametros {

    private static final Set<String> CATEGORIAS = Set.of(
            "TIPO_PRUEBA", "PROTOCOLO", "HORARIO", "POLITICA_CITACION", "PRIORIDAD");

    private ValidacionesParametros() {
    }

    public static boolean validarTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    public static boolean validarCategoria(String categoria) {
        return categoria != null && CATEGORIAS.contains(categoria.trim().toUpperCase());
    }
}
