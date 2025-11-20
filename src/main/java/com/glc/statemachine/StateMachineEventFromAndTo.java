package com.glc.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Wrapper for defining a {@link StateMachineEvent}, a 'from' state and a 'to' state of a transition.
 * Used to build simple state machine definitions
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class StateMachineEventFromAndTo<T extends StatefulEntity> {
    StateMachineEvent stateMachineEvent;
    State fromState;
    State toState;
    TransitionEvaluator<T> evaluator;

    public Optional<TransitionEvaluator<T>> getEvaluator() {
        return Optional.ofNullable(evaluator);
    }

    List<TransitionAction<T>> actions;

    public void addAction(TransitionAction<T> action) {
        actions.add(action);
    }

    public StateMachineEventFromAndTo(
        StateMachineEvent stateMachineEvent,
        State fromState,
        State toState
    ) {
        this(stateMachineEvent, fromState, toState, null);
    }

    public StateMachineEventFromAndTo(
        StateMachineEvent stateMachineEvent,
        State fromState,
        State toState,
        TransitionEvaluator<T> evaluator
    ) {
        this.stateMachineEvent = stateMachineEvent;
        this.fromState = fromState;
        this.toState = toState;
        this.evaluator = evaluator;
        this.actions = new ArrayList<>();
    }
}
