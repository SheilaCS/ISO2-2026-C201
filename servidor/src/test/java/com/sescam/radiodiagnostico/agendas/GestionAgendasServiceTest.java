
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

    // Antes de cada test creamos un servicio independiente
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
    @DisplayName("No permitir un ID de médico negativo")
    public void testIdMedicoNegativo() {

        boolean resultado = servicio.crearHorario(
                -1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir un ID de médico igual a cero")
    public void testIdMedicoCero() {

        boolean resultado = servicio.crearHorario(
                0, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("Permitir el ID mínimo válido de médico")
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
    @DisplayName("Permitir un horario con fecha de hoy")
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
    @DisplayName("No permitir la misma hora de inicio y fin")
    public void testHorasIguales() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(9, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir una hora final anterior a la inicial")
    public void testHoraFinalAnterior() {

        boolean resultado = servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(12, 0), LocalTime.of(10, 0));

        assertFalse(resultado);
    }

    @Test
    @DisplayName("No permitir horarios solapados para el mismo médico")
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
    @DisplayName("Permitir horarios consecutivos sin solapamiento")
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
    @DisplayName("Permitir dos médicos en el mismo horario")
    public void testDosMedicosMismoHorario() {

        servicio.crearHorario(
                1, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        boolean resultado = servicio.crearHorario(
                2, fechaFutura,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(resultado);
    }

    // -----------------------------------------
    // PRUEBAS DE CONSULTAR AGENDA
    // -----------------------------------------

    @Test
    @DisplayName("Consultar la agenda de un médico con horarios")
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
    @DisplayName("Consultar un médico sin horarios")
    public void testConsultarAgendaVacia() {

        String resultado = servicio.consultarAgenda(1);

        assertEquals("No hay horarios para este médico", resultado);
    }

    @Test
    @DisplayName("Consultar con un ID de médico incorrecto")
    public void testConsultarMedicoIncorrecto() {

        String resultado = servicio.consultarAgenda(-1);

        assertEquals("ID de médico no válido", resultado);
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
