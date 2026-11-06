package com.sescam.radiodiagnostico.autenticacion;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;


public class AutenticacionServiceImpl implements IAutenticacionService {

    private static final String CLAVE_DEMO = "demo";

    private static final Map<String, Rol> USUARIOS = Map.of(
            "paciente.demo", Rol.PACIENTE,
            "medico.demo", Rol.MEDICO,
            "administrativo.demo", Rol.ADMINISTRATIVO,
            "admin.demo", Rol.ADMINISTRADOR
    );

    @Override
    public Optional<Sesion> iniciarSesion(String usuario, char[] clave) {
        if (usuario == null || clave == null) {
            return Optional.empty();
        }

        Rol rol = USUARIOS.get(usuario);
        char[] claveEsperada = CLAVE_DEMO.toCharArray();

        try {
            if (rol != null && Arrays.equals(clave, claveEsperada)) {
                return Optional.of(new Sesion(usuario, rol));
            }
            return Optional.empty();
        } finally {
            Arrays.fill(claveEsperada, '\0');
        }
    }
}
