package com.sescam.radiodiagnostico.encuestas;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import java.time.LocalDateTime;
import java.util.*;

public class GestionEncuestasServiceImpl implements IGestionEncuestasService {
    private final IEncuestasProveedor proveedor;
    private final List<IncidenciaPaciente> incidencias = new ArrayList<>();
    private final Map<Integer, EncuestaSatisfaccion> encuestas = new HashMap<>();

    public GestionEncuestasServiceImpl() { this(new ProveedorEncuestasHttp()); }
    public GestionEncuestasServiceImpl(IEncuestasProveedor proveedor) {
        this.proveedor = Objects.requireNonNull(proveedor);
    }

    @Override
    public synchronized ResultadoOperacion comunicarIncidencia(int idPaciente,
            int idCita, String descripcion) {
        if (idPaciente <= 0 || idCita <= 0)
            return ResultadoOperacion.rechazada("Identificadores no validos");
        if (descripcion == null || descripcion.isBlank() || descripcion.trim().length() > 500)
            return ResultadoOperacion.rechazada("Descripcion obligatoria (maximo 500 caracteres)");
        if (!proveedor.citaPertenecePaciente(idCita, idPaciente))
            return ResultadoOperacion.rechazada("La cita no pertenece al paciente");
        incidencias.add(new IncidenciaPaciente(incidencias.size() + 1,
                idPaciente, idCita, descripcion.trim(), LocalDateTime.now()));
        return ResultadoOperacion.correcta("Incidencia registrada correctamente");
    }

    @Override
    public synchronized ResultadoOperacion responderEncuesta(int idPaciente,
            int idCita, int puntuacion, String comentario) {
        if (idPaciente <= 0 || idCita <= 0)
            return ResultadoOperacion.rechazada("Identificadores no validos");
        if (puntuacion < 1 || puntuacion > 5)
            return ResultadoOperacion.rechazada("La puntuacion debe estar entre 1 y 5");
        String texto = comentario == null ? "" : comentario.trim();
        if (texto.length() > 500)
            return ResultadoOperacion.rechazada("Comentario demasiado largo");
        if (!proveedor.citaPertenecePaciente(idCita, idPaciente))
            return ResultadoOperacion.rechazada("La cita no pertenece al paciente");
        if (!proveedor.pruebaRealizada(idCita))
            return ResultadoOperacion.rechazada("La prueba todavia no se ha realizado");
        if (encuestas.containsKey(idCita))
            return ResultadoOperacion.rechazada("Ya existe una encuesta para esta cita");
        encuestas.put(idCita, new EncuestaSatisfaccion(idPaciente, idCita,
                puntuacion, texto, LocalDateTime.now()));
        return ResultadoOperacion.correcta("Encuesta registrada correctamente");
    }
}
