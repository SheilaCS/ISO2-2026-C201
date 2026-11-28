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

### Pruebas de la pantalla de inicio de sesión

**Decisión implementada en la PR #50:** añadir `LoginGUITest` para probar la pantalla con un servicio de autenticación falso, sin depender del servidor.

Las pruebas comprueban el acceso correcto, la clave incorrecta, los campos vacíos sin llamar al servicio, los espacios alrededor del usuario, el vaciado del campo de contraseña, el título de la ventana y el método `main`.

**Resultado:** `LoginGUI` pasa del 0 % al 100 % de cobertura. El CU01 queda con 26 pruebas: 14 del servicio del servidor, 4 del adaptador y 8 de la pantalla.

### Pruebas de acceso no autorizado

**Decisión:** en el CDU1, las pruebas de acceso no autorizado comprueban que nadie obtiene una sesión ni un rol que no le corresponde, ya que este caso de uso asigna los roles y no tiene acciones reservadas a un rol concreto.

Se utilizan las pruebas existentes de `AutenticacionServiceImplTest` (clave incorrecta, usuario desconocido, variantes, valores vacíos y nulos, y rol propio de cada usuario) y una comprobación manual en la pantalla de inicio de sesión. El control de las acciones reservadas a cada rol se prueba en los casos de uso que las implementan, como el CDU4.

**Justificación:** cumplir la actividad de pruebas de acceso no autorizado (RNF-SEG-02 y RNF-SEG-04) sin duplicar pruebas. El resultado y las capturas están en la wiki, página "Iteración 1", apartado "Pruebas de acceso no autorizado".

### Trazabilidad

- [Issue #9 — Entregar Iteración 1](https://github.com/SheilaCS/ISO2-2026-C201/issues/9).
- [PR #35 — Integración de CU01](https://github.com/SheilaCS/ISO2-2026-C201/pull/35).
- [PR #37 — Revisión de tests y eliminación del simulador](https://github.com/SheilaCS/ISO2-2026-C201/pull/37).
- [Documentación de la iteración 1](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-01).
- [PR #40 — Ampliación de pruebas con clases de equivalencia y valores límite](https://github.com/SheilaCS/ISO2-2026-C201/pull/40).
- [PR #50 — Pruebas de la pantalla de inicio de sesión](https://github.com/SheilaCS/ISO2-2026-C201/pull/50).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.


## Iteración 2 — CDU2: Solicitar pruebas diagnósticas (RF06)

### Corrección de la iteración

**Decisión:** corregir el CDU2 para que cumpla el RF06: la solicitud incluye la indicación clínica y la justificación, se guarda y se puede consultar, el servidor valida el paciente y responde con códigos HTTP de error, y solo pueden solicitar pruebas los usuarios con el permiso `SOLICITAR_PRUEBAS` del CDU10.

**Justificación:** el enunciado pide la solicitud "con indicación clínica, justificación y nivel de prioridad". La primera versión no recogía la indicación ni la justificación, no guardaba las solicitudes (la consulta devolvía un texto fijo), aceptaba un paciente vacío y respondía 200 ante datos incorrectos.

### Servicio y almacenamiento

**Decisión:** definir el contrato en `ISolicitudService`, implementado por `SolicitudServiceImpl`, que guarda las solicitudes en memoria. Cada solicitud se representa con el record `Solicitud` (identificador, paciente, médico, tipo de prueba, indicación clínica, justificación, prioridad y fecha) y la prioridad con el enum `Prioridad`.

**Justificación:** seguir la misma estructura que el resto de casos de uso y mantener los datos en memoria sin incorporar una base de datos.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas:

- El ID de paciente debe ser un entero mayor que 0.
- El tipo de prueba, la indicación clínica y la justificación son obligatorios.
- La prioridad debe ser `URGENTE`, `PREFERENTE` u `ORDINARIO`, sin distinguir mayúsculas.
- El médico de la solicitud es siempre el usuario autenticado, nunca un dato enviado por el cliente.
- El sistema asigna el identificador y la fecha de la solicitud.

**Justificación:** evitar solicitudes sin información clínica útil y garantizar la trazabilidad de quién solicita cada prueba.

### Comunicación y permisos

**Decisión:** comunicar `SolicitudHttpClient` con `SolicitudHttpHandler` mediante HTTP, en `127.0.0.1`, puerto 8083, con formularios codificados en UTF-8. Las operaciones son `GET /solicitudes`, `POST /solicitudes/crear` y `GET /solicitudes/consultar?id=`. El servidor autentica con HTTP Basic mediante el servicio del CDU1 y comprueba el permiso `SOLICITAR_PRUEBAS` del CDU10: responde 400 ante datos incorrectos, 401 ante credenciales inválidas, 403 sin el permiso y 404 si la solicitud no existe.

**Justificación:** comprobar el permiso, y no un rol fijo, mantiene la misma pauta que el CDU10 y el CDU11.

### Interfaz del cliente

**Decisión:** construir `SolicitudGUI` con los campos `Usuario`, `Contraseña`, `ID Paciente`, `Tipo de prueba`, `Indicación clínica`, `Justificación`, `Prioridad` (lista desplegable) e `ID Solicitud`, y los botones `Crear Solicitud` y `Consultar Solicitud`. `ValidacionesSolicitudes` comprueba los datos antes de enviarlos. `mostrarError(String)` es `protected` y `SolicitudGUI(SolicitudHttpClient)` permite inyectar un cliente simulado.

**Justificación:** mantener la misma pauta visual que el resto de pantallas Swing y evitar errores de escritura en la prioridad.

### Alcance

**Decisión:** no se comprueba que el paciente exista en el CDU4 ni que el tipo de prueba esté en el catálogo del CDU5.

**Justificación:** conectar los servidores entre sí afectaría a casos de uso ya integrados. Se abordará en una iteración posterior.

### Pruebas

**Decisión:** mantener las pruebas en `src/test/java`, con clases de equivalencia, valores límite y conjetura de errores. La tabla de casos está en la wiki, página "Iteración 2", apartado "Pruebas".

**Resultado:** [X] pruebas superadas: [X] del servidor y [X] del cliente.

### Trazabilidad

- [Issue #10 — Entregar Iteración 2 (RF06)](https://github.com/SheilaCS/ISO2-2026-C201/issues/10).
- [PR #57 — Integración de CDU2](https://github.com/SheilaCS/ISO2-2026-C201/pull/57).
- [PR #XX — Corrección del CDU2 (RF06)](https://github.com/SheilaCS/ISO2-2026-C201/pull/XX).
- [Documentación de la iteración 2](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-02).

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
- [Documentación de la iteración 3](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-03).
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

La baja lógica es definitiva: no se ofrece una operación para reactivar a un paciente inactivo. Si fuera necesaria, se incorporaría como un requisito nuevo en una versión posterior.

### Comunicación y permisos

**Decisión:** comunicar `PacientesHttpClient` con `GestionPacientesHttpServer` mediante HTTP, en el puerto predeterminado 8092, utilizando formularios codificados en UTF-8. El puerto se cambió primero de 8084 a 8087 (PR #66), por coincidir con el de parámetros, y después a 8092, por coincidir con el de seguridad.

El servidor reutiliza la autenticación de demostración mediante HTTP Basic y permite las operaciones solo al rol `ADMINISTRATIVO`: responde 401 ante credenciales inválidas y 403 ante un rol no autorizado.

**Justificación:** seguir el mecanismo HTTP del proyecto sin añadir dependencias. La autenticación básica se limita al entorno local; fuera de él requiere HTTPS.

### Integración

**Decisión:** no crear una rama de integración independiente. El servidor se incorporó a `develop` en la PR #51, y la rama del cliente incorporó `develop` antes de integrarse mediante la PR #53.

**Justificación:** la rama del cliente ya reunía ambas partes, por lo que una tercera rama no aportaba nada. Antes de fusionar se comprobó el flujo completo contra el servidor real: alta, consulta, modificación y baja.

### Correcciones del cliente

**Decisión:** utilizar UUID también en el cliente, cargar las respuestas en la tabla y decodificar cada campo por separado.

La baja solo se refleja si el servidor la acepta. El botón `Nuevo/Limpiar` desbloquea el CIP para gestionar otro paciente, conservando la tabla y las credenciales.

**Justificación:** asegurar la compatibilidad con el servidor y evitar estados incorrectos en la pantalla.

### Pruebas de acceso no autorizado

**Decisión implementada en la PR #62:** comprobar que los roles distintos de `ADMINISTRATIVO` (`MEDICO`, `PACIENTE` y `ADMINISTRADOR`) no pueden dar de alta, modificar ni dar de baja pacientes, y que los datos no cambian tras el rechazo.

Se añade una prueba parametrizada a `GestionPacientesHttpServerTest` con 9 casos (3 roles × 3 operaciones). Cada caso comprueba la respuesta `403`, que el paciente conserva sus datos y sigue activo, y que no se crea ningún paciente nuevo. Hasta entonces, el rechazo solo se comprobaba en la consulta.

**Justificación:** el CDU4 maneja datos clínicos y tiene acciones de modificación, por lo que debe evaluar la confidencialidad (RNF-SEG-02 y RNF-SEG-04) y la integridad (RNF-SEG-05). La prueba falla si se elimina la comprobación del rol en el servidor. La comprobación manual y las capturas están en la wiki, página "Iteración 4".

### Diagramas

**Decisión:** realizar los diagramas en Visual Paradigm con el mismo formato que el resto de iteraciones. Las clases de diseño se obtienen mediante ingeniería inversa del código; la clase `Paciente`, al ser un record, se completa manualmente. Los diagramas de secuencia representan el alta de un paciente, la operación principal, en el servidor y en el cliente.

**Justificación:** mantener la correspondencia entre los diagramas y el código, y la coherencia con la documentación de las demás iteraciones.

### Pruebas y revisión

**Decisión:** mantener los tests en `src/test/java`, con `@DisplayName` en español y casos representativos de validaciones, operaciones y permisos.

**Resultado:** 61 pruebas del servidor (incluidas las 9 de acceso no autorizado) y 21 del cliente superadas. El paquete de pacientes del servidor alcanza un 96,93 % de cobertura de líneas y un 100 % de ramas, y el del cliente un 96,34 % de líneas y un 83,55 % de ramas.

Mateo implementó el servidor y sus pruebas, y Tomás el cliente. Juan Manuel y Mateo corrigieron el cliente tras la revisión. La revisión independiente y la comprobación contra el servidor real quedaron registradas en la PR #53.

### Documentación de las pruebas de acceso no autorizado

**Decisión:** cada iteración con datos clínicos incluye en su página de la wiki un apartado "Pruebas de acceso no autorizado" con una tabla de intentos por rol, la captura de la prueba unitaria y la captura de la comprobación manual, y una fila en su resumen.

**Justificación:** dejar evidencia homogénea de RNF-SEG-02, RNF-SEG-04 y, en las acciones de modificación, RNF-SEG-05. Aplicado en las iteraciones 1 y 4; la plantilla de la iteración 7 ya lo incluye.

### Trazabilidad

- [Issue #13 — Iteración 4](https://github.com/SheilaCS/ISO2-2026-C201/issues/13).
- [PR #51 — Servidor y pruebas](https://github.com/SheilaCS/ISO2-2026-C201/pull/51).
- [PR #53 — Cliente y correcciones](https://github.com/SheilaCS/ISO2-2026-C201/pull/53).
- [Documentación de la iteración 4](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-04).
- [PR #59 — Decisiones de la iteración 4](https://github.com/SheilaCS/ISO2-2026-C201/pull/59).
- [PR #62 — Pruebas de acceso no autorizado](https://github.com/SheilaCS/ISO2-2026-C201/pull/62).

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

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo y dentro del mismo paquete que la clase probada para facilitar la localización de incidencias y el mantenimiento de la batería de pruebas.

- El servicio se prueba verificando el almacenamiento, recuperación y actualización de parámetros.
- El servidor HTTP se prueba arrancándolo sobre un puerto libre y enviando peticiones HTTP reales.
- El cliente HTTP se prueba mediante un servidor embebido que simula las respuestas del servidor.
- La interfaz gráfica se prueba mediante clientes simulados que permiten validar el comportamiento sin dependencias externas.
- Se validan tanto operaciones correctas como situaciones de error y entradas inválidas.
- Los casos de prueba se diseñan utilizando clases de equivalencia, valores límite y conjetura de errores. La tabla de casos se encuentra en la wiki, apartado “Testing”.

**Resultado:**

Se ejecutaron correctamente **45 pruebas automatizadas**:

- **Servidor:** 12 pruebas.
- **Cliente:** 33 pruebas.

La ejecución de `mvn clean verify` finaliza correctamente, sin errores, fallos ni pruebas omitidas.

### Trazabilidad

- [Issue #14 — Entregar Iteración 5 (RF17)](https://github.com/SheilaCS/ISO2-2026-C201/issues/14).
- [PR #60 — Integración de CDU5](https://github.com/SheilaCS/ISO2-2026-C201/pull/60).
- [Documentación de la iteración 5](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-05).

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
- [Documentación de la iteración 6](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-06).
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

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo y dentro del mismo paquete que la clase probada para asegurar la correspondencia directa entre cada componente y sus pruebas asociadas.

- El servicio se prueba verificando las validaciones funcionales y las restricciones de seguridad basadas en roles.
- El servidor HTTP se prueba mediante peticiones reales enviadas a un servidor embebido arrancado en un puerto libre.
- El cliente HTTP se prueba utilizando servidores simulados para validar la construcción y envío de peticiones.
- La interfaz gráfica se prueba mediante clientes simulados para verificar validaciones, permisos, mensajes de error y resultados mostrados al usuario.
- Se comprueba tanto el comportamiento correcto como los escenarios de error, acceso denegado y fallos de comunicación.
- Los casos de prueba se diseñan utilizando clases de equivalencia, valores límite y conjetura de errores. La tabla de casos se encuentra en la wiki, apartado “Testing”.

**Resultado:**

Se ejecutaron correctamente **75 pruebas automatizadas**:

- **Servidor:** 27 pruebas.
- **Cliente:** 48 pruebas.

La ejecución de `mvn clean verify` finaliza correctamente, sin errores, fallos ni pruebas omitidas.

### Trazabilidad

- [Issue #17 — Entregar Iteración 8 (RFXX)](https://github.com/SheilaCS/ISO2-2026-C201/issues/17).
- [PR #61 — Integración de CDU8](https://github.com/SheilaCS/ISO2-2026-C201/pull/61).
- [Documentación de la iteración 8](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-08).

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
- [Documentación de la iteración 9](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-09).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.

## Iteración 10 — CDU10: Gestionar roles y permisos (RF18)

### Servicio y almacenamiento

**Decisión:** definir el contrato en `IRolesService`, implementado por `RolesServiceImpl`, y separar el almacenamiento mediante `RepositorioRoles` y `RepositorioRolesEnMemoria`. Las acciones del sistema se representan con el enum `Permiso` y se reutiliza el enum `Rol` del CDU1.

El repositorio empieza con los cuatro usuarios de demostración y con los permisos que el sistema ya aplica (por ejemplo, `GESTIONAR_PACIENTES` para el administrativo y `REGISTRAR_REALIZACION` para el médico).

**Justificación:** seguir la misma estructura que el resto de casos de uso y mantener los datos en memoria sin incorporar una base de datos.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas:

- Solo se puede consultar o cambiar el rol de usuarios existentes.
- Un administrador no puede cambiar su propio rol.
- El sistema debe conservar al menos un administrador.
- No se puede retirar al rol `ADMINISTRADOR` el permiso `GESTIONAR_ROLES`.
- La comprobación y el cambio se realizan como una sola operación.

**Justificación:** evitar que el sistema quede sin nadie capaz de gestionar roles y permisos.

### Comunicación y permisos

**Decisión:** comunicar `RolesHttpClient` con `GestionRolesHttpServer` mediante HTTP, en el puerto 8091, con formularios codificados en UTF-8. El servidor autentica con HTTP Basic mediante el servicio del CDU1 y solo atiende a los usuarios con el permiso `GESTIONAR_ROLES`: responde 401 ante credenciales inválidas y 403 sin ese permiso.

**Justificación:** comprobar el permiso, y no un rol fijo, hace que el propio CDU10 sea configurable. Los puertos 8080 a 8090 ya están en uso.

### Alcance

**Decisión:** en esta iteración, el inicio de sesión y los demás servidores siguen utilizando los roles predefinidos. Su conexión con la gestión de roles se abordará en una iteración posterior.

**Justificación:** modificar el CDU1 y el control de acceso del resto de servidores afectaría a todos los casos de uso ya integrados.

### Pruebas

**Decisión:** mantener las pruebas en `src/test/java`, con `@DisplayName` en español y la estructura preparar, ejecutar y comprobar. Las pruebas del cliente utilizan el servidor real en un puerto libre.

Las pruebas de acceso no autorizado comprueban que `MEDICO`, `PACIENTE` y `ADMINISTRATIVO` reciben 403 en las cuatro operaciones (12 casos) y que los roles y permisos no cambian tras el rechazo. También se comprueba que, al conceder `GESTIONAR_ROLES` a otro rol, este obtiene acceso.

**Resultado:** 52 pruebas superadas: 34 del servidor y 18 del cliente.

### Trazabilidad

- [Issue #19 — Entregar Iteración 10 (RF18)](https://github.com/SheilaCS/ISO2-2026-C201/issues/19).
- [Documentación de la iteración 10](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-10).

La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.


## Iteración 11 — CDU11: Gestionar calidad y seguridad (RF18)

### Servicio y almacenamiento

**Decisión:** definir el contrato en `ISeguridadService`, implementado por `SeguridadServiceImpl`. El servicio gestiona las reglas de control de acceso (RBAC) y almacena los registros de auditoría en memoria mediante una lista de textos.

**Justificación:** el objetivo del CDU11 es fijar la lógica de seguridad y la trazabilidad de las acciones. Al igual que en el resto del proyecto, no se utiliza una base de datos real en esta fase, sino estructuras en memoria que posteriormente podrán ser sustituidas por repositorios.

### Reglas de negocio y seguridad

**Decisión:** aplicar las siguientes reglas de control de acceso y calidad.

- **Seguridad (Control de acceso):** La acción `GESTIONAR_USUARIOS` está reservada exclusivamente al rol `ADMINISTRADOR`. El rol debe compararse tras aplicar `trim()` y `toUpperCase()`.
- **Seguridad (Privacidad):** Para la acción `VER_HISTORIAL`, un usuario con rol `PACIENTE` solo tendrá permitido el acceso a su propio historial (simulado en esta fase).
- **Calidad (Trazabilidad):** Todas las peticiones, tanto si son concedidas como denegadas, deben registrarse obligatoriamente en un log de auditoría mediante `registrarAuditoria(...)`.
- **Formato del log:** Cada registro de auditoría debe incluir la marca temporal exacta (`LocalDateTime.now()`), el usuario, la acción realizada y el resultado (permitido/denegado).
- **Consulta de logs:** Solo el rol `ADMINISTRADOR` tiene permiso para consultar los logs del sistema.

**Justificación:** garantizar la integridad y confidencialidad de los datos clínicos, cumpliendo con los requisitos de seguridad y calidad exigidos en el proyecto.

### Comunicación entre cliente y servidor

**Decisión:** comunicar el cliente y el servidor mediante HTTP. El cliente utiliza `SeguridadHttpClient` y el servidor `SeguridadHttpHandler`, que escucha en `127.0.0.1`, puerto 8087 (para evitar conflictos con el 8084 de parámetros y el 8085 de informes). La verificación se expone en `/seguridad/verificar` (POST) y la consulta de logs en `/seguridad/logs` (GET), con formularios codificados en UTF-8.

**Justificación:** mantener la misma arquitectura de comunicación que el resto de módulos del sistema.

### Validación en cliente y servidor

**Decisión:** el cliente comprueba que los campos no estén vacíos antes de enviar la petición. Sin embargo, el servidor (`SeguridadServiceImpl`) repite y centraliza toda la lógica de validación de roles y permisos.

**Justificación:** aunque el cliente valide para dar una respuesta rápida, el control de seguridad real y la auditoría deben residir siempre en el servidor, ya que un cliente manipulado podría intentar saltarse las restricciones.

### Interfaz del cliente

**Decisión:** construir `SeguridadGUI` con `JFrame` y `DISPOSE_ON_CLOSE`, formulario `GridLayout(3, 2)` con los campos `Usuario`, `Rol` y `Acción`, y dos botones `Verificar Permiso` y `Ver Logs`. El resultado se muestra en un `JTextArea` no editable. `SeguridadGUI(SeguridadHttpClient)` permite inyectar un cliente simulado para las pruebas.

**Justificación:** mantener la misma pauta visual y de inyección de dependencias que el resto de pantallas Swing del proyecto.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo y dentro del mismo paquete que la clase probada para facilitar la trazabilidad entre código de producción y código de prueba.

- El servicio se prueba verificando los distintos niveles de acceso según el rol del usuario y las acciones solicitadas.
- El servicio también se prueba validando el registro y recuperación de auditorías de seguridad.
- El servidor HTTP se prueba arrancándolo sobre un puerto libre y realizando peticiones HTTP reales mediante GET y POST.
- El cliente HTTP se prueba mediante un servidor simulado que permite validar las peticiones enviadas sin depender de servicios externos.
- La interfaz gráfica se prueba utilizando clientes simulados para aislar la lógica de presentación y comprobar la gestión de resultados y errores.
- Se verifican tanto los escenarios de acceso autorizado como los intentos de acceso denegado y los errores de comunicación.
- Los casos de prueba se diseñan utilizando clases de equivalencia, valores límite y conjetura de errores. La tabla de casos se encuentra en la wiki, apartado "Testing".

**Resultado:**

Se ejecutaron correctamente **36 pruebas automatizadas**:

- **Servidor:** 22 pruebas.
- **Cliente:** 14 pruebas.

La ejecución de `mvn clean verify` finaliza correctamente, sin errores, fallos ni pruebas omitidas.

### Trazabilidad

- [Issue #72 — Entregar Iteración 11 (RF18)](https://github.com/SheilaCS/ISO2-2026-C201/issues/72). 
- [PR #20 — Integración de CDU11](https://github.com/SheilaCS/ISO2-2026-C201/pull/20). 
- [Documentación de la iteración 11](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-11).

La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.


## Iteración 12 — CDU12: Historial de pruebas (RF04)

### Acceso a las pruebas realizadas mediante una interfaz

**Decisión:** el servicio de historial no accede directamente a la lógica de otros módulos. Utiliza la interfaz `IHistorialPruebasProveedor` y su implementación `ProveedorHistorialRealizacion`, que consulta por HTTP, en cada petición, al servidor de realizaciones de la iteración 7 (puerto 8086) y al servidor de citas (puerto 8080). El historial no guarda datos propios.

**Justificación:** reducir las dependencias entre casos de uso y poder probar `HistorialServiceImpl` con un proveedor simulado. Cumple el criterio de integración por interfaz del plan de calidad.

### Servidor y comunicación

**Decisión:** definir el contrato en `IHistorialService`, implementado por `HistorialServiceImpl`, y exponerlo por HTTP con `GestionHistorialHttpServer` en `127.0.0.1`, puerto 8088, mediante `GET /historial?paciente=ID`. El cliente `GestionHistorialGUI` valida con `ValidacionesHistorial` y se comunica mediante `HistorialHttpClient`.

Para funcionar, necesita que estén disponibles los servidores de realizaciones y de citas. Si no lo están, responde 503.

**Justificación:** seguir la misma estructura que los módulos anteriores. Se usa el puerto 8088 porque el 8085 lo utiliza el servidor de informes (CDU8).

### Control de acceso y cuenta de servicio

**Decisión:** el servidor identifica al usuario con HTTP Basic. El médico puede consultar el historial de cualquier paciente, y el paciente solo el del número vinculado a su usuario mediante `IVinculoPaciente`. El resto de roles recibe 403, y sin credenciales válidas, 401. Cada consulta queda anotada en `RegistroConsultas`.

Como el servidor de realizaciones solo admite al rol `MEDICO`, `ProveedorHistorialRealizacion` lo consulta con una cuenta de servicio, configurable con `historial.servicio.usuario` y `historial.servicio.clave` (por defecto, la de la demostración).

**Justificación:** el servidor no se fía del identificador que envía el cliente, sino del usuario autenticado. Reenviar las credenciales del paciente haría que recibiera 403 de realizaciones. Cumple las pruebas de acceso no autorizado que el plan de calidad exige en los casos de uso clínicos.

### Limitaciones conocidas

**Decisión:** aceptar dos limitaciones.

- El tipo de prueba se muestra como «Prueba de la solicitud N», porque el módulo de solicitudes no lo almacena.
- Los vínculos usuario-paciente están en memoria (`VinculoPacienteEnMemoria`): por defecto solo existe `paciente.demo` → paciente 1, y se amplían con la propiedad `historial.vinculos`. El CDU4 no relaciona hoy usuarios con pacientes.

**Justificación:** no cargar datos que ningún módulo guarda. Al acceder por interfaz, bastará con sustituir esas implementaciones cuando existan los datos reales.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, con clases de equivalencia, valores límite y conjetura de errores, mediante `@ParameterizedTest` y `assertThrows`. Incluyen pruebas de acceso no autorizado y una prueba de extremo a extremo con servidores falsos de realizaciones y citas. La tabla de casos está en la wiki, página "Iteración 12", apartado "Pruebas".

**Resultado:** `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas. La cobertura del paquete `historial` supera el 90 % de líneas.

### Trazabilidad

- [PR #70 — Integración de CDU12](https://github.com/SheilaCS/ISO2-2026-C201/pull/70).
- [Documentación de la iteración 12](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-12).
La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.

## Iteración 15 — CDU15: Listas de espera y huecos (RF15)

### Acceso a los datos mediante interfaces

**Decisión:** el servicio de lista de espera no accede directamente a la lógica de otros módulos. Utiliza tres interfaces: `ISolicitudesPendientesProveedor` (solicitudes en espera), `IHuecosProveedor` (huecos libres) e `IAsignadorCitas` (asignación de la cita). Esta última tiene una implementación real, `AsignadorCitasHttp`, que llama por HTTP a `POST /citas/asignar` del servidor de citas (puerto 8080).

**Justificación:** reducir las dependencias entre casos de uso y poder probar `ListaEsperaServiceImpl`. Cumple el criterio de integración por interfaz del plan de calidad.

### Servidor y comunicación

**Decisión:** definir el contrato en `IListaEsperaService`, implementado por `ListaEsperaServiceImpl`, y exponerlo por HTTP con `GestionListaEsperaHttpServer` en `127.0.0.1`, puerto 8089, configurable con la propiedad `listaespera.puerto`. El cliente `GestionListaEsperaGUI` valida con `ValidacionesListaEspera` y se comunica mediante `ListaEsperaHttpClient`.

**Justificación:** seguir la misma estructura que los módulos anteriores. Se usa el puerto 8089 porque el 8086 lo utiliza el servidor de realizaciones (CDU7) y el 8088 el de historial (CDU12).

### Control de acceso

**Decisión:** todas las operaciones exigen autenticación Basic de un usuario `ADMINISTRATIVO`. Responde 401 sin credenciales válidas y 403 con otro rol. Las operaciones rechazadas por las reglas de negocio responden 409 con un mensaje.

**Justificación:** la lista de espera maneja datos clínicos de pacientes y solo debe gestionarla personal administrativo.

### Limitaciones conocidas

**Decisión:** aceptar que la lista de espera y los huecos libres se obtengan de un proveedor simulado en memoria (`ProveedorListaEsperaEnMemoria`), porque los módulos de origen no exponen todavía los datos necesarios:

- Solicitudes pendientes: el módulo de solicitudes (CDU2) solo valida y no almacena las solicitudes.
- Huecos libres: el módulo de agendas (CDU6) solo permite consultar la agenda de un médico concreto, y el módulo de citas (CDU3) no ofrece un listado general ni guarda qué médico atiende cada cita.

**Justificación:** mantener el alcance del MVP sin modificar módulos de otros casos de uso (RNF-MAN-07). Al acceder por interfaz, bastará con sustituir el proveedor cuando esos módulos expongan los datos. La asignación de la cita sí es real.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, con clases de equivalencia, valores límite y conjetura de errores, mediante `@ParameterizedTest` y `assertThrows`. Incluyen pruebas de acceso no autorizado y pruebas del asignador contra un servidor de citas falso. La tabla de casos está en la wiki, página "Iteración 15", apartado "Pruebas".

**Resultado:** `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas. 

### Trazabilidad

- [PR #74 — Integración de CDU15](https://github.com/SheilaCS/ISO2-2026-C201/pull/74).
- [Documentación de la iteración 15](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteración-15).
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



## Iteración 18 — CDU18: Comunicar incidencias y responder encuestas de satisfacción (RF05)

### Acceso a los datos mediante una interfaz

**Decisión:** el servicio de encuestas no accede directamente a la lógica de otros módulos. Utiliza la interfaz `IEncuestasProveedor` y su implementación `ProveedorEncuestasHttp`, que consulta mediante HTTP al servidor de Mis citas de la iteración 9 (puerto 8082) y al servidor de realización de pruebas de la iteración 7 (puerto 8086).

**Justificación:** reducir las dependencias entre casos de uso y poder probar `GestionEncuestasServiceImpl` con un proveedor simulado. Cumple el criterio de integración por interfaz del plan de calidad.

### Servidor y comunicación

**Decisión:** definir el contrato en `IGestionEncuestasService`, implementado por `GestionEncuestasServiceImpl`, y exponerlo por HTTP con `GestionEncuestasHttpServer` en el puerto 8090. El cliente `GestionEncuestasGUI` valida con `ValidacionesEncuestas` y se comunica mediante `EncuestasHttpClient`.

Para funcionar, necesita que estén disponibles los servidores de Mis citas y realización de pruebas, y de forma indirecta el servidor de citas (puerto 8080).

**Justificación:** seguir la misma estructura que los módulos anteriores y evitar conflictos entre los puertos utilizados por los distintos servidores.

### Servicio y almacenamiento

**Decisión:** representar las incidencias mediante `IncidenciaPaciente` y las encuestas de satisfacción mediante `EncuestaSatisfaccion`. Los registros se almacenan en memoria en el servidor.

**Justificación:** mantener la estructura del proyecto sin incorporar una base de datos en esta fase y separar la lógica de negocio de la comunicación HTTP.

### Reglas de negocio

**Decisión:** aplicar las siguientes reglas.

- El ID de cita debe ser un entero mayor que 0.
- La cita debe pertenecer al paciente autenticado.
- La descripción de la incidencia es obligatoria y no puede superar los 500 caracteres.
- Solo se permite responder a una encuesta cuando la prueba se ha realizado.
- La puntuación debe estar comprendida entre 1 y 5.
- El comentario de la encuesta es opcional y no puede superar los 500 caracteres.
- Solo se permite una encuesta de satisfacción por cita.

**Justificación:** evitar registros incoherentes, impedir encuestas duplicadas y garantizar que el paciente únicamente pueda realizar operaciones sobre sus propias citas.

### Control de acceso

**Decisión:** reutilizar el servicio de autenticación de demostración del proyecto y restringir las operaciones al rol `PACIENTE`. El servidor comprueba las credenciales y la pertenencia de la cita al paciente autenticado.

**Justificación:** evitar accesos no autorizados y garantizar la confidencialidad de los datos asociados a los pacientes.

### Validación en cliente y servidor

**Decisión:** comprobar los datos en el cliente con `ValidacionesEncuestas` antes de enviar la petición y repetir las validaciones correspondientes en el servidor.

**Justificación:** mostrar respuestas rápidas al usuario sin depender únicamente de la validación del cliente.

### Interfaz del cliente

**Decisión:** construir `GestionEncuestasGUI` mediante Java Swing, con los campos `Usuario`, `Clave`, `ID de cita`, `Descripción de incidencia`, `Puntuación` y `Comentario opcional`, y dos botones: `Comunicar incidencia` y `Enviar encuesta`.

Las operaciones se ejecutan mediante `SwingWorker` para evitar bloquear la interfaz durante la comunicación con el servidor. El campo de contraseña se limpia después de cada operación.

**Justificación:** mantener la misma pauta visual que el resto de pantallas Swing, separar la lógica de presentación de la comunicación HTTP y proteger las credenciales del usuario.

### Integración

**Decisión:** desarrollar el cliente, el servidor y sus pruebas en la rama `feature/cdu18-encuestas`, sin modificar los módulos de otros casos de uso.

Antes de la integración se comprobó manualmente el funcionamiento contra los servidores reales de citas, Mis citas y realización de pruebas, registrando correctamente una incidencia y una encuesta de satisfacción.

**Justificación:** garantizar la compatibilidad con las funcionalidades desarrolladas en las iteraciones anteriores y comprobar el funcionamiento completo del CDU18.

### Limitaciones conocidas

**Decisión:** mantener el almacenamiento de incidencias y encuestas en memoria, por lo que los registros se pierden al reiniciar el servidor.

**Justificación:** mantener el alcance del proyecto en esta fase, sin incorporar mecanismos adicionales de persistencia.

### Organización de las pruebas

**Decisión:** mantener las pruebas en `src/test/java` de cada módulo, en el mismo paquete que la clase probada.

- El servicio se prueba mediante proveedores simulados, verificando las reglas de negocio.
- El servidor HTTP se prueba arrancándolo en un puerto libre y realizando peticiones reales.
- El proveedor HTTP se prueba mediante servidores simulados de los módulos dependientes.
- El cliente HTTP y la interfaz gráfica se prueban mediante componentes simulados.
- Se verifican tanto las operaciones correctas como los errores de validación, autenticación y comunicación.
- Los casos de prueba se diseñan mediante clases de equivalencia, valores límite y conjetura de errores.

**Resultado:** se ejecutaron correctamente **88 pruebas automatizadas**:

- **Servidor:** 48 pruebas.
- **Cliente:** 40 pruebas.

La ejecución de `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas.

### Trazabilidad

- [Documentación de la iteración 18](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-18).
- [Rama de desarrollo de CDU18](https://github.com/SheilaCS/ISO2-2026-C201/tree/feature/cdu18-encuestas).

La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.