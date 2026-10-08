package com.glc.statemachine.definition;

import static com.glc.statemachine.definition.StateMachineDefinitionUtil.mockStateMachine;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionEvaluator;
import com.glc.statemachine.impl.DefaultTransitionAction;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.State;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.definition.testcase.TestState;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class StateMachineDefinitionTest {

    private final StateMachineDefinition<TestCase> stateMachineDefinition = mockStateMachine();

    @Test
    public void shouldGenerateSchema() throws JsonProcessingException {
        // Given
        StateMachineDefinition<TestCase> definition = new StateMachineDefinitionBuilder<TestCase>()
            .withTransition(TestStateMachineEvent.BEGIN, TestState.START, TestState.MIDDLE,
                new TransitionEvaluator<TestCase>() {
                    @Override
                    public String getDescription() {
                        return "Has a name";
                    }

                    @Override
                    public boolean evaluate(ActionContext<TestCase> context) {
                        return true;
                    }
                },
                Collections.singletonList(new DefaultTransitionAction<>("setName")))
            .withTransition(TestStateMachineEvent.STOP, TestState.MIDDLE, TestState.END)
            .withDefaultPath(Arrays.asList("Start", "Middle", "End"))
            .build();

        // When
        String json = new ObjectMapper().writeValueAsString(definition);
        JsonNode tree = new ObjectMapper().readTree(json);

        // Then
        assertEquals("Has a name", tree.at("/matrix/START/BEGIN/0/evaluator/description").asText());
        assertEquals("MIDDLE", tree.at("/matrix/START/BEGIN/0/transition/toState").asText());
        assertEquals("setName", tree.at("/matrix/START/BEGIN/0/transition/transitionActions/0/name").asText());
        assertFalse(tree.at("/matrix/MIDDLE/STOP/0").has("evaluator"));
        assertEquals("[\"START\",\"MIDDLE\",\"END\"]", tree.at("/paths/default").toString());
        assertFalse(tree.has("transitionListeners"));
        assertFalse(json.contains("\"present\""), "Optional values must not be serialized as beans: " + json);
    }

    @Test
    public void shouldReturnActions() {
        // Given
        ActionContext<TestCase> actionContext = new ActionContext<>(
            TestStateMachineEvent.BEGIN,
            new TestCase(),
            stateMachineDefinition
        );

        // When / then
        assertTrue(stateMachineDefinition.getTransition(actionContext).isPresent());
    }

    @Test
    public void shouldNotReturnActions() {
        // Given
        ActionContext<TestCase> actionContext = new ActionContext<>(
            TestStateMachineEvent.STOP,
            new TestCase(),
            stateMachineDefinition
        );

        // When / then
        assertFalse(stateMachineDefinition.getTransition(actionContext).isPresent());
    }


    @Test
    public void shouldReturnToState() {
        // Given
        ActionContext<TestCase> actionContext = new ActionContext<>(
            TestStateMachineEvent.STOP,
            new TestCase(TestState.MIDDLE),
            stateMachineDefinition
        );

        // When / then
        assertTrue(stateMachineDefinition.getTransition(actionContext).isPresent());
        assertEquals(TestState.END,stateMachineDefinition.getTransition(actionContext).get().getToState(actionContext));
    }

    @Test
    public void shouldGetAllEvents() {
        // Given
        Comparator<? super StateMachineEvent> sorter = Comparator.comparing(Object::toString);
        List<StateMachineEvent> expectedList = Arrays.stream(TestStateMachineEvent.values()).sorted(sorter).collect(Collectors.toList());

        // When / then
        assertIterableEquals(expectedList, stateMachineDefinition.getEvents().stream().sorted(sorter).collect(Collectors.toList()));
    }

    @Test
    public void shouldGetAllStates() {
        // Given
        Comparator<? super State> sorter = Comparator.comparing(Object::toString);
        List<State> expectedList = Arrays.stream( TestState.values()).sorted(sorter).collect(Collectors.toList());

        // When / then
        assertIterableEquals(expectedList, stateMachineDefinition.getStates().stream().sorted(sorter).collect(Collectors.toList()));
    }

    @Test
    public void shouldReturnEmptySetOfTargetStatesForTerminalState() {
        // TestState.END has no outgoing transitions in the mock matrix, so it is
        // never a key in the matrix map. This should not throw a NullPointerException.
        assertTrue(stateMachineDefinition.getTargetStatesFromState(TestState.END).isEmpty());
    }

    @Test
    public void shouldReturnTrueWhenTargetStateIsReachableFromCurrentState() {
        // When / then
        assertTrue(stateMachineDefinition.isTargetStateViable(TestState.START, TestState.MIDDLE));
    }

    @Test
    public void shouldReturnFalseWhenTargetStateIsNotReachableFromCurrentState() {
        // When / then
        assertFalse(stateMachineDefinition.isTargetStateViable(TestState.START, TestState.END));
    }
}
