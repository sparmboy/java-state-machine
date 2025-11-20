package com.glc.statemachine.definition;

import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.Transition;
import com.glc.statemachine.TransitionEvaluator;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TransitionEvaluationActions<T extends StatefulEntity> {

    TransitionEvaluator<T> evaluator;

    public Optional<TransitionEvaluator<T>> getEvaluator() {
        return Optional.ofNullable(evaluator);
    }

    Transition<T> transition;

    public TransitionEvaluationActions(Transition<T> transition) {
        this.transition = transition;
        this.evaluator = null;
    }
}
