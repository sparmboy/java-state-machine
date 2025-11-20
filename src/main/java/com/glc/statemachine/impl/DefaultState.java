package com.glc.statemachine.impl;

import com.glc.statemachine.State;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "stateName")
@ToString(includeFieldNames = false)
public class DefaultState implements State {
    String stateName;

    public DefaultState(State state) {
        this.stateName = state.getStateName();
    }


}
