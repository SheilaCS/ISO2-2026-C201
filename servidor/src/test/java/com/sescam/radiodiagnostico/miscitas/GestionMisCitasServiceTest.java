package com.sescam.radiodiagnostico.miscitas;

import static org.junit.jupiter.api.Assertions.*;

import com.sescam.radiodiagnostico.citas.ResultadoOperacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del servicio de "Mis citas" con un origen de citas simulado.
 * Las reglas de las citas (estados, fechas, propietario) se prueban
 * en GestionCitasPacienteServiceTest, y la comunicacion real con el
 * servidor de citas en ProveedorCitasHttpTest.
 */
public class GestionMisCitasServiceTest {

    // Origen de citas simulado que anota las llamadas recibidas
    private static class ProveedorSimulado
            implements ICitasPacienteProveedor {

        String listado = "";
        boolean respuesta = true;
        boolean fallo = false;

        int llamadas = 0;
        String operacion;
        int idCita = -1;
        int idPaciente = -1;
        String motivo;

        private ResultadoOperacion resultado() {

            return respuesta
                    ? ResultadoOperacion.correcta("Operacion realizada")
                    : ResultadoOperacion.rechazada("Rechazo del servidor");
        }

        private void anotar(String nombre) {

            llamadas++;
            operacion = nombre;

            if (fallo) {
                throw new IllegalStateException(
                        "No se pudo conectar con el servidor de citas");
            }
        }

        @Override
        public String listarCitasPaciente(int idPaciente) {
            this.idPaciente = idPaciente;
            anotar("listar");
            return listado;
        }

        @Override
        public ResultadoOperacion confirmarCita(int idCita,
                                                int idPaciente) {
            this.idCita = idCita;
            this.idPaciente = idPaciente;
            anotar("confirmar");
            return resultado();
        }

        @Override
        public ResultadoOperacion solicitarCambioCita(int idCita,
                int idPaciente, String motivo) {
            this.idCita = idCita;
            this.idPaciente = idPaciente;
            this.motivo = motivo;
            anotar("cambio");
            return resultado();
        }

        @Override
        public ResultadoOperacion cancelarCita(int idCita,
                                               int idPaciente) {
            this.idCita = idCita;
            this.idPaciente = idPaciente;
            anotar("cancelar");
            return resultado();
        }
    }

    private ProveedorSimulado proveedor;
    private GestionMisCitasServiceImpl servicio;

    @BeforeEach
    public void preparar() {
        proveedor = new ProveedorSimulado();
        servicio = new GestionMisCitasServiceImpl(proveedor);
    }

    // Operaciones del paciente: devuelven si se han realizado
    private boolean confirmar(int idCita, int idPaciente) {
        return servicio.confirmarCita(idCita, idPaciente).isCorrecto();
    }

    private boolean cambio(int idCita, int idPaciente, String motivo) {
        return servicio.solicitarCambioCita(
                idCita, idPaciente, motivo).isCorrecto();
    }

    private boolean cancelar(int idCita, int idPaciente) {
        return servicio.cancelarCita(idCita, idPaciente).isCorrecto();
    }

    // -----------------------------------------
    // PRUEBAS DE CONSULTAR MIS CITAS
    // -----------------------------------------

    @Test
    @DisplayName("Consultar las citas de un paciente")
    public void testConsultarMisCitas() {

        proveedor.listado = "Cita 1 | Fecha: 2027-01-10 | Hora: 10:00"
                + " | Estado: PROGRAMADA";

        assertEquals(proveedor.listado, servicio.consultarMisCitas(4));
        assertEquals(4, proveedor.idPaciente);
        assertEquals("listar", proveedor.operacion);
    }

    @Test
    @DisplayName("Consultar un paciente sin citas")
    public void testConsultarPacienteSinCitas() {

        proveedor.listado = "";

        assertEquals("No hay citas para este paciente",
                servicio.consultarMisCitas(9));
    }

    @Test
    @DisplayName("Consultar un paciente cuyo listado solo tiene espacios")
    public void testConsultarListadoEnBlanco() {

        proveedor.listado = "  \n ";

        assertEquals("No hay citas para este paciente",
                servicio.consultarMisCitas(9));
    }

    @Test
    @DisplayName("Consultar con un ID de paciente no valido")
    public void testConsultarIdNoValido() {

        assertEquals("ID de paciente no válido",
                servicio.consultarMisCitas(0));

        assertEquals("ID de paciente no válido",
                servicio.consultarMisCitas(-1));

        assertEquals(0, proveedor.llamadas);
    }

    @Test
    @DisplayName("Propagar el fallo del servidor de citas al consultar")
    public void testConsultarFalloDelServidorDeCitas() {

        proveedor.fallo = true;

        assertThrows(IllegalStateException.class,
                () -> servicio.consultarMisCitas(1));
    }

    // -----------------------------------------
    // PRUEBAS DE CONFIRMAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Confirmar una cita y enviar cada ID en su sitio")
    public void testConfirmarCitaCorrecta() {

        assertTrue(confirmar(3, 5));

        assertEquals("confirmar", proveedor.operacion);
        assertEquals(3, proveedor.idCita);
        assertEquals(5, proveedor.idPaciente);
    }

    @Test
    @DisplayName("Devolver el rechazo del servidor de citas al confirmar")
    public void testConfirmarRechazada() {

        proveedor.respuesta = false;

        assertFalse(confirmar(3, 5));
    }

    @Test
    @DisplayName("No confirmar con IDs no validos")
    public void testConfirmarIdsNoValidos() {

        assertFalse(confirmar(0, 1));
        assertFalse(confirmar(-1, 1));
        assertFalse(confirmar(1, 0));
        assertFalse(confirmar(1, -1));

        assertEquals(0, proveedor.llamadas);
    }

    @Test
    @DisplayName("Propagar el fallo del servidor de citas al confirmar")
    public void testConfirmarFalloDelServidorDeCitas() {

        proveedor.fallo = true;

        assertThrows(IllegalStateException.class,
                () -> confirmar(1, 1));
    }

    // -----------------------------------------
    // PRUEBAS DE SOLICITAR CAMBIO
    // -----------------------------------------

    @Test
    @DisplayName("Solicitar el cambio y enviar los datos en su sitio")
    public void testSolicitarCambioCorrecto() {

        assertTrue(cambio(
                3, 5, "Tengo otra cita medica"));

        assertEquals("cambio", proveedor.operacion);
        assertEquals(3, proveedor.idCita);
        assertEquals(5, proveedor.idPaciente);
        assertEquals("Tengo otra cita medica", proveedor.motivo);
    }

    @Test
    @DisplayName("Devolver el rechazo del servidor de citas al solicitar el cambio")
    public void testSolicitarCambioRechazado() {

        proveedor.respuesta = false;

        assertFalse(cambio(3, 5, "Motivo"));
    }

    @Test
    @DisplayName("Enviar el motivo sin espacios al principio ni al final")
    public void testMotivoSinEspaciosLaterales() {

        cambio(1, 1, "  Cambio de turno  ");

        assertEquals("Cambio de turno", proveedor.motivo);
    }

    @Test
    @DisplayName("No solicitar el cambio sin motivo")
    public void testSolicitarCambioSinMotivo() {

        assertFalse(cambio(1, 1, null));
        assertFalse(cambio(1, 1, ""));
        assertFalse(cambio(1, 1, "   "));

        assertEquals(0, proveedor.llamadas);
    }

    @Test
    @DisplayName("Permitir un motivo de 200 caracteres")
    public void testMotivoLongitudMaxima() {

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
    @DisplayName("Permitir un motivo de 200 caracteres con espacios laterales")
    public void testMotivoLongitudMaximaConEspacios() {

        assertTrue(cambio(
                1, 1, "  " + "a".repeat(200) + "  "));

        assertEquals(200, proveedor.motivo.length());
    }

    @Test
    @DisplayName("No permitir un motivo demasiado largo")
    public void testMotivoDemasiadoLargo() {

        assertFalse(cambio(
                1, 1, "a".repeat(201)));

        assertEquals(0, proveedor.llamadas);
    }

    @Test
    @DisplayName("No solicitar el cambio con IDs no validos")
    public void testSolicitarCambioIdsNoValidos() {

        assertFalse(cambio(0, 1, "Motivo"));
        assertFalse(cambio(-1, 1, "Motivo"));
        assertFalse(cambio(1, 0, "Motivo"));
        assertFalse(cambio(1, -1, "Motivo"));

        assertEquals(0, proveedor.llamadas);
    }

    @Test
    @DisplayName("Propagar el fallo del servidor de citas al solicitar el cambio")
    public void testSolicitarCambioFalloDelServidorDeCitas() {

        proveedor.fallo = true;

        assertThrows(IllegalStateException.class,
                () -> cambio(1, 1, "Motivo"));
    }

    // -----------------------------------------
    // PRUEBAS DE CANCELAR CITA
    // -----------------------------------------

    @Test
    @DisplayName("Cancelar una cita y enviar cada ID en su sitio")
    public void testCancelarCitaCorrecta() {

        assertTrue(cancelar(3, 5));

        assertEquals("cancelar", proveedor.operacion);
        assertEquals(3, proveedor.idCita);
        assertEquals(5, proveedor.idPaciente);
    }

    @Test
    @DisplayName("Devolver el rechazo del servidor de citas al cancelar")
    public void testCancelarRechazada() {

        proveedor.respuesta = false;

        assertFalse(cancelar(3, 5));
    }

    @Test
    @DisplayName("No cancelar con IDs no validos")
    public void testCancelarIdsNoValidos() {

        assertFalse(cancelar(0, 1));
        assertFalse(cancelar(-1, 1));
        assertFalse(cancelar(1, 0));
        assertFalse(cancelar(1, -1));

        assertEquals(0, proveedor.llamadas);
    }

    @Test
    @DisplayName("Propagar el fallo del servidor de citas al cancelar")
    public void testCancelarFalloDelServidorDeCitas() {

        proveedor.fallo = true;

        assertThrows(IllegalStateException.class,
                () -> cancelar(1, 1));
    }

    // -----------------------------------------
    // PRUEBA DEL CONSTRUCTOR NORMAL
    // -----------------------------------------

    @Test
    @DisplayName("Crear el servicio con el origen de citas normal")
    public void testConstructorNormal() {

        assertNotNull(new GestionMisCitasServiceImpl());
    }

    // -----------------------------------------
    // PRUEBAS DE LOS MENSAJES
    // -----------------------------------------

    @Test
    @DisplayName("Devolver el mensaje del servidor de citas")
    public void testMensajeDelServidorDeCitas() {

        assertEquals("Operacion realizada",
                servicio.confirmarCita(1, 1).getMensaje());
        assertEquals("Operacion realizada",
                servicio.solicitarCambioCita(1, 1, "Motivo")
                        .getMensaje());
        assertEquals("Operacion realizada",
                servicio.cancelarCita(1, 1).getMensaje());

        proveedor.respuesta = false;

        assertEquals("Rechazo del servidor",
                servicio.confirmarCita(1, 1).getMensaje());
        assertEquals("Rechazo del servidor",
                servicio.solicitarCambioCita(1, 1, "Motivo")
                        .getMensaje());
        assertEquals("Rechazo del servidor",
                servicio.cancelarCita(1, 1).getMensaje());
    }

    @Test
    @DisplayName("Explicar que los ID no son validos")
    public void testMensajeIdNoValido() {

        String motivo = ": los ID deben ser enteros mayores que 0";

        assertAll(
                () -> assertEquals("No se pudo confirmar la cita"
                        + motivo,
                        servicio.confirmarCita(0, 1).getMensaje()),
                () -> assertEquals("No se pudo solicitar el cambio "
                        + "de la cita" + motivo,
                        servicio.solicitarCambioCita(1, 0, "Motivo")
                                .getMensaje()),
                () -> assertEquals("No se pudo cancelar la cita"
                        + motivo,
                        servicio.cancelarCita(1, -1).getMensaje())
        );
    }

    @Test
    @DisplayName("Explicar que el motivo no es valido")
    public void testMensajeMotivoNoValido() {

        String esperado = "No se pudo solicitar el cambio de la cita: "
                + "el motivo es obligatorio y no puede superar "
                + "200 caracteres";

        assertAll(
                () -> assertEquals(esperado,
                        servicio.solicitarCambioCita(1, 1, null)
                                .getMensaje()),
                () -> assertEquals(esperado,
                        servicio.solicitarCambioCita(1, 1, "  ")
                                .getMensaje()),
                () -> assertEquals(esperado,
                        servicio.solicitarCambioCita(
                                1, 1, "a".repeat(201)).getMensaje())
        );
    }
}
