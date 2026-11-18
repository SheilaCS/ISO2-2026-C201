package com.sescam.radiodiagnostico.autenticacion;

import java.util.Optional;

/**
 * CONTRATO DEL SERVICIO DE AUTENTICACIÓN (CDU1, RF01).
 * El cliente solo depende de esta interfaz.
 */
public interface IAutenticacionService {
    Optional<Sesion> iniciarSesion(String usuario, char[] clave);
}
