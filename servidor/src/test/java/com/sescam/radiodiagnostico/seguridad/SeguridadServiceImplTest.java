package com.sescam.radiodiagnostico.seguridad;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.roles.IRolesService;
import com.sescam.radiodiagnostico.roles.Permiso;
import com.sescam.radiodiagnostico.roles.RolesException;

class SeguridadServiceImplTest {

    private SeguridadServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new SeguridadServiceImpl(new RolesStub());
    }

    private static class RolesStub implements IRolesService {

        @Override
        public Rol consultarRol(String usuario) {

            if ("desconocido".equals(usuario)) {
                throw new RolesException(
                        RolesException.Codigo.USUARIO_NO_ENCONTRADO,
                        "No existe");
            }

            return Rol.ADMINISTRADOR;
        }

        @Override
        public boolean tienePermiso(
                String usuario,
                Permiso permiso) {

            return "admin".equals(usuario);
        }

        @Override
        public Map<String, Rol> listarUsuarios() {
            return Map.of();
        }

        @Override
        public Rol cambiarRol(
                String administrador,
                String usuario,
                Rol nuevoRol) {

            return nuevoRol;
        }

        @Override
        public Set<Permiso> consultarPermisos(
                Rol rol) {

            return Set.of();
        }

        @Override
        public Set<Permiso> concederPermiso(
                Rol rol,
                Permiso permiso) {

            return Set.of();
        }

        @Override
        public Set<Permiso> retirarPermiso(
                Rol rol,
                Permiso permiso) {

            return Set.of();
        }
    }

    @Test
    void constructorNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SeguridadServiceImpl(null));
    }

    @Test
    void registrarCorrectamente() {

        assertTrue(
                servicio.registrarAuditoria(
                        "admin",
                        "LOGIN",
                        "correcto"));
    }

    @Test
    void usuarioNull() {

        assertFalse(
                servicio.registrarAuditoria(
                        null,
                        "LOGIN",
                        "ok"));
    }

    @Test
    void usuarioVacio() {

        assertFalse(
                servicio.registrarAuditoria(
                        " ",
                        "LOGIN",
                        "ok"));
    }

    @Test
    void accionNull() {

        assertFalse(
                servicio.registrarAuditoria(
                        "admin",
                        null,
                        "ok"));
    }

    @Test
    void accionVacia() {

        assertFalse(
                servicio.registrarAuditoria(
                        "admin",
                        "",
                        "ok"));
    }

    @Test
    void detallesNull() {

        assertTrue(
                servicio.registrarAuditoria(
                        "admin",
                        "LOGIN",
                        null));
    }

    @Test
    void rolDesconocido() {

        servicio.registrarAuditoria(
                "desconocido",
                "LOGIN",
                "ok");

        String log =
                servicio.consultarLogs().get(0);

        assertTrue(
                log.contains("DESCONOCIDO"));
    }

    @Test
    void incluyeRolAdministrador() {

        servicio.registrarAuditoria(
                "admin",
                "LOGIN",
                "ok");

        String log =
                servicio.consultarLogs().get(0);

        assertTrue(
                log.contains("ADMINISTRADOR"));
    }

    @Test
    void puedeConsultar() {

        assertTrue(
                servicio.puedeConsultarAuditoria(
                        "admin"));
    }

    @Test
    void noPuedeConsultar() {

        assertFalse(
                servicio.puedeConsultarAuditoria(
                        "usuario"));
    }

    @Test
    void usuarioNullConsultar() {

        assertFalse(
                servicio.puedeConsultarAuditoria(
                        null));
    }

    @Test
    void usuarioVacioConsultar() {

        assertFalse(
                servicio.puedeConsultarAuditoria(
                        " "));
    }

    @Test
    void logsVacios() {

        assertEquals(
                0,
                servicio.consultarLogs().size());
    }

    @Test
    void logsConContenido() {

        servicio.registrarAuditoria(
                "admin",
                "LOGIN",
                "ok");

        assertEquals(
                1,
                servicio.consultarLogs().size());
    }

    @Test
    void logsInmutables() {

        servicio.registrarAuditoria(
                "admin",
                "LOGIN",
                "ok");

        List<String> logs =
                servicio.consultarLogs();

        assertThrows(
                UnsupportedOperationException.class,
                () -> logs.add("otro"));
    }
}