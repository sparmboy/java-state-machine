package com.glc.statemachine.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import java.io.IOException;
import lombok.Data;
import org.junit.jupiter.api.Test;

class EventDeserializerTest {
    @Test
    public void shouldDeserializeEvent() throws IOException {
        String json = "{\"stateMachineEvent\":\"Begin\"}";
        CustomObject customEvent = new ObjectMapper().readValue(json,CustomObject.class);
        assertEquals(TestStateMachineEvent.BEGIN.getEventName(),customEvent.getStateMachineEvent().getEventName());
    }

    @Data
    private static class CustomObject {
        StateMachineEvent stateMachineEvent;
    }

}
