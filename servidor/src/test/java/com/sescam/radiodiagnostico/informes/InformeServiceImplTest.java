package com.sescam.radiodiagnostico.informes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 PRUEBAS UNITARIAS PARA EL SERVICIO DE INFORMES (JUnit 5)
 */
public class InformeServiceImplTest {

    private IInformeService informeService;

    @BeforeEach
    public void setUp() {
        informeService = new InformeServiceImpl();
    }

    @Test
    public void testConsultarEstadoSolicitud() {
        String estado = informeService.consultarEstadoSolicitud("SOL-123");
        assertNotNull(estado);
        assertTrue(estado.contains("PENDIENTE"));
    }

    @Test
    public void testCrearInformeExitoso() {
        boolean exito = informeService.crearInforme("SOL-123", "El paciente presenta...");
        assertTrue(exito);
    }

    @Test
    public void testObtenerImagenes() {
        List<String> imagenes = informeService.obtenerImagenes("SOL-123");
        assertEquals(2, imagenes.size());
    }
}