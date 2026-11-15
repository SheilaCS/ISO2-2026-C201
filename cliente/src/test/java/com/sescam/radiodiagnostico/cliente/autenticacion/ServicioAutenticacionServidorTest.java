package com.sescam.radiodiagnostico.cliente.autenticacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.sescam.radiodiagnostico.autenticacion.IAutenticacionService;
import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;

/**
 * Pruebas del adaptador que conecta la pantalla del cliente con el servidor (CDU1, RF01).
 * Se usa un servidor falso para probar solo la conversión que hace el adaptador.
 */
@DisplayName("CDU1 · Cliente · ServicioAutenticacionServidor (adaptador)")
class ServicioAutenticacionServidorTest {

    @Test
    @DisplayName("A1. La sesión del servidor se convierte en SesionUsuario")
    void sesionDelServidorSeConvierte() {
        // Preparar
        IAutenticacionService servidorFalso =
                (usuario, clave) -> Optional.of(new Sesion("medico.demo", Rol.MEDICO));
        ServicioAutenticacion adaptador = new ServicioAutenticacionServidor(servidorFalso);

        // Ejecutar
        SesionUsuario sesion = adaptador.iniciarSesion("medico.demo", "demo".toCharArray());

        // Comprobar
        assertNotNull(sesion);
        assertAll(
                () -> assertEquals("medico.demo", sesion.identificador()),
                () -> assertEquals("MEDICO", sesion.rol()));
    }

    @Test
    @DisplayName("A2. Cada uno de los 4 roles llega al cliente con su nombre")
    void cadaRolLlegaConSuNombre() {
        assertAll(Arrays.stream(Rol.values()).map(rol -> (Executable) () -> {
            // Preparar
            IAutenticacionService servidorFalso =
                    (usuario, clave) -> Optional.of(new Sesion("usuario.demo", rol));

            // Ejecutar
            SesionUsuario sesion = new ServicioAutenticacionServidor(servidorFalso)
                    .iniciarSesion("usuario.demo", "demo".toCharArray());

            // Comprobar
            assertEquals(rol.name(), sesion.rol(), "Rol mal convertido: " + rol);
        }));
    }

    @Test
    @DisplayName("A3. Si el servidor rechaza el acceso, el adaptador devuelve null")
    void rechazoDelServidorDevuelveNull() {
        // Preparar
        IAutenticacionService servidorFalso = (usuario, clave) -> Optional.empty();

        // Ejecutar
        SesionUsuario sesion = new ServicioAutenticacionServidor(servidorFalso)
                .iniciarSesion("medico.demo", "mal".toCharArray());

        // Comprobar
        assertNull(sesion);
    }

    @Test
    @DisplayName("A4. El adaptador pasa al servidor el mismo usuario y la misma clave")
    void pasaAlServidorLosMismosDatos() {
        // Preparar: el servidor falso guarda lo que recibe
        String[] usuarioRecibido = new String[1];
        char[][] claveRecibida = new char[1][];
        IAutenticacionService servidorFalso = (usuario, clave) -> {
            usuarioRecibido[0] = usuario;
            claveRecibida[0] = clave;
            return Optional.empty();
        };

        // Ejecutar
        new ServicioAutenticacionServidor(servidorFalso)
                .iniciarSesion("medico.demo", "demo".toCharArray());

        // Comprobar
        assertAll(
                () -> assertEquals("medico.demo", usuarioRecibido[0]),
                () -> assertArrayEquals("demo".toCharArray(), claveRecibida[0]));
    }
}
