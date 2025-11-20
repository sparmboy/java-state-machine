package com.glc.statemachine.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.glc.statemachine.definition.StateMachineEventTransitionEvaluations;
import java.io.IOException;

public class EventTransitionEvaluationsSerializer extends JsonSerializer<StateMachineEventTransitionEvaluations> {
    @Override
    public void serialize(
        StateMachineEventTransitionEvaluations stateMachineEventTransitionEvaluations,
        JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
        if (stateMachineEventTransitionEvaluations.getTransitionEvaluationActions().isPresent()) {
            jsonGenerator.writeObject(stateMachineEventTransitionEvaluations.getTransitionEvaluationActions().get());
        }
    }
}
