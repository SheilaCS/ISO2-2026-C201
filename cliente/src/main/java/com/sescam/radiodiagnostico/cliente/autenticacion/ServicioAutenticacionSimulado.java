package com.sescam.radiodiagnostico.cliente.autenticacion;

import java.util.Arrays;

public class ServicioAutenticacionSimulado
        implements ServicioAutenticacion {

    @Override
    public SesionUsuario iniciarSesion(String usuario, char[] clave) {
        char[] claveDemo = {'d', 'e', 'm', 'o'};

        try {
            if ("medico.demo".equals(usuario)
                    && Arrays.equals(clave, claveDemo)) {
                return new SesionUsuario("medico.demo", "MEDICO");
            }

            return null;
        } finally {
            Arrays.fill(claveDemo, '\0');
        }
    }
}