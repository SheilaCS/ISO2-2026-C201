package com.sescam.radiodiagnostico.listasespera;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU15 · Servidor · ListaEsperaServiceImpl")
class ListaEsperaServiceImplTest {

    private static final LocalDate DIA = LocalDate.of(2026, 12, 10);

    private ProveedorListaEsperaEnMemoria proveedor;
    private List<String> asignaciones;
    private boolean asignadorAcepta;
    private boolean asignadorCaido;
    private ListaEsperaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        proveedor = new ProveedorListaEsperaEnMemoria();
        asignaciones = new ArrayList<>();
        asignadorAcepta = true;
        asignadorCaido = false;

        IAsignadorCitas asignador = (solicitud, paciente, fecha, hora) -> {
            if (asignadorCaido) {
                throw new IllegalStateException("Sin conexion");
            }
            asignaciones.add(solicitud + "/" + paciente + "/" + fecha
                    + "/" + hora);
            return asignadorAcepta;
        };

        servicio = new ListaEsperaServiceImpl(proveedor, proveedor, asignador);

        proveedor.agregarSolicitud(new SolicitudEnEspera(
                1, 11, "Radiografia", Prioridad.ORDINARIO,
                LocalDate.of(2026, 11, 1)));
        proveedor.agregarSolicitud(new SolicitudEnEspera(
                2, 12, "Ecografia", Prioridad.URGENTE,
                LocalDate.of(2026, 11, 5)));
        proveedor.agregarSolicitud(new SolicitudEnEspera(
                3, 13, "Resonancia", Prioridad.URGENTE,
                LocalDate.of(2026, 11, 2)));
        proveedor.agregarSolicitud(new SolicitudEnEspera(
                4, 14, "TAC", Prioridad.PREFERENTE,
                LocalDate.of(2026, 11, 3)));

        proveedor.agregarHueco(new Hueco(
                7, DIA, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        proveedor.agregarHueco(new Hueco(
                5, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        proveedor.agregarHueco(new Hueco(
                6, DIA.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    private List<Integer> ids(List<SolicitudEnEspera> lista) {
        return lista.stream().map(SolicitudEnEspera::idSolicitud).toList();
    }

    // ------------------------------------
    // PRIORIDAD
    // ------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"URGENTE", "urgente", " Preferente ", "ordinario"})
    @DisplayName("Reconoce las prioridades sin importar mayúsculas ni espacios")
    void prioridadValida(String texto) {
        assertTrue(Prioridad.desde(texto).isPresent());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "MUY URGENTE", "alta"})
    @DisplayName("Rechaza prioridades nulas, vacías o desconocidas")
    void prioridadInvalida(String texto) {
        assertTrue(Prioridad.desde(texto).isEmpty());
    }

    @Test
    @DisplayName("Convierte el texto en la prioridad correcta")
    void prioridadConvertida() {
        assertAll(
                () -> assertEquals(Prioridad.URGENTE,
                        Prioridad.desde("urgente").get()),
                () -> assertEquals(Prioridad.PREFERENTE,
                        Prioridad.desde(" PREFERENTE ").get()),
                () -> assertEquals(Prioridad.ORDINARIO,
                        Prioridad.desde("Ordinario").get())
        );
    }

    // ------------------------------------
    // LISTA DE ESPERA
    // ------------------------------------

    @Test
    @DisplayName("La lista va de más a menos prioritaria y, a igual prioridad, de más antigua a más reciente")
    void ordenDeLaLista() {
        assertEquals(List.of(3, 2, 4, 1),
                ids(servicio.consultarListaEspera()));
    }

    @Test
    @DisplayName("Sin solicitudes la lista está vacía")
    void listaVacia() {
        ListaEsperaServiceImpl vacio = new ListaEsperaServiceImpl(
                new ProveedorListaEsperaEnMemoria(), proveedor,
                (s, p, f, h) -> true);

        assertTrue(vacio.consultarListaEspera().isEmpty());
    }

    @Test
    @DisplayName("Cambiar la prioridad reordena la lista")
    void cambiarPrioridadReordena() {
        // Ejecutar
        ResultadoOperacion resultado =
                servicio.cambiarPrioridad(1, Prioridad.URGENTE);

        // Comprobar: la 1 pasa a urgente y es la mas antigua de las urgentes
        assertAll(
                () -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Prioridad actualizada.",
                        resultado.getMensaje()),
                () -> assertEquals(List.of(1, 3, 2, 4),
                        ids(servicio.consultarListaEspera())),
                () -> assertEquals(Prioridad.URGENTE,
                        servicio.consultarListaEspera().get(0).prioridad())
        );
    }

    @Test
    @DisplayName("No se cambia la prioridad con un valor nulo")
    void prioridadNulaSeRechaza() {
        ResultadoOperacion resultado = servicio.cambiarPrioridad(1, null);

        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("Prioridad incorrecta.",
                        resultado.getMensaje())
        );
    }

    @Test
    @DisplayName("No se cambia la prioridad de una solicitud que no está en la lista")
    void prioridadDeSolicitudInexistente() {
        ResultadoOperacion resultado =
                servicio.cambiarPrioridad(99, Prioridad.URGENTE);

        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals(
                        "No existe esa solicitud en la lista de espera.",
                        resultado.getMensaje())
        );
    }

    @Test
    @DisplayName("Retirar una solicitud la quita de la lista")
    void retirarSolicitud() {
        // Ejecutar
        ResultadoOperacion resultado = servicio.retirarDeLista(2);

        // Comprobar
        assertAll(
                () -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals(List.of(3, 4, 1),
                        ids(servicio.consultarListaEspera()))
        );
    }

    @Test
    @DisplayName("No se puede retirar dos veces la misma solicitud ni una inexistente")
    void retirarSolicitudInvalida() {
        servicio.retirarDeLista(2);

        assertAll(
                () -> assertFalse(servicio.retirarDeLista(2).isCorrecto()),
                () -> assertFalse(servicio.retirarDeLista(99).isCorrecto())
        );
    }

    // ------------------------------------
    // HUECOS
    // ------------------------------------

    @Test
    @DisplayName("Los huecos de una fecha van ordenados por hora")
    void huecosDeUnaFecha() {
        List<Hueco> lista = servicio.consultarHuecos(DIA);

        assertAll(
                () -> assertEquals(2, lista.size()),
                () -> assertEquals(5, lista.get(0).idMedico()),
                () -> assertEquals(7, lista.get(1).idMedico())
        );
    }

    @Test
    @DisplayName("Sin fecha se devuelven todos los huecos ordenados por fecha y hora")
    void todosLosHuecos() {
        List<Hueco> lista = servicio.consultarHuecos(null);

        assertAll(
                () -> assertEquals(3, lista.size()),
                () -> assertEquals(5, lista.get(0).idMedico()),
                () -> assertEquals(7, lista.get(1).idMedico()),
                () -> assertEquals(6, lista.get(2).idMedico())
        );
    }

    @Test
    @DisplayName("Una fecha sin huecos devuelve una lista vacía")
    void fechaSinHuecos() {
        assertTrue(servicio.consultarHuecos(DIA.plusDays(30)).isEmpty());
    }

    @Test
    @DisplayName("Dos médicos con el mismo horario se ordenan por médico")
    void mismoHorarioOrdenaPorMedico() {
        proveedor.agregarHueco(new Hueco(
                3, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        List<Hueco> lista = servicio.consultarHuecos(DIA);

        assertAll(
                () -> assertEquals(3, lista.get(0).idMedico()),
                () -> assertEquals(5, lista.get(1).idMedico())
        );
    }

    // ------------------------------------
    // PROPUESTA
    // ------------------------------------

    @Test
    @DisplayName("Propone la solicitud más prioritaria con el primer hueco libre")
    void propuesta() {
        Optional<Propuesta> propuesta = servicio.proponerAsignacion();

        assertAll(
                () -> assertTrue(propuesta.isPresent()),
                () -> assertEquals(3,
                        propuesta.get().solicitud().idSolicitud()),
                () -> assertEquals(5, propuesta.get().hueco().idMedico()),
                () -> assertEquals(LocalTime.of(9, 0),
                        propuesta.get().hueco().inicio())
        );
    }

    @Test
    @DisplayName("Sin solicitudes no hay propuesta")
    void propuestaSinSolicitudes() {
        ListaEsperaServiceImpl sinSolicitudes = new ListaEsperaServiceImpl(
                new ProveedorListaEsperaEnMemoria(), proveedor,
                (s, p, f, h) -> true);

        assertTrue(sinSolicitudes.proponerAsignacion().isEmpty());
    }

    @Test
    @DisplayName("Sin huecos no hay propuesta")
    void propuestaSinHuecos() {
        ListaEsperaServiceImpl sinHuecos = new ListaEsperaServiceImpl(
                proveedor, new ProveedorListaEsperaEnMemoria(),
                (s, p, f, h) -> true);

        assertTrue(sinHuecos.proponerAsignacion().isEmpty());
    }

    // ------------------------------------
    // ASIGNAR HUECO
    // ------------------------------------

    @Test
    @DisplayName("Asignar un hueco crea la cita, saca la solicitud de la lista y ocupa el hueco")
    void asignarHueco() {
        // Ejecutar
        ResultadoOperacion resultado = servicio.asignarHueco(
                3, 5, DIA, LocalTime.of(9, 0));

        // Comprobar
        assertAll(
                () -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Cita asignada correctamente.",
                        resultado.getMensaje()),
                () -> assertEquals(List.of("3/13/" + DIA + "/09:00"),
                        asignaciones),
                () -> assertEquals(List.of(2, 4, 1),
                        ids(servicio.consultarListaEspera())),
                () -> assertEquals(1, servicio.consultarHuecos(DIA).size())
        );
    }

    @Test
    @DisplayName("Un hueco ya ocupado no se puede asignar otra vez")
    void huecoOcupado() {
        servicio.asignarHueco(3, 5, DIA, LocalTime.of(9, 0));

        ResultadoOperacion resultado = servicio.asignarHueco(
                2, 5, DIA, LocalTime.of(9, 0));

        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("El hueco no existe o ya esta ocupado.",
                        resultado.getMensaje()),
                () -> assertEquals(1, asignaciones.size())
        );
    }

    @Test
    @DisplayName("Un hueco que no existe se rechaza")
    void huecoInexistente() {
        ResultadoOperacion resultado = servicio.asignarHueco(
                3, 99, DIA, LocalTime.of(9, 0));

        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertTrue(asignaciones.isEmpty())
        );
    }

    @Test
    @DisplayName("Una solicitud que no está en la lista no se puede asignar")
    void asignarSolicitudInexistente() {
        ResultadoOperacion resultado = servicio.asignarHueco(
                99, 5, DIA, LocalTime.of(9, 0));

        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals(
                        "No existe esa solicitud en la lista de espera.",
                        resultado.getMensaje()),
                () -> assertTrue(asignaciones.isEmpty())
        );
    }

    @Test
    @DisplayName("Sin fecha o sin hora se rechaza como dato incorrecto")
    void asignarConDatosNulos() {
        assertAll(
                () -> assertEquals("Datos incorrectos.", servicio
                        .asignarHueco(3, 5, null, LocalTime.of(9, 0))
                        .getMensaje()),
                () -> assertEquals("Datos incorrectos.", servicio
                        .asignarHueco(3, 5, DIA, null).getMensaje())
        );
    }

    @Test
    @DisplayName("Si el módulo de citas rechaza la cita, la solicitud sigue en la lista")
    void citasRechazaLaAsignacion() {
        // Preparar
        asignadorAcepta = false;

        // Ejecutar
        ResultadoOperacion resultado = servicio.asignarHueco(
                3, 5, DIA, LocalTime.of(9, 0));

        // Comprobar
        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("No se ha podido asignar la cita.",
                        resultado.getMensaje()),
                () -> assertEquals(List.of(3, 2, 4, 1),
                        ids(servicio.consultarListaEspera())),
                () -> assertEquals(2, servicio.consultarHuecos(DIA).size())
        );
    }

    @Test
    @DisplayName("Si el módulo de citas no está disponible se informa del error")
    void citasCaido() {
        // Preparar
        asignadorCaido = true;

        // Ejecutar
        ResultadoOperacion resultado = servicio.asignarHueco(
                3, 5, DIA, LocalTime.of(9, 0));

        // Comprobar
        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("El servicio de citas no esta disponible.",
                        resultado.getMensaje()),
                () -> assertEquals(4, servicio.consultarListaEspera().size())
        );
    }
}
