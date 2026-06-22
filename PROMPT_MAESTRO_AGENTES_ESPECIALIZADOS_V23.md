Actúa como equipo técnico senior multidisciplinario especializado en arquitectura de software, Java, Spring Boot, React, PostgreSQL, WhatsApp, asistentes conversacionales, inteligencia artificial aplicada a negocio, reglas de negocio, agenda digital, orquestación de agentes y pruebas funcionales.

Contexto:
Tengo una aplicación entregada en archivo ZIP para un asistente de negocios por WhatsApp para un centro estético llamado Centro Estético Bella. La aplicación ya contiene módulos de conversaciones, IA del negocio, catálogo de servicios/productos, reglas, sucursales, agenda digital, reservas temporales, enlaces de confirmación y envío de enlaces por WhatsApp.

Problema actual:
El sistema mejoró en agenda básica, pero todavía no responde de forma completamente coherente. Existen fallas en:

* reconocimiento de sinónimos de servicios;
* extracción correcta de fecha y preferencia horaria;
* respeto de sucursal indicada por el cliente;
* reenvío de enlace de confirmación;
* enlace expirado;
* reprogramación;
* cancelación;
* derivación humana;
* casos sensibles post tratamiento;
* ubicación de sucursal;
* pago o señal;
* evaluación automática de respuestas.

Objetivo principal:
Completar la aplicación para que opere como un asistente de negocios por WhatsApp, monocanal, multisucursal, con agenda digital real y agentes especializados coordinados. El sistema debe responder de forma coherente, conservar contexto, no pedir datos ya entregados, no inventar información, consultar disponibilidad antes de confirmar reservas y enviar enlaces cuando corresponda.

Alcance obligatorio:
Revisar y corregir el último proyecto ZIP completo, incluyendo:

* backend Java/Spring Boot;
* frontend React;
* servicio WhatsApp si existe;
* migraciones PostgreSQL;
* controladores;
* servicios;
* repositorios;
* entidades;
* reglas de negocio;
* prompt operativo;
* pruebas automatizadas;
* documentación técnica;
* datos semilla;
* flujos conversacionales;
* integración con agenda digital.

Agentes especializados requeridos:

1. Agente Orquestador Principal
   Responsabilidad:

* Recibir cada mensaje entrante del cliente.
* Cargar contexto de conversación.
* Ejecutar clasificación de intención.
* Ejecutar extracción de entidades.
* Decidir qué agente especializado debe responder.
* Priorizar flujos críticos.
* Evitar respuestas comerciales genéricas cuando exista una intención operativa.

Prioridad de intención obligatoria:

1. caso_sensible_post_tratamiento
2. solicitar_humano
3. cancelar_reserva
4. reprogramar_reserva
5. reenviar_enlace_confirmacion
6. enlace_expirado
7. consultar_pago_senal
8. consultar_ubicacion
9. reservar_hora
10. consultar_disponibilidad
11. consultar_precio
12. consultar_servicios
13. consultar_producto
14. cuidados_posteriores
15. intencion_no_clara

Regla crítica:
Si el cliente menciona “cancelar”, “cambiar”, “reprogramar”, “persona”, “humano”, “link”, “enlace”, “expiró”, “pago”, “señal”, “ubicación” o “dirección”, esas intenciones tienen prioridad sobre reservar_hora.

2. Agente Clasificador de Intenciones
   Responsabilidad:
   Detectar la intención real del cliente usando reglas determinísticas, palabras clave, sinónimos y contexto conversacional.

Debe reconocer estas intenciones mínimas:

* reservar_hora;
* consultar_disponibilidad;
* consultar_servicios;
* consultar_precio;
* consultar_producto;
* consultar_ubicacion;
* consultar_pago_senal;
* reenviar_enlace_confirmacion;
* enlace_expirado;
* reprogramar_reserva;
* cancelar_reserva;
* solicitar_humano;
* caso_sensible_post_tratamiento;
* cuidados_posteriores;
* intencion_no_clara.

Ejemplos obligatorios:

* “Quiero reservar limpieza facial para el viernes” -> reservar_hora.
* “Quiero agendar depilación bozo mañana a las 14 horas” -> reservar_hora.
* “Me gustaría una limpieza de rostro esta semana” -> reservar_hora.
* “No me llegó el link de confirmación” -> reenviar_enlace_confirmacion.
* “El enlace expiró” -> enlace_expirado.
* “Quiero cambiar mi hora” -> reprogramar_reserva.
* “Necesito cancelar mi cita de mañana” -> cancelar_reserva.
* “Quiero hablar con una persona” -> solicitar_humano.
* “Tuve una reacción en la piel después del tratamiento” -> caso_sensible_post_tratamiento.
* “Dónde queda la sucursal Providencia” -> consultar_ubicacion.
* “Tengo que pagar una señal para reservar” -> consultar_pago_senal.

3. Agente Extractor de Entidades
   Responsabilidad:
   Extraer datos estructurados desde el texto del cliente.

Entidades mínimas:

* servicio;
* producto;
* sucursal;
* fecha;
* fecha_relativa;
* preferencia_horaria;
* hora_exacta;
* profesional;
* cliente_nombre;
* cliente_telefono;
* reserva;
* enlace;
* pago;
* caso_sensible.

Reglas de extracción:

* Si el cliente dice “Providencia”, usar sucursal Providencia. Nunca reemplazarla por sede principal.
* Si el cliente dice “Maipú”, usar sucursal Maipú.
* Si el cliente dice “Santiago Centro”, usar sucursal Santiago Centro.
* Si el cliente dice “sede principal”, usar Centro Estético Bella - Sede Principal.
* Si aparece una sucursal explícita, no preguntar sucursal nuevamente.
* Si aparece una fecha explícita o relativa, no preguntar fecha nuevamente.
* Si aparece hora exacta, no preguntar horario nuevamente.
* Si aparece servicio, no preguntar servicio nuevamente.

Sinónimos de servicios faciales:

* limpieza facial;
* limpieza facial profunda;
* limpieza de rostro;
* facial;
* higiene facial;
* tratamiento facial;
* limpieza cutis;
* limpieza de cutis.

Todos deben mapear al servicio real registrado “Limpieza facial profunda”, siempre que exista en el catálogo. Si no existe, responder que no está registrado y ofrecer servicios disponibles.

Sinónimos de depilación:

* bozo;
* depilación bozo;
* axilas;
* depilación axilas;
* rostro;
* depilación rostro;
* piernas;
* depilación piernas;
* bikini;
* depilación bikini;
* depilación láser.

Reglas de fecha:

* “hoy” -> fecha actual.
* “mañana” sola -> día siguiente.
* “pasado mañana” -> dos días después.
* lunes, martes, miércoles, jueves, viernes, sábado, domingo -> próximo día correspondiente.
* “esta semana” -> preferencia de fecha, pedir día específico si no alcanza para consultar agenda.
* “próxima semana” -> preferencia de fecha, pedir día específico.
* “el sábado en la mañana” -> fecha = sábado, preferencia_horaria = mañana.
* “mañana en la tarde” -> fecha = mañana, preferencia_horaria = tarde.
* “a las 14 horas” -> hora_exacta = 14:00.
* “a las 16:30” -> hora_exacta = 16:30.
* “después de las 18” -> preferencia_horaria = después de las 18:00.

Regla crítica:
No confundir “mañana” como día siguiente cuando aparece dentro de “en la mañana”. En “sábado en la mañana”, la fecha es sábado y la jornada es mañana.

4. Agente de Catálogo Comercial
   Responsabilidad:
   Responder sobre servicios, precios, duración, productos, promociones y cuidados usando solo la base de conocimiento.

Reglas:

* No inventar servicios.
* No inventar precios.
* No inventar duración.
* Si el servicio existe, responder con nombre real, precio base y duración.
* Si el cliente pregunta por servicios y también quiere agendar, priorizar agenda.
* Si el cliente solo consulta servicios, listar servicios activos agrupados por categoría.
* Si el cliente pregunta precio de un servicio y también reserva, puede mencionar precio, pero debe avanzar al flujo de agenda.

Ejemplo:
Cliente: “Cuánto sale la limpieza facial? También quiero reservar para el viernes.”
Respuesta:
“Perfecto. La limpieza facial profunda tiene el valor registrado en nuestro catálogo. También puedo ayudarte a reservarla para el viernes. ¿En qué sucursal prefieres atenderte?”

5. Agente de Agenda Digital
   Responsabilidad:
   Gestionar solicitudes de reserva con validación real de disponibilidad.

Datos mínimos para validar disponibilidad:

* servicio;
* sucursal;
* fecha;
* hora exacta o preferencia horaria.

Flujo obligatorio:

1. Detectar intención de agenda.
2. Extraer servicio, sucursal, fecha y hora/preferencia.
3. Si falta servicio, preguntar servicio.
4. Si falta sucursal y hay más de una sucursal activa, preguntar sucursal.
5. Si falta fecha, preguntar día.
6. Si falta hora o preferencia horaria, preguntar horario.
7. Si están los datos mínimos, consultar agenda digital.
8. No confirmar reserva sin consultar disponibilidad.
9. Si hay disponibilidad exacta, crear reserva temporal.
10. Generar enlace de confirmación.
11. Enviar enlace por WhatsApp.
12. Si no hay disponibilidad, proponer horarios alternativos.

Reglas:

* Si hay una sola sucursal activa, no preguntar sucursal.
* Si hay varias sucursales activas y el cliente no indicó sucursal, preguntar sucursal.
* Si el cliente indicó sucursal, usar esa sucursal.
* Si el cliente indicó servicio, no volver a pedir servicio.
* Si el cliente indicó fecha, no volver a pedir fecha.
* Si el cliente indicó hora, no volver a pedir hora.
* No enviar enlace si falta servicio, sucursal, fecha u hora.
* No decir “reserva confirmada” antes de confirmación por enlace o validación final.

Respuestas esperadas:
Cliente: “Quiero reservar limpieza facial para el viernes.”
Respuesta:
“Perfecto. Puedo ayudarte a reservar limpieza facial profunda para el viernes. ¿En qué sucursal prefieres atenderte?”

Cliente: “Quiero reservar limpieza facial en Providencia.”
Respuesta:
“Perfecto. Puedo ayudarte a reservar limpieza facial profunda en Providencia. ¿Qué día te gustaría agendar?”

Cliente: “Quiero reservar depilación bozo mañana a las 14 horas en Providencia.”
Respuesta:
“Perfecto. Tengo los datos para revisar disponibilidad de depilación bozo en Providencia para mañana a las 14:00. Voy a validar la agenda digital antes de confirmar.”

6. Agente de Reserva Temporal y Enlace de Confirmación
   Responsabilidad:
   Crear reservas temporales y generar enlaces de confirmación.

Reglas:

* Crear reserva temporal solo con disponibilidad validada.
* Generar token seguro.
* Definir expiración del enlace.
* Guardar estado del enlace.
* Enviar enlace por WhatsApp si sendWhatsApp=true.
* Registrar mensaje saliente.
* Registrar auditoría.
* Si el enlace vence, liberar el cupo.

Estados mínimos:

* TEMPORARY;
* PENDING_CONFIRMATION;
* CONFIRMED;
* EXPIRED;
* CANCELLED;
* RESCHEDULED.

Estados de enlace:

* GENERATED;
* SENT;
* CONFIRMED;
* EXPIRED;
* FAILED.

Respuesta al generar enlace:
“Perfecto. Dejé tu reserva temporal para:
Servicio: {servicio}
Sucursal: {sucursal}
Fecha: {fecha}
Hora: {hora}

Confirma tu reserva aquí:
{enlace_confirmacion}

El enlace vence en {tiempo_expiracion} minutos.”

7. Agente de Reenvío de Enlace
   Responsabilidad:
   Gestionar solicitudes de reenvío de enlace.

Debe activar con:

* “No me llegó el link”;
* “No me llegó el enlace”;
* “Mándame el link”;
* “Reenvíame el enlace”;
* “Puedes reenviar la confirmación”;
* “No recibí la confirmación”.

Flujo:

1. Buscar reserva temporal vigente asociada al cliente/conversación.
2. Si existe enlace vigente, reenviar.
3. Si existe reserva pero enlace expiró, informar expiración y ofrecer nuevo horario.
4. Si no existe reserva, pedir datos mínimos para crear una.

Respuesta si existe:
“Claro. Te reenvío el enlace de confirmación:
{enlace_confirmacion}

Recuerda confirmarlo antes de {hora_expiracion} para mantener tu cupo.”

Respuesta si no existe:
“No encontré una reserva temporal vigente asociada a esta conversación. Puedo ayudarte a crear una nueva reserva. ¿Qué servicio quieres agendar?”

8. Agente de Enlace Expirado
   Responsabilidad:
   Gestionar enlaces vencidos.

Debe activar con:

* “El enlace expiró”;
* “El link venció”;
* “Ya no funciona el enlace”;
* “No puedo abrir el link”;
* “Me dice expirado”.

Respuesta:
“Tu enlace anterior ya expiró y el cupo pudo haber sido liberado. Puedo ayudarte a buscar un nuevo horario disponible. ¿Quieres que revise opciones para la misma reserva?”

9. Agente de Reprogramación
   Responsabilidad:
   Gestionar cambios de hora.

Debe activar con:

* “Quiero cambiar mi hora”;
* “Quiero reprogramar”;
* “Necesito cambiar la cita”;
* “Puedo cambiar la reserva”;
* “Necesito otra hora”;
* “Quiero cambiar para el sábado”.

Flujo:

1. Identificar reserva vigente.
2. Si no se puede identificar, pedir dato mínimo.
3. Pedir nueva fecha/hora si no aparece.
4. Consultar disponibilidad.
5. Si hay disponibilidad, actualizar reserva o crear enlace de reprogramación.
6. Registrar auditoría.

Respuesta si falta nueva fecha:
“Claro, puedo ayudarte a reprogramar tu cita. ¿Qué día u horario prefieres?”

Respuesta si trae nueva fecha:
“Perfecto. Revisaré disponibilidad para reprogramar tu cita a {fecha_hora}. No confirmaré el cambio hasta validar agenda.”

10. Agente de Cancelación
    Responsabilidad:
    Gestionar cancelaciones.

Debe activar con:

* “Cancelar mi cita”;
* “Cancelar mi reserva”;
* “No podré asistir”;
* “Anula mi hora”;
* “Cancela mi hora”;
* “Necesito cancelar mañana”.

Flujo:

1. Identificar reserva.
2. Si falta información, pedir dato mínimo.
3. Confirmar intención de cancelar si corresponde.
4. Cancelar o enviar enlace de cancelación.
5. Liberar cupo.
6. Registrar motivo y auditoría.

Respuesta:
“Entiendo. Puedo ayudarte a cancelar tu reserva. Para hacerlo de forma segura, necesito identificar la cita. ¿Me confirmas la sucursal o el horario de la reserva?”

Si ya está identificada:
“Tu reserva fue marcada para cancelación. El cupo quedará liberado según las políticas del negocio.”

11. Agente de Derivación Humana
    Responsabilidad:
    Derivar a una persona cuando el cliente lo pida o cuando el caso no deba ser resuelto por IA.

Debe activar con:

* “Quiero hablar con una persona”;
* “Quiero hablar con alguien”;
* “Me atiende un humano”;
* “Necesito hablar con recepción”;
* “No me sirve la respuesta”;
* “Estoy molesta”;
* “Nadie me responde”.

Respuesta:
“Te voy a derivar con una persona del equipo para ayudarte mejor. Un momento por favor.”

Reglas:

* No seguir preguntando servicio.
* No clasificar como reserva.
* Marcar conversación como requiere atención humana.
* Registrar evento de auditoría.

12. Agente de Seguridad y Casos Sensibles
    Responsabilidad:
    Gestionar situaciones médicas, reacciones adversas, urgencias o información sensible.

Debe activar con:

* “Tuve una reacción”;
* “Me ardió la piel”;
* “Se me inflamó”;
* “Tengo alergia”;
* “Me quemó”;
* “Tengo dolor fuerte”;
* “Me dio infección”;
* “Se me irritó mucho”.

Reglas:

* No diagnosticar.
* No recomendar tratamiento médico.
* No vender productos como solución.
* Derivar a humano.
* Si el mensaje sugiere urgencia, recomendar buscar atención profesional de salud.

Respuesta:
“Lamento que hayas tenido esa reacción. Te voy a derivar con una persona del equipo para ayudarte de inmediato. Si tienes molestias importantes o síntomas intensos, consulta con un profesional de salud.”

13. Agente de Ubicación y Sucursales
    Responsabilidad:
    Responder direcciones, horarios y ubicación de sucursales.

Debe activar con:

* “Dónde queda”;
* “Dirección”;
* “Ubicación”;
* “Cómo llego”;
* “Dónde están”;
* “Sucursal Providencia”.

Flujo:

1. Detectar sucursal si aparece.
2. Buscar dirección registrada.
3. Buscar enlace de mapa si existe.
4. Si no hay dirección, informar que falta configuración.
5. Si no hay sucursal específica y existen varias, listar sucursales.

Respuesta con dirección:
“La sucursal {sucursal} está ubicada en:
{direccion}

Puedes verla aquí:
{enlace_mapa}”

Respuesta sin dirección cargada:
“Tengo registrada la sucursal {sucursal}, pero falta configurar su dirección o enlace de mapa. Te derivaré con una persona del equipo para confirmarlo.”

14. Agente de Pagos y Señal
    Responsabilidad:
    Responder consultas sobre pago, abono, señal o reserva pagada.

Debe activar con:

* “Tengo que pagar señal”;
* “Hay que abonar”;
* “Cuánto se paga para reservar”;
* “Pago online”;
* “Link de pago”;
* “Pagar reserva”;
* “Seña”.

Reglas:

* No inventar montos.
* Usar reglas de pago configuradas.
* Si no hay monto configurado, indicar que se debe confirmar con el equipo.
* Si existe enlace de pago, entregarlo.
* Si requiere humano, derivar.

Respuesta:
“Para responder sobre señal o pago debo revisar la regla configurada del servicio o reserva. No voy a inventar montos. ¿Qué servicio quieres reservar?”

Si el servicio ya está identificado:
“Revisaré si {servicio} requiere señal según las reglas del negocio.”

15. Agente de Auditoría y Calidad
    Responsabilidad:
    Registrar cada análisis de IA y permitir evaluar si la respuesta fue coherente.

Debe registrar:

* mensaje original;
* intención detectada;
* confianza;
* entidades extraídas;
* agente seleccionado;
* regla aplicada;
* respuesta generada;
* si requiere humano;
* si la respuesta fue automática o sugerida;
* motivo de derivación;
* fecha/hora.

También debe corregir o actualizar el script de pruebas para evitar falsos positivos.

Criterios mínimos:

* Si la IA responde “No entendí” ante reenvío de enlace, marcar MAL.
* Si la IA responde “No entendí” ante enlace expirado, marcar MAL.
* Si la IA ignora sucursal explícita, marcar MAL.
* Si “sábado en la mañana” se detecta como “mañana”, marcar MAL.
* Si el cliente pide humano y no deriva, marcar MAL.
* Si hay caso sensible y no deriva, marcar MAL.
* Si cancelación o reprogramación se clasifican como reservar_hora, marcar MAL.

Correcciones técnicas obligatorias:

A. Backend
Revisar y modificar:

* clasificador de intención;
* extractor de entidades;
* orquestador de agenda;
* servicio de respuesta IA;
* reglas de negocio;
* repositorios de sucursales;
* servicios de enlaces;
* auditoría IA.

B. Base de datos
Agregar migración nueva, por ejemplo:
V23__complete_specialized_ai_agents_for_business.sql

La migración debe:

* insertar o actualizar reglas AI_PROMPT;
* insertar o actualizar reglas AI_RESPONSE;
* insertar alias faltantes;
* insertar tipos de intención faltantes si existe tabla;
* insertar reglas de derivación;
* insertar reglas de reprogramación;
* insertar reglas de cancelación;
* insertar reglas de ubicación;
* insertar reglas de pago/señal;
* insertar reglas de seguridad;
* evitar duplicados con ON CONFLICT.

C. Frontend
Revisar:

* pantalla IA del negocio;
* filtros de reglas;
* prompt operativo;
* visualización de respuestas;
* auditoría;
* ejecución de test;
* codificación UTF-8 en reportes.

D. Script de pruebas
Actualizar el script para:

* generar reporte CSV, JSON y HTML;
* corregir codificación UTF-8;
* eliminar dependencia de emojis;
* validar intención esperada;
* validar entidades esperadas;
* validar textos esperados;
* validar textos prohibidos;
* marcar MAL reenvío/enlace expirado si responde “No entendí”;
* marcar MAL si no respeta sucursal explícita;
* marcar MAL si una intención sensible no deriva.

Casos mínimos que deben pasar:

A01:
Cliente: “Hola, quiero reservar limpieza facial para el viernes”
Esperado:

* intención = reservar_hora;
* servicio = Limpieza facial profunda;
* fecha = viernes;
* no pedir servicio nuevamente;
* preguntar sucursal si falta.

A03:
Cliente: “Necesito una hora para depilación axilas el sábado en la mañana”
Esperado:

* intención = reservar_hora;
* servicio = Depilación axilas;
* fecha = sábado;
* preferencia_horaria = mañana;
* no confundir con mañana como día siguiente.

A04:
Cliente: “Me gustaría una limpieza de rostro esta semana”
Esperado:

* intención = reservar_hora o consultar_disponibilidad;
* servicio = Limpieza facial profunda;
* fecha/preferencia = esta semana;
* pedir día específico o sucursal según falte.

C01:
Cliente: “Quiero reservar depilación bozo mañana a las 14 horas en Providencia”
Esperado:

* intención = reservar_hora;
* servicio = Depilación bozo;
* fecha = mañana;
* hora = 14:00;
* sucursal = Providencia;
* no usar sede principal;
* pasar a validación de agenda.

E01:
Cliente: “No me llegó el link de confirmación, me lo puedes reenviar?”
Esperado:

* intención = reenviar_enlace_confirmacion;
* no responder “No entendí”;
* buscar enlace vigente o pedir identificar reserva.

E02:
Cliente: “El enlace expiró”
Esperado:

* intención = enlace_expirado;
* explicar que el cupo pudo liberarse;
* ofrecer buscar nuevo horario.

F01:
Cliente: “Quiero cambiar mi hora”
Esperado:

* intención = reprogramar_reserva;
* pedir nueva fecha u horario;
* no preguntar servicio como reserva nueva.

F02:
Cliente: “Necesito cancelar mi cita de mañana”
Esperado:

* intención = cancelar_reserva;
* pedir dato para identificar reserva o iniciar cancelación;
* no clasificar como reservar_hora.

G01:
Cliente: “Quiero hablar con una persona”
Esperado:

* intención = solicitar_humano;
* derivar a humano.

G02:
Cliente: “Tuve una reacción en la piel después del tratamiento”
Esperado:

* intención = caso_sensible_post_tratamiento;
* derivar a humano;
* no diagnosticar;
* no recomendar producto como solución.

H01:
Cliente: “Dónde queda la sucursal Providencia?”
Esperado:

* intención = consultar_ubicacion;
* sucursal = Providencia;
* responder dirección o indicar falta de configuración.

I01:
Cliente: “Tengo que pagar una señal para reservar?”
Esperado:

* intención = consultar_pago_senal;
* no clasificar como reservar_hora;
* responder según reglas de pago sin inventar monto.

Entregables requeridos:

1. ZIP actualizado completo.
2. Patch o diff unificado.
3. Migración SQL nueva.
4. Informe técnico de cambios.
5. Script de test actualizado.
6. Reporte de reglas y prompts incorporados.
7. Lista de archivos modificados.
8. Instrucciones de ejecución local.

Restricciones:

* No inventar funcionalidades que no existan.
* No eliminar funcionalidades existentes.
* No romper migraciones previas.
* No guardar secretos.
* No incluir sesión real de WhatsApp.
* No afirmar compilación exitosa si no se ejecutó.
* Mantener compatibilidad con Docker Compose local.
* Usar nombres consistentes y normalizados.
* Toda regla nueva debe tener evidencia funcional o estar marcada como regla requerida por el flujo objetivo.

Resultado esperado final:
La aplicación debe comportarse como un asistente especializado de negocio para centro estético, capaz de conversar por WhatsApp, identificar intención real, extraer datos, coordinar agentes especializados, responder coherentemente, validar agenda, crear reservas temporales, enviar enlaces, gestionar reenvíos, manejar expiraciones, reprogramar, cancelar, derivar a humano, responder ubicación, responder pago/señal y registrar auditoría.
