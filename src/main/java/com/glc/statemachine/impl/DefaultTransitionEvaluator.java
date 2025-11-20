package com.glc.statemachine.impl;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionEvaluator;

public class DefaultTransitionEvaluator implements TransitionEvaluator {
    @Override
    public String getDescription() {
        return "Default";
    }

    @Override
    public boolean evaluate(ActionContext context) {
        return true;
    }
}
