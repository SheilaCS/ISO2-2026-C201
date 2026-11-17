
package com.sescam.radiodiagnostico.agendas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GestionAgendasServiceTest {

    private GestionAgendasServiceImpl servicio;
    private LocalDate fechaFutura;

    // Preparar un servicio nuevo antes de cada prueba
    @BeforeEach
    public void preparar() {
        servicio = new GestionAgendasServiceImpl();
        fechaFutura = LocalDate.now().plusDays(5);
    }

    // -----------------------------------------
    // PRUEBAS DE CREAR HORARIO
    // -----------------------------------------

    @Test
    @DisplayName("Crear un horario con datos correctos")
    public void testCrearHorarioCorrecto() {

        // Preparar
        int idMedico = 1;

        // Ejecutar
        boolean resultado = servicio.crearHorario(
                idMedico, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        // Comprobar
        assertTrue(resultado);
    }

    @Test
    @DisplayName("No permitir un ID de medico negativo")
    public void testIdMedicoNegativo() {

        boolean resultado = servicio.crearHorario(
                -1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir un ID de medico igual a cero")
    public void testIdMedicoCero() {

        boolean resultado = servicio.crearHorario(
                0, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("Permitir el ID minimo valido")
    public void testIdMedicoMinimoValido() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("No permitir una fecha pasada")
    public void testFechaPasada() {

        boolean resultado = servicio.crearHorario(
                1, LocalDate.now().minusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("Permitir la fecha actual")
    public void testFechaActual() {

        boolean resultado = servicio.crearHorario(
                1, LocalDate.now(),
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("No permitir una fecha nula")
    public void testFechaNula() {

        boolean resultado = servicio.crearHorario(
                1, null,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir una hora inicial nula")
    public void testCrearHoraInicialNula() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                null, LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir una hora final nula")
    public void testCrearHoraFinalNula() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), null);

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir horas iguales")
    public void testHorasIguales() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(9, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir horas invertidas")
    public void testHoraFinalAnterior() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(12, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir horarios solapados")
    public void testHorariosSolapados() {

        // Preparar
        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(11, 0));

        // Ejecutar
        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        // Comprobar
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Permitir horarios consecutivos")
    public void testHorariosConsecutivos() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Permitir horarios consecutivos en orden inverso")
    public void testHorariosConsecutivosInversos() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Permitir dos medicos en el mismo horario")
    public void testDosMedicosMismoHorario() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.crearHorario(
                2, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Permitir horarios en fechas distintas")
    public void testCrearHorariosDiferentesFechas() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.crearHorario(
                1, fechaFutura.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Permitir un horario anterior sin solapamiento")
    public void testCrearHorarioAnterior() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    // -----------------------------------------
    // PRUEBAS DE CONSULTAR AGENDA
    // -----------------------------------------

    @Test
    @DisplayName("Consultar una agenda con horarios")
    public void testConsultarAgendaExistente() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        String resultado = servicio.consultarAgenda(1);

        assertNotNull(resultado);
        assertTrue(resultado.contains("Horario 1"));
        assertTrue(resultado.contains("09:00"));
    }

    @Test
    @DisplayName("Consultar una agenda vacia")
    public void testConsultarAgendaVacia() {

        String resultado = servicio.consultarAgenda(1);

        assertEquals(
                "No hay horarios para este médico", resultado);
    }

    @Test
    @DisplayName("Rechazar la consulta con ID negativo")
    public void testConsultarMedicoIncorrecto() {

        String resultado = servicio.consultarAgenda(-1);

        assertEquals("ID de médico no válido", resultado);
    }

    @Test
    @DisplayName("Rechazar la consulta con ID cero")
    public void testConsultarIdCero() {

        String resultado = servicio.consultarAgenda(0);

        assertEquals("ID de médico no válido", resultado);
    }

    @Test
    @DisplayName("Consultar solamente los horarios del medico indicado")
    public void testConsultarDistintosMedicos() {

        // Preparar
        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        servicio.crearHorario(
                2, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        // Ejecutar
        String resultado = servicio.consultarAgenda(1);

        // Comprobar
        assertTrue(resultado.contains("Horario 1"));
        assertFalse(resultado.contains("Horario 2"));
    }

    // -----------------------------------------
    // PRUEBAS DE MODIFICAR HORARIO
    // -----------------------------------------

    @Test
    @DisplayName("Modificar un horario correctamente")
    public void testModificarHorarioCorrecto() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertTrue(resultado);

        String agenda = servicio.consultarAgenda(1);
        assertTrue(agenda.contains("11:00"));
    }

    @Test
    @DisplayName("No modificar un horario inexistente")
    public void testModificarHorarioInexistente() {

        boolean resultado = servicio.modificarHorario(
                99, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario con fecha nula")
    public void testModificarFechaNula() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, null,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario con hora inicial nula")
    public void testModificarHoraInicialNula() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                null, LocalTime.of(12, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario con hora final nula")
    public void testModificarHoraFinalNula() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), null);

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario con fecha pasada")
    public void testModificarFechaPasada() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, LocalDate.now().minusDays(1),
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario con horas iguales")
    public void testModificarHorasIguales() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(11, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario con horas invertidas")
    public void testModificarHorasInvertidas() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                LocalTime.of(12, 0), LocalTime.of(11, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No modificar un horario si provoca solapamiento")
    public void testModificarHorarioSolapado() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        boolean resultado = servicio.modificarHorario(
                2, fechaFutura,
                LocalTime.of(9, 30), LocalTime.of(10, 30));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("Modificar un horario conservando su intervalo")
    public void testModificarMismoHorario() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Modificar horarios de distintos medicos")
    public void testModificarDistintosMedicos() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        servicio.crearHorario(
                2, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        boolean resultado = servicio.modificarHorario(
                2, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Modificar horarios en distintas fechas")
    public void testModificarDistintasFechas() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        servicio.crearHorario(
                1, fechaFutura.plusDays(1),
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        boolean resultado = servicio.modificarHorario(
                2, fechaFutura.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Modificar sin solapamiento posterior")
    public void testModificarSinSolapamientoPosterior() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        boolean resultado = servicio.modificarHorario(
                2, fechaFutura,
                LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Modificar sin solapamiento anterior")
    public void testModificarSinSolapamientoAnterior() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(11, 0), LocalTime.of(12, 0));

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(13, 0), LocalTime.of(14, 0));

        boolean resultado = servicio.modificarHorario(
                2, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Conservar el horario original tras una modificacion fallida")
    public void testConservarHorarioTrasError() {

        // Preparar
        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        // Ejecutar
        boolean resultado = servicio.modificarHorario(
                1, fechaFutura,
                LocalTime.of(12, 0), LocalTime.of(11, 0));

        // Comprobar
        assertFalse(resultado);

        String agenda = servicio.consultarAgenda(1);

        assertTrue(agenda.contains("09:00"));
        assertTrue(agenda.contains("10:00"));
        assertFalse(agenda.contains("12:00"));
    }

    // -----------------------------------------
    // PRUEBAS DE ELIMINAR HORARIO
    // -----------------------------------------

    @Test
    @DisplayName("Eliminar un horario existente")
    public void testEliminarHorarioCorrecto() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.eliminarHorario(1);

        assertTrue(resultado);

        assertEquals("No hay horarios para este médico",
                servicio.consultarAgenda(1));
    }

    @Test
    @DisplayName("No eliminar un horario inexistente")
    public void testEliminarHorarioInexistente() {

        boolean resultado = servicio.eliminarHorario(99);

        assertFalse(resultado);
    }
}
