package com.glc.statemachine.definition.testcase;

import com.glc.statemachine.StateMachineEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public enum TestStateMachineEvent implements StateMachineEvent {
    BEGIN("Begin"),
    STOP("Stop"),
    ;
    String eventName;
}
