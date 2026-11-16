package com.sescam.radiodiagnostico.cdu05_parametros;

import java.util.HashMap;
import java.util.Map;

public class ParametroServiceImpl implements IParametroService {
    private Map<String, String> baseDatosParametros = new HashMap<>();

    @Override
    public void guardarParametro(String clave, String valor) {
        if (clave != null && valor != null) {
            baseDatosParametros.put(clave, valor);
        }
    }

    @Override
    public String obtenerParametro(String clave) {
        return baseDatosParametros.getOrDefault(clave, "No encontrado");
    }
}