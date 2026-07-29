# Preguntas y consultas que puede realizar un cliente por WhatsApp

Documento derivado de la matriz `agenda_digital_whatsapp_casuisticas(2).xlsx`.

## Objetivo

Este catálogo reúne expresiones reales que un cliente puede escribir al asistente de un centro estético. Puede utilizarse como base para pruebas conversacionales, ejemplos de intenciones, extracción de datos, criterios de aceptación y validación del flujo de agenda.

> Importante: las preguntas son ejemplos de lenguaje del cliente. La respuesta y la acción final siempre deben validarse contra la configuración real del centro, sus servicios, sucursales, profesionales, disponibilidad, políticas y pagos.

## Resumen

- Secciones temáticas: **25**
- Consultas únicas: **460**
- Incluye frases formales, informales, abreviadas, ambiguas y con errores frecuentes.
- Considera reserva, disponibilidad, sucursales, profesionales, recursos, pagos, reprogramación, cancelación, lista de espera, restricciones y derivación humana.

## Criterios de uso

1. No asumir que una lista de opciones es cerrada; siempre permitir que el cliente escriba libremente.
2. Solicitar un dato principal por turno cuando falte información.
3. Conservar el contexto para no volver a preguntar datos ya entregados.
4. No inventar precios, disponibilidad, políticas, profesionales ni servicios.
5. Confirmar servicio, sucursal, fecha, hora, profesional y monto antes de ejecutar operaciones críticas.
6. Derivar a una persona los reclamos, reacciones adversas, pagos contradictorios y casos fuera de política.

## Índice temático

- 1. Inicio, orientación y ayuda general
- 2. Consulta de servicios y tratamientos
- 3. Recomendación de servicios
- 4. Precios, promociones y cotizaciones
- 5. Solicitud inicial de reserva
- 6. Selección de fecha y horario
- 7. Consulta de disponibilidad real
- 8. Sucursales, dirección y cobertura
- 9. Selección y consulta de profesionales
- 10. Reserva de recursos, cabinas y equipos
- 11. Confirmación de la reserva y estado
- 12. Pagos, abonos y comprobantes
- 13. Recordatorios y notificaciones
- 14. Reprogramación de citas
- 15. Cancelación de citas
- 16. Lista de espera y cupos liberados
- 17. Servicios múltiples, paquetes y reservas grupales
- 18. Evaluación previa, consentimiento y restricciones de edad
- 19. Preparación, requisitos y condiciones antes de asistir
- 20. Atrasos, inasistencias y bloqueos del cliente
- 21. Horarios de atención y días especiales
- 22. Cambios operativos del centro o de la profesional
- 23. Reclamos, problemas y situaciones sensibles
- 24. Frases breves, coloquiales y con errores frecuentes
- 25. Casos ambiguos que requieren aclaración

## 1. Inicio, orientación y ayuda general

**Intención esperada:** `ORIENTACIÓN_GENERAL / SOLICITUD_PERSONA`

**Trazabilidad en la matriz:** `CAP-001`, `CAP-017`

1. Hola, ¿me pueden ayudar?
2. Quisiera hacer una consulta.
3. ¿Qué cosas puedo hacer por este WhatsApp?
4. Necesito información del centro.
5. ¿Me puedes orientar?
6. No sé por dónde empezar.
7. Quiero hablar con alguien de recepción.
8. Necesito hablar con una persona.
9. ¿Me puede atender un ejecutivo?
10. Quiero que me llame alguien del centro.
11. ¿Pueden contactarme por teléfono?
12. Tengo una situación especial y necesito ayuda.

## 2. Consulta de servicios y tratamientos

**Intención esperada:** `INFORMACIÓN_SERVICIO / CONSULTA_COMERCIAL`

**Trazabilidad en la matriz:** `CAP-003`, `CAP-028`

13. ¿Qué servicios tienen disponibles?
14. ¿Qué tratamientos realizan?
15. ¿Qué servicios faciales ofrecen?
16. ¿Qué servicios corporales tienen?
17. ¿Tienen limpieza facial?
18. ¿Realizan depilación láser?
19. ¿Hacen masajes?
20. ¿Tienen manicure y pedicure?
21. ¿Qué incluye la limpieza facial profunda?
22. ¿Cuánto dura este tratamiento?
23. ¿En qué consiste el servicio?
24. ¿Este tratamiento es invasivo?
25. ¿Cuántas sesiones necesito?
26. ¿Qué resultados puedo esperar?
27. ¿Este servicio necesita evaluación previa?
28. ¿Puedo reservar directamente o primero necesito una evaluación?
29. ¿Qué requisitos tiene el tratamiento?
30. ¿Qué servicio está disponible en mi sucursal?
31. No encuentro el servicio que busco, ¿lo realizan?
32. ¿Tienen otro tratamiento parecido?
33. Quiero un servicio distinto a los que aparecen en la lista.
34. ¿Puedo escribir el nombre del tratamiento que necesito?

## 3. Recomendación de servicios

**Intención esperada:** `RECOMENDACIÓN_SERVICIO`

**Trazabilidad en la matriz:** `CAP-003`, `CAP-028`

35. No sé qué tratamiento necesito, ¿me pueden orientar?
36. ¿Qué me recomiendan para piel sensible?
37. ¿Qué tratamiento sirve para hidratar la piel?
38. ¿Qué me recomiendan para manchas?
39. ¿Qué servicio sirve para una limpieza profunda?
40. Quiero mejorar mi piel, pero no sé qué elegir.
41. Busco algo no invasivo.
42. Quiero un tratamiento para relajarme.
43. ¿Qué tratamiento me conviene antes de un evento?
44. ¿Cuál es la diferencia entre estos dos servicios?
45. ¿Qué servicio es mejor para mí?
46. ¿Necesito una evaluación para que me recomienden algo?
47. ¿Me pueden recomendar un tratamiento según mi necesidad?
48. ¿Puedo hablar con una profesional antes de reservar?

## 4. Precios, promociones y cotizaciones

**Intención esperada:** `CONSULTA_PRECIO / SOLICITUD_COTIZACIÓN`

**Trazabilidad en la matriz:** `CAP-003`, `CAP-034`, `Detalle-6`

49. ¿Cuánto cuesta la limpieza facial?
50. ¿Cuál es el precio del tratamiento?
51. ¿Cuánto vale una sesión?
52. ¿El precio cambia según la sucursal?
53. ¿El precio cambia según la profesional?
54. ¿Tienen promociones vigentes?
55. ¿Tienen descuentos por varias sesiones?
56. ¿Hay paquetes de tratamientos?
57. ¿Me pueden enviar una cotización?
58. ¿Cuánto cuesta el paquete completo?
59. ¿El valor incluye todos los insumos?
60. ¿El precio incluye la evaluación?
61. ¿Hay algún costo adicional?
62. ¿Cuánto debo pagar para reservar?
63. ¿El abono se descuenta del total?
64. ¿Puedo pagar el total el día de la atención?
65. ¿Tienen precio especial por primera atención?
66. ¿La promoción aplica en todas las sucursales?

## 5. Solicitud inicial de reserva

**Intención esperada:** `SOLICITUD_RESERVA`

**Trazabilidad en la matriz:** `CAP-007`, `PRE-RES-001`

67. Quiero reservar una hora.
68. Necesito agendar una hora.
69. Quiero pedir una cita.
70. ¿Me pueden dar una hora?
71. Necesito un cupo.
72. Quiero tomar una hora.
73. Quisiera reservar un tratamiento.
74. ¿Cómo puedo agendar?
75. ¿Puedo reservar por WhatsApp?
76. Quiero hacer una reserva.
77. Necesito una cita para esta semana.
78. Quiero agendar para mañana.
79. ¿Me pueden anotar para una atención?
80. Necesito atenderme lo antes posible.
81. Quiero reservar para otra persona.
82. Quiero agendarle una hora a mi hija.
83. Quiero reservar dos horas distintas.
84. Quiero reservar más de un servicio.
85. Quiero reservar para dos personas.
86. Quiero una hora, pero todavía no sé qué servicio elegir.

## 6. Selección de fecha y horario

**Intención esperada:** `SOLICITUD_RESERVA / CONSULTA_DISPONIBILIDAD`

**Trazabilidad en la matriz:** `CAP-006`, `CAP-025`, `CAP-026`, `MOT-001`, `MOT-002`

87. Quiero una hora para hoy.
88. Quiero reservar para mañana.
89. ¿Tienen horas para el viernes?
90. ¿Hay disponibilidad esta semana?
91. ¿Qué horas tienen para la próxima semana?
92. ¿Tienen algo en la mañana?
93. ¿Tienen horas en la tarde?
94. Necesito una hora después de las seis.
95. ¿Cuál es la primera hora disponible?
96. ¿Cuál es la última hora disponible?
97. ¿Tienen un cupo a las diez?
98. ¿Está disponible el horario de las quince horas?
99. ¿Hay horas el sábado?
100. ¿Atienden los domingos?
101. ¿Tienen disponibilidad en un feriado?
102. ¿Puedo reservar para el próximo mes?
103. ¿Con cuánta anticipación puedo reservar?
104. ¿Puedo reservar para una fecha muy lejana?
105. ¿Puedo reservar una hora para ayer?
106. ¿El horario que aparece considera la duración completa del servicio?
107. ¿Hay un horario más temprano?
108. ¿Hay un horario más tarde?
109. ¿Qué alternativa tienen cerca de las cuatro?
110. ¿Tienen dos horarios consecutivos?
111. ¿Puedo elegir un tramo horario en vez de una hora exacta?

## 7. Consulta de disponibilidad real

**Intención esperada:** `CONSULTA_DISPONIBILIDAD`

**Trazabilidad en la matriz:** `CAP-006`, `CAP-016`, `MOT-003`, `MOT-004`, `MOT-005`, `MOT-006`, `MOT-007`, `MOT-008`

112. ¿Tienen horas disponibles para una limpieza facial?
113. ¿Qué horarios quedan libres hoy?
114. ¿Hay algún cupo disponible mañana?
115. ¿Tienen una hora libre con cualquier profesional?
116. ¿Hay disponibilidad en cualquier sucursal?
117. ¿Qué sede tiene la hora más próxima?
118. ¿Hay una cabina disponible para ese tratamiento?
119. ¿Está disponible la máquina necesaria para el servicio?
120. ¿Puedo reservar si la profesional está libre, pero la cabina está ocupada?
121. ¿Queda capacidad para una atención grupal?
122. ¿El horario sigue disponible?
123. ¿Pueden verificar nuevamente el cupo?
124. ¿Por qué no aparece el horario de las cuatro?
125. ¿Hay alguna pausa o bloqueo a esa hora?
126. ¿Pueden ofrecerme horarios cercanos al que elegí?
127. ¿Qué horarios reales quedan después de considerar la duración del tratamiento?
128. ¿Existe un cupo si no elijo profesional?
129. ¿Hay disponibilidad con otro equipo o cabina?

## 8. Sucursales, dirección y cobertura

**Intención esperada:** `CONSULTA_UBICACIÓN / SOLICITUD_RESERVA`

**Trazabilidad en la matriz:** `CAP-004`, `CAP-014`, `Detalle-1`, `Detalle-2`, `Detalle-3`, `Detalle-8`

130. ¿Dónde están ubicados?
131. ¿Cuál es la dirección de la sucursal?
132. ¿Qué sucursales tienen?
133. ¿Cuál es la sucursal más cercana?
134. Estoy en Providencia, ¿qué sucursal me conviene?
135. ¿Tienen una sede en Las Condes?
136. ¿Este tratamiento está disponible en todas las sucursales?
137. ¿En qué sucursal realizan este servicio?
138. ¿Qué horarios tiene la sucursal de Providencia?
139. ¿La sucursal abre los sábados?
140. ¿La sede estará abierta el feriado?
141. ¿Tienen estacionamiento?
142. ¿Cómo llego a la sucursal?
143. ¿Tienen alguna referencia para encontrar el lugar?
144. ¿La atención es presencial o a domicilio?
145. ¿Puedo elegir cualquier sucursal?
146. ¿Puedo cambiar mi reserva a otra sucursal?
147. ¿El precio cambia si elijo otra sede?
148. ¿Las políticas de cancelación son iguales en todas las sucursales?
149. ¿Hay disponibilidad en otra sucursal si aquí está lleno?

## 9. Selección y consulta de profesionales

**Intención esperada:** `CONSULTA_PROFESIONAL / SOLICITUD_RESERVA`

**Trazabilidad en la matriz:** `CAP-005`, `CAP-015`, `PRO-001`, `PRO-003`, `PRO-004`, `PRO-005`, `PRO-013`, `PRO-014`

150. ¿Quién realiza este tratamiento?
151. ¿Qué profesionales tienen disponibles?
152. Quiero reservar con Carla.
153. ¿Carla atiende mañana?
154. ¿En qué sucursal atiende esa profesional?
155. ¿La profesional está habilitada para este servicio?
156. ¿Puedo elegir una profesional específica?
157. No tengo preferencia, ¿me asignan a alguien?
158. ¿Quién tiene la primera hora disponible?
159. ¿Pueden asignarme a la profesional con menos carga?
160. ¿Hay una profesional especialista en este tratamiento?
161. ¿Este servicio requiere una profesional de nivel avanzado?
162. ¿Puedo cambiar de profesional?
163. Si mi profesional no está disponible, ¿me pueden ofrecer otra?
164. No quiero que cambien a la profesional sin avisarme.
165. ¿La profesional debe confirmar mi cita?
166. ¿Mi cita todavía está esperando confirmación de la profesional?
167. ¿Quién me atenderá finalmente?
168. ¿La profesional estará en esa sucursal a esa hora?
169. ¿Tiene disponibilidad después de su colación?

## 10. Reserva de recursos, cabinas y equipos

**Intención esperada:** `SOLICITUD_RESERVA / CONSULTA_DISPONIBILIDAD`

**Trazabilidad en la matriz:** `CAP-016`, `Detalle-5`, `Detalle-9`, `MOT-007`, `MOT-008`

170. ¿El tratamiento necesita una cabina especial?
171. ¿Hay una cabina disponible a esa hora?
172. ¿La máquina está disponible en esa sucursal?
173. ¿En qué sede tienen el equipo para este tratamiento?
174. ¿Puedo reservar si el equipo está en mantención?
175. ¿Tienen otra máquina disponible?
176. ¿Cuántas personas pueden atender al mismo tiempo?
177. ¿Hay cupos para una atención grupal?
178. ¿El recurso queda reservado junto con mi cita?
179. ¿Pueden cambiarme de cabina sin cambiar la hora?
180. ¿Qué pasa si el equipo deja de estar disponible?
181. ¿Me pueden ofrecer otra sede con el mismo equipo?

## 11. Confirmación de la reserva y estado

**Intención esperada:** `ESTADO_RESERVA / CONFIRMACIÓN_RESERVA`

**Trazabilidad en la matriz:** `CAP-008`, `CAP-010`, `CAP-024`, `POST-RES-001`, `POST-RES-002`, `POST-RES-003`, `POST-RES-004`, `POST-RES-005`

182. ¿Mi hora quedó reservada?
183. ¿Mi reserva está confirmada?
184. ¿Cuál es el estado de mi cita?
185. ¿Me pueden enviar el resumen de mi reserva?
186. ¿Cuál es el código de mi reserva?
187. ¿Qué servicio tengo agendado?
188. ¿Para qué fecha tengo la hora?
189. ¿A qué sucursal debo ir?
190. ¿Quién me atenderá?
191. ¿Mi reserva está pendiente de pago?
192. ¿Mi reserva está pendiente de recepción?
193. ¿Tengo más de una reserva creada?
194. Creo que reservé dos veces, ¿pueden revisar?
195. ¿El horario quedó bloqueado mientras confirmo?
196. ¿Cuánto tiempo tengo para confirmar?
197. ¿Qué pasa si no confirmo dentro del plazo?
198. No me llegó el enlace de confirmación.
199. El enlace de confirmación venció.
200. El enlace de confirmación no funciona.
201. ¿Me pueden reenviar el enlace?
202. Quiero confirmar mi hora.
203. ¿Puedo responder por WhatsApp para confirmar?
204. ¿La cita ya aparece en la agenda?
205. ¿Me pueden agregar la cita a mi calendario?

## 12. Pagos, abonos y comprobantes

**Intención esperada:** `CONSULTA_PAGO / PROBLEMA_PAGO`

**Trazabilidad en la matriz:** `CAP-009`, `CAP-033`, `CAP-034`, `CAP-035`

206. ¿Necesito pagar para reservar?
207. ¿Este servicio requiere abono?
208. ¿Cuánto tengo que abonar?
209. ¿El abono confirma inmediatamente la cita?
210. ¿Qué medios de pago aceptan?
211. ¿Me pueden enviar el enlace de pago?
212. No me llegó el enlace de pago.
213. El enlace de pago venció.
214. El enlace de pago no funciona.
215. Ya pagué, ¿pueden confirmar mi reserva?
216. ¿Recibieron mi pago?
217. El pago aparece pendiente.
218. Mi pago fue rechazado.
219. Me cobraron dos veces.
220. El monto cobrado no corresponde.
221. ¿Puedo enviar el comprobante por WhatsApp?
222. ¿Me pueden enviar una boleta?
223. ¿Me pueden enviar una factura?
224. ¿El abono es reembolsable?
225. ¿Qué pasa con mi abono si cancelo?
226. ¿Qué pasa con mi abono si reprogramo?
227. ¿Puedo usar el abono para otra cita?
228. ¿Puedo pagar la diferencia si cambio de servicio?
229. ¿Me corresponde una devolución?
230. ¿Cuánto demora el reembolso?

## 13. Recordatorios y notificaciones

**Intención esperada:** `ESTADO_RESERVA / CONSULTA_NOTIFICACIÓN`

**Trazabilidad en la matriz:** `CAP-010`, `CAP-011`

231. ¿Me enviarán una confirmación por WhatsApp?
232. ¿Me enviarán la confirmación por correo?
233. ¿Me pueden recordar la cita?
234. ¿Cuándo enviarán el recordatorio?
235. ¿Pueden enviarme un recordatorio el día anterior?
236. ¿Pueden recordarme la cita unas horas antes?
237. No me llegó el recordatorio.
238. ¿A qué número enviarán las notificaciones?
239. ¿Pueden enviar la información a otro correo?
240. ¿Puedo dejar de recibir recordatorios?
241. ¿El recordatorio incluye la dirección?
242. ¿El recordatorio incluye un enlace para cancelar o reprogramar?
243. ¿También le avisarán a la profesional?
244. ¿Me avisarán si cambia la profesional?
245. ¿Me avisarán si cambia la sucursal?
246. ¿Me avisarán si el centro cancela la cita?

## 14. Reprogramación de citas

**Intención esperada:** `REPROGRAMACIÓN_RESERVA`

**Trazabilidad en la matriz:** `CAP-012`, `REP-001`, `REP-002`, `REP-003`, `REP-004`

247. Quiero cambiar mi hora.
248. Necesito reprogramar mi cita.
249. ¿Puedo cambiar la fecha?
250. ¿Puedo cambiar el horario?
251. ¿Puedo mover mi cita para mañana?
252. Quiero cambiar mi reserva para la próxima semana.
253. ¿Puedo cambiar de sucursal?
254. ¿Puedo cambiar de profesional al reprogramar?
255. ¿Puedo cambiar el servicio de mi reserva?
256. No encuentro mi reserva para reprogramarla.
257. No tengo el código de la reserva.
258. ¿Pueden buscar mi cita por teléfono?
259. ¿Esta reserva permite reprogramación?
260. Mi cita está pendiente de pago, ¿la puedo mover?
261. ¿Hasta cuándo puedo reprogramar?
262. ¿Existe un cobro por reprogramar?
263. ¿Cuántas veces puedo cambiar mi cita?
264. ¿Puedo reprogramar una cita vencida?
265. ¿Puedo reprogramar una cita cancelada?
266. ¿Puedo reprogramar después de no asistir?
267. ¿Qué horarios tienen para reemplazar mi cita?
268. ¿Pueden mantener mi hora actual mientras elijo una nueva?
269. ¿Qué pasa si el nuevo horario se ocupa antes de confirmar?
270. ¿Me enviarán una nueva confirmación?
271. ¿La reserva anterior quedará anulada?
272. ¿Qué pasa con mi pago después del cambio?
273. ¿El precio cambia al elegir otra sucursal?
274. ¿El precio cambia al elegir otra profesional?
275. Quiero volver a mi horario anterior.
276. Me equivoqué al reprogramar, ¿pueden ayudarme?

## 15. Cancelación de citas

**Intención esperada:** `CANCELACIÓN_RESERVA`

**Trazabilidad en la matriz:** `CAP-013`, `CAN-001`, `CAN-002`, `CAN-003`, `CAN-004`, `CAN-005`

277. Quiero cancelar mi hora.
278. Necesito anular mi reserva.
279. ¿Cómo cancelo mi cita?
280. No puedo asistir, quiero cancelar.
281. No encuentro mi reserva para cancelarla.
282. No tengo el código de la cita.
283. ¿Pueden buscar la reserva por mi teléfono?
284. ¿Esta reserva todavía se puede cancelar?
285. ¿Puedo cancelar una cita pendiente de pago?
286. ¿Puedo cancelar una cita ya confirmada?
287. ¿Puedo cancelar una reserva de otra persona?
288. ¿Necesitan validar que soy el titular?
289. ¿Hasta cuándo puedo cancelar sin cobro?
290. ¿Hay una penalización por cancelar tarde?
291. ¿Debo indicar el motivo de la cancelación?
292. ¿Qué pasa con el cupo después de cancelar?
293. ¿Le avisarán a la profesional?
294. ¿Me enviarán una confirmación de cancelación?
295. ¿Qué pasa con el abono?
296. ¿Me devolverán el dinero?
297. ¿Puedo dejar el pago como saldo a favor?
298. ¿Puedo cancelar solo uno de los servicios del paquete?
299. Cancelé por error, ¿pueden recuperar mi cita?
300. Mi reserva ya aparece cancelada, ¿pueden revisar?
301. El centro canceló mi hora, ¿qué alternativas tengo?

## 16. Lista de espera y cupos liberados

**Intención esperada:** `SOLICITUD_LISTA_ESPERA`

**Trazabilidad en la matriz:** `CAP-023`, `MOT-014`

302. No hay horas, ¿me pueden dejar en lista de espera?
303. ¿Tienen lista de espera?
304. ¿Me pueden avisar si se libera un cupo?
305. Quiero quedar esperando una hora para mañana.
306. ¿Puedo elegir varios horarios en la lista de espera?
307. ¿Puedo quedar en espera para cualquier profesional?
308. ¿Puedo quedar en espera en varias sucursales?
309. ¿Qué posición tengo en la lista de espera?
310. ¿Cuánto tiempo tengo para aceptar un cupo liberado?
311. Me avisaron de un cupo, ¿todavía está disponible?
312. Quiero aceptar el cupo que se liberó.
313. No puedo tomar el cupo, ¿pueden ofrecérselo a otra persona?
314. Quiero salir de la lista de espera.
315. ¿Me pueden avisar solo por WhatsApp?
316. ¿Me avisarán a todos al mismo tiempo o por orden?

## 17. Servicios múltiples, paquetes y reservas grupales

**Intención esperada:** `SOLICITUD_RESERVA`

**Trazabilidad en la matriz:** `CAP-027`, `MOT-008`

317. Quiero reservar dos servicios el mismo día.
318. ¿Puedo hacerme una limpieza y después un masaje?
319. ¿Pueden agendar los servicios uno después del otro?
320. ¿Cuánto tiempo necesito para el paquete completo?
321. ¿Los servicios pueden ser con profesionales diferentes?
322. ¿Los servicios deben realizarse en la misma sucursal?
323. ¿Puedo cambiar solo uno de los servicios del paquete?
324. ¿Puedo cancelar solo una parte del paquete?
325. Quiero reservar varias sesiones del tratamiento.
326. ¿Puedo dejar agendadas todas mis sesiones?
327. Quiero reservar para mí y otra persona.
328. ¿Pueden atendernos a las dos a la misma hora?
329. ¿Tienen capacidad para un grupo?
330. ¿Puedo reservar una atención grupal?
331. ¿El pago se realiza por persona o por la reserva completa?

## 18. Evaluación previa, consentimiento y restricciones de edad

**Intención esperada:** `INFORMACIÓN_SERVICIO / SOLICITUD_RESERVA / SOLICITUD_PERSONA`

**Trazabilidad en la matriz:** `CAP-028`, `CAP-029`, `CAP-030`, `PRE-RES-005`

332. ¿Este tratamiento necesita evaluación previa?
333. ¿Puedo reservar la evaluación por WhatsApp?
334. ¿La evaluación tiene costo?
335. ¿Puedo reservar el tratamiento el mismo día de la evaluación?
336. ¿Necesito firmar un consentimiento?
337. ¿Me pueden enviar el consentimiento antes de la cita?
338. ¿Puedo aceptar el consentimiento por WhatsApp?
339. ¿Qué información incluye el consentimiento?
340. ¿Existe una edad mínima para este servicio?
341. Soy menor de edad, ¿puedo reservar?
342. Quiero reservar para mi hija menor de edad.
343. ¿Debe asistir un adulto responsable?
344. ¿Necesitan autorización del tutor?
345. ¿Qué datos del tutor necesitan?
346. ¿Hay tratamientos que no se realizan a menores?
347. ¿Puedo hablar con recepción por una restricción especial?

## 19. Preparación, requisitos y condiciones antes de asistir

**Intención esperada:** `INFORMACIÓN_SERVICIO`

**Trazabilidad en la matriz:** `CAP-003`, `CAP-028`, `CAP-029`

348. ¿Cómo debo prepararme para la cita?
349. ¿Debo hacer algo antes del tratamiento?
350. ¿Tengo que llegar sin maquillaje?
351. ¿Debo suspender algún producto antes de la sesión?
352. ¿Necesito llevar algún documento?
353. ¿Debo llevar el comprobante de pago?
354. ¿Cuánto antes debo llegar?
355. ¿Puedo ir acompañada?
356. ¿Qué pasa si estoy embarazada?
357. Tengo una condición médica, ¿puedo realizarme el tratamiento?
358. Tuve una reacción anterior, ¿puedo reservar nuevamente?
359. ¿Necesito informar alergias?
360. ¿Hay contraindicaciones para este servicio?
361. ¿Puedo comer antes de la atención?
362. ¿Qué ropa conviene llevar?

## 20. Atrasos, inasistencias y bloqueos del cliente

**Intención esperada:** `ESTADO_RESERVA / SOLICITUD_PERSONA`

**Trazabilidad en la matriz:** `CAP-021`, `CAP-031`, `CAP-032`

363. Voy atrasada, ¿todavía me pueden atender?
364. ¿Cuántos minutos de atraso permiten?
365. ¿Qué pasa si llego tarde?
366. No pude asistir a mi cita, ¿qué debo hacer?
367. ¿Puedo reprogramar después de una inasistencia?
368. ¿Existe una penalización por no asistir?
369. ¿Perdí el abono por no presentarme?
370. ¿Cuántas inasistencias tengo registradas?
371. El sistema no me deja reservar, ¿por qué?
372. Me informaron que estoy bloqueada, ¿qué significa?
373. ¿Cómo puedo regularizar una deuda pendiente?
374. ¿Puedo hablar con recepción para desbloquear mi cuenta?
375. Creo que el bloqueo es un error.
376. ¿Puedo reservar si tengo una deuda?
377. ¿Qué debo hacer para volver a agendar?

## 21. Horarios de atención y días especiales

**Intención esperada:** `CONSULTA_HORARIO_ATENCIÓN`

**Trazabilidad en la matriz:** `CAP-014`, `CAP-022`, `Detalle-1`, `Detalle-2`, `MOT-004`, `MOT-005`

378. ¿A qué hora abren?
379. ¿Hasta qué hora atienden?
380. ¿Cuál es el horario de atención?
381. ¿Atienden a la hora de almuerzo?
382. ¿Abren los sábados?
383. ¿Trabajan los domingos?
384. ¿Atienden en días feriados?
385. ¿Hoy tienen horario normal?
386. ¿Mañana abren más temprano?
387. ¿Tienen horario extendido esta semana?
388. ¿La sucursal cerrará más temprano?
389. ¿Hay una apertura extraordinaria?
390. ¿El horario es igual en todas las sucursales?
391. ¿La profesional tiene el mismo horario que la sucursal?
392. ¿Hay atención después de las siete?

## 22. Cambios operativos del centro o de la profesional

**Intención esperada:** `ESTADO_RESERVA / SOLICITUD_PERSONA`

**Trazabilidad en la matriz:** `PRO-006`, `PRO-009`, `PRO-012`, `PRO-019`, `PRO-020`

393. ¿Qué pasa si la profesional se ausenta?
394. ¿Me pueden asignar otra profesional?
395. ¿Puedo aceptar o rechazar el cambio de profesional?
396. ¿Puedo cambiar la cita si no está mi profesional?
397. ¿Qué pasa si la profesional cambia de sucursal?
398. ¿Consideran el tiempo de traslado entre sucursales?
399. ¿Mi cita puede atrasarse por el traslado de la profesional?
400. ¿Me avisarán si la profesional está con licencia?
401. ¿Me avisarán si hay una capacitación o reunión?
402. ¿Qué pasa si la cabina o el equipo entra en mantención?
403. ¿Me ofrecerán otra hora si el centro cambia mi cita?
404. ¿Puedo elegir entre reprogramar o cancelar?
405. ¿Qué pasa con mi pago si el cambio lo hace el centro?
406. ¿Mi cita está pendiente de aceptación de la profesional?

## 23. Reclamos, problemas y situaciones sensibles

**Intención esperada:** `RECLAMO / SOLICITUD_PERSONA / PROBLEMA_PAGO`

**Trazabilidad en la matriz:** `CAP-017`

407. Quiero hacer un reclamo.
408. Tuve un problema con mi atención.
409. No quedé conforme con el servicio.
410. Necesito hablar con un supervisor.
411. Nadie responde mi solicitud.
412. Me cobraron un monto incorrecto.
413. Tengo un problema con una devolución.
414. Me hicieron un cobro duplicado.
415. Tuve una reacción después del tratamiento.
416. Tengo irritación después de la sesión.
417. Siento dolor fuerte después del tratamiento.
418. Tuve una quemadura y necesito ayuda.
419. Creo que hubo un error en mi reserva.
420. Mi reserva desapareció de la agenda.
421. Llegué a la sucursal y no tenían registrada mi cita.
422. Quiero que una persona revise mi caso.

## 24. Frases breves, coloquiales y con errores frecuentes

**Intención esperada:** `VARIAS INTENCIONES`

**Trazabilidad en la matriz:** `CAP-001`, `CAP-006`, `CAP-007`, `CAP-012`, `CAP-013`

423. Hola, una horita porfa.
424. Quiero agendarme.
425. Necesito hora pa mañana.
426. ¿Tienen cupito hoy?
427. Quiero reservar altiro.
428. ¿Hay algo tipo cinco?
429. ¿Qué tienen libre en la tarde?
430. Agéndame con Carla.
431. Ajendar hora para mañana.
432. Quiero reserbar una cita.
433. Necesito una hroa.
434. Cambiarme la hora.
435. Mover mi cita porfa.
436. Anular hora.
437. No voy a poder ir.
438. No me llegó el enlace.
439. El enlace expiró.
440. Ya pagué.
441. ¿Dónde queda?
442. Quiero hablar con alguien.

## 25. Casos ambiguos que requieren aclaración

**Intención esperada:** `AMBIGUA / REQUIERE_ACLARACIÓN`

**Trazabilidad en la matriz:** `CAP-001`, `CAP-017`, `CAP-024`

443. Quiero ver lo de mañana.
444. Necesito cambiar eso.
445. No puedo ir.
446. Quiero otra opción.
447. ¿Está listo?
448. ¿Todavía sirve?
449. La misma de la otra vez.
450. Quiero con ella.
451. A la misma hora.
452. En la otra sucursal.
453. Quiero el tratamiento anterior.
454. No quiero ese.
455. Quiero confirmar.
456. Quiero cancelarlo.
457. ¿Cuánto sale?
458. ¿Tienen para después?
459. Quiero reservar, pero no ahora.
460. No quiero cancelar, solo necesito información.

## Datos que el asistente debería extraer cuando correspondan

- Nombre del cliente.
- Número de WhatsApp normalizado.
- Servicio o tratamiento.
- Categoría del servicio.
- Sucursal.
- Fecha exacta o expresión relativa, por ejemplo, “mañana”.
- Hora exacta o tramo horario, por ejemplo, “en la tarde”.
- Profesional solicitado o asignación automática.
- Recurso requerido: cabina, sala, equipo o máquina.
- Código o identificador de reserva.
- Motivo de reprogramación o cancelación cuando la política lo exija.
- Medio de pago, estado del pago y comprobante.
- Tutor o adulto responsable cuando corresponda.
- Consentimiento informado cuando el servicio lo requiera.

## Resultado esperado del motor conversacional

Para cada consulta, el asistente debería producir como mínimo:

1. Intención principal y, cuando corresponda, intención secundaria.
2. Datos detectados y datos faltantes.
3. Estado conversacional actual y siguiente estado.
4. Regla de negocio que debe validarse.
5. Acción segura permitida.
6. Respuesta breve y contextual para WhatsApp.
7. Indicador de derivación humana cuando exista riesgo, ambigüedad o excepción.

## Fuente

- Archivo: `agenda_digital_whatsapp_casuisticas(2).xlsx`.
- Hojas utilizadas: capacidades, prevalidaciones, postvalidaciones, reprogramación, cancelación, múltiples sucursales, profesionales, motor de disponibilidad, pruebas y datos mínimos.
