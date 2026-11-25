package com.sescam.radiodiagnostico.realizacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Servicio de realizacion de pruebas (CDU7)")
class RealizacionServiceImplTest {

    @Test
    @DisplayName("Registra una realizacion valida sin incidencia y la consulta por cita")
    void registraYConsultaRealizacionValida() {
        // Preparar
        IRealizacionProveedor proveedor = idCita -> idCita == 10;
        RealizacionServiceImpl servicio = new RealizacionServiceImpl(proveedor);
        LocalDate hoy = LocalDate.now();

        // Ejecutar
        RealizacionPrueba creada = servicio.registrarRealizacion(
                10, 5, hoy, "Prueba completada sin hallazgos", "");
        Optional<RealizacionPrueba> consultada = servicio.consultarPorCita(10);

        // Comprobar
        assertAll(
                () -> assertNotNull(creada.id()),
                () -> assertEquals("REALIZADA", creada.estado()),
                () -> assertTrue(consultada.isPresent()),
                () -> assertEquals(1, servicio.listarRealizaciones().size())
        );
    }

    @Test
    @DisplayName("Registra una realizacion con incidencia y asigna estado REALIZADA_CON_INCIDENCIA")
    void registraRealizacionConIncidencia() {
        // Preparar
        RealizacionServiceImpl servicio = new RealizacionServiceImpl(idCita -> true);

        // Ejecutar
        RealizacionPrueba creada = servicio.registrarRealizacion(
                20, 8, LocalDate.now().minusDays(1), "", "Paciente con claustrofobia");

        // Comprobar
        assertAll(
                () -> assertEquals("REALIZADA_CON_INCIDENCIA", creada.estado()),
                () -> assertEquals("Paciente con claustrofobia", creada.incidencia())
        );
    }

    @Test
    @DisplayName("Rechaza datos invalidos, cita inexistente y duplicados por cita")
    void rechazaDatosInvalidosYDuplicados() {
        // Preparar
        RealizacionServiceImpl servicio = new RealizacionServiceImpl(idCita -> idCita == 1);
        servicio.registrarRealizacion(1, 1, LocalDate.now(), "OK", "");

        // Ejecutar y Comprobar
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.registrarRealizacion(0, 1, LocalDate.now(), "OK", "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.registrarRealizacion(2, 1, LocalDate.now().plusDays(1), "OK", "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.registrarRealizacion(2, 1, LocalDate.now(), "", "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.registrarRealizacion(99, 1, LocalDate.now(), "OK", "")),
                () -> assertThrows(IllegalStateException.class,
                        () -> servicio.registrarRealizacion(1, 1, LocalDate.now(), "Duplicada", "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.consultarPorCita(0))
        );
    }
}