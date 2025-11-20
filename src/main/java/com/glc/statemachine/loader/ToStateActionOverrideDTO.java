package com.glc.statemachine.loader;

import com.glc.statemachine.State;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.TransitionAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Wrapper to define an override for the transition action when it moves
 * to the specified state
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@Getter
public class ToStateActionOverrideDTO<T extends StatefulEntity> {
    State toState;
    TransitionAction<T> transitionAction;
}
