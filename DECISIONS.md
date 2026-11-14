# DECISIONS.md — Registro de decisiones del proyecto

> Registro de decisiones de requisitos, análisis, diseño y arquitectura del proyecto
> Sistema Integral de Gestión de Radiodiagnóstico e Imagen Médica — SESCAM.
> Cada decisión se enlaza al acta donde se acordó.



## D-001 • Estructura inicial del repositorio

**Fecha:** 11/10/2026 · **Iteración:** 0 · **Acta:** 11/10
**Contexto:** el enunciado exige una arquitectura cliente-servidor con múltiples clientes (escritorio, web, móvil) y Maven multimódulo.
**Decisión:** usar un único repositorio con la estructura:

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

### Eliminación del simulador del cliente

**Decisión implementada en la PR #37:** eliminar `ServicioAutenticacionSimulado` y `ServicioAutenticacionSimuladoTest`, porque la pantalla ya utiliza `ServicioAutenticacionServidor`.

Se conserva `AutenticacionServiceImpl`, que sigue proporcionando la autenticación de demostración del servidor.

**Resultado:** quedan siete pruebas: cinco del servicio del servidor y dos del adaptador del cliente. La ejecución de `mvn clean verify` termina correctamente, sin fallos, errores ni pruebas omitidas.

### Trazabilidad

- [Issue #9 — Entregar Iteración 1](https://github.com/SheilaCS/ISO2-2026-C201/issues/9).
- [PR #35 — Integración de CU01](https://github.com/SheilaCS/ISO2-2026-C201/pull/35).
- [PR #37 — Revisión de tests y eliminación del simulador](https://github.com/SheilaCS/ISO2-2026-C201/pull/37).
- [Documentación de la iteración 1](https://github.com/SheilaCS/ISO2-2026-C201/wiki/Iteraci%C3%B3n-1).

La aprobación y la integración de cada cambio se acreditan mediante el historial de su pull request.
