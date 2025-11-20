package com.myorg.statemachine.evaluators;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionEvaluator;

public class TestTransitionEvaluator implements TransitionEvaluator {
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean evaluate(ActionContext context) {
        return true;
    }
}
