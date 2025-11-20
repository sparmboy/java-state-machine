package com.glc.statemachine;

/**
 * Implementations are responsible for returning true if a certain logic path or {@link Transition} should
 * be followed. The intention is for the implementations to be reusable logic blocks for use within the
 * state machine matrix
 */
public interface TransitionEvaluator<T extends StatefulEntity> {

    /**
     * @return A text description of the logic of the evaluation in a human-readable form e.g. "Is the value greater than 3"
     */
    String getDescription();

    /**
     * Performs the logical evaluation
     * @param context
     * @return The outcome of teh performed boolean logic
     */
    boolean evaluate(ActionContext<T> context);
}
