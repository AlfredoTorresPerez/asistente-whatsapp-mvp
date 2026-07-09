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
        assertThat(BookingStateMachine.canonical("SOLICITADA")).isEqualTo(BookingStateMachine.REQUESTED);
        assertThat(BookingStateMachine.canonical("PENDIENTE_PAGO")).isEqualTo(BookingStateMachine.PENDING_PAYMENT);
        assertThat(BookingStateMachine.canonical("ATENDIDA")).isEqualTo(BookingStateMachine.ATTENDED);
        assertThat(BookingStateMachine.canonical("EXPIRADA")).isEqualTo(BookingStateMachine.EXPIRED);
        assertThat(BookingStateMachine.canonical("NO_ASISTE")).isEqualTo(BookingStateMachine.NO_SHOW);
    }

    @Test
    void allowsOperationalTransitionsFromActiveStatuses() {
        BookingStateMachine.assertTransition("PENDIENTE_CONFIRMACION", "CONFIRMED", "confirmarse");
        BookingStateMachine.assertTransition("CONFIRMED", "REPROGRAMADA", "reprogramarse");
        BookingStateMachine.assertTransition("REPROGRAMADA", "CANCELADA", "cancelarse");
        BookingStateMachine.assertTransition("CONFIRMED", "ATENDIDA", "completarse");
        BookingStateMachine.assertTransition("SOLICITADA", "PENDIENTE_PAGO", "solicitar pago");
        BookingStateMachine.assertTransition("PENDIENTE_PAGO", "CONFIRMADA", "confirmarse");
    }

    @Test
    void rejectsTransitionsFromClosedStatuses() {
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("CANCELADA", "CONFIRMED", "confirmarse"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertCanChange("EXPIRADA", "reprogramarse"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("ATENDIDA", "REPROGRAMADA", "reprogramarse"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("NO_ASISTE", "CONFIRMADA", "confirmarse"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertCanChange("CANCELADA", "cancelarse"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertCanReceiveConfirmationLink("CANCELADA"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertCanReceiveConfirmationLink("ATENDIDA"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> BookingStateMachine.assertCanReceiveConfirmationLink("EXPIRADA"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void allowsConfirmationLinkForOpenStatuses() {
        BookingStateMachine.assertCanReceiveConfirmationLink("SOLICITADA");
        BookingStateMachine.assertCanReceiveConfirmationLink("PENDIENTE_CONFIRMACION");
        BookingStateMachine.assertCanReceiveConfirmationLink("PENDIENTE_PAGO");
    }

    @Test
    void allowsChangeForModifiableStatuses() {
        BookingStateMachine.assertCanChange("SOLICITADA", "cambiarse");
        BookingStateMachine.assertCanChange("CONFIRMADA", "cambiarse");
        BookingStateMachine.assertCanChange("REPROGRAMADA", "cambiarse");
        BookingStateMachine.assertCanChange("PENDIENTE_PAGO", "cambiarse");
    }

    @Test
    void isClosedDetectsTerminalStatuses() {
        assertThat(BookingStateMachine.isClosed("CANCELADA")).isTrue();
        assertThat(BookingStateMachine.isClosed("EXPIRADA")).isTrue();
        assertThat(BookingStateMachine.isClosed("ATENDIDA")).isTrue();
        assertThat(BookingStateMachine.isClosed("NO_ASISTE")).isTrue();
        assertThat(BookingStateMachine.isClosed("CONFIRMADA")).isFalse();
        assertThat(BookingStateMachine.isClosed("PENDIENTE_CONFIRMACION")).isFalse();
        assertThat(BookingStateMachine.isClosed("REPROGRAMADA")).isFalse();
    }

    @Test
    void rejectsInvalidTransitionFromCancelledToRescheduled() {
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("CANCELADA", "REPROGRAMADA", "reprogramarse"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsInvalidTransitionFromExpiredToConfirmed() {
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("EXPIRADA", "CONFIRMADA", "confirmarse"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsInvalidTransitionFromCompletedToRescheduled() {
        assertThatThrownBy(() -> BookingStateMachine.assertTransition("ATENDIDA", "REPROGRAMADA", "reprogramarse"))
                .isInstanceOf(ApiException.class);
    }
}
