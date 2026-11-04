package com.sescam.radiodiagnostico;

import com.sescam.radiodiagnostico.cdu02_solicitudes.SolicitudServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestGlobalIteracion2 {
    
    @Test
    public void testSolicitudes() { 
        SolicitudServiceImpl service = new SolicitudServiceImpl();
        assertTrue(service.solicitarPrueba("P1", "TAC"));
    }

}