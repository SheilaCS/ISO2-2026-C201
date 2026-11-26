package com.sescam.radiodiagnostico.listasespera;

import java.util.Optional;

/**
 * PRIORIDAD DE UNA SOLICITUD DE PRUEBA.
 * Iteracion 15 - RF15.
 *
 * Son las mismas prioridades que admite la solicitud de pruebas (RF06).
 * El orden de declaracion es el orden de atencion: la primera es la mas
 * urgente.
 */
public enum Prioridad {
    URGENTE,
    PREFERENTE,
    ORDINARIO;

    // Convertir un texto en prioridad (vacio si no es una prioridad valida)
    public static Optional<Prioridad> desde(String texto) {

        if (texto == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(texto.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
