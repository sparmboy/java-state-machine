package com.glc.statemachine.impl;

import com.glc.statemachine.StateMachineEvent;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(of = "eventName")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DefaultStateMachineEvent implements StateMachineEvent {
    String eventName;
}
