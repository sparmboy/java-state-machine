package com.glc.statemachine.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.glc.statemachine.State;
import com.glc.statemachine.StateMachineEvent;
import java.io.IOException;

/**
 * Writes {@link State} and {@link StateMachineEvent} map keys by name. Enums keep their constant name, matching how
 * Jackson writes enum values; other implementations use their state or event name rather than {@code toString()}.
 */
public class NameKeySerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object key, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeFieldName(keyName(key));
    }

    static String keyName(Object key) {
        if (key instanceof Enum) {
            return ((Enum<?>) key).name();
        }
        if (key instanceof State) {
            return ((State) key).getStateName();
        }
        if (key instanceof StateMachineEvent) {
            return ((StateMachineEvent) key).getEventName();
        }
        return String.valueOf(key);
    }
}
