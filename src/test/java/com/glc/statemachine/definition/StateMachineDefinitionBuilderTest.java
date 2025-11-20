package com.glc.statemachine.definition;

import static com.glc.statemachine.definition.StateMachineDefinitionUtil.NAME_PARAM;
import static com.glc.statemachine.definition.StateMachineDefinitionUtil.mockComplexStateMachine;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionManager;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestState;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.impl.DefaultTransitionManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateMachineDefinitionBuilderTest {
    private final StateMachineDefinition<TestCase> stateMachineDefinition = mockComplexStateMachine();
    private final TransitionManager<TestCase> transitionManager = new DefaultTransitionManager<TestCase>() {
        @Override
        protected void persistEntity(ActionContext<TestCase> actionContext) {

        }
    };

    @Test
    public void shouldSetNameOnEntityFromConditionalTransition() {
        // Given
        TestCase testCase = new TestCase();
        String name = "bob";
        Map<String, Object> params = new HashMap<String, Object>(){{put(NAME_PARAM,name);}};
        ActionContext<TestCase> actionContext = new ActionContext<>(
            TestStateMachineEvent.BEGIN,
            testCase,
            stateMachineDefinition,
            params
        );
        assertEquals("A",testCase.getName());

        // When
        transitionManager.triggerEvent(actionContext);

        // Then
        assertEquals(TestState.MIDDLE,testCase.getState());
        assertEquals(name,testCase.getName());
    }

    @Test
    public void shouldNotSetNameOnEntityFromConditionalTransition() {
        // Given
        TestCase testCase = new TestCase();
        ActionContext<TestCase> actionContext = new ActionContext<>(
            TestStateMachineEvent.BEGIN,
            testCase,
            stateMachineDefinition
        );
        assertEquals("A",testCase.getName());

        // When
        transitionManager.triggerEvent(actionContext);

        // Then
        assertEquals(TestState.MIDDLE,testCase.getState());
        assertEquals("A",testCase.getName());
    }
}
