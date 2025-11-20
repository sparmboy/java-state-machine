package com.glc.statemachine;

import lombok.NonNull;

/**
 * Any entity that requires state management via the
 * statemachine must implement this interface
 */
public interface StatefulEntity {

    @NonNull State getState();
    String getId();

    void setState(State state);
}
