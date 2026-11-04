package com.sescam.radiodiagnostico.cdu05_parametros;
public class ParametroServiceImpl implements IParametroService {
    @Override
    public void guardarParametro(String clave, String valor) {
        System.out.println("Guardado: " + clave + "=" + valor);
    }
}