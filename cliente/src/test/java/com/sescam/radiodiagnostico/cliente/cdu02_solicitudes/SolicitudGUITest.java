package com.sescam.radiodiagnostico.cliente.cdu02_solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SolicitudGUITest {

    private SolicitudGUI ventana;

    @BeforeEach
    void setUp() {
        // Preparar: creamos la ventana para probarla
        ventana = new SolicitudGUI();
    }

    @Test
    @DisplayName("La ventana se inicializa con el título correcto")
    void testTituloVentana() {
        // Comprobar que el título es el esperado según el diseño
        assertEquals("SESCAM - Módulo de Solicitudes (CDU2)", ventana.getTitle());
    }

    @Test
    @DisplayName("La ventana tiene el tamaño correcto")
    void testTamanioVentana() {
        // Comprobar que el tamaño es 400x300
        assertEquals(400, ventana.getWidth());
        assertEquals(300, ventana.getHeight());
    }
}