package com.sescam.radiodiagnostico.parametros;

public interface IParametroService {
    void guardarParametro(String clave, String valor);
    String obtenerParametro(String clave);
}