package com.glc.statemachine.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.definition.StateMachineEventTransitionEvaluations;
import java.io.IOException;
import java.util.Map;

public class EventTransitionEvaluationsSerializer extends JsonSerializer<StateMachineEventTransitionEvaluations<?>> {
    @Override
    public void serialize(
        StateMachineEventTransitionEvaluations<?> stateMachineEventTransitionEvaluations,
        JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
        if (!stateMachineEventTransitionEvaluations.getTransitionEvaluationActions().isPresent()) {
            jsonGenerator.writeNull();
            return;
        }
        jsonGenerator.writeStartObject();
        for (Map.Entry<StateMachineEvent, ?> entry : stateMachineEventTransitionEvaluations.getTransitionEvaluationActions().get().entrySet()) {
            jsonGenerator.writeFieldName(NameKeySerializer.keyName(entry.getKey()));
            serializerProvider.defaultSerializeValue(entry.getValue(), jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }
}
