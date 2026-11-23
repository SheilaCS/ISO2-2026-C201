# DECISIONS.md — Registro de decisiones del proyecto

> Registro de decisiones de requisitos, análisis, diseño y arquitectura del proyecto
> Sistema Integral de Gestión de Radiodiagnóstico e Imagen Médica — SESCAM.
> Cada decisión se enlaza al acta donde se acordó.


## D-001 • Estructura inicial del repositorio

**Fecha:** 11/10/2026 · **Iteración:** 0 · **Acta:** 11/10

**Contexto:** el enunciado exige una arquitectura cliente-servidor con múltiples clientes (escritorio, web, móvil) y Maven multimódulo.

**Decisión:** usar un único repositorio con la estructura Maven.

### Uso de Copilot en el repositorio.
Se han llevado a cabo dos commits con la colaboración de Copilot en la rama feature/estructura-maven al implementar el pom.xml padre. Está rama se desarrollo durante una sesión de prácticas y se utilizó la ayuda del agente de Copilot para generar una propuesta válida del mensaje del propio commit. El resto de la implementación de la rama mencionada se hizo manualmente por los miembros del equipo de desarrollo. 

## Iteración 1 — CDU1: Inicio de sesión (RF01)

### Autenticación de demostración

**Decisión:** utilizar un servicio de autenticación local con usuarios ficticios mientras no esté disponible la integración con el SSO corporativo.

**Justificación:** desarrollar y verificar el inicio de sesión sin depender de un sistema externo. La integración real se abordará en RF20.

Se utilizan cuatro roles predefinidos: `PACIENTE`, `MEDICO`, `ADMINISTRATIVO` y `ADMINISTRADOR`. La gestión configurable de roles y permisos corresponde a RF18.

### Separación entre cliente y servidor

**Decisión:** mantener la pantalla Swing en el módulo `cliente` y la comprobación de credenciales en el módulo `servidor`.

El servidor ofrece `IAutenticacionService`, implementada por `AutenticacionServiceImpl`. La pantalla depende de `ServicioAutenticacion` y utiliza el adaptador `ServicioAutenticacionServidor`.

**Justificación:** separar la presentación de la autenticación y permitir probar el adaptador mediante un servidor falso.

En esta versión, la comunicación consiste en llamadas Java locales dentro del mismo proceso. La conexión HTTP definida en la arquitectura del proyecto queda pendiente de adaptación para CU01.

### Respuesta de autenticación

**Decisión:** devolver `Optional<Sesion>` desde el servidor.

- Acceso correcto: sesión con identificador y rol.
- Acceso rechazado o datos nulos: `Optional.empty()`.

El adaptador transforma la respuesta en `SesionUsuario`, o devuelve `null` cuando se rechaza el acceso. Convierte el rol del servidor, de tipo `Rol`, a `String` mediante `name()`.

### Validación y limpieza de contraseña

**Decisión:** comprobar los campos vacíos en el cliente y validar las credenciales en el servidor.

Las credenciales incorrectas se tratan como un resultado de rechazo. Los bloques `finally` limpian los arrays de contraseña utilizados; el cliente también vacía el campo de contraseña.

**Justificación:** mostrar respuestas controladas y reducir el tiempo durante el que se conservan las contraseñas utilizadas en memoria.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, en el mismo paquete que la clase probada.

Tras la revisión de los tests se utilizan:

- `@DisplayName` en español.
- Estructura preparar, ejecutar y comprobar.
- `assertAll` para comprobaciones relacionadas.
- Casos representativos del comportamiento, evitando pruebas independientes de getters y setters triviales.
- Casos diseñados con clases de equivalencia, valores límite y conjetura de errores. La tabla de casos está en la wiki, página "Iteración 1", apartado "Pruebas".

### Eliminación del simulador del cliente

**Decisión implementada en la PR #37:** eliminar `ServicioAutenticacionSimulado` y `ServicioAutenticacionSimuladoTest`, porque la pantalla ya utiliza `ServicioAutenticacionServidor`.

Se conserva `AutenticacionServiceImpl`, que sigue proporcionando la autenticación de demostración del servidor.

**Resultado:** quedan 18 pruebas: 14 del servicio del servidor y 4 del adaptador del cliente. La ejecución de `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas.

### Trazabilidad

- [Issue #9 — Entregar Iteración 1](https://github.com/SheilaCS/ISO2-2026-C201/issues/9).
- [PR #35 — Integración de CU01](https://github.com/SheilaCS/ISO2-2026-C201/pull/35).
- [PR #37 — Revisión de tests y eliminación del simulador](https://github.com/SheilaCS/ISO2-2026-C201/pull/37).
- [Documentación de la iteración 1](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-1).
- [PR #40 — Ampliación de pruebas con clases de equivalencia y valores límite](https://github.com/SheilaCS/ISO2-2026-C201/pull/40).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.


## Iteración 3 — CDU3: Gestionar citas (RF13)

### Comunicación entre cliente y servidor

**Decisión:** comunicar el cliente y el servidor mediante HTTP. El cliente utiliza `CitasHttpClient` y el servidor `GestionCitasHttpServer`, que escucha en `127.0.0.1`, puerto 8080, configurable con la propiedad `citas.puerto`.

**Justificación:** seguir la arquitectura cliente-servidor definida para el proyecto y poder probar cada parte con un servidor o un cliente simulado.

### Servicio y almacenamiento en memoria

**Decisión:** definir el contrato en `IGestionCitasService` e implementarlo en `GestionCitasServiceImpl`, que guarda las citas en memoria mediante un `HashMap` con identificador autoincremental.

**Justificación:** el proyecto no utiliza base de datos en esta fase.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas.

- No puede haber dos citas `PROGRAMADA` en la misma fecha y hora.
- Una cita cancelada libera su horario y no se puede reprogramar ni cancelar de nuevo.
- Reprogramar puede mantener el propio horario, pero no moverse a uno ocupado.
- Los ID de solicitud, paciente y cita deben ser enteros mayores que 0.
- La fecha y la hora deben ser válidas y posteriores al momento actual.

**Justificación:** evitar citas duplicadas y estados incoherentes.

### Validación en cliente y servidor

**Decisión:** comprobar los datos en el cliente con `ValidacionesCitas`, antes de enviar la petición, y repetir las comprobaciones en el servidor.

**Justificación:** mostrar respuestas rápidas al usuario sin depender únicamente de la validación del cliente.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, en el mismo paquete que la clase probada.

- El servicio se prueba con casos válidos y todos los rechazos de las reglas de negocio.
- El servidor HTTP se prueba arrancándolo en un puerto libre y haciendo peticiones reales.
- El cliente HTTP y la interfaz se prueban con un servidor simulado.
- Los casos están diseñados con clases de equivalencia, valores límite y conjetura de errores. La tabla de casos está en la wiki, página "Iteración 3", apartado "Testing".

**Resultado:** 117 pruebas: 54 del servidor y 63 del cliente. La ejecución de `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas.

### Trazabilidad

- [PR #45 — Integración de CDU3](https://github.com/SheilaCS/ISO2-2026-C201/pull/45).
- [Documentación de la iteración 3](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-3).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.

## Iteración 6 — CDU6: Gestionar agendas (RF14)

### Servicio y comunicación

**Decisión:** definir el contrato en `IGestionAgendasService`, implementado por `GestionAgendasServiceImpl`, y exponerlo por HTTP con `GestionAgendasHttpServer` en `127.0.0.1`, puerto 8081, configurable con la propiedad `agendas.puerto`. El cliente `GestionAgendasGUI` valida los datos con `ValidacionesAgendas` y se comunica con el servidor mediante `AgendasHttpClient`.

**Justificación:** mantener la misma estructura que el módulo de citas y evitar que los puertos choquen.

Los horarios se guardan en memoria.

### Médico como identificador

**Decisión:** recibir el médico como un identificador, sin comprobar que exista en el sistema.

**Justificación:** el CDU6 da soporte a la gestión de citas y a otros casos de uso que aún no están integrados. Cuando lo estén, el identificador se sustituirá por sus interfaces.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas.

- El ID de médico debe ser un entero mayor que 0.
- La fecha no puede ser anterior a hoy, y el día actual es válido.
- La hora de inicio debe ser anterior a la de fin.
- Un médico no puede tener dos horarios solapados el mismo día.
- Los horarios consecutivos sí se permiten, y dos médicos pueden coincidir en el mismo horario.
- Al modificar, el horario no choca consigo mismo.

**Justificación:** evitar agendas incoherentes sin impedir horarios legítimos.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, diseñadas con clases de equivalencia, valores límite y conjetura de errores. La tabla de casos está en la wiki, página "Iteración 6", apartado "Pruebas".

**Resultado:** La ejecución de `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas.

### Trazabilidad

- [PR #46 — Integración de CDU6](https://github.com/SheilaCS/ISO2-2026-C201/pull/46).
- [Documentación de la iteración 6](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-6).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.

## Iteración 9 — CDU9: Gestionar mis citas (RF02)

### Acceso a las citas mediante una interfaz

**Decisión:** el servidor de Mis citas no accede directamente a la lógica del módulo de citas. Utiliza la interfaz `ICitasPacienteProveedor` y su implementación `ProveedorCitasHttp`, que se comunica por HTTP con el servidor de citas, en el puerto 8080.

**Justificación:** reducir las dependencias entre casos de uso y poder probar `GestionMisCitasServiceImpl` con un proveedor simulado. Cumple el criterio de integración por interfaz del plan de calidad.

### Servidor y comunicación

**Decisión:** definir el contrato en `IGestionMisCitasService`, implementado por `GestionMisCitasServiceImpl`, y exponerlo por HTTP con `GestionMisCitasHttpServer` en `127.0.0.1`, puerto 8082. El cliente `GestionMisCitasGUI` valida con `ValidacionesMisCitas` y se comunica mediante `MisCitasHttpClient`.

Para funcionar, necesita que el servidor de citas esté disponible en el puerto 8080. Los datos se mantienen en memoria.

**Justificación:** seguir la misma estructura que los módulos anteriores.

### Ampliación compatible del módulo de citas

**Decisión:** ampliar el módulo `citas` con métodos nuevos, sin modificar el comportamiento de los existentes. 

**Justificación:** cumplir RNF-MAN-07 (nuevos casos de uso sin modificar los existentes).

### Pruebas independientes del idioma

**Decisión implementada en la PR #52:** en `GestionCitasGUITest`, obtener el título del aviso con `UIManager.getString("OptionPane.messageDialogTitle")` en lugar de escribir "Message".

**Justificación:** el título por defecto de `JOptionPane` depende del idioma del sistema, y la prueba fallaba con idioma español. El fallo estaba en la prueba, no en la aplicación.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, con clases de equivalencia, valores límite y conjetura de errores. La tabla de casos está en la wiki, página "Iteración 9", apartado "Pruebas".

**Resultado:** La ejecución de `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas.

### Trazabilidad

- [PR #52 — Integración de CDU9](https://github.com/SheilaCS/ISO2-2026-C201/pull/52).
- [Documentación de la iteración 9](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-9).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.



## Seguridad — Control de acceso por rol en el MVP

### Control de acceso limitado a los casos de uso clínicos

**Decisión:** en la versión 1.0.0, el servidor comprueba la sesión y el rol únicamente en los casos de uso clínicos: CDU1, CDU4, CDU7 y CDU8, y CDU12 en la versión 1.1.0. Responde 401 si no hay sesión y 403 si el rol no corresponde, como ya hace el servidor de pacientes. En los demás casos de uso no se aplica por ahora.

**Justificación:** el plan de calidad exige pruebas de acceso no autorizado en los casos de uso que manejan datos clínicos. Aplicar el control al resto queda fuera del alcance del MVP.

### Limitación conocida

**Decisión:** aceptar que los servidores de Mis citas, citas, agendas, solicitudes y parámetros no comprueben sesión ni rol (hallazgos H-01 y H-02 de la página "Evaluación de calidad").

En Mis citas, el servidor confía en el ID de paciente que recibe en la petición. No hay datos clínicos expuestos, pero sí información de citas.

**Justificación:** mantener el alcance del MVP. La corrección se abordará con la gestión de roles y permisos (RF18), en una versión posterior.

### Trazabilidad

- [Evaluación de calidad](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Evaluaci%C3%B3n-de-calidad) 
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.
