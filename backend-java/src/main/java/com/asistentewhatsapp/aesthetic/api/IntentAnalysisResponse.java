package com.asistentewhatsapp.aesthetic.api;

import java.math.BigDecimal;

public record IntentAnalysisResponse(String intencion, BigDecimal confianza, IntentEntitiesResponse entidades,
		boolean requiereConsultaBaseDatos, boolean requiereDerivacionHumana, String motivoDerivacion,
		String respuestaSugerida, String modelo) {
}
