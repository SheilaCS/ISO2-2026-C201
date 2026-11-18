package com.sescam.radiodiagnostico.cliente.autenticacion;

import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;

/**
 * Adaptador: implementa la interfaz que usa la pantalla del cliente
 * delegando en el servicio de autenticación del servidor.
 */
public class ServicioAutenticacionServidor implements ServicioAutenticacion {

    private final IAutenticacionService servidor;

    public ServicioAutenticacionServidor(IAutenticacionService servidor) {
        this.servidor = servidor;
    }

    @Override
    public SesionUsuario iniciarSesion(String usuario, char[] clave) {
        return servidor.iniciarSesion(usuario, clave)
                .map(sesion -> new SesionUsuario(sesion.identificador(), sesion.rol().name()))
                .orElse(null);
    }
}
