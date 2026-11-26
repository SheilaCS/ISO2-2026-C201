package com.sescam.radiodiagnostico.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SeguridadServiceImplTest {

    private SeguridadServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new SeguridadServiceImpl();
    }

    // ==========================
    // verificarPermiso()
    // ==========================

    @Test
    @DisplayName("Administrador puede gestionar usuarios")
    void testAdministradorGestionUsuarios() {

        assertTrue(
                servicio.verificarPermiso(
                        "admin",
                        "ADMINISTRADOR",
                        "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("Paciente no puede gestionar usuarios")
    void testPacienteGestionUsuarios() {

        assertFalse(
                servicio.verificarPermiso(
                        "juan",
                        "PACIENTE",
                        "GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("Paciente puede ver historial")
    void testPacienteVerHistorial() {

        assertTrue(
                servicio.verificarPermiso(
                        "juan",
                        "PACIENTE",
                        "VER_HISTORIAL"));
    }

    @Test
    @DisplayName("Medico puede realizar accion genérica")
    void testMedicoOperacionGenerica() {

        assertTrue(
                servicio.verificarPermiso(
                        "medico1",
                        "MEDICO",
                        "CONSULTAR"));
    }

    @Test
    @DisplayName("Usuario nulo")
    void testUsuarioNulo() {

        assertFalse(
                servicio.verificarPermiso(
                        null,
                        "ADMINISTRADOR",
                        "CONSULTAR"));
    }

    @Test
    @DisplayName("Rol nulo")
    void testRolNulo() {

        assertFalse(
                servicio.verificarPermiso(
                        "juan",
                        null,
                        "CONSULTAR"));
    }

    @Test
    @DisplayName("Accion nula")
    void testAccionNula() {

        assertFalse(
                servicio.verificarPermiso(
                        "juan",
                        "ADMINISTRADOR",
                        null));
    }

    // ==========================
    // registrarAuditoria()
    // ==========================

    @Test
    @DisplayName("Registrar auditoria correctamente")
    void testRegistrarAuditoria() {

        assertTrue(
                servicio.registrarAuditoria(
                        "juan",
                        "CONSULTAR",
                        "Operación realizada"));
    }

    @Test
    @DisplayName("Registrar auditoria con usuario nulo")
    void testRegistrarAuditoriaUsuarioNulo() {

        assertFalse(
                servicio.registrarAuditoria(
                        null,
                        "CONSULTAR",
                        "Detalle"));
    }

    @Test
    @DisplayName("Registrar auditoria con accion nula")
    void testRegistrarAuditoriaAccionNula() {

        assertFalse(
                servicio.registrarAuditoria(
                        "juan",
                        null,
                        "Detalle"));
    }

    // ==========================
    // obtenerLogs()
    // ==========================

    @Test
    @DisplayName("Logs vacíos")
    void testObtenerLogsVacios() {

        assertEquals(
                "No hay registros de auditoría.",
                servicio.obtenerLogs());
    }

    @Test
    @DisplayName("Logs con un registro")
    void testObtenerLogsConRegistro() {

        servicio.registrarAuditoria(
                "juan",
                "CONSULTAR",
                "Acceso correcto");

        String logs =
                servicio.obtenerLogs();

        assertTrue(
                logs.contains("juan"));

        assertTrue(
                logs.contains("CONSULTAR"));

        assertTrue(
                logs.contains("Acceso correcto"));
    }

    @Test
    @DisplayName("Logs con varios registros")
    void testObtenerLogsVariosRegistros() {

        servicio.registrarAuditoria(
                "juan",
                "CONSULTAR",
                "Detalle 1");

        servicio.registrarAuditoria(
                "maria",
                "MODIFICAR",
                "Detalle 2");

        String logs =
                servicio.obtenerLogs();

        assertTrue(
                logs.contains("juan"));

        assertTrue(
                logs.contains("maria"));

        assertTrue(
                logs.contains("Detalle 1"));

        assertTrue(
                logs.contains("Detalle 2"));
    }
}