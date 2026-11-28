package com.sescam.radiodiagnostico.informes;

import java.util.List;
import java.util.Optional;

/**
 * Contrato del servicio de informes e imágenes de pacientes (CDU8, RF09).
 */
public interface IInformeService {

    /**
     * Crea el informe de una prueba realizada.
     *
     * @param imagenes referencias de las imágenes separadas por comas (puede estar vacío)
     * @throws IllegalArgumentException si algún dato no es válido
     */
    Informe crearInforme(String medico, String idPaciente, String idPrueba,
            String tipoPrueba, String contenido, String imagenes);

    /** Devuelve el informe con ese identificador, si existe. */
    Optional<Informe> consultarInforme(int idInforme);

    /** Devuelve los informes de un paciente, del más antiguo al más reciente. */
    List<Informe> consultarInformesPaciente(int idPaciente);
}
