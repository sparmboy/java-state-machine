package com.glc.statemachine.impl;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.TransitionAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class DefaultTransitionAction<T extends StatefulEntity> implements TransitionAction<T> {
    String name;
    String description;

    public DefaultTransitionAction(String name) {
        this.name = name;
        this.description = null;
    }

    @Override
    public void execute(ActionContext<T> actionContext) {
        log.trace("Executing action {}", this.name);
    }
}
