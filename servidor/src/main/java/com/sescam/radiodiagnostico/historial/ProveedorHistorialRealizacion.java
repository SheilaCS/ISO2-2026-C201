package com.sescam.radiodiagnostico.historial;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ORIGEN REAL DE LAS PRUEBAS REALIZADAS DE UN PACIENTE.
 * Iteracion 12 - RF04, con los datos del registro de la
 * realizacion (iteracion 7, CDU7).
 *
 * Cada vez que se pide un historial se consulta en directo:
 * - al servidor de realizaciones (GET /realizaciones), que solo
 *   admite medicos, usando una cuenta de servicio y no las
 *   credenciales de quien consulta (asi el paciente tambien puede
 *   ver su historial; el control de acceso lo hace el servicio
 *   del historial);
 * - al servidor de citas (GET /citas/consultar), para saber a que
 *   paciente pertenece cada realizacion.
 * No se guarda nada en memoria: lo que se registre en CDU7
 * aparece en el historial en la siguiente consulta.
 *
 * Cuenta de servicio: propiedades del sistema
 * historial.servicio.usuario y historial.servicio.clave.
 * Si CDU7 o citas no responden o rechazan la cuenta, se lanza
 * IllegalStateException (el historial responde "no disponible").
 */
public class ProveedorHistorialRealizacion
        implements IHistorialPruebasProveedor {

    // Linea de la cita: "ID paciente: 12"
    private static final Pattern PATRON_PACIENTE =
            Pattern.compile("(?m)^ID paciente:\\s*(\\d+)\\s*$");

    private static final String ERROR_REALIZACIONES =
            "Servidor de realizaciones no disponible";
    private static final String ERROR_CITAS =
            "Servidor de citas no disponible";

    private final String urlRealizaciones;
    private final String urlCitas;
    private final String autorizacion;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    // Constructor normal: direcciones y cuenta de servicio por defecto
    public ProveedorHistorialRealizacion() {
        this("http://127.0.0.1:"
                        + Integer.getInteger("realizacion.puerto", 8086)
                        + "/realizaciones",
                "http://127.0.0.1:"
                        + Integer.getInteger("citas.puerto", 8080)
                        + "/citas/consultar",
                System.getProperty("historial.servicio.usuario",
                        "medico.demo"),
                System.getProperty("historial.servicio.clave", "demo"));
    }

    // Constructor para indicar direcciones y cuenta (pruebas)
    public ProveedorHistorialRealizacion(String urlRealizaciones,
                                         String urlCitas,
                                         String usuario,
                                         String clave) {
        this.urlRealizaciones = urlRealizaciones;
        this.urlCitas = urlCitas;
        this.autorizacion = "Basic " + Base64.getEncoder().encodeToString(
                (usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<PruebaRealizada> pruebasDePaciente(String idPaciente) {
        String buscado = normalizar(idPaciente);
        List<PruebaRealizada> pruebas = new ArrayList<>();

        if (buscado == null) {
            return pruebas;
        }

        // Cada cita se consulta una sola vez por peticion
        Map<Integer, Optional<String>> pacientesPorCita = new HashMap<>();

        for (String linea : listarRealizaciones().split("\n")) {
            Map<String, String> datos = leerLinea(linea);
            PruebaRealizada prueba = aPrueba(datos);

            if (prueba == null) {
                continue;
            }

            int idCita = Integer.parseInt(datos.get("idCita"));
            Optional<String> duenio = pacientesPorCita.computeIfAbsent(
                    idCita, this::pacienteDeCita);

            if (duenio.isPresent() && duenio.get().equals(buscado)) {
                pruebas.add(prueba);
            }
        }

        return pruebas;
    }

    // -----------------------------------------
    // CONSULTA A REALIZACIONES (CDU7)
    // -----------------------------------------

    private String listarRealizaciones() {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder()
                        .uri(URI.create(urlRealizaciones))
                        .header("Authorization", autorizacion)
                        .GET(),
                ERROR_REALIZACIONES);

        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException(ERROR_REALIZACIONES);
        }

        return respuesta.body() == null ? "" : respuesta.body();
    }

    // Convierte "clave=valor&clave=valor" en un mapa
    private Map<String, String> leerLinea(String linea) {
        Map<String, String> datos = new HashMap<>();

        for (String parte : linea.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length == 2) {
                datos.put(par[0], URLDecoder.decode(
                        par[1], StandardCharsets.UTF_8));
            }
        }

        return datos;
    }

    // Devuelve null si la linea esta vacia o es incorrecta
    private PruebaRealizada aPrueba(Map<String, String> datos) {
        try {
            String id = datos.get("id");
            String estado = datos.get("estado");

            if (id == null || estado == null
                    || datos.get("idCita") == null
                    || datos.get("idSolicitud") == null) {
                return null;
            }

            Integer.parseInt(datos.get("idCita"));

            return new PruebaRealizada(
                    id,
                    LocalDate.parse(datos.get("fechaRealizacion")),
                    "Prueba de la solicitud "
                            + Integer.parseInt(datos.get("idSolicitud")),
                    estado,
                    construirInforme(datos.get("observaciones"),
                            datos.get("incidencia")));

        } catch (NumberFormatException | DateTimeParseException
                 | NullPointerException e) {
            return null;
        }
    }

    // Observaciones y, si la hay, la incidencia
    private String construirInforme(String observaciones,
                                    String incidencia) {
        String texto = observaciones == null ? "" : observaciones.trim();

        if (incidencia != null && !incidencia.isBlank()) {
            texto = (texto.isEmpty() ? "" : texto + ". ")
                    + "Incidencia: " + incidencia.trim();
        }

        return texto.isEmpty() ? "Sin observaciones" : texto;
    }

    // -----------------------------------------
    // CONSULTA A CITAS (CDU3)
    // -----------------------------------------

    // Paciente de una cita; vacio si la cita no existe
    private Optional<String> pacienteDeCita(int idCita) {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder()
                        .uri(URI.create(urlCitas + "?id=" + idCita))
                        .GET(),
                ERROR_CITAS);

        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException(ERROR_CITAS);
        }

        Matcher m = PATRON_PACIENTE.matcher(
                respuesta.body() == null ? "" : respuesta.body());

        return m.find() ? Optional.ofNullable(normalizar(m.group(1)))
                : Optional.empty();
    }

    // -----------------------------------------
    // METODOS AUXILIARES
    // -----------------------------------------

    private HttpResponse<String> enviar(HttpRequest.Builder peticion,
                                        String mensajeError) {
        try {
            return cliente.send(
                    peticion.timeout(Duration.ofSeconds(3)).build(),
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException(mensajeError, e);
        }
    }

    // "007" y "7" son el mismo paciente; null si no es un numero
    private String normalizar(String id) {
        if (id == null) {
            return null;
        }

        try {
            return Integer.toString(Integer.parseInt(id.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
