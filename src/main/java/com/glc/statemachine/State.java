package com.glc.statemachine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.glc.statemachine.impl.DefaultState;

public interface State {
    String getStateName();

    @JsonCreator
    static State forValue(String value) {
        return new DefaultState(value);
    }
}
