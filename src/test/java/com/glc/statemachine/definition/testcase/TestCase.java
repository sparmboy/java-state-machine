package com.glc.statemachine.definition.testcase;

import com.glc.statemachine.State;
import com.glc.statemachine.StatefulEntity;
import lombok.Getter;
import lombok.Setter;

public class TestCase implements StatefulEntity {
    @Getter
    @Setter
    State state;

    @Getter
    @Setter
    String name = "A";

    @Getter
    @Setter
    String id = "A";

    public TestCase() {
        state = TestState.START;
    }

    public TestCase(State state) {
        this.state = state;
    }
}
