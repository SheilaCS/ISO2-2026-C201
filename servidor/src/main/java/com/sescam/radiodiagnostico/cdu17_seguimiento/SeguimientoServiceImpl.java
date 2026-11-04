package com.sescam.radiodiagnostico.cdu17_seguimiento;
import java.util.List;
public class SeguimientoServiceImpl implements ISeguimientoService {
    @Override
    public List<String> consultarSeguimiento(String idPaciente) {
        return List.of("Revisión de nódulos 2026", "Control post-operatorio");
    }
}