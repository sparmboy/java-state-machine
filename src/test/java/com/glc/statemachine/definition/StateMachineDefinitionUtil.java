package com.glc.statemachine.definition;

import com.glc.statemachine.ActionContext;
import com.glc.statemachine.StateMachineEventFromAndTo;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.TransitionEvaluator;
import com.glc.statemachine.definition.testcase.TestCase;
import com.glc.statemachine.definition.testcase.TestState;
import com.glc.statemachine.definition.testcase.TestStateMachineEvent;
import com.glc.statemachine.impl.DefaultTransitionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for creating state machine definitions in tests
 */
@Slf4j
public class StateMachineDefinitionUtil {

    public static com.glc.statemachine.definition.StateMachineDefinition<TestCase> mockStateMachine() {
        return new com.glc.statemachine.definition.StateMachineDefinition<>(
            Arrays.asList(
                new StateMachineEventFromAndTo<>(TestStateMachineEvent.BEGIN,TestState.START,TestState.MIDDLE),
                new StateMachineEventFromAndTo<>(TestStateMachineEvent.STOP,TestState.MIDDLE,TestState.END)
            )
        );
    }

    public final static String NAME_PARAM = "NAME";

    public static StateMachineDefinition<TestCase> mockComplexStateMachine() {
        return new StateMachineDefinitionBuilder<TestCase>()
           .withTransition(
                TestStateMachineEvent.BEGIN,
                TestState.START,
                TestState.MIDDLE,
                new TransitionEvaluator<TestCase>() {
                    @Override
                    public String getDescription() {
                        return null;
                    }

                    @Override
                    public boolean evaluate(ActionContext<TestCase> context) {
                        return context.getParams().isPresent() && context.getParams().get().get(NAME_PARAM) != null;
                    }
                },
               Collections.singletonList(
                   new TransitionAction<TestCase>() {
                       @Override
                       public String getName() {
                           return "setName";
                       }

                       @Override
                       public String getDescription() {
                           return "Sets the name of the entity based on the passed in parameter value";
                       }

                       @Override
                       public void execute(ActionContext<TestCase> actionContext) {
                           TestCase testCase = actionContext.getEntity();
                           testCase.setName(actionContext.getParams().orElse(new HashMap<>()).get(NAME_PARAM).toString());
                       }
                   }
               ))

            .withTransition(
                TestStateMachineEvent.BEGIN,
                TestState.START,
                TestState.MIDDLE,
                new TransitionEvaluator<TestCase>() {
                    @Override
                    public String getDescription() {
                        return null;
                    }

                    @Override
                    public boolean evaluate(ActionContext<TestCase> context) {
                        return !context.getParams().isPresent();
                    }
                },
                Collections.singletonList(
                    new TransitionAction<TestCase>() {
                        @Override
                        public String getName() {
                            return "log";
                        }

                        @Override
                        public String getDescription() {
                            return "prints a log message";
                        }

                        @Override
                        public void execute(ActionContext<TestCase> actionContext) {
                            log.info("Hello");
                        }
                    }
                ))
            .withTransition(TestStateMachineEvent.STOP,TestState.MIDDLE,TestState.END,Arrays.asList(
                new DefaultTransitionAction<>(String.format("%s_%s_%s action",TestState.MIDDLE.getStateName(),TestStateMachineEvent.STOP.getEventName(),TestState.END.getStateName())),
                new DefaultTransitionAction<>(String.format("%s_%s_%s additional action",TestState.MIDDLE.getStateName(),TestStateMachineEvent.STOP.getEventName(),TestState.END.getStateName()))
            ))

            .build();
    }
}
