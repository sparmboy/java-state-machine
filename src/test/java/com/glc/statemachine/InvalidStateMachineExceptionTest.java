package com.glc.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InvalidStateMachineExceptionTest {

    @Test
    public void shouldRetainMessageOnlyWhenNoCauseSupplied() {
        InvalidStateMachineException exception = new InvalidStateMachineException("failure message");

        assertEquals("failure message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void shouldPreserveCauseWhenSupplied() {
        RuntimeException cause = new RuntimeException("root cause");

        InvalidStateMachineException exception = new InvalidStateMachineException("failure message", cause);

        assertEquals("failure message", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
