package com.sescam.radiodiagnostico.cliente.autenticacion;

import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServicioAutenticacionServidorTest {

    @Test
    void sesionDelServidorSeConvierteEnSesionDeUsuario() {
        IAutenticacionService servidorFalso =
                (usuario, clave) -> Optional.of(new Sesion(usuario, Rol.MEDICO));

        SesionUsuario sesion = new ServicioAutenticacionServidor(servidorFalso)
                .iniciarSesion("medico.demo", "demo".toCharArray());

        assertNotNull(sesion);
        assertEquals("medico.demo", sesion.identificador());
        assertEquals("MEDICO", sesion.rol());
    }

    @Test
    void rechazoDelServidorDevuelveNull() {
        IAutenticacionService servidorFalso = (usuario, clave) -> Optional.empty();

        assertNull(new ServicioAutenticacionServidor(servidorFalso)
                .iniciarSesion("medico.demo", "mala".toCharArray()));
    }
}