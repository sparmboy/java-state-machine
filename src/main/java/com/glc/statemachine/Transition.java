package com.glc.statemachine;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface used to define concrete implementations of transition actions
 * between states.
 */
public interface Transition<T extends StatefulEntity> {

    Logger log = LoggerFactory.getLogger(Transition.class);

    /**
     * Implementations must return the final state that the {@link StatefulEntity}
     * is to transition to in this transition.
     *
     * @param context The action context that is associated with this transition.
     * @return The target state
     */
    State getToState(ActionContext<T> context);


    /**
     * Implementations must return the initial state at the start of this transition.
     *
     * @return The initial state
     */
    State getFromState();

    /**
     * Implementations must return actions to be performed in order during this transition.
     *
     * @return The ordered list of actions to be executed
     */
    Optional<List<TransitionAction<T>>> getTransitionActions();

    /**
     * Executes a default implementation of the transition actions by
     * executing each of the TransitionAction defined and setting the
     * state on the entity to the defined target state.
     *
     * @param context
     */
    default void perform(ActionContext<T> context) {
        getTransitionActions().ifPresent((actions) -> actions.forEach(action -> {
            log.trace("Executing transition action {}", action.getName());
            action.execute(context);
        }));

        State toState = getToState(context);
        log.trace("Updating entity state to {}", toState);
        context.getEntity().setState(toState);


        context.getStateMachineDefinition().getTransitionListeners()
            .ifPresent((transitionListeners -> transitionListeners
                .forEach(transitionListener -> {
                    log.trace("Calling transition listener {}", transitionListener);
                    transitionListener.onTransition(this, context);
                })));

    }

}
