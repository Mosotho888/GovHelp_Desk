package za.gov.helpdesk.unit.services.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.model.Status;
import za.gov.helpdesk.ticket.policy.TicketStatusTransitionPolicy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("TicketStatusTransitionPolicy unit tests")
class TicketStatusTransitionPolicyTest {

    private final TicketStatusTransitionPolicy policy = new TicketStatusTransitionPolicy();

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
        "OPEN, IN_PROGRESS",
        "ESCALATED, IN_PROGRESS",
        "IN_PROGRESS, RESOLVED",
        "IN_PROGRESS, ESCALATED",
        "RESOLVED, CLOSED",
        "RESOLVED, OPEN"
    })
    @DisplayName("canTransition() allows every legal transition in the state machine")
    void canTransition_legalTransitions_returnsTrue(final Status current, final Status next) {
        assertThat(policy.canTransition(current, next)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
        "OPEN, RESOLVED",
        "OPEN, CLOSED",
        "OPEN, ESCALATED",
        "IN_PROGRESS, OPEN",
        "IN_PROGRESS, CLOSED",
        "ESCALATED, RESOLVED",
        "ESCALATED, CLOSED",
        "ESCALATED, OPEN",
        "RESOLVED, IN_PROGRESS",
        "RESOLVED, ESCALATED",
        "CLOSED, OPEN",
        "CLOSED, IN_PROGRESS",
        "CLOSED, RESOLVED",
        "CLOSED, ESCALATED"
    })
    @DisplayName("canTransition() rejects every illegal transition in the state machine")
    void canTransition_illegalTransitions_returnsFalse(final Status current, final Status next) {
        assertThat(policy.canTransition(current, next)).isFalse();
    }

    @Test
    @DisplayName("canTransition() treats CLOSED as a terminal state with no outgoing transitions")
    void canTransition_fromClosed_alwaysFalse() {
        for (final Status next : Status.values()) {
            assertThat(policy.canTransition(Status.CLOSED, next)).isFalse();
        }
    }

    @Test
    @DisplayName("assertCanTransition() does not throw for a legal transition")
    void assertCanTransition_legalTransition_doesNotThrow() {
        assertThatCode(() -> policy.assertCanTransition(Status.OPEN, Status.IN_PROGRESS))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName(
            "assertCanTransition() throws InvalidStatusTransitionException for an illegal"
                    + " transition")
    void assertCanTransition_illegalTransition_throws() {
        assertThatThrownBy(() -> policy.assertCanTransition(Status.CLOSED, Status.OPEN))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
