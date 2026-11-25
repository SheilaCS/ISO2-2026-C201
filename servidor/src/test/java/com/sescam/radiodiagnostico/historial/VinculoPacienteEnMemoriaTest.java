package com.sescam.radiodiagnostico.historial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CDU12 · Servidor · VinculoPacienteEnMemoria")
class VinculoPacienteEnMemoriaTest {

    // Clase de equivalencia: listas correctas (una o varias entradas,
    // con espacios) y usuario buscado
    @ParameterizedTest(name = "lista \"{0}\": {1} -> {2}")
    @CsvSource(delimiter = '|', value = {
            "paciente.demo=1|paciente.demo|1",
            "a=1,b=2|b|2",
            " a = 1 , b = 2 |a|1",
            "a=1,a=5|a|5"
    })
    @DisplayName("Una lista correcta vincula cada usuario con su número de paciente")
    void listaCorrecta(String lista, String usuario, String esperado) {
        // Ejecutar
        Optional<String> id =
                new VinculoPacienteEnMemoria(lista).idPacienteDe(usuario);

        // Comprobar
        assertEquals(Optional.of(esperado), id);
    }

    // Clase de equivalencia: listas vacías o mal formadas
    @ParameterizedTest(name = "lista \"{0}\" no vincula a nadie")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sinigual", "=3", "a=", "=", " = ", ","})
    @DisplayName("Una lista vacía o mal formada no vincula a nadie")
    void listaIncorrecta(String lista) {
        // Ejecutar
        VinculoPacienteEnMemoria vinculo = new VinculoPacienteEnMemoria(lista);

        // Comprobar
        assertAll(
                () -> assertTrue(vinculo.idPacienteDe("a").isEmpty()),
                () -> assertTrue(vinculo.idPacienteDe("sinigual").isEmpty())
        );
    }

    @Test
    @DisplayName("Una entrada incorrecta no impide leer las demás")
    void entradaIncorrectaSeIgnora() {
        // Ejecutar
        VinculoPacienteEnMemoria vinculo =
                new VinculoPacienteEnMemoria("mala,a=1,=,b=2");

        // Comprobar
        assertAll(
                () -> assertEquals(Optional.of("1"), vinculo.idPacienteDe("a")),
                () -> assertEquals(Optional.of("2"), vinculo.idPacienteDe("b")),
                () -> assertTrue(vinculo.idPacienteDe("mala").isEmpty())
        );
    }

    @Test
    @DisplayName("Un usuario nulo o desconocido no tiene paciente asociado")
    void usuarioNuloODesconocido() {
        // Preparar
        VinculoPacienteEnMemoria vinculo = new VinculoPacienteEnMemoria("a=1");

        // Ejecutar y comprobar
        assertAll(
                () -> assertTrue(vinculo.idPacienteDe(null).isEmpty()),
                () -> assertTrue(vinculo.idPacienteDe("b").isEmpty()),
                () -> assertTrue(vinculo.idPacienteDe("A").isEmpty())
        );
    }

    @Test
    @DisplayName("Se puede añadir y cambiar un vínculo")
    void vincularAnadeYCambia() {
        // Preparar
        VinculoPacienteEnMemoria vinculo = new VinculoPacienteEnMemoria("a=1");

        // Ejecutar
        vinculo.vincular(" nuevo ", " 7 ");
        vinculo.vincular("a", "9");

        // Comprobar
        assertAll(
                () -> assertEquals(Optional.of("7"),
                        vinculo.idPacienteDe("nuevo")),
                () -> assertEquals(Optional.of("9"), vinculo.idPacienteDe("a"))
        );
    }

    @ParameterizedTest(name = "vincular(\"{0}\", \"{1}\") se rechaza")
    @CsvSource(nullValues = "NULL", value = {
            "NULL, 1",
            "'', 1",
            "'  ', 1",
            "a, NULL",
            "a, ''",
            "a, '  '"
    })
    @DisplayName("No se puede vincular con usuario o identificador vacío o nulo")
    void vincularIncorrecto(String usuario, String id) {
        // Preparar
        VinculoPacienteEnMemoria vinculo = new VinculoPacienteEnMemoria("");

        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class,
                () -> vinculo.vincular(usuario, id));
    }

    @Test
    @DisplayName("Sin propiedad, el constructor normal solo vincula a paciente.demo con el 1")
    void constructorNormalPorDefecto() {
        // Preparar
        String anterior = System.getProperty("historial.vinculos");
        System.clearProperty("historial.vinculos");

        try {
            // Ejecutar
            VinculoPacienteEnMemoria vinculo = new VinculoPacienteEnMemoria();

            // Comprobar
            assertAll(
                    () -> assertEquals(Optional.of("1"),
                            vinculo.idPacienteDe("paciente.demo")),
                    () -> assertTrue(vinculo.idPacienteDe("otro").isEmpty())
            );
        } finally {
            restaurar(anterior);
        }
    }

    @Test
    @DisplayName("Con la propiedad historial.vinculos, el constructor normal la usa")
    void constructorNormalConPropiedad() {
        // Preparar
        String anterior = System.getProperty("historial.vinculos");
        System.setProperty("historial.vinculos", "x=3,y=4");

        try {
            // Ejecutar
            VinculoPacienteEnMemoria vinculo = new VinculoPacienteEnMemoria();

            // Comprobar
            assertAll(
                    () -> assertEquals(Optional.of("3"), vinculo.idPacienteDe("x")),
                    () -> assertEquals(Optional.of("4"), vinculo.idPacienteDe("y")),
                    () -> assertTrue(
                            vinculo.idPacienteDe("paciente.demo").isEmpty())
            );
        } finally {
            restaurar(anterior);
        }
    }

    private static void restaurar(String anterior) {
        if (anterior == null) {
            System.clearProperty("historial.vinculos");
        } else {
            System.setProperty("historial.vinculos", anterior);
        }
    }
}
