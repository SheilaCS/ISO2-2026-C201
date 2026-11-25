package com.sescam.radiodiagnostico.historial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sescam.radiodiagnostico.autenticacion.Rol;
import com.sescam.radiodiagnostico.autenticacion.Sesion;
import com.sescam.radiodiagnostico.historial.ResultadoHistorial.Estado;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU12 · Servidor · HistorialServiceImpl")
class HistorialServiceImplTest {

    private static final Sesion PACIENTE =
            new Sesion("paciente1", Rol.PACIENTE);
    private static final Sesion MEDICO =
            new Sesion("medico1", Rol.MEDICO);

    private ProveedorHistorialEnMemoria proveedor;
    private RegistroConsultas registro;
    private VinculoPacienteEnMemoria vinculo;
    private HistorialServiceImpl servicio;

    @BeforeEach
    void preparar() {
        proveedor = new ProveedorHistorialEnMemoria();
        registro = new RegistroConsultas();
        vinculo = new VinculoPacienteEnMemoria("");
        vinculo.vincular("paciente1", "paciente1");
        servicio = new HistorialServiceImpl(proveedor, registro, vinculo);

        proveedor.agregar("paciente1", new PruebaRealizada(
                "P1", LocalDate.of(2026, 1, 10), "Radiografia",
                "REALIZADA", "Sin hallazgos"));
        proveedor.agregar("paciente1", new PruebaRealizada(
                "P2", LocalDate.of(2026, 3, 5), "Ecografia",
                "REALIZADA", "Control"));
        proveedor.agregar("paciente2", new PruebaRealizada(
                "P3", LocalDate.of(2026, 2, 1), "Resonancia",
                "REALIZADA", "Pendiente de informe"));
    }

    @Test
    @DisplayName("El paciente consulta su historial, de la prueba más reciente a la más antigua")
    void pacienteConsultaSuHistorial() {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(PACIENTE, "paciente1");

        // Comprobar
        assertAll(
                () -> assertEquals(Estado.CORRECTO, resultado.estado()),
                () -> assertEquals(2, resultado.pruebas().size()),
                () -> assertEquals("P2", resultado.pruebas().get(0).id()),
                () -> assertEquals("P1", resultado.pruebas().get(1).id())
        );
    }

    @Test
    @DisplayName("El médico consulta el historial de cualquier paciente")
    void medicoConsultaCualquierPaciente() {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(MEDICO, "paciente2");

        // Comprobar
        assertAll(
                () -> assertEquals(Estado.CORRECTO, resultado.estado()),
                () -> assertEquals(1, resultado.pruebas().size()),
                () -> assertEquals("P3", resultado.pruebas().get(0).id())
        );
    }

    @Test
    @DisplayName("Acceso no autorizado: un paciente no puede consultar el historial de otro")
    void pacienteNoConsultaHistorialAjeno() {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(PACIENTE, "paciente2");

        // Comprobar
        assertAll(
                () -> assertEquals(Estado.PROHIBIDO, resultado.estado()),
                () -> assertTrue(resultado.pruebas().isEmpty())
        );
    }

    // Tabla de casos del vinculo: clases de equivalencia y valores limite
    // del identificador pedido por un paciente cuyo usuario es
    // "paciente.demo" y cuyo numero de paciente es 1
    @ParameterizedTest(name = "paciente.demo pide \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "1|CORRECTO",
            "2|PROHIBIDO",
            "01|PROHIBIDO",
            "10|PROHIBIDO",
            "0|PROHIBIDO",
            "paciente.demo|PROHIBIDO"
    })
    @DisplayName("El paciente solo accede al número de paciente vinculado a su usuario, no al nombre de usuario")
    void pacienteConVinculoDistintoDelUsuario(String pedido, Estado esperado) {
        // Preparar
        vinculo.vincular("paciente.demo", "1");
        proveedor.agregar("1", new PruebaRealizada(
                "R1", LocalDate.of(2026, 4, 1), "Radiografia",
                "REALIZADA", "Normal"));
        Sesion sesion = new Sesion("paciente.demo", Rol.PACIENTE);

        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(sesion, pedido);

        // Comprobar
        assertAll(
                () -> assertEquals(esperado, resultado.estado()),
                () -> assertEquals(esperado == Estado.CORRECTO ? 1 : 0,
                        resultado.pruebas().size())
        );
    }

    @Test
    @DisplayName("Un identificador con espacios alrededor se acepta si es el del paciente")
    void identificadorConEspacios() {
        // Preparar
        vinculo.vincular("paciente.demo", "1");
        Sesion sesion = new Sesion("paciente.demo", Rol.PACIENTE);

        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(sesion, " 1 ");

        // Comprobar
        assertEquals(Estado.CORRECTO, resultado.estado());
    }

    @Test
    @DisplayName("Un paciente sin número vinculado se rechaza y la consulta queda anotada")
    void pacienteSinVinculo() {
        // Preparar
        Sesion sinVinculo = new Sesion("paciente.nuevo", Rol.PACIENTE);

        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(sinVinculo, "paciente1");

        // Comprobar
        List<RegistroConsultas.Entrada> entradas = registro.consultas();
        assertAll(
                () -> assertEquals(Estado.PROHIBIDO, resultado.estado()),
                () -> assertEquals("Su usuario no tiene un paciente asociado.",
                        resultado.mensaje()),
                () -> assertTrue(resultado.pruebas().isEmpty()),
                () -> assertEquals(1, entradas.size()),
                () -> assertEquals(Estado.PROHIBIDO,
                        entradas.get(0).resultado())
        );
    }

    @Test
    @DisplayName("El vínculo solo limita al paciente: el médico consulta cualquier número")
    void medicoNoNecesitaVinculo() {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(MEDICO, "paciente1");

        // Comprobar
        assertEquals(Estado.CORRECTO, resultado.estado());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class,
            names = {"ADMINISTRATIVO", "ADMINISTRADOR"})
    @DisplayName("Acceso no autorizado: administrativos y administradores no consultan historiales")
    void rolesSinPermisoSonRechazados(Rol rol) {
        // Preparar
        Sesion sesion = new Sesion("usuario", rol);

        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(sesion, "paciente1");

        // Comprobar
        assertAll(
                () -> assertEquals(Estado.PROHIBIDO, resultado.estado()),
                () -> assertTrue(resultado.pruebas().isEmpty())
        );
    }

    @Test
    @DisplayName("Acceso no autorizado: sin sesión se rechaza la consulta")
    void sinSesionSeRechaza() {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(null, "paciente1");

        // Comprobar
        assertAll(
                () -> assertEquals(Estado.NO_AUTENTICADO, resultado.estado()),
                () -> assertTrue(resultado.pruebas().isEmpty())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Un identificador de paciente vacío o nulo se rechaza como dato incorrecto")
    void identificadorIncorrecto(String idPaciente) {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(MEDICO, idPaciente);

        // Comprobar
        assertEquals(Estado.DATOS_INVALIDOS, resultado.estado());
    }

    @Test
    @DisplayName("Un paciente sin pruebas realizadas recibe un historial vacío")
    void historialVacio() {
        // Ejecutar
        ResultadoHistorial resultado =
                servicio.consultarHistorial(MEDICO, "paciente9");

        // Comprobar
        assertAll(
                () -> assertEquals(Estado.CORRECTO, resultado.estado()),
                () -> assertTrue(resultado.pruebas().isEmpty())
        );
    }

    @Test
    @DisplayName("Si el origen de pruebas no está disponible se informa del error")
    void origenNoDisponible() {
        // Preparar
        IHistorialPruebasProveedor caido = idPaciente -> {
            throw new IllegalStateException("Sin conexion");
        };
        HistorialServiceImpl servicioCaido =
                new HistorialServiceImpl(caido, registro, vinculo);

        // Ejecutar
        ResultadoHistorial resultado =
                servicioCaido.consultarHistorial(MEDICO, "paciente1");

        // Comprobar
        assertEquals(Estado.NO_DISPONIBLE, resultado.estado());
    }

    @Test
    @DisplayName("Cada consulta, aceptada o rechazada, queda anotada en el registro")
    void registraConsultas() {
        // Ejecutar
        servicio.consultarHistorial(PACIENTE, "paciente1");
        servicio.consultarHistorial(PACIENTE, "paciente2");
        servicio.consultarHistorial(null, "paciente1");

        // Comprobar
        List<RegistroConsultas.Entrada> entradas = registro.consultas();
        assertAll(
                () -> assertEquals(3, entradas.size()),
                () -> assertEquals(Estado.CORRECTO,
                        entradas.get(0).resultado()),
                () -> assertEquals("paciente1", entradas.get(0).usuario()),
                () -> assertEquals(Rol.PACIENTE, entradas.get(0).rol()),
                () -> assertNotNull(entradas.get(0).momento()),
                () -> assertNull(entradas.get(2).rol()),
                () -> assertEquals(Estado.PROHIBIDO,
                        entradas.get(1).resultado()),
                () -> assertEquals("paciente2",
                        entradas.get(1).idPaciente()),
                () -> assertEquals(Estado.NO_AUTENTICADO,
                        entradas.get(2).resultado())
        );
    }
}
