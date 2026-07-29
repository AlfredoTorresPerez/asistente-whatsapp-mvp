package com.asistentewhatsapp.aiagents.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.asistentewhatsapp.aiagents.domain.AgentIntent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationSpecIntentDetectorTest {

	private final IntentDetectorService detector = new IntentDetectorService(new ConversationSpecCatalog());

	@Test
	void negatedCancelWithChangeRoutesToReschedule() {
		IntentDetectionResult result = detector.detect(request("No quiero cancelar, solo cambiar la hora"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_CHANGE);
	}

	@Test
	void negatedCancelWithPriceQuestionDoesNotRouteToCancellation() {
		IntentDetectionResult result = detector.detect(request("No quiero cancelar, solo saber el precio"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.PRICE_REQUEST);
	}

	@Test
	void negatedBookingWithConsultationDoesNotRouteToBooking() {
		IntentDetectionResult result = detector.detect(request("No quiero agendar, solo consultar"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.COMMERCIAL_INQUIRY);
	}

	@Test
	void taxonomyExamplesActAsFallbackWithoutReplacingExistingRules() {
		IntentDetectionResult result = detector.detect(request("Me gustaría reservar"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void isolatedAckDoesNotExecuteAction() {
		IntentDetectionResult result = detector.detect(request("ok"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.AMBIGUOUS);
	}

	@Test
	void standaloneOraNormalizedToHoraAndDetectsBooking() {
		IntentDetectionResult result = detector.detect(request("Quiero pedir ora para mañana"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void bookingTypoReserbarNormalizedAndDetected() {
		IntentDetectionResult result = detector.detect(request("Quiero reserbar para mañana"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void urgentWithoutComplaintWordsRoutesToBooking() {
		IntentDetectionResult result = detector.detect(request("Necesito una hora urgente"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void quieroSepararDetectedAsBooking() {
		IntentDetectionResult result = detector.detect(request("Quiero separar una hora para depilación"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void citaXfaDetectedAsBooking() {
		IntentDetectionResult result = detector.detect(request("Cita xfa para hoy"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void primeraHoraDetectedAsTime() {
		IntentDetectionResult result = detector.detect(request("Tienen primera hora el lunes?"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.AVAILABILITY_QUERY);
	}

	@Test
	void hacerUnaReservaDetectedAsBooking() {
		IntentDetectionResult result = detector.detect(request("Hola, quiero hacer una reserva"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void programarUnaCitaDetectedAsBooking() {
		IntentDetectionResult result = detector.detect(request("Necesito programar una cita para esta semana"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void bookingConPagoNoSeDesviaAPayment() {
		IntentDetectionResult result = detector.detect(request("Quiero reservar y pagar ahora"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void bookingTypoRecervarNormalizedAndDetected() {
		IntentDetectionResult result = detector.detect(request("Necesito recervar una hora"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void bookingTypoAjendarNormalizedAndDetected() {
		IntentDetectionResult result = detector.detect(request("Quiero ajendar una hora"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	@Test
	void bookingTypoAgndarNormalizedAndDetected() {
		IntentDetectionResult result = detector.detect(request("Necesito agndar para hoy"));

		assertThat(result.primaryIntent()).isEqualTo(AgentIntent.BOOKING_REQUEST);
	}

	private AgentConversationRequest request(String body) {
		return new AgentConversationRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				"56900000000", "Cliente", body, OffsetDateTime.now(ZoneOffset.UTC), null, null, null, false);
	}
}
