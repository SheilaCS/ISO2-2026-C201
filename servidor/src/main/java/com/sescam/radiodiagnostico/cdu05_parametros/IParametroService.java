package com.sescam.radiodiagnostico.cdu05_parametros;

public interface IParametroService {
    void guardarParametro(String clave, String valor);
    String obtenerParametro(String clave);
}