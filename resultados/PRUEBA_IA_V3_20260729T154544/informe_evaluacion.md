# Evaluación semántica v2

## Resumen

| Métrica | Valor |
|---|---|
| Total evaluados | 460 |
| Óptimas (>=85) | 79 (17%) |
| Aceptables (65-84) | 182 (40%) |
| Deficientes (<65) | 182 (40%) |
| Error crítico | 17 (4%) |
| Excluidas | 0 |
| Puntaje promedio | 64/100 |

## Errores críticos

- **P015**: "¿Qué servicios faciales ofrecen?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P017**: "¿Tienen limpieza facial?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P018**: "¿Realizan depilación láser?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P020**: "¿Tienen manicure y pedicure?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P021**: "¿Qué incluye la limpieza facial profunda?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P027**: "¿Este servicio necesita evaluación previa?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P059**: "¿El valor incluye todos los insumos?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P081**: "Quiero reservar para otra persona...." → ERROR_CRITICO: no deriva solicitud humana a persona
- **P085**: "Quiero reservar para dos personas...." → ERROR_CRITICO: no deriva solicitud humana a persona
- **P176**: "¿Cuántas personas pueden atender al mismo tiempo?..." → ERROR_CRITICO: no deriva solicitud humana a persona
- **P236**: "¿Pueden recordarme la cita unas horas antes?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P327**: "Quiero reservar para mí y otra persona...." → ERROR_CRITICO: no deriva solicitud humana a persona
- **P331**: "¿El pago se realiza por persona o por la reserva c..." → ERROR_CRITICO: no deriva solicitud humana a persona
- **P332**: "¿Este tratamiento necesita evaluación previa?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P350**: "¿Tengo que llegar sin maquillaje?..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P403**: "¿Me ofrecerán otra hora si el centro cambia mi cit..." → ERROR_CRITICO: posible invención de precio no solicitado
- **P457**: "¿Cuánto sale?..." → ERROR_CRITICO: posible invención de precio no solicitado

## Casos deficientes

- **P002** (36/100): "Quisiera hacer una consulta...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P003** (36/100): "¿Qué cosas puedo hacer por este WhatsApp?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P004** (34/100): "Necesito información del centro...." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P005** (36/100): "¿Me puedes orientar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P006** (36/100): "No sé por dónde empezar...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P010** (59/100): "Quiero que me llame alguien del centro...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P011** (59/100): "¿Pueden contactarme por teléfono?..." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P013** (34/100): "¿Qué servicios tienen disponibles?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P014** (34/100): "¿Qué tratamientos realizan?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P016** (34/100): "¿Qué servicios corporales tienen?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P019** (36/100): "¿Hacen masajes?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P022** (34/100): "¿Cuánto dura este tratamiento?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P023** (36/100): "¿En qué consiste el servicio?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P024** (34/100): "¿Este tratamiento es invasivo?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P025** (36/100): "¿Cuántas sesiones necesito?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P026** (36/100): "¿Qué resultados puedo esperar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P029** (36/100): "¿Qué requisitos tiene el tratamiento?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P031** (36/100): "No encuentro el servicio que busco, ¿lo realizan?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P032** (36/100): "¿Tienen otro tratamiento parecido?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P033** (36/100): "Quiero un servicio distinto a los que aparecen en ..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P034** (36/100): "¿Puedo escribir el nombre del tratamiento que nece..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P035** (36/100): "No sé qué tratamiento necesito, ¿me pueden orienta..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P040** (36/100): "Quiero mejorar mi piel, pero no sé qué elegir...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P043** (36/100): "¿Qué tratamiento me conviene antes de un evento?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P044** (36/100): "¿Cuál es la diferencia entre estos dos servicios?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P045** (36/100): "¿Qué servicio es mejor para mí?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P054** (36/100): "¿Tienen promociones vigentes?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P055** (36/100): "¿Tienen descuentos por varias sesiones?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P056** (36/100): "¿Hay paquetes de tratamientos?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P061** (36/100): "¿Hay algún costo adicional?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P063** (36/100): "¿El abono se descuenta del total?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P069** (36/100): "Quiero pedir una cita...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P070** (36/100): "¿Me pueden dar una hora?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P072** (36/100): "Quiero tomar una hora...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P080** (36/100): "Necesito atenderme lo antes posible...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P092** (36/100): "¿Tienen algo en la mañana?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P098** (36/100): "¿Está disponible el horario de las quince horas?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P100** (36/100): "¿Atienden los domingos?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P106** (36/100): "¿El horario que aparece considera la duración comp..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P109** (36/100): "¿Qué alternativa tienen cerca de las cuatro?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P118** (36/100): "¿Hay una cabina disponible para ese tratamiento?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P119** (36/100): "¿Está disponible la máquina necesaria para el serv..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P121** (36/100): "¿Queda capacidad para una atención grupal?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P122** (36/100): "¿El horario sigue disponible?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P123** (36/100): "¿Pueden verificar nuevamente el cupo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P124** (59/100): "¿Por qué no aparece el horario de las cuatro?..." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P125** (36/100): "¿Hay alguna pausa o bloqueo a esa hora?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P130** (36/100): "¿Dónde están ubicados?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P134** (57/100): "Estoy en Providencia, ¿qué sucursal me conviene?..." → "La sucursal Providencia está ubicada en:
Av. Providencia 245..."
- **P135** (55/100): "¿Tienen una sede en Las Condes?..." → "La sucursal Las Condes está ubicada en:
Av. Apoquindo 4800, ..."
- **P138** (57/100): "¿Qué horarios tiene la sucursal de Providencia?..." → "La sucursal Providencia está ubicada en:
Av. Providencia 245..."
- **P141** (36/100): "¿Tienen estacionamiento?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P143** (36/100): "¿Tienen alguna referencia para encontrar el lugar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P144** (36/100): "¿La atención es presencial o a domicilio?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P157** (36/100): "No tengo preferencia, ¿me asignan a alguien?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P167** (36/100): "¿Quién me atenderá finalmente?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P170** (36/100): "¿El tratamiento necesita una cabina especial?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P171** (36/100): "¿Hay una cabina disponible a esa hora?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P175** (36/100): "¿Tienen otra máquina disponible?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P177** (36/100): "¿Hay cupos para una atención grupal?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P178** (36/100): "¿El recurso queda reservado junto con mi cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P179** (36/100): "¿Pueden cambiarme de cabina sin cambiar la hora?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P180** (36/100): "¿Qué pasa si el equipo deja de estar disponible?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P182** (36/100): "¿Mi hora quedó reservada?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P184** (36/100): "¿Cuál es el estado de mi cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P188** (36/100): "¿Para qué fecha tengo la hora?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P190** (36/100): "¿Quién me atenderá?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P193** (36/100): "¿Tengo más de una reserva creada?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P194** (36/100): "Creo que reservé dos veces, ¿pueden revisar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P196** (36/100): "¿Cuánto tiempo tengo para confirmar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P197** (36/100): "¿Qué pasa si no confirmo dentro del plazo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P198** (59/100): "No me llegó el enlace de confirmación...." → "🔁 *Reenvío de enlace de confirmación*

Revisaré si tienes u..."
- **P199** (36/100): "El enlace de confirmación venció...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P201** (59/100): "¿Me pueden reenviar el enlace?..." → "🔁 *Reenvío de enlace de confirmación*

Revisaré si tienes u..."
- **P203** (36/100): "¿Puedo responder por WhatsApp para confirmar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P204** (36/100): "¿La cita ya aparece en la agenda?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P205** (36/100): "¿Me pueden agregar la cita a mi calendario?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P207** (36/100): "¿Este servicio requiere abono?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P208** (36/100): "¿Cuánto tengo que abonar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P209** (36/100): "¿El abono confirma inmediatamente la cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P212** (59/100): "No me llegó el enlace de pago...." → "🔁 *Reenvío de enlace de confirmación*

Revisaré si tienes u..."
- **P219** (59/100): "Me cobraron dos veces...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P220** (36/100): "El monto cobrado no corresponde...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P224** (36/100): "¿El abono es reembolsable?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P226** (36/100): "¿Qué pasa con mi abono si reprogramo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P227** (36/100): "¿Puedo usar el abono para otra cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P229** (59/100): "¿Me corresponde una devolución?..." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P230** (59/100): "¿Cuánto demora el reembolso?..." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P231** (36/100): "¿Me enviarán una confirmación por WhatsApp?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P232** (36/100): "¿Me enviarán la confirmación por correo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P233** (36/100): "¿Me pueden recordar la cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P238** (36/100): "¿A qué número enviarán las notificaciones?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P239** (34/100): "¿Pueden enviar la información a otro correo?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P246** (36/100): "¿Me avisarán si el centro cancela la cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P249** (36/100): "¿Puedo cambiar la fecha?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P257** (36/100): "No tengo el código de la reserva...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P258** (36/100): "¿Pueden buscar mi cita por teléfono?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P263** (36/100): "¿Cuántas veces puedo cambiar mi cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P268** (36/100): "¿Pueden mantener mi hora actual mientras elijo una..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P270** (36/100): "¿Me enviarán una nueva confirmación?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P279** (36/100): "¿Cómo cancelo mi cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P282** (36/100): "No tengo el código de la cita...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P283** (36/100): "¿Pueden buscar la reserva por mi teléfono?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P290** (36/100): "¿Hay una penalización por cancelar tarde?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P295** (36/100): "¿Qué pasa con el abono?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P296** (36/100): "¿Me devolverán el dinero?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P303** (36/100): "¿Tienen lista de espera?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P309** (36/100): "¿Qué posición tengo en la lista de espera?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P312** (36/100): "Quiero aceptar el cupo que se liberó...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P314** (36/100): "Quiero salir de la lista de espera...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P315** (36/100): "¿Me pueden avisar solo por WhatsApp?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P320** (36/100): "¿Cuánto tiempo necesito para el paquete completo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P323** (36/100): "¿Puedo cambiar solo uno de los servicios del paque..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P326** (36/100): "¿Puedo dejar agendadas todas mis sesiones?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P328** (36/100): "¿Pueden atendernos a las dos a la misma hora?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P329** (36/100): "¿Tienen capacidad para un grupo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P336** (36/100): "¿Necesito firmar un consentimiento?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P337** (36/100): "¿Me pueden enviar el consentimiento antes de la ci..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P338** (36/100): "¿Puedo aceptar el consentimiento por WhatsApp?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P339** (34/100): "¿Qué información incluye el consentimiento?..." → "Puedo ayudarte con información del catálogo, pero necesito e..."
- **P340** (36/100): "¿Existe una edad mínima para este servicio?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P341** (36/100): "Soy menor de edad, ¿puedo reservar?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P343** (36/100): "¿Debe asistir un adulto responsable?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P344** (36/100): "¿Necesitan autorización del tutor?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P345** (36/100): "¿Qué datos del tutor necesitan?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P346** (36/100): "¿Hay tratamientos que no se realizan a menores?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P348** (36/100): "¿Cómo debo prepararme para la cita?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P349** (36/100): "¿Debo hacer algo antes del tratamiento?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P351** (36/100): "¿Debo suspender algún producto antes de la sesión?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P354** (36/100): "¿Cuánto antes debo llegar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P355** (36/100): "¿Puedo ir acompañada?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P356** (36/100): "¿Qué pasa si estoy embarazada?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P357** (36/100): "Tengo una condición médica, ¿puedo realizarme el t..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P360** (36/100): "¿Hay contraindicaciones para este servicio?..." → "Perfecto, puedo ayudarte. ¿Qué producto o servicio estás bus..."
- **P361** (36/100): "¿Puedo comer antes de la atención?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P362** (36/100): "¿Qué ropa conviene llevar?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P363** (36/100): "Voy atrasada, ¿todavía me pueden atender?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P364** (36/100): "¿Cuántos minutos de atraso permiten?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P365** (36/100): "¿Qué pasa si llego tarde?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P366** (36/100): "No pude asistir a mi cita, ¿qué debo hacer?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P368** (36/100): "¿Existe una penalización por no asistir?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P369** (36/100): "¿Perdí el abono por no presentarme?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P370** (36/100): "¿Cuántas inasistencias tengo registradas?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P372** (36/100): "Me informaron que estoy bloqueada, ¿qué significa?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P381** (36/100): "¿Atienden a la hora de almuerzo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P382** (36/100): "¿Abren los sábados?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P384** (36/100): "¿Atienden en días feriados?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P386** (36/100): "¿Mañana abren más temprano?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P389** (36/100): "¿Hay una apertura extraordinaria?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P392** (36/100): "¿Hay atención después de las siete?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P401** (36/100): "¿Me avisarán si hay una capacitación o reunión?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P402** (36/100): "¿Qué pasa si la cabina o el equipo entra en manten..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P407** (59/100): "Quiero hacer un reclamo...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P408** (59/100): "Tuve un problema con mi atención...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P409** (59/100): "No quedé conforme con el servicio...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P411** (59/100): "Nadie responde mi solicitud...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P412** (59/100): "Me cobraron un monto incorrecto...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P413** (59/100): "Tengo un problema con una devolución...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P414** (59/100): "Me hicieron un cobro duplicado...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P419** (59/100): "Creo que hubo un error en mi reserva...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P420** (59/100): "Mi reserva desapareció de la agenda...." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P421** (59/100): "Llegué a la sucursal y no tenían registrada mi cit..." → "Te derivaré con una persona del equipo para que pueda ayudar..."
- **P426** (36/100): "¿Tienen cupito hoy?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P428** (36/100): "¿Hay algo tipo cinco?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P429** (36/100): "¿Qué tienen libre en la tarde?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P437** (36/100): "No voy a poder ir...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P438** (59/100): "No me llegó el enlace...." → "🔁 *Reenvío de enlace de confirmación*

Revisaré si tienes u..."
- **P440** (36/100): "Ya pagué...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P443** (36/100): "Quiero ver lo de mañana...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P444** (36/100): "Necesito cambiar eso...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P445** (36/100): "No puedo ir...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P446** (36/100): "Quiero otra opción...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P447** (36/100): "¿Está listo?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P448** (36/100): "¿Todavía sirve?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P449** (36/100): "La misma de la otra vez...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P450** (36/100): "Quiero con ella...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P451** (36/100): "A la misma hora...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P453** (36/100): "Quiero el tratamiento anterior...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P454** (36/100): "No quiero ese...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P455** (36/100): "Quiero confirmar...." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P458** (36/100): "¿Tienen para después?..." → "Hola, gracias por escribirnos. ¿Te ayudo con servicios, prec..."
- **P460** (34/100): "No quiero cancelar, solo necesito información...." → "Puedo ayudarte con información del catálogo, pero necesito e..."

## Desglose por intención

| Intención | Casos | Promedio | Mín | Máx |
|---|---:|---:|---:|---:|
| greeting | 3 | 78 | 75 | 83 |
| ambiguous | 136 | 49 | 36 | 83 |
| service_info | 40 | 45 | 34 | 77 |
| human | 9 | 71 | 70 | 75 |
| booking | 81 | 68 | 36 | 91 |
| service_info+location | 4 | 76 | 71 | 77 |
| booking+human+professional | 1 | 70 | 70 | 70 |
| price | 6 | 83 | 67 | 89 |
| price+service_info | 3 | 85 | 81 | 89 |
| price+location | 6 | 77 | 77 | 77 |
| price+professional | 2 | 89 | 89 | 89 |
| booking+service_info | 7 | 90 | 87 | 91 |
| booking+human | 5 | 87 | 70 | 92 |
| booking+availability | 4 | 81 | 73 | 85 |
| availability | 4 | 76 | 75 | 77 |
| booking+schedule | 18 | 69 | 36 | 83 |
| booking+schedule+availability | 3 | 85 | 85 | 85 |
| schedule | 5 | 43 | 36 | 69 |
| booking+service_info+schedule | 2 | 60 | 36 | 83 |
| booking+professional | 8 | 87 | 83 | 91 |
| location+availability | 2 | 77 | 77 | 77 |
| location | 12 | 75 | 57 | 77 |
| price+service_info+location | 1 | 77 | 77 | 77 |
| booking+location+schedule | 1 | 57 | 57 | 57 |
| location+schedule | 1 | 77 | 77 | 77 |
| booking+location+status | 1 | 81 | 81 | 81 |
| cancel+price+location | 1 | 77 | 77 | 77 |
| service_info+professional | 5 | 77 | 77 | 77 |
| professional | 16 | 77 | 77 | 77 |
| location+professional | 2 | 77 | 77 | 77 |
| booking+status+professional | 1 | 85 | 85 | 85 |
| booking+location+professional | 1 | 77 | 77 | 77 |
| booking+status | 10 | 74 | 59 | 81 |
| service_info+status | 1 | 81 | 81 | 81 |
| status | 3 | 36 | 36 | 36 |
| cancel | 8 | 71 | 36 | 81 |
| cancel+reschedule+service_info | 1 | 71 | 71 | 71 |
| booking+cancel | 11 | 72 | 36 | 81 |
| booking+reschedule | 5 | 89 | 89 | 89 |
| reschedule+professional | 1 | 81 | 81 | 81 |
| booking+service_info+status | 1 | 81 | 81 | 81 |
| booking+reschedule+status | 1 | 81 | 81 | 81 |
| reschedule | 4 | 82 | 81 | 83 |
| booking+cancel+reschedule | 1 | 81 | 81 | 81 |
| booking+schedule+status | 1 | 75 | 75 | 75 |
| greeting+reschedule | 1 | 81 | 81 | 81 |
| booking+cancel+status | 3 | 78 | 73 | 81 |
| booking+cancel+human | 1 | 76 | 76 | 76 |
| cancel+service_info | 2 | 58 | 34 | 81 |
| booking+sensitive | 1 | 92 | 92 | 92 |
| sensitive | 2 | 92 | 92 | 92 |
| booking+price+location+schedule | 1 | 77 | 77 | 77 |
| booking+location+schedule+professional | 1 | 77 | 77 | 77 |
| cancel+reschedule | 1 | 81 | 81 | 81 |
| complaint | 3 | 59 | 59 | 59 |
| service_info+complaint | 1 | 59 | 59 | 59 |
| service_info+sensitive | 2 | 92 | 92 | 92 |
| greeting+sensitive | 1 | 92 | 92 | 92 |
| booking+location | 1 | 59 | 59 | 59 |

## Ejemplos óptimos

- **P028** (87/100): "¿Puedo reservar directamente o primero necesito un..."
- **P049** (85/100): "¿Cuánto cuesta la limpieza facial?..."
- **P050** (89/100): "¿Cuál es el precio del tratamiento?..."
- **P051** (89/100): "¿Cuánto vale una sesión?..."
- **P053** (89/100): "¿El precio cambia según la profesional?..."

## Respuestas duplicadas

| Respuesta | Ocurrencias |
|---|---:|
| "hola, gracias por escribirnos. ¿te ayudo con servicios, prec..." | 129 |
| "claro 😊 ¿que servicio quieres agendar? tengo opciones como ..." | 50 |
| "tenemos estas sucursales activas: centro estetico bella - se..." | 34 |
| "te derivare con una persona del equipo para que pueda ayudar..." | 26 |
| "📭 *sin reservas activas*

no encontre reservas futuras acti..." | 22 |
| "perfecto, puedo ayudarte. ¿que producto o servicio estas bus..." | 21 |
| "puedo revisar profesionales disponibles. ¿con que profesiona..." | 21 |
| "gracias. para revisar el pago, ¿me indicas el numero de pedi..." | 19 |
| "no encontre una reserva activa asociada a este numero. ¿me p..." | 15 |
| "puedo ayudarte con eso. ¿me indicas el numero de solicitud o..." | 14 |

