package com.sescam.radiodiagnostico.pacientes;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class PacientesServiceImplTest {

    private PacientesServiceImpl servicio;

    private static final LocalDate FECHA =
            LocalDate.of(2000, 5, 15);

    @BeforeEach
    void prepararServicio() {
        servicio = new PacientesServiceImpl(
                new RepositorioPacientesEnMemoria()
        );
    }

    @Test
    @DisplayName("El alta normaliza los datos y guarda un paciente activo")
    void altaValida() {
        // Preparar y ejecutar
        Paciente paciente = servicio.altaPaciente(
                " pac001 ", " Ana ", " García López ", FECHA
        );

        // Comprobar
        assertAll(
                () -> assertNotNull(paciente.id()),
                () -> assertEquals("PAC001", paciente.cip()),
                () -> assertEquals("Ana", paciente.nombre()),
                () -> assertEquals("García López", paciente.apellidos()),
                () -> assertEquals(FECHA, paciente.fechaNacimiento()),
                () -> assertTrue(paciente.activo()),
                () -> assertEquals(
                        paciente,
                        servicio.consultarPorCip(" pac001 ").orElseThrow()
                )
        );
    }

    @Test
    @DisplayName("Un CIP duplicado se rechaza aunque cambien mayúsculas y espacios")
    void rechazaCipDuplicado() {
        // Preparar
        Paciente original = registrarPaciente();

        // Ejecutar
        PacienteException error = assertThrows(
                PacienteException.class,
                () -> servicio.altaPaciente(
                        " pac001 ", "Otra", "Persona", FECHA
                )
        );

        // Comprobar
        assertAll(
                () -> assertEquals(
                        PacienteException.Codigo.CIP_DUPLICADO,
                        error.getCodigo()
                ),
                () -> assertEquals(
                        original,
                        servicio.consultarPorCip("PAC001").orElseThrow()
                )
        );
    }

    @Test
    @DisplayName("La consulta de un CIP inexistente devuelve un resultado vacío")
    void consultaInexistente() {
        // Ejecutar y comprobar
        assertTrue(servicio.consultarPorCip("NOEXISTE").isEmpty());
    }

    @Test
    @DisplayName("La modificación cambia los datos editables y conserva la identidad")
    void modificaPacienteActivo() {
        // Preparar
        Paciente original = registrarPaciente();
        LocalDate nuevaFecha = LocalDate.of(1995, 3, 10);

        // Ejecutar
        Paciente actualizado = servicio.modificarPaciente(
                original.id(), " Luis ", " Pérez ", nuevaFecha
        );

        // Comprobar
        assertAll(
                () -> assertEquals(original.id(), actualizado.id()),
                () -> assertEquals(original.cip(), actualizado.cip()),
                () -> assertEquals("Luis", actualizado.nombre()),
                () -> assertEquals("Pérez", actualizado.apellidos()),
                () -> assertEquals(
                        nuevaFecha, actualizado.fechaNacimiento()
                ),
                () -> assertTrue(actualizado.activo()),
                () -> assertEquals(
                        actualizado,
                        servicio.consultarPorCip("PAC001").orElseThrow()
                ),
                () -> assertEquals("Ana", original.nombre())
        );
    }

    @Test
    @DisplayName("Una modificación inválida no guarda cambios parciales")
    void modificacionInvalidaConservaDatos() {
        // Preparar
        Paciente original = registrarPaciente();

        // Ejecutar
        PacienteException error = assertThrows(
                PacienteException.class,
                () -> servicio.modificarPaciente(
                        original.id(),
                        "Nombre nuevo",
                        "Apellidos nuevos",
                        LocalDate.now().plusDays(1)
                )
        );

        // Comprobar
        assertAll(
                () -> assertEquals(
                        PacienteException.Codigo.DATOS_INVALIDOS,
                        error.getCodigo()
                ),
                () -> assertEquals(
                        original,
                        servicio.consultarPorCip("PAC001").orElseThrow()
                )
        );
    }

    @Test
    @DisplayName("La baja conserva los datos e impide reutilizar el CIP")
    void bajaLogicaConservaPaciente() {
        // Preparar
        Paciente original = registrarPaciente();

        // Ejecutar
        servicio.bajaPaciente(original.id());

        // Comprobar
        Paciente inactivo = servicio.consultarPorCip("PAC001")
                .orElseThrow();

        assertAll(
                () -> assertFalse(inactivo.activo()),
                () -> assertEquals(original.id(), inactivo.id()),
                () -> assertEquals(original.nombre(), inactivo.nombre()),
                () -> assertEquals(
                        original.apellidos(), inactivo.apellidos()
                ),
                () -> assertEquals(
                        original.fechaNacimiento(),
                        inactivo.fechaNacimiento()
                ),
                () -> assertEquals(
                        PacienteException.Codigo.CIP_DUPLICADO,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.altaPaciente(
                                        "PAC001", "Otra", "Persona", FECHA
                                )
                        ).getCodigo()
                )
        );
    }

    @Test
    @DisplayName("Un paciente inactivo no admite modificación ni una segunda baja")
    void rechazaOperacionesSobreInactivo() {
        // Preparar
        Paciente paciente = registrarPaciente();
        servicio.bajaPaciente(paciente.id());

        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(
                        PacienteException.Codigo.PACIENTE_INACTIVO,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.modificarPaciente(
                                        paciente.id(), "Luis", "Pérez", FECHA
                                )
                        ).getCodigo()
                ),
                () -> assertEquals(
                        PacienteException.Codigo.PACIENTE_INACTIVO,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.bajaPaciente(paciente.id())
                        ).getCodigo()
                )
        );
    }

    @Test
    @DisplayName("No se puede modificar ni dar de baja un paciente inexistente")
    void rechazaPacienteInexistente() {
        // Preparar
        UUID id = UUID.randomUUID();

        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(
                        PacienteException.Codigo.PACIENTE_NO_ENCONTRADO,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.modificarPaciente(
                                        id, "Ana", "García", FECHA
                                )
                        ).getCodigo()
                ),
                () -> assertEquals(
                        PacienteException.Codigo.PACIENTE_NO_ENCONTRADO,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.bajaPaciente(id)
                        ).getCodigo()
                )
        );
    }

    @Test
    @DisplayName("Las operaciones rechazan un identificador nulo")
    void rechazaIdNulo() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(
                        PacienteException.Codigo.DATOS_INVALIDOS,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.modificarPaciente(
                                        null, "Ana", "García", FECHA
                                )
                        ).getCodigo()
                ),
                () -> assertEquals(
                        PacienteException.Codigo.DATOS_INVALIDOS,
                        assertThrows(
                                PacienteException.class,
                                () -> servicio.bajaPaciente(null)
                        ).getCodigo()
                )
        );
    }

    @Test
    @DisplayName("La fecha de nacimiento igual a hoy es válida")
    void aceptaFechaActual() {
        // Preparar
        LocalDate hoy = LocalDate.now();

        // Ejecutar
        Paciente paciente = servicio.altaPaciente(
                "PAC002", "Ana", "García", hoy
        );

        // Comprobar
        assertEquals(hoy, paciente.fechaNacimiento());
    }

    @ParameterizedTest
    @MethodSource("datosInvalidos")
    @DisplayName("El alta rechaza datos obligatorios inválidos sin guardar el paciente")
    void rechazaAltaInvalida(
            String cip,
            String nombre,
            String apellidos,
            LocalDate fecha) {

        // Ejecutar
        PacienteException error = assertThrows(
                PacienteException.class,
                () -> servicio.altaPaciente(
                        cip, nombre, apellidos, fecha
                )
        );

        // Comprobar
        assertAll(
                () -> assertEquals(
                        PacienteException.Codigo.DATOS_INVALIDOS,
                        error.getCodigo()
                ),
                () -> assertTrue(
                        servicio.consultarPorCip("PAC001").isEmpty()
                )
        );
    }

    static Stream<Arguments> datosInvalidos() {
        return Stream.of(
                Arguments.of(null, "Ana", "García", FECHA),
                Arguments.of("   ", "Ana", "García", FECHA),
                Arguments.of("PAC001", null, "García", FECHA),
                Arguments.of("PAC001", "   ", "García", FECHA),
                Arguments.of("PAC001", "Ana", null, FECHA),
                Arguments.of("PAC001", "Ana", "   ", FECHA),
                Arguments.of("PAC001", "Ana", "García", null),
                Arguments.of(
                        "PAC001", "Ana", "García",
                        LocalDate.now().plusDays(1)
                )
        );
    }

    private Paciente registrarPaciente() {
        return servicio.altaPaciente(
                "PAC001", "Ana", "García López", FECHA
        );
    }
}