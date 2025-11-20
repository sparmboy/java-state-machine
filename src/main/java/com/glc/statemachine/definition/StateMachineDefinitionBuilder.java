package com.glc.statemachine.definition;

import com.glc.statemachine.State;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.TransitionEvaluator;
import com.glc.statemachine.impl.DefaultTransition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateMachineDefinitionBuilder<T extends StatefulEntity> {
    private final HashMap<State, StateMachineEventTransitionEvaluations<T>> matrix;
    private final List<String> defaultPath;

    public StateMachineDefinitionBuilder() {
        defaultPath = new ArrayList<>();
        matrix = new HashMap<>();
    }

    public StateMachineDefinitionBuilder<T> withTransition(
        StateMachineEvent event,
        State from,
        State to
    ) {
        return withTransition(event, from, to, null, Collections.emptyList());
    }

    public StateMachineDefinitionBuilder<T> withTransition(
        StateMachineEvent event,
        State from,
        State to,
        List<TransitionAction<T>> actions
    ) {
        return withTransition(event, from, to, null, actions);
    }

    public StateMachineDefinitionBuilder<T> withTransition(
        StateMachineEvent event,
        State from,
        State to,
        TransitionEvaluator<T> evaluator,
        List<TransitionAction<T>> actions
    ) {
        // Get any existing transition evaluations, and if not create an empty one
        StateMachineEventTransitionEvaluations<T> evals = matrix.get(from);
        if (evals == null) {
            evals = new StateMachineEventTransitionEvaluations<>(new HashMap<>());
        }

        // Check if we have any existing evaluation actions for this event
        Map<StateMachineEvent, List<TransitionEvaluationActions<T>>> evaluationMap = evals.getTransitionEvaluationActions().orElse(new HashMap<>());
        List<TransitionEvaluationActions<T>> evaluationActions = evaluationMap.get(event);
        if (evaluationActions == null) {
            evaluationActions = new ArrayList<>();
        }

        // Add the new transition from the 'from' state, to the 'to' state with the list of actions to execute and an optional condition pass before for the transition will execute
        evaluationActions.add(new TransitionEvaluationActions<>(evaluator, new DefaultTransition<>(from, to, actions)));
        evaluationMap.put(event, evaluationActions);

        // Update the matrix and return
        matrix.put(from, new StateMachineEventTransitionEvaluations<>(evaluationMap));
        return this;
    }

    public StateMachineDefinitionBuilder<T> withDefaultPath(
        List<String> path
    ) {
        this.defaultPath.clear();
        this.defaultPath.addAll(path);
        return this;
    }

    public StateMachineDefinition<T> build() {
        return new StateMachineDefinition<>(matrix, defaultPath);
    }
}
