package com.myorg.statemachine.evaluators;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionEvaluator;
import com.glc.statemachine.definition.testcase.TestCase;

public class NameIsBEvaluator implements TransitionEvaluator {
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean evaluate(ActionContext context) {
        TestCase testCase = (TestCase) context.getEntity();
        return "B".equals(testCase.getName());
    }
}
