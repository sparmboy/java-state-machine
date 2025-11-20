package com.glc.statemachine;

/**
 * Interface to define how the actions are performed for a given transition between states.
 */
public interface TransitionAction<T extends StatefulEntity> {

    /**
     * @return The text for the name of the action, mainly helpful in logging actions and building state machine schemas.
     */
    String getName();

    /**
     * @return A text description for what happens during the transition, mainly helpful in logging actions and building state machine schemas.
     */
    String getDescription();

    /**
     * Implementations must perform any action that needs to occur for the transition.
     *
     * @param actionContext
     */
    void execute(ActionContext<T> actionContext);
}
