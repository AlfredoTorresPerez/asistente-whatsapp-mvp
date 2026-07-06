# Fix 7 - Contexto conversacional y mensajes vacíos

Cambios incluidos:

- El adaptador `whatsapp-web-service` ya no convierte eventos vacíos en el texto "Mensaje recibido sin texto.".
- Los mensajes entrantes sin texto ni multimedia útil se ignoran y no reinician el flujo conversacional.
- La sugerencia IA ignora mensajes vacíos/placeholder al buscar el último mensaje útil.
- El detector de intención reconoce servicios de depilación, zonas como axilas/piernas/bikini/rostro y servicios estéticos como intención comercial.
- La extracción de entidades reconoce `Depilacion axilas`, `Depilacion rostro`, `Depilacion bozo`, etc.
- El coordinador multiagente reutiliza contexto previo de la conversación cuando el cliente responde solo con su nombre o con un dato parcial.
- La sugerencia IA consulta el catálogo aunque el cliente no use explícitamente la palabra "precio".
- Para `Depilacion axilas`, si hay más de una modalidad en catálogo, ofrece opciones reales sin inventar disponibilidad.

Ejemplos esperados:

Cliente: `Depilacion axilas`
Respuesta sugerida:
`Hola Contacto, para depilación de axilas tengo estas opciones en catálogo: Depilacion axilas $19.990; Depilacion laser axilas $24.990. ¿Cuál modalidad quieres revisar?`

Cliente: `Alfredo`
Respuesta sugerida:
`Gracias. ¿Qué necesitas resolver hoy?`

Cliente: mensaje vacío/evento sin body
Resultado:
No se crea respuesta automática comercial ni se reinicia el flujo.
