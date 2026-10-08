package com.glc.statemachine.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.glc.statemachine.StateMachineEvent;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * {@link StateMachineEvent} implementation that has a list of roles
 * associated with it to enforce authorisation to trigger events
 */
@Getter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(of = "eventName")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthorisedStateMachineEvent implements StateMachineEvent {
    String eventName;
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<String> roles;

    public AuthorisedStateMachineEvent( StateMachineEvent event ) {
        this.eventName = event.getEventName();
        roles = null;
    }

    public AuthorisedStateMachineEvent( StateMachineEvent event,List<String> roles ) {
        this.eventName = event.getEventName();
        this.roles = roles;
    }

    @Override
    public Optional<List<String>> getRoles() {
        return Optional.ofNullable(roles);
    }

}
