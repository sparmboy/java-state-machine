# State Machine

Library for implementing a state machine model for workflow. The basic implementation works as follows:

1. Define your domain entity that requires a stateful workflow.
2. Define a list of states that your entity can be in at any one time.
3. Define a list of events that can occur within the entity's lifetime.
4. Model which events trigger a change from one state to another.
5. Optionally implement any custom actions that are required when transitioning between states.

State machines can be defined in code, or loaded from a CSV matrix and a JSON manifest
(see [Loading a state machine from CSV](#loading-a-state-machine-from-csv)).

_(The following examples are taken from the test directory)_

### 1. Define a domain entity
Our domain entity is a very simple entity that implements ```StatefulEntity```:
```java
public class TestCase implements StatefulEntity {
    @Getter
    @Setter
    State state;

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
```
This allows the framework to determine what state the entity is currently in and allow it to set the next state.

_(Note, the lombok ```@Getter``` and ```@Setter``` annotations provide the ```getState```, ```setState``` and ```getId``` methods that the ```StatefulEntity``` interface requires)_

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
The only events in our system are ```Begin``` and ```Stop``` so similar to the States, we model these as enums that implement the ```StateMachineEvent``` interface:

````java
import com.glc.statemachine.StateMachineEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum TestStateMachineEvent implements StateMachineEvent {
    BEGIN("Begin"),
    STOP("Stop"),
    ;
    String eventName;
}
````
_(Note, the lombok ```@Getter``` annotation is providing the ```getEventName``` getter that the ```StateMachineEvent``` interface requires)_

### 4. Model the state machine matrix
Our state machine matrix, now looks like this:

| State / Event | BEGIN                                        | STOP                                      |
|---------------|----------------------------------------------|-------------------------------------------|
| START         | Display a message and move to state "MIDDLE" |                                           |
| MIDDLE        |                                              | Display a message and move to state "END" |
| END           |                                              |                                           |

Translated that means:
1. When we are in a state of "START" and we receive a "BEGIN" event, then display a message and move to state "MIDDLE"
2. When we are in a state of "MIDDLE" and we receive a "STOP" event, then display a message and move to state "END"

Events that occur when we are not in these states will have no effect on the state machine.

So to model this, we create a new ```StateMachineDefinition``` like so:

```java
new StateMachineDefinition<TestCase>(
    Arrays.asList(
        new StateMachineEventFromAndTo<>(TestStateMachineEvent.BEGIN, TestState.START, TestState.MIDDLE),
        new StateMachineEventFromAndTo<>(TestStateMachineEvent.STOP, TestState.MIDDLE, TestState.END)
    )
);
```
We supply details of our two transitions (intersections on the matrix) by supplying the triggering event, the state that is the 'from' part of the transition and the 'to' state which is the target state when the event fires.

Now that we have everything modelled, we can see it in action by creating our entity and firing it into the state machine. We can see that in action by the following test:

```java
import static com.glc.statemachine.definition.StateMachineDefinitionUtil.mockStateMachine;
import static org.junit.jupiter.api.Assertions.*;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.TransitionManager;
import com.glc.statemachine.definition.StateMachineDefinition;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.definition.testcase.TestState;
import com.glc.statemachine.impl.DefaultTransitionManager;
import org.junit.jupiter.api.Test;

class DefaultTransitionManagerTest {

    private final StateMachineDefinition<TestCase> stateMachineDefinition = mockStateMachine();

    /**
     * The transition manager is used to manage all transitions on a state machine
     * and update the state in the associated entity. The DefaultTransitionManager
     * only requires an implementation to implement persistence of the entity, the
     * transition of entity between states as per the configured matrix is all
     * handled for you.
     */
    TransitionManager<TestCase> transitionManager = new DefaultTransitionManager<TestCase>() {
        @Override
        protected void persistEntity(ActionContext<TestCase> actionContext) {
            // In a real implementation we would persist our entity
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
            // Action contexts are used to fire events into the state machine.
            // Create an ActionContext with an event, entity and state machine definition
            // then send it to the 'triggerEvent' method in the transition manager
            new ActionContext<>(
                TestStateMachineEvent.BEGIN,
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

`triggerEvent` returns the transition that was performed, or an empty `Optional` if the entity's current state has no
transition for the event. If `persistEntity` throws, the entity's state is rolled back to what it was before the
transition and the exception is rethrown.

## Complex State Machines with Conditional Events and Transition Actions

`StateMachineDefinitionBuilder` adds two things to a transition:

* A `TransitionEvaluator` decides whether the transition applies. When a state has several transitions for the same
  event, they are evaluated in the order they were added, and the first one whose evaluator returns `true` is used. A
  transition without an evaluator always applies.
* `TransitionAction`s run, in order, before the entity's state is changed.

```java
// hasNameParameter is a TransitionEvaluator<TestCase>, setNameFromParameter is a TransitionAction<TestCase>
StateMachineDefinition<TestCase> definition = new StateMachineDefinitionBuilder<TestCase>()
    // BEGIN moves START -> MIDDLE and sets the entity's name, but only when a NAME parameter was supplied...
    .withTransition(TestStateMachineEvent.BEGIN, TestState.START, TestState.MIDDLE,
        hasNameParameter,
        Collections.singletonList(setNameFromParameter))
    // ...otherwise BEGIN moves START -> END
    .withTransition(TestStateMachineEvent.BEGIN, TestState.START, TestState.END)
    .withTransition(TestStateMachineEvent.STOP, TestState.MIDDLE, TestState.END)
    .withDefaultPath(Arrays.asList("Start", "Middle", "End"))
    .build();
```

Evaluators and actions receive the `ActionContext`, which carries the event, the entity, the definition and an optional
map of parameters supplied by the caller:

```java
Map<String, Object> params = new HashMap<>();
params.put("NAME", "bob");
transitionManager.triggerEvent(new ActionContext<>(TestStateMachineEvent.BEGIN, testCase, definition, params));
```

### Transition listeners
A `TransitionListener` is called after every transition, once the entity's state has been updated and before the
entity is persisted. Listeners are supplied when constructing a definition from a list of transitions:

```java
new StateMachineDefinition<>(transitions, defaultPath, Collections.singletonList(myListener));
```

### Restricting events by role
An event can declare the roles allowed to trigger it by overriding `StateMachineEvent.getRoles()` (events loaded from
CSV get them from the column header). `getEventsForState(state, roles)` returns the events available from a state to
someone holding the given roles: events without roles, plus events that share at least one role with them.

Roles are not checked by `triggerEvent` - use `getEventsForState` to decide which events a user may trigger before
calling it.

### Paths
A definition can carry a default path - an ordered list of state names through the machine - for use in
visualisations such as progress bars. Retrieve it with `getStatesForPath(StateMachineDefinition.DEFAULT_PATH)`.

## Loading a state machine from CSV
A state machine can also be defined as a CSV matrix plus a JSON manifest that maps short names to evaluator and action
classes:

```java
StateMachineDefinition<TestCase> definition = (StateMachineDefinition<TestCase>) new StateMachineLoader(
    getClass().getClassLoader().getResourceAsStream("manifest.json")
).load();
```

### The manifest

```json
{
  "definition": "definitions/state-machine-definition.csv",
  "transitionActions": {
    "TA1": "com.myorg.statemachine.actions.TestTransitionAction"
  },
  "transitionEvaluators": {
    "TE1": "com.myorg.statemachine.evaluators.TestTransitionEvaluator",
    "TE2": "com.myorg.statemachine.evaluators.NameIsAEvaluator",
    "TE3": "com.myorg.statemachine.evaluators.NameIsBEvaluator"
  },
  "defaultPath": ["Start", "Middle", "End"]
}
```

* `definition` - classpath location of the CSV matrix.
* `transitionActions` / `transitionEvaluators` - short names used in the CSV, mapped to fully qualified class names.
* `defaultPath` - optional, see [Paths](#paths).

Evaluator and action classes are instantiated with a no-arg constructor. If they need a dependency (a service, a
repository, an application context...), pass it to the loader - `new StateMachineLoader(manifestStream, dependency)` -
and give the class a constructor taking exactly that object's class.

### The CSV matrix

```csv
,Event1[assistant],Event2,Event3
Start,TE1/Middle,,
Middle,,[TE2/Start][TE3/End],
End,,,Start
```

* The first row lists the events. An event can be restricted to roles with `Event[role]`; a header with several roles
  contains commas, so it must be quoted: `"Event1[admin,user]"`.
* Each following row starts with a state, followed by one cell per event. Every state used as a target must also have
  a row. States and events must be unique.
* A cell describes the transition(s) for that state and event. An empty cell means the event has no effect.

| Cell | Meaning |
|------|---------|
| `Middle` | Move to `Middle` |
| `TE1/Middle` | Move to `Middle` if evaluator `TE1` returns `true` |
| `Middle/TA1` | Move to `Middle`, running action `TA1` |
| `TE1/Middle/TA1` | Move to `Middle` if `TE1` returns `true`, running action `TA1` |
| `[TE2/Start][TE3/End]` | Several transitions, evaluated left to right; the first that applies is used |

`load(overrides)` can add an action to every transition into a given state, and `load(overrides, listeners)` also
registers [transition listeners](#transition-listeners).

### Working with a loaded definition
States and events in a loaded definition are `DefaultState` and `DefaultStateMachineEvent` (or
`AuthorisedStateMachineEvent` when the event has roles) instances, compared by name:

* An entity driven by a loaded definition must hold a `DefaultState`, e.g. `entity.setState(State.forValue("Start"))`.
  An enum constant with the same name is not equal to it.
* Take the event to trigger from `definition.getEvents()` or `definition.getEventsForState(...)` rather than
  constructing one, so that events with roles match.

## Installation
Releases are published to GitHub Packages at `https://maven.pkg.github.com/sparmboy/java-state-machine`, which requires
[authenticating with a GitHub token](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
even for public packages. Then add:

```xml
<dependency>
    <groupId>com.glc</groupId>
    <artifactId>java-state-machine</artifactId>
    <version>${java-state-machine.version}</version>
</dependency>
```

The library supports Java 8 and later.
