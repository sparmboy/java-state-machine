package com.glc.statemachine.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.impl.DefaultStateMachineEvent;
import java.io.IOException;

public class EventDeserializer extends JsonDeserializer<StateMachineEvent> {

    private static final String EVENT_FIELD_NAME = "event";

    @Override
    public StateMachineEvent deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        if (root.has(EVENT_FIELD_NAME)) {
            return mapper.readValue(root.toString(), DefaultStateMachineEvent.class);
        } else {
            throw new IOException("Unable to deserialize to State as there is no field of type State with the name '" + EVENT_FIELD_NAME + "'");
        }
    }
}
