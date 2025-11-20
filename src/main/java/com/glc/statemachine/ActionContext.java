package com.glc.statemachine;

import com.glc.statemachine.definition.StateMachineDefinition;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Defines context and parameters for a transition between states
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Getter
public class ActionContext<T extends StatefulEntity> {
    /**
     * The stateMachineEvent that triggered the transition
     */
    @NonNull StateMachineEvent stateMachineEvent;

    /**
     * The entity that is associated with the state machine
     */
    @NonNull T entity;

    /**
     * The state machine definition that defines the transitions
     */
    @NonNull StateMachineDefinition<T> stateMachineDefinition;

    /**
     * User defined parameters for the use within the transitions
     */
    Map<String, Object> params;

    public Optional<Map<String, Object>> getParams() {
        return Optional.ofNullable(params);
    }

    public ActionContext(
        @NonNull StateMachineEvent stateMachineEvent,
        @NonNull T entity,
        @NonNull StateMachineDefinition<T> stateMachineDefinition) {
        this.stateMachineEvent = stateMachineEvent;
        this.entity = entity;
        this.stateMachineDefinition = stateMachineDefinition;
        this.params = null;
    }
}
