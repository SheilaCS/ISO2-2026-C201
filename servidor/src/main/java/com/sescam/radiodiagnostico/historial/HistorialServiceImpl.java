package com.sescam.radiodiagnostico.historial;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sescam.radiodiagnostico.historial.ResultadoHistorial.Estado;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * SERVICIO DE HISTORIAL DE PRUEBAS.
 * Iteracion 12 - RF04.
 *
 * Aplica el control de acceso por rol y anota cada consulta.
 * Orden de las comprobaciones: sesion, rol, datos y propiedad.
 *
 * El paciente solo puede ver el historial del numero de paciente
 * que tiene vinculado su usuario (IVinculoPaciente); el servicio no
 * se fia del identificador que envia el cliente.
 */
public class HistorialServiceImpl implements IHistorialService {

    private final IHistorialPruebasProveedor proveedor;
    private final RegistroConsultas registro;
    private final IVinculoPaciente vinculo;

    public HistorialServiceImpl(IHistorialPruebasProveedor proveedor,
                                RegistroConsultas registro,
                                IVinculoPaciente vinculo) {
        this.proveedor = proveedor;
        this.registro = registro;
        this.vinculo = vinculo;
    }

    @Override
    public ResultadoHistorial consultarHistorial(Sesion sesion,
                                                 String idPaciente) {
        if (sesion == null) {
            return rechazar(null, null, idPaciente, Estado.NO_AUTENTICADO,
                    "Sesion no iniciada.");
        }

        Rol rol = sesion.rol();
        String usuario = sesion.identificador();

        if (rol != Rol.PACIENTE && rol != Rol.MEDICO) {
            return rechazar(usuario, rol, idPaciente, Estado.PROHIBIDO,
                    "Acceso reservado a pacientes y medicos.");
        }

        if (idPaciente == null || idPaciente.isBlank()) {
            return rechazar(usuario, rol, idPaciente, Estado.DATOS_INVALIDOS,
                    "Identificador de paciente incorrecto.");
        }

        if (rol == Rol.PACIENTE) {
            Optional<String> propio = vinculo.idPacienteDe(usuario);

            if (propio.isEmpty()) {
                return rechazar(usuario, rol, idPaciente, Estado.PROHIBIDO,
                        "Su usuario no tiene un paciente asociado.");
            }

            if (!propio.get().equals(idPaciente.trim())) {
                return rechazar(usuario, rol, idPaciente, Estado.PROHIBIDO,
                        "Un paciente solo puede consultar su historial.");
            }
        }

        try {
            List<PruebaRealizada> pruebas =
                    proveedor.pruebasDePaciente(idPaciente).stream()
                            .sorted(Comparator.comparing(
                                    PruebaRealizada::fecha).reversed())
                            .toList();
            registro.registrar(usuario, rol, idPaciente, Estado.CORRECTO);
            return ResultadoHistorial.correcto(pruebas);
        } catch (IllegalStateException e) {
            return rechazar(usuario, rol, idPaciente, Estado.NO_DISPONIBLE,
                    "El servicio de pruebas no esta disponible.");
        }
    }

    private ResultadoHistorial rechazar(String usuario, Rol rol,
            String idPaciente, Estado estado, String mensaje) {
        registro.registrar(usuario, rol, idPaciente, estado);
        return ResultadoHistorial.rechazado(estado, mensaje);
    }
}
