package com.sescam.radiodiagnostico.cliente.autenticacion;

public interface ServicioAutenticacion {

    SesionUsuario iniciarSesion(String usuario, char[] clave);
}