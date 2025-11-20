package com.glc.statemachine.definition;

import static com.glc.statemachine.definition.StateMachineDefinitionUtil.mockStateMachine;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glc.statemachine.ActionContext;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.State;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.definition.testcase.TestState;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class StateMachineDefinitionTest {

    private final StateMachineDefinition<TestCase> stateMachineDefinition = mockStateMachine();

    @Test
    public void shouldGenerateSchema() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(stateMachineDefinition));
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
}
