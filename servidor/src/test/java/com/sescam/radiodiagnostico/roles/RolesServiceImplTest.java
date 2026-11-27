package com.sescam.radiodiagnostico.roles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sescam.radiodiagnostico.autenticacion.Rol;

@DisplayName("CDU10 · Servidor · RolesServiceImpl")
class RolesServiceImplTest {

    private RolesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new RolesServiceImpl(new RepositorioRolesEnMemoria());
    }

    @Test
    @DisplayName("R1. Consulta el rol de un usuario existente, ignorando espacios")
    void consultaRolDeUsuarioExistente() {
        assertAll(
                () -> assertEquals(Rol.MEDICO, servicio.consultarRol("medico.demo")),
                () -> assertEquals(Rol.MEDICO, servicio.consultarRol("  medico.demo  ")));
    }

    @Test
    @DisplayName("R2. Un usuario desconocido, vacío o nulo se rechaza")
    void usuarioDesconocidoOVacioSeRechaza() {
        RolesException desconocido = assertThrows(RolesException.class,
                () -> servicio.consultarRol("nadie.demo"));
        RolesException vacio = assertThrows(RolesException.class,
                () -> servicio.consultarRol("  "));
        RolesException nulo = assertThrows(RolesException.class,
                () -> servicio.consultarRol(null));

        assertAll(
                () -> assertEquals(RolesException.Codigo.USUARIO_NO_ENCONTRADO, desconocido.getCodigo()),
                () -> assertEquals(RolesException.Codigo.DATOS_INVALIDOS, vacio.getCodigo()),
                () -> assertEquals(RolesException.Codigo.DATOS_INVALIDOS, nulo.getCodigo()));
    }

    @Test
    @DisplayName("R3. El administrador cambia el rol de otro usuario")
    void administradorCambiaRolDeOtroUsuario() {
        Rol resultado = servicio.cambiarRol("admin.demo", "medico.demo", Rol.ADMINISTRATIVO);

        assertAll(
                () -> assertEquals(Rol.ADMINISTRATIVO, resultado),
                () -> assertEquals(Rol.ADMINISTRATIVO, servicio.consultarRol("medico.demo")));
    }

    @Test
    @DisplayName("R4. Un administrador no puede cambiar su propio rol")
    void administradorNoCambiaSuPropioRol() {
        RolesException error = assertThrows(RolesException.class,
                () -> servicio.cambiarRol("admin.demo", " admin.demo ", Rol.MEDICO));

        assertAll(
                () -> assertEquals(RolesException.Codigo.OPERACION_NO_PERMITIDA, error.getCodigo()),
                () -> assertEquals(Rol.ADMINISTRADOR, servicio.consultarRol("admin.demo")));
    }

    @Test
    @DisplayName("R5. No se puede dejar el sistema sin administradores")
    void noSeQuedaSinAdministradores() {
        RolesException error = assertThrows(RolesException.class,
                () -> servicio.cambiarRol("medico.demo", "admin.demo", Rol.MEDICO));

        assertAll(
                () -> assertEquals(RolesException.Codigo.OPERACION_NO_PERMITIDA, error.getCodigo()),
                () -> assertEquals(Rol.ADMINISTRADOR, servicio.consultarRol("admin.demo")));
    }

    @Test
    @DisplayName("R6. Con dos administradores, uno puede dejar de serlo")
    void conDosAdministradoresUnoPuedeDejarDeSerlo() {
        servicio.cambiarRol("admin.demo", "medico.demo", Rol.ADMINISTRADOR);

        servicio.cambiarRol("medico.demo", "admin.demo", Rol.MEDICO);

        assertEquals(Rol.MEDICO, servicio.consultarRol("admin.demo"));
    }

    @Test
    @DisplayName("R7. Cambiar el rol de un usuario inexistente o sin indicar rol se rechaza")
    void cambioConDatosInvalidosSeRechaza() {
        RolesException inexistente = assertThrows(RolesException.class,
                () -> servicio.cambiarRol("admin.demo", "nadie.demo", Rol.MEDICO));
        RolesException sinRol = assertThrows(RolesException.class,
                () -> servicio.cambiarRol("admin.demo", "medico.demo", null));

        assertAll(
                () -> assertEquals(RolesException.Codigo.USUARIO_NO_ENCONTRADO, inexistente.getCodigo()),
                () -> assertEquals(RolesException.Codigo.DATOS_INVALIDOS, sinRol.getCodigo()));
    }

    @Test
    @DisplayName("R8. Se concede un permiso a un rol y el usuario lo obtiene")
    void concedePermiso() {
        assertFalse(servicio.tienePermiso("medico.demo", Permiso.GESTIONAR_CITAS));

        Set<Permiso> permisos = servicio.concederPermiso(Rol.MEDICO, Permiso.GESTIONAR_CITAS);

        assertAll(
                () -> assertTrue(permisos.contains(Permiso.GESTIONAR_CITAS)),
                () -> assertTrue(servicio.tienePermiso("medico.demo", Permiso.GESTIONAR_CITAS)));
    }

    @Test
    @DisplayName("R9. Se retira un permiso a un rol y el usuario lo pierde")
    void retiraPermiso() {
        Set<Permiso> permisos = servicio.retirarPermiso(Rol.ADMINISTRATIVO, Permiso.GESTIONAR_PACIENTES);

        assertAll(
                () -> assertFalse(permisos.contains(Permiso.GESTIONAR_PACIENTES)),
                () -> assertFalse(servicio.tienePermiso("administrativo.demo", Permiso.GESTIONAR_PACIENTES)));
    }

    @Test
    @DisplayName("R10. No se puede retirar al administrador la gestión de roles")
    void noSeRetiraGestionDeRolesAlAdministrador() {
        RolesException error = assertThrows(RolesException.class,
                () -> servicio.retirarPermiso(Rol.ADMINISTRADOR, Permiso.GESTIONAR_ROLES));

        assertAll(
                () -> assertEquals(RolesException.Codigo.OPERACION_NO_PERMITIDA, error.getCodigo()),
                () -> assertTrue(servicio.tienePermiso("admin.demo", Permiso.GESTIONAR_ROLES)));
    }

    @Test
    @DisplayName("R11. Un rol o un permiso nulos se rechazan")
    void rolOPermisoNulosSeRechazan() {
        assertAll(
                () -> assertThrows(RolesException.class, () -> servicio.consultarPermisos(null)),
                () -> assertThrows(RolesException.class, () -> servicio.concederPermiso(Rol.MEDICO, null)),
                () -> assertThrows(RolesException.class,
                        () -> servicio.retirarPermiso(null, Permiso.GESTIONAR_CITAS)));
    }

    @Test
    @DisplayName("R12. Sin un usuario válido no se tiene ningún permiso")
    void sinUsuarioValidoNoHayPermiso() {
        assertAll(
                () -> assertFalse(servicio.tienePermiso(null, Permiso.CONSULTAR_HISTORIAL)),
                () -> assertFalse(servicio.tienePermiso(" ", Permiso.CONSULTAR_HISTORIAL)),
                () -> assertFalse(servicio.tienePermiso("nadie.demo", Permiso.CONSULTAR_HISTORIAL)),
                () -> assertFalse(servicio.tienePermiso("medico.demo", null)));
    }

    @Test
    @DisplayName("R13. Los permisos devueltos son una copia y no cambian el sistema")
    void permisosDevueltosSonUnaCopia() {
        Set<Permiso> copia = servicio.consultarPermisos(Rol.PACIENTE);
        copia.add(Permiso.GESTIONAR_ROLES);

        assertFalse(servicio.tienePermiso("paciente.demo", Permiso.GESTIONAR_ROLES));
    }

    @Test
    @DisplayName("R14. Lista los cuatro usuarios de demostración con su rol")
    void listaUsuarios() {
        assertAll(
                () -> assertEquals(4, servicio.listarUsuarios().size()),
                () -> assertEquals(Rol.PACIENTE, servicio.listarUsuarios().get("paciente.demo")));
    }

    @Test
    @DisplayName("R15. Un rol puede quedarse sin permisos y volver a recibirlos")
    void rolSinPermisos() {
        servicio.retirarPermiso(Rol.PACIENTE, Permiso.CONSULTAR_MIS_CITAS);
        Set<Permiso> vacio = servicio.retirarPermiso(Rol.PACIENTE, Permiso.CONSULTAR_HISTORIAL);

        Set<Permiso> otraVez = servicio.concederPermiso(Rol.PACIENTE, Permiso.CONSULTAR_HISTORIAL);

        assertAll(
                () -> assertTrue(vacio.isEmpty()),
                () -> assertEquals(Set.of(Permiso.CONSULTAR_HISTORIAL), otraVez));
    }
}