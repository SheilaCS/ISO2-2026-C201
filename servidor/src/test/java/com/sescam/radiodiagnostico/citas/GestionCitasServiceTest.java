
package com.sescam.radiodiagnostico.citas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

/**
 * Pruebas unitarias de la Iteracion 3 - RF13.
 * Gestion de citas del SESCAM.
 *
 * Se comprueban las operaciones de:
 * - Asignar citas.
 * - Reprogramar citas.
 * - Cancelar citas.
 * - Consultar citas.
 *
 * Incluye casos correctos, incorrectos y valores limite.
 */
public class GestionCitasServiceTest {

    // Servicio que utilizaremos para realizar las pruebas.
    private GestionCitasServiceImpl servicio;

    /**
     * PREPARAR:
     * Antes de cada prueba se crea un servicio nuevo.
     * Asi las pruebas son independientes.
     */
    @BeforeEach
    public void preparar() {
        servicio = new GestionCitasServiceImpl();
    }

    // Devuelve una fecha futura para realizar las pruebas.
    private String fechaFutura() {
        return LocalDate.now().plusDays(10).toString();
    }

    // Devuelve una fecha pasada para realizar las pruebas.
    private String fechaPasada() {
        return LocalDate.now().minusDays(10).toString();
    }

    // ==========================================
    // PRUEBA 1: ASIGNAR CITA CORRECTAMENTE
    // ==========================================

    @Test
    @DisplayName("Asignar una cita correctamente")
    public void testAsignarCita() {

        // EJECUTAR: asignamos una cita.
        boolean resultado = servicio.asignarCita(
                12, 5, fechaFutura(), "10:00"
        );

        // Consultamos la cita creada.
        String cita = servicio.consultarCita(1);

        // COMPROBAR:
        // Verificamos que la cita se ha creado.
        assertAll(
            () -> assertTrue(resultado),
            () -> assertNotNull(cita),
            () -> assertTrue(cita.contains("PROGRAMADA")),
            () -> assertTrue(cita.contains("10:00"))
        );
    }

    // ==========================================
    // PRUEBA 2: PACIENTE INCORRECTO
    // ==========================================

    @Test
    @DisplayName("Rechazar un paciente con ID negativo")
    public void testPacienteIncorrecto() {

        // El ID del paciente no puede ser negativo.
        boolean resultado = servicio.asignarCita(
                12, -5, fechaFutura(), "10:00"
        );

        // Debe devolver false.
        assertFalse(resultado);
    }

    // ==========================================
    // PRUEBA 3: VALOR LIMITE ID CERO
    // ==========================================

    @Test
    @DisplayName("Rechazar un paciente con ID cero")
    public void testPacienteCero() {

        // Valor limite: el ID debe ser mayor que cero.
        assertFalse(servicio.asignarCita(
                12, 0, fechaFutura(), "10:00"
        ));
    }

    // ==========================================
    // PRUEBA 4: HORARIO OCUPADO
    // ==========================================

    @Test
    @DisplayName("No permitir dos citas en el mismo horario")
    public void testHorarioOcupado() {

        // Asignamos una cita a las 10:00.
        assertTrue(servicio.asignarCita(
                12, 5, fechaFutura(), "10:00"
        ));

        // Intentamos asignar otra cita a la misma hora.
        // Debe devolver false porque el horario esta ocupado.
        assertFalse(servicio.asignarCita(
                13, 6, fechaFutura(), "10:00"
        ));
    }

    // ==========================================
    // PRUEBA 5: FECHA PASADA
    // ==========================================

    @Test
    @DisplayName("No permitir asignar una cita en el pasado")
    public void testFechaPasada() {

        // No se puede reservar una cita en una fecha pasada.
        assertFalse(servicio.asignarCita(
                12, 5, fechaPasada(), "10:00"
        ));
    }

    // ==========================================
    // PRUEBA 6: REPROGRAMAR CITA
    // ==========================================

    @Test
    @DisplayName("Reprogramar una cita correctamente")
    public void testReprogramarCita() {

        // PREPARAR: asignamos una cita a las 10:00.
        assertTrue(servicio.asignarCita(
                12, 5, fechaFutura(), "10:00"
        ));

        // Guardamos la informacion anterior.
        String citaAntes = servicio.consultarCita(1);

        // EJECUTAR: cambiamos la hora a las 11:30.
        boolean resultado = servicio.reprogramarCita(
                1, fechaFutura(), "11:30"
        );

        // Consultamos la cita modificada.
        String citaDespues = servicio.consultarCita(1);

        // COMPROBAR: la operacion funciona
        // y los datos son diferentes.
        assertAll(
            () -> assertTrue(resultado),
            () -> assertNotEquals(citaAntes, citaDespues),
            () -> assertTrue(citaDespues.contains("11:30"))
        );
    }

    // ==========================================
    // PRUEBA 7: CANCELAR CITA
    // ==========================================

    @Test
    @DisplayName("Cancelar una cita correctamente")
    public void testCancelarCita() {

        // PREPARAR: asignamos una cita.
        assertTrue(servicio.asignarCita(
                12, 5, fechaFutura(), "10:00"
        ));

        // EJECUTAR: cancelamos la cita.
        boolean resultado = servicio.cancelarCita(1);

        // Consultamos su estado.
        String cita = servicio.consultarCita(1);

        // COMPROBAR: debe estar CANCELADA.
        assertAll(
            () -> assertTrue(resultado),
            () -> assertTrue(cita.contains("CANCELADA"))
        );
    }

    // ==========================================
    // PRUEBA 8: CANCELAR DOS VECES
    // ==========================================

    @Test
    @DisplayName("No permitir cancelar dos veces la misma cita")
    public void testCancelarDosVeces() {

        // PREPARAR: asignamos una cita.
        assertTrue(servicio.asignarCita(
                12, 5, fechaFutura(), "10:00"
        ));

        // Primera cancelacion: correcta.
        assertTrue(servicio.cancelarCita(1));

        // Segunda cancelacion: debe fallar.
        assertFalse(servicio.cancelarCita(1));
    }

    // ==========================================
    // PRUEBA 9: CONSULTAR CITA INEXISTENTE
    // ==========================================

    @Test
    @DisplayName("Consultar una cita que no existe")
    public void testConsultarCitaInexistente() {

        // Intentamos consultar una cita inexistente.
        String resultado = servicio.consultarCita(999);

        // El servidor debe devolver el mensaje esperado.
        assertEquals(
                "No existe una cita con ese ID.",
                resultado
        );
    }

    // ==========================================
    // PRUEBA 10: REPROGRAMAR CITA CANCELADA
    // ==========================================

    @Test
    @DisplayName("No permitir reprogramar una cita cancelada")
    public void testReprogramarCitaCancelada() {

        // PREPARAR: asignamos una cita.
        assertTrue(servicio.asignarCita(
                12, 5, fechaFutura(), "10:00"
        ));

        // Cancelamos la cita.
        assertTrue(servicio.cancelarCita(1));

        // EJECUTAR Y COMPROBAR:
        // Una cita cancelada no puede reprogramarse.
        assertFalse(servicio.reprogramarCita(
                1, fechaFutura(), "12:00"
        ));
    }
}
