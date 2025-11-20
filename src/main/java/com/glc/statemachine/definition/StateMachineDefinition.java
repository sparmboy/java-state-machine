package com.glc.statemachine.definition;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.glc.statemachine.ActionContext;
import com.glc.statemachine.InvalidStateMachineException;
import com.glc.statemachine.State;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.StateMachineEventFromAndTo;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.Transition;
import com.glc.statemachine.TransitionListener;
import com.glc.statemachine.impl.DefaultTransition;
import com.glc.statemachine.impl.DefaultTransitionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

/**
 * Defines an instance of a statemachine definition that holds
 * all states and the valid event transitions between states
 */
@Getter
@Slf4j
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class StateMachineDefinition<T extends StatefulEntity> {

    public static final String DEFAULT_PATH = "default";

    /**
     * This is the state machine matrix defining the transitions
     * that can come out of each state
     */
    Map<State, StateMachineEventTransitionEvaluations<T>> matrix;

    /**
     * Defines a set of linear paths through a state machine to aid visualisation
     */
    Map<String, List<State>> paths;

    List<TransitionListener<T>> transitionListeners;

    public Optional<List<TransitionListener<T>>> getTransitionListeners() {
        return Optional.ofNullable(transitionListeners);
    }

    /**
     * Builds a simple state machine from a supplied list of {@link StateMachineEventFromAndTo} objects that define
     * the event, the from state and the to state to transition to. A {@link DefaultTransitionAction} is supplied
     * for each transition that performs no action and simply logs a trace message to affirm the transition.
     *
     * @param simpleMatrix
     */
    public StateMachineDefinition(List<StateMachineEventFromAndTo<T>> simpleMatrix) {
        this(simpleMatrix, null, null);
    }

    public StateMachineDefinition(List<StateMachineEventFromAndTo<T>> simpleMatrix, List<String> defaultPath) {
        this(simpleMatrix, defaultPath, null);
    }

    public StateMachineDefinition(List<StateMachineEventFromAndTo<T>> simpleMatrix, List<String> defaultPath, List<TransitionListener<T>> transitionListeners) {
        this.matrix = buildMatrix(simpleMatrix);
        this.transitionListeners = transitionListeners;
        this.paths = defaultPath == null ? null : validateAndCreatePath(DEFAULT_PATH, defaultPath);
    }

    /**
     * Validates each state in the specified default path is a valid state within this State machine definitions matrix
     *
     * @param pathName  The name of the path to create
     * @param statePath The list of states that define the path through the state machine
     * @return
     */
    private Map<String, List<State>> validateAndCreatePath(String pathName, List<String> statePath) {
        if (matrix == null) {
            throw new RuntimeException("You must create the matrix before attempting to validate a path");
        }
        return new HashMap<String, List<State>>() {
            {
                put(pathName, statePath.stream()
                    .map(stateName -> getStates()
                        .stream()
                        .filter(state -> stateName.equals(state.getStateName()))
                        .findFirst()
                        .orElseThrow(() -> new InvalidStateMachineException("Failed to create path '" + pathName + "' for statemachine. The state name in the supplied path '" + stateName +
                            "' could not be found in the defined states of the associated state machine definition matrix")))
                    .collect(Collectors.toList()));
            }
        };
    }

    /**
     * Creates a new state machine using custom StateMachineEventTransitionEvaluations for each state. This is useful
     * when you may have multiple events, with custom evaluation logic on the transitions between the same
     * from and to states
     *
     * @param matrix
     */
    public StateMachineDefinition(Map<State, StateMachineEventTransitionEvaluations<T>> matrix) {
        this(matrix, null);
    }

    /**
     * Creates a new state machine using custom StateMachineEventTransitionEvaluations for each state. This is useful
     * when you may have multiple events, with custom evaluation logic on the transitions between the same
     * from and to states
     *
     * @param matrix
     */
    public StateMachineDefinition(Map<State, StateMachineEventTransitionEvaluations<T>> matrix, List<String> defaultPath) {
        this.matrix = matrix;
        this.paths = defaultPath == null ? null : validateAndCreatePath(DEFAULT_PATH, defaultPath);
        this.transitionListeners = null;
    }

    private Map<State, StateMachineEventTransitionEvaluations<T>> buildMatrix(List<StateMachineEventFromAndTo<T>> simpleMatrix) {
        Map<State, StateMachineEventTransitionEvaluations<T>> map = new HashMap<>();
        simpleMatrix.forEach(item -> {
            if (map.containsKey(item.getFromState())) {
                StateMachineEventTransitionEvaluations<T> evals = map.get(item.getFromState());
                if (evals.getTransitionEvaluationActions().isPresent()) {
                    List<TransitionEvaluationActions<T>> newList = new ArrayList<>(evals.getTransitionEvaluationActions().get().get(item.getStateMachineEvent()));
                    newList.add(buildTransitionEvaluationActionValue(item));
                    evals.getTransitionEvaluationActions().get().put(item.getStateMachineEvent(), newList);
                } else {
                    map.put(item.getFromState(), buildEventTransitionEvaluations.apply(item));
                }
            } else {
                map.put(item.getFromState(), buildEventTransitionEvaluations.apply(item));
            }
        });
        return map;
    }

    private Function<StateMachineEventFromAndTo<T>, StateMachineEventTransitionEvaluations<T>> buildEventTransitionEvaluations = stateMachineEventFromAndTo ->
        new StateMachineEventTransitionEvaluations<>(
            new HashMap<StateMachineEvent, List<TransitionEvaluationActions<T>>>() {{
                put(stateMachineEventFromAndTo.getStateMachineEvent(), buildTransitionEvaluationActions(stateMachineEventFromAndTo));
            }}
        );

    private List<TransitionEvaluationActions<T>> buildTransitionEvaluationActions(
        StateMachineEventFromAndTo<T> stateMachineEventFromAndTo) {
        return Collections.singletonList(buildTransitionEvaluationActionValue(stateMachineEventFromAndTo));
    }

    private TransitionEvaluationActions<T> buildTransitionEvaluationActionValue(StateMachineEventFromAndTo<T> stateMachineEventFromAndTo) {
        return new TransitionEvaluationActions<>(
            stateMachineEventFromAndTo.getEvaluator().orElse(null),
            new DefaultTransition<>(
                stateMachineEventFromAndTo.getFromState(),
                stateMachineEventFromAndTo.getToState(),
                CollectionUtils.isEmpty(stateMachineEventFromAndTo.getActions()) ?
                    Collections.singletonList(
                        new DefaultTransitionAction<>(
                            String.format("%s->%s->%s action", stateMachineEventFromAndTo.getFromState().getStateName(), stateMachineEventFromAndTo.getStateMachineEvent().getEventName(),
                                stateMachineEventFromAndTo.getToState().getStateName()))
                    ) : stateMachineEventFromAndTo.getActions()
            )
        );
    }


    public Set<State> getStates() {
        return Stream.concat(getFromStates(), getToStates()).collect(Collectors.toSet());
    }

    private Stream<State> getFromStates() {
        return matrix.keySet().stream();
    }

    private Stream<State> getToStates() {
        return matrix.values().stream()
            .map(ete -> ete.getTransitionEvaluationActions().orElse(Collections.emptyMap()))
            .flatMap(map -> map.values().stream())
            .flatMap(actions -> actions.stream().map(tea -> tea.getTransition().getToState(null)))
            ;
    }

    /**
     * The set of events that can cause a transition in this state machine
     */
    public Set<StateMachineEvent> getEvents() {
        return matrix.values().stream()
            .map(ete -> ete.getTransitionEvaluationActions().orElse(Collections.emptyMap()))
            .flatMap(map -> map.keySet().stream())
            .collect(Collectors.toSet());
    }

    /**
     * Returns all available events that will trigger a transition for the specified state
     *
     * @param state
     * @return
     */
    public Set<StateMachineEvent> getEventsForState(State state) {
        return getEventsForState(state, emptyList());
    }

    public Set<StateMachineEvent> getEventsForState(State state, @NotNull List<String> roles) {
        return Optional.ofNullable(matrix.get(state))
            .map(e -> e.getTransitionEvaluationActions().orElse(Collections.emptyMap()).keySet())
            .orElseGet(Collections::emptySet)
            .stream()
            .filter(sme -> {
                if (sme.getRoles().isPresent()) {
                    return !Collections.disjoint(roles, sme.getRoles().orElse(emptyList()));
                } else {
                    return true;
                }
            })
            .collect(Collectors.toSet())
            ;
    }

    public Optional<Map<String, List<State>>> getPaths() {
        return Optional.ofNullable(paths);
    }

    public List<State> getStatesForPath(String path) {
        if (paths != null) {
            return Optional.ofNullable(paths.get(path)).orElse(emptyList());
        } else {
            return emptyList();
        }
    }


    private Optional<StateMachineEventTransitionEvaluations<T>> getEventTransitionEvaluations(State state) {
        return matrix == null ? Optional.empty() : Optional.ofNullable(matrix.get(state));
    }

    /**
     * Returns the first available transition from the matrix given the state and event in the passed in context
     *
     * @param context Context containing the {@link StatefulEntity} and {@link StateMachineEvent}
     * @return The first transition found from the matrix
     */
    @JsonIgnore
    public Optional<Transition<T>> getTransition(ActionContext<T> context) {
        if (log.isTraceEnabled()) {
            log.trace("Looking for transitions from state {} for event {}", context.getEntity().getState().getStateName(), context.getStateMachineEvent().getEventName());
        }

        // Fetch logic evaluators that will determine a transition if a state has multiple transitions for the same 'from' and 'to' state
        Optional<StateMachineEventTransitionEvaluations<T>> eventTransitionEvaluationsOptional = getEventTransitionEvaluations(context.getEntity().getState());
        if (eventTransitionEvaluationsOptional.isPresent()) {
            StateMachineEventTransitionEvaluations<T> stateMachineEventTransitionEvaluations = eventTransitionEvaluationsOptional.get();
            Optional<Map<StateMachineEvent, List<TransitionEvaluationActions<T>>>> eventActionMap = stateMachineEventTransitionEvaluations.getTransitionEvaluationActions();
            if (eventActionMap.isPresent()) {
                // Fetch any evaluators for this event
                List<TransitionEvaluationActions<T>> evaluators = eventActionMap.get().get(context.getStateMachineEvent());
                if (evaluators != null) {
                    // Evaluate in order and return the first actions list for a positive evaluation
                    Optional<TransitionEvaluationActions<T>> actionsOptional = evaluators.stream().filter(e -> evaluate(e, context)).findFirst();
                    if (actionsOptional.isPresent()) {
                        return Optional.ofNullable(actionsOptional.get().getTransition());
                    }
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("No transitions found from state {} for event {}", context.getEntity().getState().getStateName(), context.getStateMachineEvent().getEventName());
        }
        return Optional.empty();
    }

    /**
     * Returns all possible target states that can be transitioned to for the specified state and definition
     *
     * @param state
     * @return
     */
    public Set<State> getTargetStatesFromState(State state) {
        return getMatrix().get(state).getTransitionEvaluationActions()
            .map(actions -> actions.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList()).stream().map(tea -> tea.getTransition().getToState(null)).collect(Collectors.toSet()))
            .orElse(emptySet());
    }

    private boolean evaluate(
        TransitionEvaluationActions<T> evaluationActions,
        ActionContext<T> context) {
        return evaluationActions.getEvaluator().map(transitionEvaluator -> transitionEvaluator.evaluate(context)).orElse(true);
    }

    /**
     * Returns true if it is possible to transition from the currentState to the targetState within this state machine definition
     *
     * @param currentState the current state to check transitioning from
     * @param targetState  the target state of the transactions (and entity is this is one of the pillar entities Counterparty, Agreement, Facility, Product, Syndicate )
     * @return true if the transition is valid otherwise false
     */
    public boolean isTargetStateViable(State currentState, State targetState) {
        return !getTargetStatesFromState(currentState).contains(targetState);
    }
}
