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


## Iteración 2 — CDU2: Solicitar pruebas médicas (RF03)

### Servicio y almacenamiento

**Decisión:** definir el contrato en `ISolicitudService`, implementado por `SolicitudServiceImpl`. El servicio valida los datos de la solicitud y confirma su registro, sin persistencia en base de datos en esta fase.

**Justificación:** el proyecto no utiliza base de datos en esta iteración y el objetivo del CDU2 es fijar el contrato y las reglas de validación del alta de solicitudes. La persistencia real se abordará en una versión posterior.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas.

- El tipo de prueba es obligatorio y no puede quedar vacío ni compuesto solo por espacios.
- La prioridad es obligatoria y debe ser una de `URGENTE`, `PREFERENTE` u `ORDINARIO`, sin distinguir mayúsculas y tolerando espacios exteriores.
- Cualquier otro valor de prioridad o un tipo de prueba vacío provocan el rechazo de la solicitud.

**Justificación:** evitar solicitudes sin información clínica útil y unificar los valores de prioridad admitidos por el sistema.

### Comunicación entre cliente y servidor

**Decisión:** comunicar el cliente y el servidor mediante HTTP. El cliente utiliza `SolicitudHttpClient` y el servidor `SolicitudHttpHandler`, que escucha en `127.0.0.1`, puerto 8083. La creación de la solicitud se expone en `/solicitudes/crear` y la consulta en `/solicitudes/consultar`, con formularios codificados en UTF-8.

**Justificación:** seguir la misma estructura que el módulo de citas y poder probar cada parte con un servidor o un cliente simulado.

### Validación en cliente y servidor

**Decisión:** comprobar los datos en el cliente con `ValidacionesSolicitudes` antes de enviar la petición, y repetir las comprobaciones en `SolicitudServiceImpl`.

Las validaciones del cliente son `validarId(String)`, `validarTexto(String)` y `validarPrioridad(String)`.

**Justificación:** mostrar respuestas rápidas al usuario sin depender únicamente de la validación del cliente.

### Interfaz del cliente

**Decisión:** construir `SolicitudGUI` con `JFrame` y `DISPOSE_ON_CLOSE`, formulario `GridLayout(4, 2)` con los campos `ID Paciente`, `ID Solicitud`, `Tipo de Prueba` y `Prioridad`, y dos botones `Crear Solicitud` y `Consultar Solicitud`. El resultado se muestra en un `JTextArea` no editable. El método `mostrarError(String)` es `protected` para poder sustituirlo en pruebas. `SolicitudGUI(SolicitudHttpClient)` permite inyectar un cliente simulado.

**Justificación:** mantener la misma pauta visual que el resto de pantallas Swing y separar la lógica de red de la presentación.

### Organización de las pruebas



### Trazabilidad

- [Issue #10 — Entregar Iteración 2 (RF06)](https://github.com/SheilaCS/ISO2-2026-C201/issues/10).
- [PR #57 — Integración de CDU2](https://github.com/SheilaCS/ISO2-2026-C201/pull/57).
- [Documentación de la iteración 2](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-2).

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


## Iteración 4 — CDU4: Gestión de pacientes (RF12)

### Servicio y almacenamiento

**Decisión:** definir el contrato en `IPacientesService`, implementado por `PacientesServiceImpl`, y separar el almacenamiento mediante `RepositorioPacientes` y `RepositorioPacientesEnMemoria`.

Los pacientes se representan mediante un record inmutable y se identifican con un UUID generado por el servidor.

**Justificación:** separar responsabilidades, facilitar las pruebas y mantener los datos en memoria sin incorporar una base de datos.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas:

- CIP obligatorio, único e inmutable, normalizado a mayúsculas y sin espacios exteriores.
- Nombre y apellidos obligatorios y fecha de nacimiento no futura.
- Modificación únicamente de nombre, apellidos y fecha de nacimiento de pacientes activos.
- Baja lógica, conservando los datos y sin permitir reutilizar el CIP.
- Rechazo de modificaciones y nuevas bajas de pacientes inactivos.
- Ninguna operación rechazada guarda cambios parciales.

**Justificación:** evitar duplicados y conservar la coherencia de los pacientes y sus referencias.

### Comunicación y permisos

**Decisión:** comunicar `PacientesHttpClient` con `GestionPacientesHttpServer` mediante HTTP, en el puerto predeterminado 8084, utilizando formularios codificados en UTF-8.

El servidor reutiliza la autenticación de demostración mediante HTTP Basic y permite las operaciones solo al rol `ADMINISTRATIVO`: responde 401 ante credenciales inválidas y 403 ante un rol no autorizado.

**Justificación:** seguir el mecanismo HTTP del proyecto sin añadir dependencias. La autenticación básica se limita al entorno local; fuera de él requiere HTTPS.

### Correcciones del cliente

**Decisión:** utilizar UUID también en el cliente, cargar las respuestas en la tabla y decodificar cada campo por separado.

La baja solo se refleja si el servidor la acepta. El botón `Nuevo/Limpiar` desbloquea el CIP para gestionar otro paciente, conservando la tabla y las credenciales.

**Justificación:** asegurar la compatibilidad con el servidor y evitar estados incorrectos en la pantalla.

### Pruebas y revisión

**Decisión:** mantener los tests en `src/test/java`, con `@DisplayName` en español y casos representativos de validaciones, operaciones y permisos.

**Resultado:** 52 pruebas del servidor y 21 del cliente superadas. El paquete de pacientes del servidor alcanza un 96,93 % de cobertura de líneas y un 100 % de ramas.

Mateo implementó el servidor y sus pruebas. La revisión independiente y la comprobación contra el servidor real quedaron registradas en la PR #53.

### Trazabilidad

- [Issue #13 — Iteración 4](https://github.com/SheilaCS/ISO2-2026-C201/issues/13).
- [PR #51 — Servidor y pruebas](https://github.com/SheilaCS/ISO2-2026-C201/pull/51).
- [PR #53 — Cliente y correcciones](https://github.com/SheilaCS/ISO2-2026-C201/pull/53).
- [Documentación de la iteración 4](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-4).

La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.


## Iteración 5 — CDU5: Configurar parámetros (RF05)

### Servicio y almacenamiento

**Decisión:** definir el contrato en `IParametroService`, implementado por `ParametroServiceImpl`, que guarda los parámetros en memoria mediante un `HashMap`.

**Justificación:** el proyecto no utiliza base de datos en esta fase. La lógica de almacenamiento queda separada del transporte HTTP.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas.

- `guardarParametro(clave, valor)` solo almacena la pareja cuando ambos son no nulos; en caso contrario no modifica el mapa.
- `obtenerParametro(clave)` devuelve `"No encontrado"` cuando la clave no existe.

**Justificación:** evitar entradas corruptas en el mapa y ofrecer una respuesta controlada al cliente cuando se consulta una clave desconocida.

### Comunicación entre cliente y servidor

**Decisión:** comunicar el cliente y el servidor mediante HTTP. El cliente utiliza `ParametrosHttpClient` y el servidor `ParametroHttpHandler`, que escucha en `127.0.0.1`, puerto 8084. El guardado se expone en `/parametros/guardar` y la consulta en `/parametros/consultar`, con formularios codificados en UTF-8.

**Justificación:** seguir la misma estructura que el resto de módulos y poder probar cada parte con un servidor o un cliente simulado.

### Limitación conocida del servidor

**Decisión:** en esta versión, `ParametroHttpHandler` no persiste los valores enviados: la respuesta de `/guardar` es un eco del par recibido y la de `/consultar` devuelve un valor simulado. La lógica real ya existe en `ParametroServiceImpl`, pero aún no está cableada al handler.

**Justificación:** el objetivo de la iteración es fijar el contrato HTTP y la pantalla de configuración. El cableado definitivo entre handler y servicio se hará cuando se integre la persistencia.

### Validación en cliente

**Decisión:** comprobar los datos en el cliente con `ValidacionesParametros`:

- `validarTexto(String)`: no nulo y no vacío tras `trim()`.
- `validarNumeroPositivo(String)`: entero mayor o igual que cero, tolerante a espacios.

La GUI lanza `IllegalArgumentException` con mensaje en español cuando la validación falla.

**Justificación:** ofrecer respuesta inmediata al usuario y reutilizar validaciones comunes con el resto de módulos.

### Interfaz del cliente

**Decisión:** construir `ParametroGUI` con `JFrame` y `DISPOSE_ON_CLOSE`, formulario `GridLayout(2, 2)` con los campos `Clave del parámetro` y `Valor`, y dos botones `Guardar` y `Consultar`. El resultado se muestra en un `JTextArea` no editable. El método `mostrarError(String)` es `protected` para poder sustituirlo en pruebas. `ParametroGUI(ParametrosHttpClient)` permite inyectar un cliente simulado.

**Justificación:** mantener la misma pauta visual que el resto de pantallas Swing y separar la lógica de red de la presentación.

### Organización de las pruebas



### Trazabilidad

- [Issue #14 — Entregar Iteración 5 (RF17)](https://github.com/SheilaCS/ISO2-2026-C201/issues/14).
- [PR #60 — Integración de CDU5](https://github.com/SheilaCS/ISO2-2026-C201/pull/60).
- [Documentación de la iteración 5](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-5).

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


## Iteración 8 — CDU8: Generar informes de mantenimiento (RF08)

### Servicio y almacenamiento

**Decisión:** definir el contrato en `IInformeService`, implementado por `InformeServiceImpl`. El servicio valida la petición, comprueba el rol y confirma la generación del informe, sin persistencia en base de datos en esta fase.

**Justificación:** el objetivo del CDU8 en esta iteración es fijar el contrato, las reglas de seguridad y las validaciones del informe de mantenimiento. La persistencia real se abordará en una versión posterior.

### Reglas de negocio y seguridad

**Decisión:** aplicar las siguientes reglas.

- El acceso está limitado a los roles `TECNICO` y `ADMIN`, comparados tras `trim()` y `toUpperCase()`.
- El `idEquipo` debe ser un entero mayor que 0.
- La `descripcion` no puede ser nula ni vacía.
- La `fecha` debe ser parseable como `LocalDate` en formato `AAAA-MM-DD`.
- Cualquier incumplimiento provoca el rechazo de la operación sin generar el informe.

**Justificación:** los informes de mantenimiento solo deben ser accesibles al personal técnico o administrador. La validación en servidor evita que un cliente manipulado pueda saltarse las comprobaciones del cliente.

### Comunicación entre cliente y servidor

**Decisión:** comunicar el cliente y el servidor mediante HTTP. El cliente utiliza `InformesHttpClient` y el servidor `InformeHttpHandler`, que escucha en `127.0.0.1`, puerto 8085. La generación se expone en `/informes/generar` y la consulta en `/informes/consultar`, con formularios codificados en UTF-8. El handler convierte los parámetros numéricos con `parsearEntero(String)`, que devuelve `-1` ante valores no numéricos para que la validación del servicio los rechace sin lanzar `NumberFormatException`.

**Justificación:** seguir la misma estructura que el resto de módulos, evitar errores no controlados ante entradas malformadas y permitir aplicar RBAC a partir del rol recibido.

### Validación en cliente y servidor

**Decisión:** comprobar los datos en el cliente con `ValidacionesInformes` antes de enviar la petición, y repetir las comprobaciones en `InformeServiceImpl`.

Las validaciones del cliente son `validarId(String)`, `validarTexto(String)`, `validarFecha(String)` y `tienePermiso(String)`, que acepta únicamente `TECNICO` o `ADMIN`.

**Justificación:** mostrar respuestas rápidas al usuario, reforzar el RBAC en el cliente y reutilizar el mismo criterio de permisos que el servidor.

### Interfaz del cliente

**Decisión:** construir `InformeGUI` con `JFrame` y `DISPOSE_ON_CLOSE`, formulario `GridLayout(5, 2)` con los campos `Rol`, `ID Equipo`, `Fecha`, `Descripción` e `ID Informe`, y dos botones `Generar Informe` y `Consultar Informe`. El campo de rol se precarga con `TECNICO`. El resultado se muestra en un `JTextArea` no editable. Los errores de seguridad se muestran con `WARNING_MESSAGE` bajo el título `Seguridad`; el resto, con `ERROR_MESSAGE`. El método `mostrarError(String)` es `protected` para poder sustituirlo en pruebas. `InformeGUI(InformesHttpClient)` permite inyectar un cliente simulado.

**Justificación:** mantener la misma pauta visual que el resto de pantallas Swing, distinguir visualmente los rechazos de seguridad y separar la lógica de red de la presentación.

### Organización de las pruebas



### Trazabilidad

- [Issue #XX — Entregar Iteración 8 (RFXX)](https://github.com/SheilaCS/ISO2-2026-C201/issues/XX).
- [PR #XX — Integración de CDU8](https://github.com/SheilaCS/ISO2-2026-C201/pull/XX).
- [Documentación de la iteración 8](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-8).

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
