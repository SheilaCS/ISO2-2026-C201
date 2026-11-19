package com.sescam.radiodiagnostico.cliente.parametros;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void testMain() throws Exception {

        ParametroGUI.main(new String[]{});

        Thread.sleep(200);

        boolean encontrada = false;

        for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
            if (frame instanceof ParametroGUI gui) {
                encontrada = true;
                gui.dispose();
            }
        }

        Assertions.assertTrue(encontrada);
    }
}
