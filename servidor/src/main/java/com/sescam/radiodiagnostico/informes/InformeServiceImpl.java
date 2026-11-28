package com.sescam.radiodiagnostico.informes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación en memoria del servicio de informes e imágenes (CDU8, RF09).
 */
public class InformeServiceImpl implements IInformeService {

    private final Map<Integer, Informe> informes = new LinkedHashMap<>();
    private int siguienteId = 1;

    @Override
    public synchronized Informe crearInforme(String medico, String idPaciente,
            String idPrueba, String tipoPrueba, String contenido, String imagenes) {

        if (vacio(medico)) {
            throw new IllegalArgumentException("Falta el médico que firma el informe.");
        }
        int paciente = leerId(idPaciente, "El ID de paciente debe ser un entero mayor que 0.");
        int prueba = leerId(idPrueba, "El ID de prueba debe ser un entero mayor que 0.");
        if (vacio(tipoPrueba)) {
            throw new IllegalArgumentException("El tipo de prueba es obligatorio.");
        }
        if (vacio(contenido)) {
            throw new IllegalArgumentException("El contenido del informe es obligatorio.");
        }

        Informe informe = new Informe(siguienteId++, paciente, prueba, medico.strip(),
                tipoPrueba.strip(), contenido.strip(), leerImagenes(imagenes),
                LocalDateTime.now());
        informes.put(informe.id(), informe);
        return informe;
    }

    @Override
    public synchronized Optional<Informe> consultarInforme(int idInforme) {
        return Optional.ofNullable(informes.get(idInforme));
    }

    @Override
    public synchronized List<Informe> consultarInformesPaciente(int idPaciente) {
        return informes.values().stream()
                .filter(i -> i.idPaciente() == idPaciente)
                .toList();
    }

    private static List<String> leerImagenes(String texto) {
        List<String> imagenes = new ArrayList<>();
        if (vacio(texto)) {
            return imagenes;
        }
        for (String imagen : texto.split(",")) {
            if (imagen.isBlank()) {
                throw new IllegalArgumentException("Hay una referencia de imagen vacía.");
            }
            imagenes.add(imagen.strip());
        }
        return imagenes;
    }

    private static int leerId(String texto, String mensaje) {
        try {
            int id = Integer.parseInt(texto.strip());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // se trata abajo
        }
        throw new IllegalArgumentException(mensaje);
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
