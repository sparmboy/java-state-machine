package com.glc.statemachine.impl;

import com.glc.statemachine.State;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.TransitionEvaluator;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * Transition wrapper that stores the next {@link State}, {@link TransitionEvaluator} (optional) and {@link TransitionAction} (optional)
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class TransitionContainer {
    TransitionEvaluator evaluator;

    public Optional<TransitionEvaluator> getEvaluator() {
        return Optional.ofNullable(evaluator);
    }

    @Getter
    State nextState;

    TransitionAction action;

    public Optional<TransitionAction> getAction() {
        return Optional.ofNullable(action);
    }

    public TransitionContainer(State nextState) {
        this.evaluator = null;
        this.nextState = nextState;
        this.action = null;
    }

    public TransitionContainer(
        TransitionEvaluator evaluator,
        State nextState
    ) {
        this.evaluator = evaluator;
        this.nextState = nextState;
        this.action = null;
    }

    public TransitionContainer(
        TransitionEvaluator evaluator,
        State nextState,
        TransitionAction action
    ) {
        this.evaluator = evaluator;
        this.nextState = nextState;
        this.action = action;
    }


    public TransitionContainer(
        State nextState,
        TransitionAction action) {
        this.evaluator = null;
        this.nextState = nextState;
        this.action = action;
    }

}
