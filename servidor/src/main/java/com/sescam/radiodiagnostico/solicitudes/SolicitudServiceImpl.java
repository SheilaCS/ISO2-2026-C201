package com.sescam.radiodiagnostico.solicitudes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación en memoria del servicio de solicitudes (CDU2, RF06).
 */
public class SolicitudServiceImpl implements ISolicitudService {

    private final Map<Integer, Solicitud> solicitudes = new HashMap<>();
    private int siguienteId = 1;

    @Override
    public synchronized Solicitud solicitarPrueba(String medico, String idPaciente,
            String tipoPrueba, String indicacionClinica, String justificacion,
            String prioridad) {

        if (vacio(medico)) {
            throw new IllegalArgumentException("Falta el médico que realiza la solicitud.");
        }
        int paciente = leerIdPaciente(idPaciente);
        String tipo = obligatorio(tipoPrueba, "El tipo de prueba es obligatorio.");
        String indicacion = obligatorio(indicacionClinica, "La indicación clínica es obligatoria.");
        String motivo = obligatorio(justificacion, "La justificación es obligatoria.");
        Prioridad nivel = leerPrioridad(prioridad);

        Solicitud solicitud = new Solicitud(siguienteId++, paciente, medico.strip(),
                tipo, indicacion, motivo, nivel, LocalDateTime.now());
        solicitudes.put(solicitud.id(), solicitud);
        return solicitud;
    }

    @Override
    public synchronized Optional<Solicitud> consultarSolicitud(int id) {
        return Optional.ofNullable(solicitudes.get(id));
    }

    private static int leerIdPaciente(String texto) {
        try {
            int id = Integer.parseInt(texto.strip());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // se trata abajo
        }
        throw new IllegalArgumentException("El ID de paciente debe ser un entero mayor que 0.");
    }

    private static Prioridad leerPrioridad(String texto) {
        if (vacio(texto)) {
            throw new IllegalArgumentException("La prioridad es obligatoria.");
        }
        try {
            return Prioridad.valueOf(texto.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Prioridad no válida. Use URGENTE, PREFERENTE u ORDINARIO.");
        }
    }

    private static String obligatorio(String texto, String mensaje) {
        if (vacio(texto)) {
            throw new IllegalArgumentException(mensaje);
        }
        return texto.strip();
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
