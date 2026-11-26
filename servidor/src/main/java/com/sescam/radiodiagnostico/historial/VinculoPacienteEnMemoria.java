package com.sescam.radiodiagnostico.historial;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * VINCULOS USUARIO-PACIENTE EN MEMORIA.
 * Iteracion 12 - RF04.
 *
 * Se cargan al crear el objeto a partir de una lista con el formato
 * "usuario=numero,usuario=numero". El constructor normal la lee de
 * la propiedad del sistema historial.vinculos y, si no existe,
 * usa solo el usuario de la demo: "paciente.demo=1".
 */
public class VinculoPacienteEnMemoria implements IVinculoPaciente {

    private static final String PROPIEDAD = "historial.vinculos";
    private static final String POR_DEFECTO = "paciente.demo=1";

    private final Map<String, String> vinculos = new HashMap<>();

    // Constructor normal
    public VinculoPacienteEnMemoria() {
        this(System.getProperty(PROPIEDAD, POR_DEFECTO));
    }

    // Constructor con una lista concreta; las entradas incorrectas
    // se ignoran
    public VinculoPacienteEnMemoria(String lista) {
        if (lista == null) {
            return;
        }

        for (String entrada : lista.split(",")) {
            String[] par = entrada.split("=", 2);

            if (par.length == 2
                    && !par[0].isBlank() && !par[1].isBlank()) {
                vinculos.put(par[0].trim(), par[1].trim());
            }
        }
    }

    // Anade o cambia el vinculo de un usuario
    public synchronized void vincular(String usuario, String idPaciente) {
        if (usuario == null || usuario.isBlank()
                || idPaciente == null || idPaciente.isBlank()) {
            throw new IllegalArgumentException(
                    "Usuario e identificador de paciente obligatorios");
        }

        vinculos.put(usuario.trim(), idPaciente.trim());
    }

    @Override
    public synchronized Optional<String> idPacienteDe(String usuario) {
        return usuario == null
                ? Optional.empty()
                : Optional.ofNullable(vinculos.get(usuario));
    }
}
