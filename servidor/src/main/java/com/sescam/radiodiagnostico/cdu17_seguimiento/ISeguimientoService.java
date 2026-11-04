package com.sescam.radiodiagnostico.cdu17_seguimiento;
import java.util.List;
public interface ISeguimientoService {
    List<String> consultarSeguimiento(String idPaciente);
}