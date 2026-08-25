package com.glc.statemachine.impl;

import static com.glc.statemachine.definition.StateMachineDefinitionUtil.mockStateMachine;
import static org.junit.jupiter.api.Assertions.*;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionManager;
import com.glc.statemachine.definition.StateMachineDefinition;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.definition.testcase.TestState;
import org.junit.jupiter.api.Test;

class DefaultTransitionManagerTest {

    private final StateMachineDefinition<TestCase> stateMachineDefinition = mockStateMachine();
    TransitionManager<TestCase> transitionManager = new DefaultTransitionManager<TestCase>() {
        @Override
        protected void persistEntity(ActionContext<TestCase> actionContext) {

        }
    };

    @Test
    public void shouldUpdateStateToMiddle() {
        // Given
        TestCase testCase = new TestCase();

        // When
        transitionManager.triggerEvent(
            new ActionContext<>(
                TestStateMachineEvent.BEGIN,
                testCase,
                stateMachineDefinition
            )
        );

        // Then
        assertEquals(TestState.MIDDLE,testCase.getState());
    }

    @Test
    public void shouldUpdateStateToEnd() {
        // Given
        TestCase testCase = new TestCase(TestState.MIDDLE);

        // When
        transitionManager.triggerEvent(
            new ActionContext<>(
                TestStateMachineEvent.STOP,
                testCase,
                stateMachineDefinition
            )
        );

        // Then
        assertEquals(TestState.END,testCase.getState());
    }

    @Test
    public void shouldUpdateNotUpdateState() {
        // Given
        TestCase testCase = new TestCase();

        // When
        transitionManager.triggerEvent(
            new ActionContext<>(
                TestStateMachineEvent.STOP,
                testCase,
                stateMachineDefinition
            )
        );

        // Then
        assertEquals(TestState.START,testCase.getState());
    }

    @Test
    public void shouldRollBackStateWhenPersistenceFails() {
        // Given
        TestCase testCase = new TestCase();
        TransitionManager<TestCase> failingTransitionManager = new DefaultTransitionManager<TestCase>() {
            @Override
            protected void persistEntity(ActionContext<TestCase> actionContext) {
                throw new RuntimeException("simulated persistence failure");
            }
        };

        // When / Then
        assertThrows(RuntimeException.class, () -> failingTransitionManager.triggerEvent(
            new ActionContext<>(
                TestStateMachineEvent.BEGIN,
                testCase,
                stateMachineDefinition
            )
        ));

        // The entity's in-memory state must be rolled back to its pre-transition value
        // since the transition was never actually persisted.
        assertEquals(TestState.START, testCase.getState());
    }

}
