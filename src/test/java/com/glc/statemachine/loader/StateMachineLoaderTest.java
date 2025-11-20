package com.glc.statemachine.loader;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.InvalidStateMachineException;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.TransitionManager;
import com.glc.statemachine.definition.StateMachineDefinition;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.impl.DefaultState;
import com.glc.statemachine.impl.DefaultTransitionManager;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"unchecked", "rawtypes"}) // As we are loading dynamically at runtime, we can't specify the generic type as types can only be specified at compile time
class StateMachineLoaderTest {

    TransitionManager<TestCase> transitionManager = new DefaultTransitionManager<TestCase>() {
        @Override
        protected void persistEntity(ActionContext<TestCase> actionContext) {

        }
    };

    @Test
    public void shouldLoadExternalStatemachineAndTransition() throws IOException, CsvValidationException, InstantiationException, IllegalAccessException {
        StateMachineDefinition stateMachineDefinition = new com.glc.statemachine.loader.StateMachineLoader(
            new FileInputStream("src/test/resources/manifest.json")
        ).load();

        assertNotNull(stateMachineDefinition);

        assertEquals(Arrays.asList(new DefaultState("Start"), new DefaultState("Middle"), new DefaultState("End")), stateMachineDefinition.getStatesForPath(StateMachineDefinition.DEFAULT_PATH));

        TestCase testCase = new TestCase();
        testCase.setState(new DefaultState("Start"));

        // On Start -> Event1 -> Middle
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event1", "Middle", Collections.singletonList("assistant"));

        // On Middle -> Event2 (name=A) -> Start
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event2", "Start");

        // On Start -> Event1 -> Middle
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event1", "Middle", Collections.singletonList("assistant"));

        // On Middle -> Event2 (name=B) -> End
        testCase.setName("B");
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event2", "End");
    }

    @Test
    public void shouldLoadExternalStatemachineAndTransitionWithActionOverride() throws IOException, CsvValidationException, InstantiationException, IllegalAccessException {
        StateMachineDefinition stateMachineDefinition = new com.glc.statemachine.loader.StateMachineLoader(
            new FileInputStream("src/test/resources/manifest.json")
        ).load(Collections.singletonList(new ToStateActionOverrideDTO(new DefaultState("End"), new TransitionAction<TestCase>() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void execute(ActionContext<TestCase> actionContext) {
                TestCase testCase = actionContext.getEntity();
                testCase.setName("C");
            }
        })));

        assertNotNull(stateMachineDefinition);

        assertEquals(Arrays.asList(new DefaultState("Start"), new DefaultState("Middle"), new DefaultState("End")), stateMachineDefinition.getStatesForPath(StateMachineDefinition.DEFAULT_PATH));

        TestCase testCase = new TestCase();
        testCase.setState(new DefaultState("Start"));

        // On Start -> Event1 -> Middle
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event1", "Middle", Collections.singletonList("assistant"));

        // On Middle -> Event2 (name=A) -> Start
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event2", "Start");

        // On Start -> Event1 -> Middle
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event1", "Middle", Collections.singletonList("assistant"));

        // On Middle -> Event2 (name=B) -> End
        testCase.setName("B");
        assertTransitionOnEvent(testCase, stateMachineDefinition, "Event2", "End");

        assertEquals("C", testCase.getName());
    }

    @Test
    public void shouldFailToLoadMissingStateMachineDefinitionFile() {
        Assertions.assertEquals(
            "Could not find state machine definition file 'definitions/missing-state-machine-definition.csv'",
            Assertions.assertThrows(FileNotFoundException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_statemachine_def.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForInvalidState() {
        Assertions.assertEquals(
            "Target state 'INVALID' is invalid as it is not defined in the state machine definition matrix",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_invalid_state.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForMissingOpeningBraceOnAuth() {
        Assertions.assertEquals(
            "Event 'Event1assistant]' appears to be invalid as it is missing an opening brace for the authorisation definition",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_opening_brace_on_auth.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForMissingClosingBraceOnAuth() {
        Assertions.assertEquals(
            "Event 'Event1[assistant' appears to be invalid as it is missing a closing brace for the authorisation definition",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_closing_brace_on_auth.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForMissingOpeningBraceOnTransition() {
        Assertions.assertEquals(
            "Transition 'TE2/Start][TE3/End]' appears to be invalid as it is missing an opening brace or closing brace",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_opening_brace_on_transition.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForMissingClosingBraceOnTransition() {
        Assertions.assertEquals(
            "Transition 'TE2/Start][TE3/End]' appears to be invalid as it is missing an opening brace or closing brace",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_closing_brace_on_transition.json")).load()).getMessage()
        );
    }


    @Test
    public void shouldFailToLoadMissingTransitionAction() {
        Assertions.assertEquals(
            "com.glc.statemachine.InvalidStateMachineException: Unable to find reference to transition action 'TA69' in manifest file",
            Assertions.assertThrows(RuntimeException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_action.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadTransitionActionForMissingClass() {
        Assertions.assertEquals(
            "Failed to load class defined in manifest: Cannot construct instance of `java.lang.Class`, problem: com.myorg.statemachine.actions.TestTransitionActionMissing\n" +
                " at [Source: (FileInputStream); line: 4, column: 12] (through reference chain: com.glc.statemachine.loader.StateMachineManifest[\"transitionActions\"]->java.util.LinkedHashMap[\"TA1\"])",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_action_class.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadMissingTransitionEvaluator() {
        Assertions.assertEquals(
            "com.glc.statemachine.InvalidStateMachineException: Unable to find reference to transition evaluator 'TE69' in manifest file",
            Assertions.assertThrows(RuntimeException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_evaluator.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadTransitionEvaluatorForMissingClass() {
        Assertions.assertEquals(
            "Failed to load class defined in manifest: Cannot construct instance of `java.lang.Class`, problem: com.myorg.statemachine.actions.TestTransitionActionMissing\n" +
                " at [Source: (FileInputStream); line: 4, column: 12] (through reference chain: com.glc.statemachine.loader.StateMachineManifest[\"transitionActions\"]->java.util.LinkedHashMap[\"TA1\"])",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_missing_evaluator_class.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadDefaultPathForInvalidStates() {
        Assertions.assertEquals(
            "State 'Pub' in defaultPath is invalid as it is not defined in the state machine definition matrix",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_invalid_path.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForDuplicateStates() {
        Assertions.assertEquals(
            "Duplicate state(s) detected: [Start]",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new com.glc.statemachine.loader.StateMachineLoader(new FileInputStream("src/test/resources/manifest_duplicate_states.json")).load()).getMessage()
        );
    }

    @Test
    public void shouldFailToLoadStateMachineDefinitionFileForDuplicateEvents() {
        Assertions.assertEquals(
            "Duplicate event(s) detected: [Event2]",
            Assertions.assertThrows(InvalidStateMachineException.class, () -> new StateMachineLoader(new FileInputStream("src/test/resources/manifest_duplicate_events.json")).load()).getMessage()
        );
    }


    private void assertTransitionOnEvent(TestCase testCase, StateMachineDefinition<TestCase> stateMachineDefinition, String event, String state) {
        assertTransitionOnEvent(testCase, stateMachineDefinition, event, state, emptyList());
    }

    private void assertTransitionOnEvent(TestCase testCase, StateMachineDefinition<TestCase> stateMachineDefinition, String eventName, String state, List<String> roles) {

        StateMachineEvent event = stateMachineDefinition.getEvents().stream().filter(e -> e.getEventName().equals(eventName)).findFirst()
            .orElseThrow(() -> new RuntimeException("No event with name " + eventName + " defined in state machine"));
        if (event.getRoles().isPresent() && Collections.disjoint(event.getRoles().orElse(emptyList()), roles)) {
            throw new RuntimeException("Unauthorised to trigger event " + event.getEventName());
        }

        // On Start -> Event1 -> Middle
        transitionManager.triggerEvent(
            new ActionContext<>(
                event,
                testCase,
                stateMachineDefinition
            )
        );
        assertEquals(state, testCase.getState().getStateName());
    }
}
