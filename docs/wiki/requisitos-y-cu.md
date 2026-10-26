# Requisitos y casos de uso — SESCAM Radiodiagnóstico

> Documentación de la fase de Inicio (Iteración 0, PUD).
> Enlace a la wiki: página "Requisitos y casos de uso".

## 1. Actores del sistema

### 1.1 Actores humanos

| Actor | Descripción |
|-------|-------------|
| Paciente | Usuario básico que consulta y gestiona sus citas, notificaciones e informes |
| Médico | Profesional sanitario que prescribe pruebas, gestiona seguimientos y consulta informes |
| Personal Administrativo / Admisión | Gestiona altas, citas, agendas, listas de espera y estadísticas operativas |
| Administrador del Sistema | Configura parámetros, roles, auditoría e integraciones externas |

### 1.2 Actores de tipo sistema

| Actor | Descripción |
|-------|-------------|
| SSO corporativo | Servicio central de inicio de sesión único |
| Historia Clínica Electrónica (HCE) | Sistema externo con datos clínicos del paciente |
| Sistema de Admisión | Sistema de gestión de pacientes del hospital |
| Plataforma RIS/PACS | Sistema de imágenes médicas (DICOM, HL7, FHIR) |
| Temporizador | Actor de tipo sistema que dispara recordatorios y seguimientos |

## 2. Requisitos funcionales

| RF | Descripción | Actor |
|----|-------------|-------|
| RF01 | Iniciar sesión mediante SSO corporativo | Todos |
| RF02 | Consultar y gestionar citas programadas (confirmar, cambiar, cancelar) | Paciente |
| RF03 | Recibir notificaciones y recordatorios de citas, preparación e informes | Paciente |
| RF04 | Consultar el historial de pruebas realizadas | Paciente |
| RF05 | Comunicar incidencias y responder encuestas de satisfacción | Paciente |
| RF06 | Solicitar/prescribir pruebas diagnósticas con indicación clínica y prioridad | Médico |
| RF07 | Prescribir pruebas de seguimiento periódicas | Médico |
| RF08 | Consultar el estado de solicitudes y agendas disponibles | Médico |
| RF09 | Crear y consultar informes e imágenes de pacientes | Médico |
| RF10 | Registrar la realización de la prueba e incidencias | Médico |
| RF11 | Consultar estadísticas de solicitudes (tiempos, recursos, adherencia) | Médico |
| RF12 | Alta, modificación y baja de pacientes | Administrativo |
| RF13 | Gestionar citas: asignar, reprogramar, cancelar | Administrativo |
| RF14 | Gestionar las agendas de los médicos | Administrativo |
| RF15 | Gestionar listas de espera, prioridades y huecos | Administrativo |
| RF16 | Generar informes operativos (ocupación, esperas, cancelaciones) | Administrativo |
| RF17 | Configurar parámetros del sistema (tipos de prueba, protocolos, horarios) | Administrador |
| RF18 | Gestionar roles y permisos | Administrador |
| RF19 | Auditoría de actividades y accesos a datos clínicos | Administrador |
| RF20 | Configurar integraciones con SSO, HCE, admisión y RIS/PACS | Administrador |
| RF21 | Generar estadísticas globales y reportes de uso | Administrador |

>  **Decisión D-005 (a registrar):** las viñetas de "consulta y gestión" se han separado en dos RF (consulta y acción) porque implican casos de uso distintos con distintos permisos.

## 3. Diagrama general de casos de uso

Ver `docs/diagramas/casos-de-uso.puml`.

## 4. Casos de uso priorizados por fases del PUD

Aplicamos la simplificación del enunciado: **1 RF = 1 CU = 1 iteración**.

| Iteración | Fase PUD | Caso de uso | Prioridad | Justificación |
|-----------|----------|-------------|-----------|---------------|
| It0 | Inicio | Visión, requisitos, planificación (este documento) | — | Fija el alcance |
| It1 | Elaboración | CU01 Iniciar sesión (SSO) | 1 | Establece la arquitectura de seguridad |
| It2 | Elaboración | CU06 Solicitar prueba diagnóstica | 2 | Entidad central del sistema |
| It3 | Elaboración | CU13 Gestionar citas | 3 | Depende de CU06 |
| It4 | Construcción | CU02 Gestionar mis citas (paciente) | 4 | Vista del paciente |
| It5 | Construcción | CU09 Crear y consultar informes | 5 | Entidad clínica clave |
| It6 | Construcción | CU14 Gestionar agendas de médicos | 6 | Soporte a CU13 |
| It7 | Construcción | CU07 Prescribir seguimientos | 7 | Amplía CU06 |
| It8 | Construcción | CU10 Registrar realización e incidencias | 8 | Cierra el flujo clínico |
| It9 | Construcción | CU12 Gestionar pacientes (CRUD) | 9 | Soporte de admisión |
| It10 | Construcción | CU15 Gestionar listas de espera | 10 | Optimiza recursos |
| It11 | Construcción | CU03 Notificaciones y recordatorios | 11 | Experiencia del paciente |
| It12 | Construcción | CU04 Consultar historial | 12 | Consulta de paciente |
| It13 | Construcción | CU17 Configurar parámetros | 13 | Administración |
| It14 | Construcción | CU18 Gestionar roles y permisos | 14 | Administración |
| It15 | Construcción | CU19 Auditoría | 15 | Seguridad |
| It16 | Construcción | CU20 Configurar integraciones | 16 | Arquitectura |
| It17 | Construcción | CU11 Estadísticas del médico | 17 | Análisis |
| It18 | Construcción | CU16 Informes operativos | 18 | Análisis |
| It19 | Construcción | CU21 Estadísticas globales | 19 | Análisis |
| It20 | Construcción | CU05 Encuestas de satisfacción | 20 | Calidad |
| It21 | Construcción | CU08 Consultar estado de solicitudes | 21 | Soporte médico |
| ItN | Transición | Integración, despliegue, manual | — | Cierre del proyecto |

**Costes por iteración** (según enunciado):
- It0 = 1000 €
- ItN = 2000 €
- It1..It21 = a estimar por el equipo (RADIT).

>  **Trazabilidad:** cada CU tendrá su rama `feature/CUxx-nombre` y su issue correspondiente, según el modelo Git Flow acordado (ver `DECISIONS.md`).