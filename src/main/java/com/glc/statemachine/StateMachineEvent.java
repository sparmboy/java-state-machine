package com.glc.statemachine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.glc.statemachine.impl.DefaultStateMachineEvent;
import java.util.List;
import java.util.Optional;

/**
 * Represents an event within the state machine definition that
 * can be used to trigger a transition between states.
 */
public interface StateMachineEvent {

    /**
     * Specifies a choice of roles that an authenticated
     * user must have to be able to trigger this event
     * @return
     */
    default Optional<List<String>> getRoles() {
        return Optional.empty();
    }

    /**
     * Implementations must return a text representation of the event.
     *
     * @return The text representation of the event
     */
    String getEventName();

    @JsonCreator
    static StateMachineEvent forValue(String value) {
        return new DefaultStateMachineEvent(value);
    }
}
