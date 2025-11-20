# State Machine

Library for implementing a state machine model for workflow. The basic implementation works as follows:

1. Define your domain entity that requires a stateful workflow.
2. Define a list of states that your entity can be in at any one time.
3. Define a list of events that can occur within the entities lifetime.
4. Model which events trigger a change from one state to another.
5. Optionally implement any custom actions that are required when transitioning between states.

_(The following examples are take from the test directory)_

### 1. Define a domain entity
Our domain entity is a very simple entity that implements ```StatefulEntity```:
```java
public class TestCase implements StatefulEntity {
    @Getter
    @Setter
    State state;
    public TestCase(){
        state = TestState.START;
    }
    public TestCase(State state){
        this.state = state;
    }
}
```
This allows the framework to determine what state the entity is currently in and allow it to set the next state.

_(Note, the lombok ```@Getter``` annotation is providing the ```getState``` getter that the ```StatefulEntity``` interface requires)_

### 2. Define a list of states
Our entity can only ever be in a state of ```Start```,```Middle``` or ```End``` so we model it in an enumeration that implements ```State```:

```java
import com.glc.statemachine.State;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public enum TestState implements State {
    START("Start"),
    MIDDLE("Middle"),
    END("End");
    String stateName;
}
```
_(Note, the lombok ```@Getter``` annotation is providing the ```getStateName``` getter that the ```State``` interface requires)_

### 3. Define a list of events
The only events in our system are ```Begin``` and ```Stop``` so similar to the States, we model these as enums that implement the ```Event``` interface:

````java
import com.glc.statemachine.StateMachineEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum TestEvent implements StateMachineEvent {
    BEGIN("Begin"),
    STOP("Stop"),
    ;
    String eventName;
}
````
_(Note, the lombok ```@Getter``` annotation is providing the ```getEventName``` getter that the ```Event``` interface requires)_

### 4. Model the state machine matrix
Our state machine matrix, now looks like this:

| State / Event | BEGIN                                                | STOP                                            |   |   |
|---------------|------------------------------------------------------|-------------------------------------------------|---|---|
| START         | Display a message move to state "MIDDLE" |                                                 |   |   |
| MIDDLE        |                                                      | Display a message and move to state "END" |   |   |
| END           |                                                      |                                                 |   |   |

Translated that means:
1. When we are in a state of "START" and we receive a "BEGIN" stateMachineEvent, then display a message and move to state "MIDDLE"
2. When we are in a state of "MIDDLE" and we receive a "STOP" stateMachineEvent, then display a message and move to state "END"

Events that occur when we are not in these states will have no effect on the state machine.

So to model this, we create a new ```StateMachineDefinition``` like so:

```java
new StateMachineDefinition<TestCase>(
    Arrays.asList(
        new EventFromAndTo(TestEvent.BEGIN,TestState.START,TestState.MIDDLE),
        new EventFromAndTo(TestEvent.STOP,TestState.MIDDLE,TestState.END)
    )
);
```
We supply details of our two transitions (intersections on the matrix) by supplying the triggering stateMachineEvent, the state that is the 'from' part of the transition and the 'to' state which is the target state when the stateMachineEvent fires.

Now that we have everything modelled, we can see it in action by creating our entity and firing it into the statemachine. We can see that in action by the following test:

```java
import static com.glc.statemachine.definition.StateMachineDefinitionUtil.mockStateMachine;
import static org.junit.jupiter.api.Assertions.*;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionManager;
import com.glc.statemachine.definition.StateMachineDefinition;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.definition.testcase.TestState;
import org.junit.jupiter.api.Test;

class DefaultTransitionManagerTest {

    private final StateMachineDefinition<TestCase> stateMachineDefinition = mockStateMachine();

    /**
     * The transition manager is used to manage all transitions on a state machine
     * and update the state in the associated entity. The DefaultTransitionManager
     * only requires an implementation to implement persitence of the entity, the 
     * transition of entity between states as per the configired matrix is all
     * handled for you.
     */
    TransitionManager<TestCase> transitionManager = new DefaultTransitionManager() {
        @Override
        public void persistEntity(ActionContext<TestCase> actionContext) {
            // In a real implementation we would persist out entity
            // to a datastore of some sort so that the change in 
            // state is saved
        }
    };

    @Test
    public void shouldUpdateStateToMiddle() {
        // Given
        TestCase testCase = new TestCase(); // Creates a new case in a starting state of 'START'

        // When
        transitionManager.triggerEvent(
            // Action context are used to fire events into the statemachine.
            // Create an ActionContext with an stateMachineEvent, entity and statemachine definition
            // then send it to the 'handleAction' method in the transition manager
            new ActionContext<>(
                TestEvent.BEGIN,
                testCase,
                stateMachineDefinition
            )
        );

        // Then

        // Hey presto! state has been changed as per our matrix
        assertEquals(TestState.MIDDLE, testCase.getState());
    }
}
```

## Complex State Machines with Conditional Events and Transition Actions
