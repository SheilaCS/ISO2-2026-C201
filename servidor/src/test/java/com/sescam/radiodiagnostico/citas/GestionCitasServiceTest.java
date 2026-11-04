
package com.sescam.radiodiagnostico.citas;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

public class GestionCitasServiceTest {

    // Fecha futura para realizar las pruebas
    private String fechaFutura() {
        return LocalDate.now().plusDays(10).toString();
    }

    @Test
    public void testAsignarCita() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        assertTrue(servicio.asignarCita(
            12, 5, fechaFutura(), "10:00"
        ));

        assertTrue(servicio.consultarCita(1).contains("PROGRAMADA"));
    }

    @Test
    public void testPacienteIncorrecto() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        assertFalse(servicio.asignarCita(
            12, -5, fechaFutura(), "10:00"
        ));
    }

    @Test
    public void testHorarioOcupado() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        assertTrue(servicio.asignarCita(
            12, 5, fechaFutura(), "10:00"
        ));

        assertFalse(servicio.asignarCita(
            13, 6, fechaFutura(), "10:00"
        ));
    }

    @Test
    public void testReprogramarCita() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        assertTrue(servicio.asignarCita(
            12, 5, fechaFutura(), "10:00"
        ));

        assertTrue(servicio.reprogramarCita(
            1, fechaFutura(), "11:30"
        ));

        assertTrue(servicio.consultarCita(1).contains("11:30"));
    }

    @Test
    public void testCancelarCita() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        assertTrue(servicio.asignarCita(
            12, 5, fechaFutura(), "10:00"
        ));

        assertTrue(servicio.cancelarCita(1));

        assertTrue(servicio.consultarCita(1).contains("CANCELADA"));
    }

    @Test
    public void testCancelarDosVeces() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        servicio.asignarCita(
            12, 5, fechaFutura(), "10:00"
        );

        assertTrue(servicio.cancelarCita(1));
        assertFalse(servicio.cancelarCita(1));
    }

    @Test
    public void testConsultarCitaInexistente() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        assertEquals(
            "No existe una cita con ese ID.",
            servicio.consultarCita(999)
        );
    }

    @Test
    public void testReprogramarCitaCancelada() {
        GestionCitasServiceImpl servicio =
            new GestionCitasServiceImpl();

        servicio.asignarCita(
            12, 5, fechaFutura(), "10:00"
        );

        servicio.cancelarCita(1);

        assertFalse(servicio.reprogramarCita(
            1, fechaFutura(), "12:00"
        ));
    }
}
