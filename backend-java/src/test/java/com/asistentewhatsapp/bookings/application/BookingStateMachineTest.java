package com.asistentewhatsapp.bookings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.asistentewhatsapp.shared.exception.ApiException;
import org.junit.jupiter.api.Test;

class BookingStateMachineTest {

    @Test
    void canonicalNormalizesLegacyStatuses() {
        assertThat(BookingStateMachine.canonical("TEMPORARY")).isEqualTo(BookingStateMachine.PENDING_CONFIRMATION);
        assertThat(BookingStateMachine.canonical("CONFIRMED")).isEqualTo(BookingStateMachine.CONFIRMED);
        assertThat(BookingStateMachine.canonical("CANCELLED")).isEqualTo(BookingStateMachine.CANCELLED);
        assertThat(BookingStateMachine.canonical("NO_SHOW")).isEqualTo(BookingStateMachine.NO_SHOW);
    }

    @Test
    void allowsOperationalTransitionsFromActiveStatuses() {
        BookingStateMachine.assertTransition("PENDIENTE_CONFIRMACION", "CONFIRMED", "confirmarse");
        BookingStateMachine.assertTransition("CONFIRMED", "REPROGRAMADA", "reprogramarse");
        BookingStateMachine.assertTransition("REPROGRAMADA", "CANCELADA", "cancelarse");
    }

    @Test
    void rejectsTransitionsFromClosedStatuses() {
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("CANCELADA", "CONFIRMED", "confirmarse"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertCanChange("EXPIRADA", "reprogramarse"))
                .isInstanceOf(ApiException.class);
    }
}
