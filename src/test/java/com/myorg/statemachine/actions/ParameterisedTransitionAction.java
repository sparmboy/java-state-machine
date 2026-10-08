package com.myorg.statemachine.actions;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.definition.testcase.TestCase;

public class ParameterisedTransitionAction implements TransitionAction<TestCase> {
    private final String name;

    public ParameterisedTransitionAction(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Sets the entity name to the loader's instantiation parameter";
    }

    @Override
    public void execute(ActionContext<TestCase> actionContext) {
        actionContext.getEntity().setName(name);
    }
}
