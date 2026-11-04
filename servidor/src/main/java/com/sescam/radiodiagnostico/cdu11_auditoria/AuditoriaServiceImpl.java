package com.sescam.radiodiagnostico.cdu11_auditoria;
import java.util.List;
public class AuditoriaServiceImpl implements IAuditoriaService {
    @Override
    public List<String> obtenerLogs() {
        return List.of("Acceso a informe por médico", "Modificación de informe");
    }
}