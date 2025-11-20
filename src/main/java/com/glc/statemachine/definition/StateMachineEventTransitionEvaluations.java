package com.glc.statemachine.definition;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.serializers.EventTransitionEvaluationsSerializer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Stores a map of {@link TransitionEvaluationActions} indexed by {@link StateMachineEvent}
 */
@JsonSerialize(using = EventTransitionEvaluationsSerializer.class)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class StateMachineEventTransitionEvaluations<T extends StatefulEntity> {
    Map<StateMachineEvent, List<TransitionEvaluationActions<T>>> transitionEvaluationActions;

    public Optional<Map<StateMachineEvent, List<TransitionEvaluationActions<T>>>> getTransitionEvaluationActions() {
        return Optional.ofNullable(transitionEvaluationActions);
    }
}
