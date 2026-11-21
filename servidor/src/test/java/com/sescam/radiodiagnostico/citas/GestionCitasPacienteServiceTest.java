package com.sescam.radiodiagnostico.citas;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las operaciones del paciente (iteracion 9, RF02)
 * en el servicio de citas.
 */
public class GestionCitasPacienteServiceTest {

    private GestionCitasServiceImpl servicio;
    private String fechaFutura;

    // Preparar un servicio con la cita 1 del paciente 1
    @BeforeEach
    public void preparar() {
        servicio = new GestionCitasServiceImpl();
        fechaFutura = LocalDate.now().plusDays(5).toString();

        servicio.asignarCita(1, 1, fechaFutura, "10:00");
    }

    // Operaciones del paciente: devuelven si se han realizado
    private boolean confirmar(int idCita, int idPaciente) {
        return servicio.confirmarCitaPaciente(idCita, idPaciente)
                .isCorrecto();
    }

    private boolean cambio(int idCita, int idPaciente, String motivo) {
        return servicio.solicitarCambioCitaPaciente(
                idCita, idPaciente, motivo).isCorrecto();
    }

    private boolean cancelar(int idCita, int idPaciente) {
        return servicio.cancelarCitaPaciente(idCita, idPaciente)
                .isCorrecto();
    }

    // Asignar una cita a un paciente en la hora indicada
    private boolean asignar(int idPaciente, String hora) {
        return servicio.asignarCita(1, idPaciente, fechaFutura, hora);
    }

    // -----------------------------------------
    // PRUEBAS DE LISTAR LAS CITAS DEL PACIENTE
    // -----------------------------------------

    @Test
    @DisplayName("Listar las citas de un paciente")
    public void testListarCitasPaciente() {

        String resultado = servicio.listarCitasPaciente(1);

        assertEquals("Cita 1 | Fecha: " + fechaFutura
                + " | Hora: 10:00 | Estado: PROGRAMADA", resultado);
    }

    @Test
    @DisplayName("Listar solo las citas del propio paciente")
    public void testListarSoloCitasPropias() {

        asignar(2, "11:00");

        String resultado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(resultado.contains("Cita 1")),
                () -> assertFalse(resultado.contains("Cita 2"))
        );
    }

    @Test
    @DisplayName("Listar las citas ordenadas por identificador")
    public void testListarCitasOrdenadas() {

        asignar(1, "12:00");
        asignar(1, "11:00");

        String resultado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(resultado.indexOf("Cita 1")
                        < resultado.indexOf("Cita 2")),
                () -> assertTrue(resultado.indexOf("Cita 2")
                        < resultado.indexOf("Cita 3")),
                () -> assertEquals(3, resultado.split("\n").length)
        );
    }

    @Test
    @DisplayName("Listar un paciente sin citas")
    public void testListarPacienteSinCitas() {

        assertEquals("", servicio.listarCitasPaciente(9));
    }

    @Test
    @DisplayName("Listar las citas con distintos estados")
    public void testListarCitasConDistintosEstados() {

        asignar(1, "11:00");
        asignar(1, "12:00");
        asignar(1, "13:00");

        confirmar(2, 1);
        cambio(3, 1, "Cambio de turno");
        cancelar(4, 1);

        String resultado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(resultado.contains(
                        "Cita 1 | Fecha: " + fechaFutura
                        + " | Hora: 10:00 | Estado: PROGRAMADA\n")),
                () -> assertTrue(resultado.contains(
                        "Cita 2 | Fecha: " + fechaFutura
                        + " | Hora: 11:00 | Estado: CONFIRMADA")),
                () -> assertTrue(resultado.contains(
                        "Estado: CAMBIO_SOLICITADO | Motivo: "
                        + "Cambio de turno")),
                () -> assertTrue(resultado.contains(
                        "Cita 4 | Fecha: " + fechaFutura
                        + " | Hora: 13:00 | Estado: CANCELADA"))
        );
    }

    @Test
    @DisplayName("Listar tambien las citas canceladas sin motivo")
    public void testListarCitaCancelada() {

        cancelar(1, 1);

        String resultado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(resultado.contains("CANCELADA")),
                () -> assertFalse(resultado.contains("Motivo"))
        );
    }

    // -----------------------------------------
    // PRUEBAS DE CONFIRMAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Confirmar una cita programada")
    public void testConfirmarCitaCorrecta() {

        assertTrue(confirmar(1, 1));
        assertTrue(servicio.listarCitasPaciente(1)
                .contains("CONFIRMADA"));
    }

    @Test
    @DisplayName("No confirmar una cita ya confirmada")
    public void testConfirmarCitaDosVeces() {

        confirmar(1, 1);

        assertFalse(confirmar(1, 1));
    }

    @Test
    @DisplayName("No confirmar una cita que no existe")
    public void testConfirmarCitaInexistente() {

        assertFalse(confirmar(99, 1));
    }

    @Test
    @DisplayName("No confirmar la cita de otro paciente")
    public void testConfirmarCitaDeOtroPaciente() {

        assertFalse(confirmar(1, 2));
        assertTrue(servicio.listarCitasPaciente(1)
                .contains("PROGRAMADA"));
    }

    @Test
    @DisplayName("No confirmar con un ID de paciente no valido")
    public void testConfirmarIdPacienteNoValido() {

        assertFalse(confirmar(1, 0));
        assertFalse(confirmar(1, -1));
    }

    @Test
    @DisplayName("No confirmar una cita cancelada")
    public void testConfirmarCitaCancelada() {

        cancelar(1, 1);

        assertFalse(confirmar(1, 1));
    }

    @Test
    @DisplayName("No confirmar una cita con cambio solicitado")
    public void testConfirmarCitaConCambioSolicitado() {

        cambio(1, 1, "No puedo asistir");

        assertFalse(confirmar(1, 1));
    }

    // -----------------------------------------
    // PRUEBAS DE SOLICITAR CAMBIO
    // -----------------------------------------

    @Test
    @DisplayName("Solicitar el cambio de una cita programada")
    public void testSolicitarCambioCorrecto() {

        boolean resultado = cambio(
                1, 1, "Tengo otra cita medica");

        String listado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(resultado),
                () -> assertTrue(listado.contains("CAMBIO_SOLICITADO")),
                () -> assertTrue(listado.contains(
                        "Motivo: Tengo otra cita medica"))
        );
    }

    @Test
    @DisplayName("Solicitar el cambio de una cita confirmada")
    public void testSolicitarCambioCitaConfirmada() {

        confirmar(1, 1);

        assertTrue(cambio(
                1, 1, "Ha surgido un imprevisto"));
    }

    @Test
    @DisplayName("No solicitar el cambio dos veces")
    public void testSolicitarCambioDosVeces() {

        cambio(1, 1, "Primer motivo");

        assertFalse(cambio(
                1, 1, "Segundo motivo"));
    }

    @Test
    @DisplayName("No solicitar el cambio sin motivo")
    public void testSolicitarCambioSinMotivo() {

        assertFalse(cambio(1, 1, null));
        assertFalse(cambio(1, 1, ""));
        assertFalse(cambio(1, 1, "   "));
    }

    @Test
    @DisplayName("Permitir un motivo con la longitud maxima")
    public void testSolicitarCambioMotivoLongitudMaxima() {

        assertTrue(cambio(
                1, 1, "a".repeat(200)));
    }

    @Test
    @DisplayName("Permitir un motivo de 199 caracteres")
    public void testMotivoUnoMenosQueElMaximo() {

        assertTrue(cambio(
                1, 1, "a".repeat(199)));
    }

    @Test
    @DisplayName("No permitir un motivo demasiado largo")
    public void testSolicitarCambioMotivoDemasiadoLargo() {

        assertFalse(cambio(
                1, 1, "a".repeat(201)));
    }

    @Test
    @DisplayName("Permitir un motivo de 200 caracteres con espacios laterales")
    public void testMotivoLongitudMaximaConEspacios() {

        assertTrue(cambio(
                1, 1, "  " + "a".repeat(200) + "  "));
    }

    @Test
    @DisplayName("Guardar el motivo sin espacios al principio ni al final")
    public void testMotivoSinEspaciosLaterales() {

        cambio(1, 1, "  Cambio de turno  ");

        assertTrue(servicio.listarCitasPaciente(1)
                .endsWith("Motivo: Cambio de turno"));
    }

    @Test
    @DisplayName("No cambiar el estado si el motivo no es valido")
    public void testMotivoNoValidoNoCambiaEstado() {

        cambio(1, 1, "   ");

        assertTrue(servicio.listarCitasPaciente(1)
                .contains("PROGRAMADA"));
    }

    @Test
    @DisplayName("No solicitar el cambio de una cita inexistente")
    public void testSolicitarCambioCitaInexistente() {

        assertFalse(cambio(
                99, 1, "No puedo asistir"));
    }

    @Test
    @DisplayName("No solicitar el cambio de la cita de otro paciente")
    public void testSolicitarCambioCitaDeOtroPaciente() {

        assertFalse(cambio(
                1, 2, "No puedo asistir"));
    }

    @Test
    @DisplayName("No solicitar el cambio de una cita cancelada")
    public void testSolicitarCambioCitaCancelada() {

        cancelar(1, 1);

        assertFalse(cambio(
                1, 1, "No puedo asistir"));
    }

    // -----------------------------------------
    // PRUEBAS DE CANCELAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Cancelar una cita programada")
    public void testCancelarCitaCorrecta() {

        assertTrue(cancelar(1, 1));
        assertTrue(servicio.listarCitasPaciente(1)
                .contains("CANCELADA"));
    }

    @Test
    @DisplayName("Cancelar una cita confirmada")
    public void testCancelarCitaConfirmada() {

        confirmar(1, 1);

        assertTrue(cancelar(1, 1));
    }

    @Test
    @DisplayName("Cancelar una cita con cambio solicitado")
    public void testCancelarCitaConCambioSolicitado() {

        cambio(1, 1, "No puedo asistir");

        assertTrue(cancelar(1, 1));
    }

    @Test
    @DisplayName("No cancelar una cita ya cancelada")
    public void testCancelarCitaDosVeces() {

        cancelar(1, 1);

        assertFalse(cancelar(1, 1));
    }

    @Test
    @DisplayName("No cancelar una cita que no existe")
    public void testCancelarCitaInexistente() {

        assertFalse(cancelar(99, 1));
    }

    @Test
    @DisplayName("No cancelar la cita de otro paciente")
    public void testCancelarCitaDeOtroPaciente() {

        assertFalse(cancelar(1, 2));
        assertTrue(servicio.listarCitasPaciente(1)
                .contains("PROGRAMADA"));
    }

    @Test
    @DisplayName("No cancelar con un ID de paciente no valido")
    public void testCancelarIdPacienteNoValido() {

        assertFalse(cancelar(1, 0));
        assertFalse(cancelar(1, -1));
    }

    // -----------------------------------------
    // PRUEBAS DE CITAS YA PASADAS
    // -----------------------------------------

    // Insertar una cita pasada directamente en el almacen
    // porque asignarCita no permite crearla
    @SuppressWarnings("unchecked")
    private void insertarCitaPasada(int idCita, int idPaciente)
            throws Exception {

        Class<?> claseCita = Class.forName(
                GestionCitasServiceImpl.class.getName() + "$Cita");

        Constructor<?> constructor = claseCita.getDeclaredConstructor(
                int.class, int.class, int.class, LocalDateTime.class);

        constructor.setAccessible(true);

        Object cita = constructor.newInstance(
                idCita, 1, idPaciente,
                LocalDateTime.now().minusDays(1));

        Field campo = GestionCitasServiceImpl.class
                .getDeclaredField("citas");

        campo.setAccessible(true);

        Map<Integer, Object> citas =
                (Map<Integer, Object>) campo.get(servicio);

        citas.put(idCita, cita);
    }

    @Test
    @DisplayName("No confirmar una cita que ya ha pasado")
    public void testConfirmarCitaPasada() throws Exception {

        insertarCitaPasada(7, 1);

        assertFalse(confirmar(7, 1));
    }

    @Test
    @DisplayName("No solicitar el cambio de una cita que ya ha pasado")
    public void testSolicitarCambioCitaPasada() throws Exception {

        insertarCitaPasada(7, 1);

        assertFalse(cambio(
                7, 1, "No puedo asistir"));
    }

    @Test
    @DisplayName("No cancelar una cita que ya ha pasado")
    public void testCancelarCitaPasada() throws Exception {

        insertarCitaPasada(7, 1);

        assertFalse(cancelar(7, 1));
    }

    @Test
    @DisplayName("Listar una cita que ya ha pasado")
    public void testListarCitaPasada() throws Exception {

        insertarCitaPasada(7, 1);

        String resultado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(resultado.contains("Cita 7")),
                () -> assertTrue(resultado.contains("PROGRAMADA"))
        );
    }

    // -----------------------------------------
    // PRUEBAS DE PACIENTES INDEPENDIENTES
    // -----------------------------------------

    @Test
    @DisplayName("Gestionar citas de varios pacientes de forma independiente")
    public void testPacientesIndependientes() {

        asignar(2, "11:00");

        cancelar(2, 2);

        assertAll(
                () -> assertTrue(servicio.listarCitasPaciente(1)
                        .contains("PROGRAMADA")),
                () -> assertTrue(servicio.listarCitasPaciente(2)
                        .contains("CANCELADA"))
        );
    }

    // -----------------------------------------
    // EFECTO EN LAS OPERACIONES DE CITAS (RF13)
    // -----------------------------------------

    @Test
    @DisplayName("No asignar una cita en la hora de una cita confirmada")
    public void testAsignarHoraDeCitaConfirmada() {

        confirmar(1, 1);

        assertFalse(asignar(2, "10:00"));
    }

    @Test
    @DisplayName("No asignar una cita en la hora de una cita con cambio solicitado")
    public void testAsignarHoraDeCitaConCambioSolicitado() {

        cambio(1, 1, "No puedo asistir");

        assertFalse(asignar(2, "10:00"));
    }

    @Test
    @DisplayName("Asignar una cita en la hora de una cita cancelada")
    public void testAsignarHoraDeCitaCancelada() {

        cancelar(1, 1);

        assertTrue(asignar(2, "10:00"));
    }

    @Test
    @DisplayName("No reprogramar a la hora de una cita confirmada")
    public void testReprogramarHoraDeCitaConfirmada() {

        asignar(2, "11:00");
        confirmar(2, 2);

        assertFalse(servicio.reprogramarCita(1, fechaFutura, "11:00"));
    }

    @Test
    @DisplayName("Una cita confirmada reprogramada vuelve a programada")
    public void testReprogramarCitaConfirmada() {

        confirmar(1, 1);

        assertTrue(servicio.reprogramarCita(1, fechaFutura, "15:00"));

        assertTrue(servicio.listarCitasPaciente(1).contains(
                "Hora: 15:00 | Estado: PROGRAMADA"));
    }

    @Test
    @DisplayName("Reprogramar una cita atiende la solicitud de cambio")
    public void testReprogramarCitaConCambioSolicitado() {

        cambio(1, 1, "No puedo asistir");

        assertTrue(servicio.reprogramarCita(1, fechaFutura, "16:00"));

        String listado = servicio.listarCitasPaciente(1);

        assertAll(
                () -> assertTrue(listado.contains("PROGRAMADA")),
                () -> assertFalse(listado.contains("Motivo"))
        );
    }

    @Test
    @DisplayName("Consultar una cita con cambio solicitado muestra el motivo")
    public void testConsultarCitaConCambioSolicitado() {

        cambio(1, 1, "No puedo asistir");

        String resultado = servicio.consultarCita(1);

        assertAll(
                () -> assertTrue(resultado.contains(
                        "Estado: CAMBIO_SOLICITADO")),
                () -> assertTrue(resultado.contains(
                        "Motivo del cambio: No puedo asistir"))
        );
    }

    @Test
    @DisplayName("Consultar una cita sin cambio solicitado no muestra motivo")
    public void testConsultarCitaSinCambioSolicitado() {

        assertFalse(servicio.consultarCita(1)
                .contains("Motivo del cambio"));
    }

    // -----------------------------------------
    // MENSAJES QUE EXPLICAN EL MOTIVO DEL RECHAZO
    // -----------------------------------------

    @Test
    @DisplayName("Mensaje al confirmar correctamente")
    public void testMensajeConfirmarCorrecto() {

        ResultadoOperacion resultado =
                servicio.confirmarCitaPaciente(1, 1);

        assertAll(
                () -> assertTrue(resultado.isCorrecto()),
                () -> assertEquals("Cita confirmada correctamente",
                        resultado.getMensaje())
        );
    }

    @Test
    @DisplayName("Mensaje al confirmar una cita ya confirmada")
    public void testMensajeConfirmarYaConfirmada() {

        confirmar(1, 1);

        ResultadoOperacion resultado =
                servicio.confirmarCitaPaciente(1, 1);

        assertAll(
                () -> assertFalse(resultado.isCorrecto()),
                () -> assertEquals("No se pudo confirmar la cita: "
                        + "la cita ya esta confirmada",
                        resultado.getMensaje())
        );
    }

    @Test
    @DisplayName("Mensaje al confirmar una cita con cambio solicitado")
    public void testMensajeConfirmarConCambioSolicitado() {

        cambio(1, 1, "No puedo asistir");

        assertEquals("No se pudo confirmar la cita: "
                + "ya se ha solicitado un cambio para esta cita",
                servicio.confirmarCitaPaciente(1, 1).getMensaje());
    }

    @Test
    @DisplayName("Mensaje al confirmar una cita cancelada")
    public void testMensajeConfirmarCancelada() {

        cancelar(1, 1);

        assertEquals("No se pudo confirmar la cita: "
                + "la cita esta cancelada",
                servicio.confirmarCitaPaciente(1, 1).getMensaje());
    }

    @Test
    @DisplayName("Mensaje al confirmar una cita que no existe o es de otro paciente")
    public void testMensajeConfirmarInexistenteOAjena() {

        String esperado = "No se pudo confirmar la cita: "
                + "la cita no existe o no pertenece a este paciente";

        assertAll(
                () -> assertEquals(esperado,
                        servicio.confirmarCitaPaciente(99, 1)
                                .getMensaje()),
                () -> assertEquals(esperado,
                        servicio.confirmarCitaPaciente(1, 2)
                                .getMensaje())
        );
    }

    @Test
    @DisplayName("Mensaje al confirmar una cita que ya ha pasado")
    public void testMensajeConfirmarPasada() throws Exception {

        insertarCitaPasada(7, 1);

        assertEquals("No se pudo confirmar la cita: "
                + "la cita ya ha pasado",
                servicio.confirmarCitaPaciente(7, 1).getMensaje());
    }

    @Test
    @DisplayName("Mensajes al solicitar el cambio")
    public void testMensajesCambio() {

        ResultadoOperacion correcto =
                servicio.solicitarCambioCitaPaciente(1, 1, "Motivo");

        ResultadoOperacion repetido =
                servicio.solicitarCambioCitaPaciente(1, 1, "Otro");

        ResultadoOperacion sinMotivo =
                servicio.solicitarCambioCitaPaciente(1, 1, "  ");

        ResultadoOperacion largo =
                servicio.solicitarCambioCitaPaciente(
                        1, 1, "a".repeat(201));

        ResultadoOperacion ajena =
                servicio.solicitarCambioCitaPaciente(1, 2, "Motivo");

        assertAll(
                () -> assertEquals(
                        "Solicitud de cambio registrada correctamente",
                        correcto.getMensaje()),
                () -> assertEquals("No se pudo solicitar el cambio "
                        + "de la cita: ya se ha solicitado un cambio "
                        + "para esta cita", repetido.getMensaje()),
                () -> assertEquals("No se pudo solicitar el cambio "
                        + "de la cita: el motivo es obligatorio y no "
                        + "puede superar 200 caracteres",
                        sinMotivo.getMensaje()),
                () -> assertEquals(sinMotivo.getMensaje(),
                        largo.getMensaje()),
                () -> assertEquals("No se pudo solicitar el cambio "
                        + "de la cita: la cita no existe o no "
                        + "pertenece a este paciente",
                        ajena.getMensaje())
        );
    }

    @Test
    @DisplayName("Mensajes al cancelar")
    public void testMensajesCancelar() {

        ResultadoOperacion correcto =
                servicio.cancelarCitaPaciente(1, 1);

        ResultadoOperacion repetido =
                servicio.cancelarCitaPaciente(1, 1);

        ResultadoOperacion ajena =
                servicio.cancelarCitaPaciente(1, 2);

        assertAll(
                () -> assertEquals("Cita cancelada correctamente",
                        correcto.getMensaje()),
                () -> assertEquals("No se pudo cancelar la cita: "
                        + "la cita esta cancelada",
                        repetido.getMensaje()),
                () -> assertEquals("No se pudo cancelar la cita: "
                        + "la cita no existe o no pertenece a este "
                        + "paciente", ajena.getMensaje())
        );
    }
}
