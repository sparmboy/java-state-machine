package com.glc.statemachine;

import java.util.Optional;
import lombok.NonNull;

/**
 * A TransitionManager implementation is responsible for evaluating actions for a transition based on the incoming
 * {@link StatefulEntity}, {@link StateMachineEvent} and {@link Transition}.
 */
public interface TransitionManager<T extends StatefulEntity> {
    Optional<Transition<T>> triggerEvent(@NonNull ActionContext<T> actionContext);
}
