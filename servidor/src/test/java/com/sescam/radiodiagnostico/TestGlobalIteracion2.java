package com.sescam.radiodiagnostico;

import com.sescam.radiodiagnostico.cdu08_informes.GestionPruebasServiceImpl;
import com.sescam.radiodiagnostico.cdu02_solicitudes.SolicitudServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestGlobalIteracion2 {
    
    @Test
    public void testSolicitudes() { // De Miriam
        SolicitudServiceImpl service = new SolicitudServiceImpl();
        assertTrue(service.solicitarPrueba("P1", "TAC"));
    }

    @Test
    public void testInformes() { // Tuyo
        GestionPruebasServiceImpl service = new GestionPruebasServiceImpl();
        assertTrue(service.crearInforme("PRB1", "Texto", "Ninguna"));
        assertEquals(2, service.obtenerImagenes("PRB1").size());
    }
}