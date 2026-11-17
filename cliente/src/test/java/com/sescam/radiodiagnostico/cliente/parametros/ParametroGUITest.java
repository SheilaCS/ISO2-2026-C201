package com.sescam.radiodiagnostico.cliente.parametros;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.*;

public class ParametroGUITest {
    
    private ParametroGUI ventana;

    @Test
    void testParametroGUIInitialization() {
        Assertions.assertNotNull(ventana);
        Assertions.assertEquals(
            "SESCAM - Configuración de Parámetros (CDU5)",
            ventana.getTitle()
        );
        Assertions.assertEquals(400, ventana.getWidth());
        Assertions.assertEquals(300, ventana.getHeight());
    }

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> ventana = new ParametroGUI());
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> ventana.dispose());
    }

}
