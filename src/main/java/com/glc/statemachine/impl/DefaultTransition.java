package com.glc.statemachine.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.glc.statemachine.ActionContext;
import com.glc.statemachine.State;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.Transition;
import com.glc.statemachine.TransitionAction;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Default transition implementation
 */
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Getter
public class DefaultTransition<T extends StatefulEntity> implements Transition<T> {
    State fromState;
    State toState;
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<TransitionAction<T>> transitionActions;

    @JsonIgnore
    public Optional<List<TransitionAction<T>>> getTransitionActions() {
        return Optional.ofNullable(transitionActions);
    }

    @Override
    public State getToState(ActionContext<T> context) {
        return toState;
    }
}
