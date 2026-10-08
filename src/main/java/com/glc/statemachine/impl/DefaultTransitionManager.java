package com.glc.statemachine.impl;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.State;
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
 * <p>
 * If {@link #persistEntity(ActionContext)} throws, the entity's state is rolled back to what it was before this
 * transition ran and the exception is rethrown. Note that only the state field is rolled back; any other side
 * effects performed by transition actions or transition listeners are not undone.
 */
@Slf4j
public abstract class DefaultTransitionManager<T extends StatefulEntity> implements TransitionManager<T> {
    @Override
    public Optional<Transition<T>> triggerEvent(@NonNull ActionContext<T> actionContext) {
        log.trace("Handling event {} on entity state {}", actionContext.getStateMachineEvent(), actionContext.getEntity().getState());
        Optional<Transition<T>> transitionOptional = actionContext.getStateMachineDefinition().getTransition(actionContext);
        if (transitionOptional.isPresent()) {
            Transition<T> transition = transitionOptional.get();
            State previousState = actionContext.getEntity().getState();
            log.trace("Executing transition from {} to {}", transition.getFromState(), transition.getToState(actionContext));
            transition.perform(actionContext);
            try {
                persistEntity(actionContext);
            } catch (RuntimeException e) {
                log.error("Failed to persist entity after transition from {} to {}; rolling back in-memory state to {}",
                    previousState, actionContext.getEntity().getState(), previousState, e);
                actionContext.getEntity().setState(previousState);
                throw e;
            }
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
