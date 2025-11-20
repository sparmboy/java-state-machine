package com.glc.statemachine.definition.testcase;

import com.glc.statemachine.State;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public enum TestState implements State {
    START("Start"),
    MIDDLE("Middle"),
    END("End");
    String stateName;
}
