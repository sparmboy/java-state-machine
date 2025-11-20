package com.glc.statemachine.serializers;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.definition.StateMachineEventTransitionEvaluations;
import com.glc.statemachine.definition.TransitionEvaluationActions;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class StateMachineEventTransitionEvaluationsSerializerTest {

    @Test
    public void shouldRenderAsMap() throws JsonProcessingException {
        String expectedJson = "{\"BEGIN\":[]}";
        StateMachineEventTransitionEvaluations stateMachineEventTransitionEvaluations = new StateMachineEventTransitionEvaluations(
            new HashMap<StateMachineEvent, List<TransitionEvaluationActions>>(){{
                put(TestStateMachineEvent.BEGIN, emptyList());
            }}
        );

        // When
        String json = new ObjectMapper().writeValueAsString(stateMachineEventTransitionEvaluations);

        // Then
        assertEquals(expectedJson,json);
    }
}
