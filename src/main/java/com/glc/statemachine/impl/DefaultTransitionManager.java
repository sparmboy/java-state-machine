package com.glc.statemachine.impl;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.Transition;
import com.glc.statemachine.TransitionManager;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link TransitionManager} that looks
 * up the transition for the event and state, and if found performs the transition and calls abstract method
 * to persist entity. Implementations of this class must therefore only handle the persisting of the {@link StatefulEntity}
 * and all state machine transitioning will be handled.
 * <p>
 * Note that if a transition is not found, then no actions are performed and only a trace message will be logged.
 */
@Slf4j
public abstract class DefaultTransitionManager<T extends StatefulEntity> implements TransitionManager<T> {
    @Override
    public Optional<Transition<T>> triggerEvent(@NonNull ActionContext<T> actionContext) {
        log.trace("Handling event {} on entity state {}", actionContext.getStateMachineEvent(), actionContext.getEntity().getState());
        Optional<Transition<T>> transitionOptional = actionContext.getStateMachineDefinition().getTransition(actionContext);
        if (transitionOptional.isPresent()) {
            Transition<T> transition = transitionOptional.get();
            log.trace("Executing transition from {} to {}", transition.getFromState(), transition.getToState(actionContext));
            transition.perform(actionContext);
            persistEntity(actionContext);
        } else {
            log.trace("No transition found");
        }
        return transitionOptional;
    }

    /**
     * Implementations should persist the {@link StatefulEntity} within
     * the action context to preserve changes that occur during the transition actions
     *
     * @param actionContext
     */
    protected abstract void persistEntity(ActionContext<T> actionContext);
}
