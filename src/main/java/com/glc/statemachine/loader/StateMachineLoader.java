package com.glc.statemachine.loader;


import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.glc.statemachine.InvalidStateMachineException;
import com.glc.statemachine.State;
import com.glc.statemachine.StateMachineEvent;
import com.glc.statemachine.StateMachineEventFromAndTo;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.TransitionEvaluator;
import com.glc.statemachine.TransitionListener;
import com.glc.statemachine.definition.StateMachineDefinition;
import com.glc.statemachine.impl.AuthorisedStateMachineEvent;
import com.glc.statemachine.impl.DefaultState;
import com.glc.statemachine.impl.DefaultStateMachineEvent;
import com.glc.statemachine.impl.TransitionContainer;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class that will load in a state machine manifest that defines a statemachine matrix, evaluators and action classes and generate
 * a {@link StateMachineDefinition} instance
 */
@SuppressWarnings({"rawtypes", "unchecked"}) // As we are loading dynamically at runtime, we can't specify the generic type as types can only be specified at compile time
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StateMachineLoader {
    InputStream manifestFile;
    Object evaluatorInstantiationParam;

    /**
     * Creates a new instance of the loader with the input stream to a manifest json file
     *
     * @param manifestFile
     */
    public StateMachineLoader(InputStream manifestFile) {
        this(manifestFile, null);
    }

    /**
     * Reads in the manifest file from the defined input stream from the constructor and returns a constructed {@link StateMachineDefinition}
     *
     * @return
     * @throws IOException
     * @throws CsvValidationException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public StateMachineDefinition<? extends StatefulEntity> load() throws IOException, CsvValidationException, InstantiationException, IllegalAccessException {
        return load(null, null);
    }

    /**
     * Reads in the manifest file from the defined input stream from the constructor and returns a constructed {@link StateMachineDefinition}.
     * The list of transition actions specified will be added to all transitions that match the "To" state in the {@link ToStateActionOverrideDTO}
     *
     * @return
     * @throws IOException
     * @throws CsvValidationException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public StateMachineDefinition<? extends StatefulEntity> load(List<ToStateActionOverrideDTO<? extends StatefulEntity>> toStateTransitionAction)
        throws IOException, CsvValidationException, InstantiationException, IllegalAccessException {
        return load(toStateTransitionAction, null);
    }

    /**
     * Reads in the manifest file from the defined input stream from the constructor and returns a constructed {@link StateMachineDefinition}. The list
     * of transition actions specified will be added to all transitions that match the "To" state in the {@link ToStateActionOverrideDTO}
     *
     * @param toStateTransitionAction
     * @param listeners
     * @return
     * @throws IOException
     * @throws CsvValidationException
     * @throws InstantiationException
     */
    public StateMachineDefinition<? extends StatefulEntity> load(
        List<ToStateActionOverrideDTO<? extends StatefulEntity>> toStateTransitionAction,
        List<TransitionListener<? extends StatefulEntity>> listeners) throws IOException, CsvValidationException, InstantiationException {
        // Read in the manifest JSON and the defined state machine CSV as a CSV reader
        StateMachineManifest manifest = null;
        try {
            manifest = new ObjectMapper().readValue(manifestFile, StateMachineManifest.class);
        } catch (ValueInstantiationException e) {
            if (e.getCause().getClass() == ClassNotFoundException.class) {
                throw new InvalidStateMachineException("Failed to load class defined in manifest: " + e.getMessage());
            }
        }

        if (manifest == null) {
            throw new InvalidStateMachineException("Failed to load manifest into json from file: " + manifestFile);
        }

        InputStream stream = StateMachineLoader.class.getClassLoader().getResourceAsStream(manifest.getDefinition());
        if (stream == null) {
            throw new FileNotFoundException("Could not find state machine definition file '" + manifest.getDefinition() + "'");
        }
        Reader definitionCsvFile = new InputStreamReader(stream);

        // Map the CSV to a 2D array matrix
        List<List<String>> records = readCsv(definitionCsvFile);

        // Event names are extracted from position 1 (zero index) of the first row of the CSV
        List<String> eventKeys = records.get(0).subList(1, records.get(0).size());
        List<String> duplicateEvents = getDuplicates(eventKeys);
        if (!CollectionUtils.isEmpty(duplicateEvents)) {
            throw new InvalidStateMachineException("Duplicate event(s) detected: " + duplicateEvents);
        }

        // Check if the event names have an associated set of authorisation roles appended to it, and construct
        // either standard event objects or Authorised event objects
        List<StateMachineEvent> events = eventKeys.stream().map(key -> {
            List<String> roles = extractEventRoles.apply(key);
            if (CollectionUtils.isEmpty(roles)) {
                return new DefaultStateMachineEvent(key);
            } else {
                return new AuthorisedStateMachineEvent(extractEventName.apply(key), roles);
            }
        }).collect(Collectors.toList());

        // The transitions start from row 1 (zero indexed)
        records = records.subList(1, records.size());
        List<String> allStates = records.stream().map(list -> list.get(0)).collect(Collectors.toList());
        List<String> duplicateStates = getDuplicates(allStates);
        if (!CollectionUtils.isEmpty(duplicateStates)) {
            throw new InvalidStateMachineException("Duplicate state(s) detected: " + duplicateStates);
        }

        // Map to a collection of from and to transitions based on the events / states intersections in the CSV matrix
        List<StateMachineEventFromAndTo<? extends StatefulEntity>> simpleMatrix = createMatrix(records, events, manifest, toStateTransitionAction);

        // Validate the loaded matrix
        validate(simpleMatrix, allStates, manifest.getDefaultPath());

        // Construct the StateMachineDefinition along with the default transitions path (if defined)
        return new StateMachineDefinition(simpleMatrix, manifest.getDefaultPath(), listeners);
    }

    /**
     * Validates the matrix is valid by checking for the following discrepancies:
     * - Transition to undefined state
     * - Undefined states in default path
     */
    private void validate(List<StateMachineEventFromAndTo<? extends StatefulEntity>> simpleMatrix, List<String> allStates, List<String> defaultPath) {
        // Check that all target states are valid
        simpleMatrix.stream().map(StateMachineEventFromAndTo::getToState).forEach(state -> {
            if (!allStates.contains(state.getStateName())) {
                throw new InvalidStateMachineException("Target state '" + state.getStateName() + "' is invalid as it is not defined in the state machine definition matrix");
            }
        });

        // Check that the path has valid states
        if (defaultPath != null) {
            defaultPath.forEach(state -> {
                if (!allStates.contains(state)) {
                    throw new InvalidStateMachineException("State '" + state + "' in defaultPath is invalid as it is not defined in the state machine definition matrix");
                }
            });
        }
    }

    /**
     * Returns true if the specified flist has duplicates
     *
     * @param list
     * @return
     */
    private static <T> List<T> getDuplicates(Collection<T> list) {

        final List<T> duplicatedObjects = new ArrayList<T>();
        Set<T> set = new HashSet<T>() {
            @Override
            public boolean add(T e) {
                if (contains(e)) {
                    duplicatedObjects.add(e);
                }
                return super.add(e);
            }
        };
        for (T t : list) {
            set.add(t);
        }
        return duplicatedObjects;
    }

    /**
     * Extracts just the event name from an event column header
     */
    private Function<? super String, String> extractEventName = eventText -> {
        int index = eventText.indexOf("[");
        return index == -1 ? eventText : eventText.substring(0, index).trim();
    };

    /**
     * Extracts a list of authoriation roles from the event column header if defined in a "[role1,role2]" format
     */
    private Function<? super String, List<String>> extractEventRoles = eventText -> {
        int index = eventText.indexOf("[");
        int endIndex = eventText.indexOf("]");

        // No opening brace, but closing brace exists
        if (index == -1 && endIndex > -1) {
            throw new InvalidStateMachineException("Event '" + eventText + "' appears to be invalid as it is missing an opening brace for the authorisation definition");
        }
        // Opening brace, but no closing brace
        if (index > -1 && endIndex == -1) {
            throw new InvalidStateMachineException("Event '" + eventText + "' appears to be invalid as it is missing a closing brace for the authorisation definition");
        }


        return index == -1 || endIndex < index ? emptyList() : Arrays.asList(eventText.substring(index + 1, endIndex).trim().split(","));
    };

    /**
     * Iterates the rows and columns in the supplied records to generate a list of from and to transitions that define
     * the state machine matrix
     *
     * @param records                 The CSV matrix as a 2D array
     * @param events                  List of all created event objects from the event header columns
     * @param manifest                The loaded in manifest that defines the transition evaluator and transition action keys and location
     * @param toStateTransitionAction Optional list of action overrides for certain To states
     * @return
     */
    private List<StateMachineEventFromAndTo<? extends StatefulEntity>> createMatrix(
        List<List<String>> records,
        List<StateMachineEvent> events,
        StateMachineManifest<? extends StatefulEntity> manifest,
        List<ToStateActionOverrideDTO<? extends StatefulEntity>> toStateTransitionAction) {
        List<StateMachineEventFromAndTo<? extends StatefulEntity>> matrix = new ArrayList<>();
        for (List<String> row : records) {
            for (int colIndex = 1; colIndex < row.size(); colIndex++) {
                // For the given cell in the state / event matrix, check if there is a defined transition(s) and return in a collection of TransitionContainer wrapper
                List<TransitionContainer> nextStates = extractTransitionContainer(row.get(colIndex), manifest);

                // Iterate the transitions defined for this state / event cell in the matrix
                for (TransitionContainer nextState : nextStates) {
                    // Extract any optional transition actions for the "to" state of this transition
                    Optional<? extends TransitionAction<? extends StatefulEntity>> actionOverride = getTransitionActionOverride(nextState.getNextState(), toStateTransitionAction);

                    // Construct a from and to wrapper with associated evaluators
                    StateMachineEventFromAndTo fromAndTo = new StateMachineEventFromAndTo(
                        events.get(colIndex - 1),
                        new DefaultState(row.get(0)),
                        nextState.getNextState(),
                        nextState.getEvaluator().orElse(null)
                    );

                    // Add Actions
                    if (nextState.getAction().isPresent()) {
                        fromAndTo.addAction(nextState.getAction().get());
                    }

                    // Add addtional actions
                    actionOverride.ifPresent(fromAndTo::addAction);

                    matrix.add(fromAndTo);
                }
            }
        }

        return matrix;
    }

    /**
     * Given the specified next state, the collection of {@link ToStateActionOverrideDTO} is searched for a matching to state and if found
     * returns the transition action within it
     *
     * @param nextState
     * @param toStateTransitionAction
     * @return
     */
    private Optional<? extends TransitionAction<? extends StatefulEntity>> getTransitionActionOverride(State nextState,
                                                                                                       List<ToStateActionOverrideDTO<? extends StatefulEntity>> toStateTransitionAction) {
        if (!CollectionUtils.isEmpty(toStateTransitionAction)) {
            return toStateTransitionAction.stream()
                .filter(ao -> ao.getToState().getStateName().equals(nextState.getStateName()))
                .map(ToStateActionOverrideDTO::getTransitionAction)
                .findFirst();
        }
        return Optional.empty();
    }


    /**
     * Constructs a collection {@link TransitionContainer} from the passed in text string by splitting it by the tokens.
     * Single transitions do not contain [], where as multiple transitions are wrapper in [][]
     * <p>
     * A transition is defined as:
     * <p>
     * 1. Single - Next state  e.g "COMPLETE"
     * 2. Double - Evaluator / Next State or Next State / Action e.g "COMPLETE/TA1"
     * 3. Triple - Evaluator / Next State / Action e..g "TE1/COMPLEX/TA1"
     * <p>
     * Multple transitions could be defined like this:
     * <p>
     * "[COMPLETED/TA1][TE1/REFER/TA1]"
     *
     * @param transitionConfig
     * @param manifest
     * @return
     */
    private List<TransitionContainer> extractTransitionContainer(String transitionConfig, StateMachineManifest manifest) {
        // Single Transition
        if (!isEmpty(transitionConfig) && !transitionConfig.contains("/") && !transitionConfig.contains("[")) {
            return Collections.singletonList(new TransitionContainer(new DefaultState(transitionConfig)));
        }
        // Single transition with combination of evaluators and / or actions
        else if (!isEmpty(transitionConfig) && transitionConfig.contains("/") && !transitionConfig.contains("[")) {
            List<String> tokens = Arrays.asList(transitionConfig.split("/"));
            return Collections.singletonList(extractTransitionContainerFromTokens(tokens, manifest));
        }
        // Multiple transitions
        else if (!isEmpty(transitionConfig) && transitionConfig.contains("/") && transitionConfig.contains("[")) {
            if (StringUtils.countMatches(transitionConfig, "[") != StringUtils.countMatches(transitionConfig, "]")) {
                throw new InvalidStateMachineException("Transition '" + transitionConfig + "' appears to be invalid as it is missing an opening brace or closing brace");
            }
            transitionConfig = transitionConfig.replaceAll("\\[", "");
            return Arrays.stream(transitionConfig.split("]")).map(tc -> {
                try {
                    return extractTransitionContainerFromTokens(Arrays.asList(tc.split("/")), manifest);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } else if (!isEmpty(transitionConfig) && transitionConfig.contains("/") && transitionConfig.contains("]")) {
            throw new InvalidStateMachineException("Transition '" + transitionConfig + "' appears to be invalid as it is missing an opening brace");
        }
        return emptyList();
    }

    /**
     * Constructs a {@link TransitionContainer} from the passed in list of tokens. Tokens can be in the following format:
     * <p>
     * 1. Single - Next state  e.g "COMPLETE"
     * 2. Double - Evaluator / Next State or Next State / Action e.g "COMPLETE/TA1"
     * 3. Triple - Evaluator / Next State / Action e..g "TE1/COMPLEX/TA1"
     *
     * @param tokens
     * @param manifest
     * @return
     */
    private TransitionContainer extractTransitionContainerFromTokens(List<String> tokens, StateMachineManifest<? extends StatefulEntity> manifest) {
        if (tokens.size() == 2) {
            Class<? extends TransitionEvaluator<? extends StatefulEntity>> evaluatorClass = manifest.getTransitionEvaluators().get(tokens.get(0));
            Class<? extends TransitionAction<? extends StatefulEntity>> actionClass = manifest.getTransitionActions() != null ? manifest.getTransitionActions().get(tokens.get(1)) : null;

            // Check if it is [Evaluator/State]
            if (evaluatorClass != null) {
                TransitionEvaluator<? extends StatefulEntity> evaluator = instantiateEvaluator(evaluatorClass);
                return new TransitionContainer(evaluator, new DefaultState(tokens.get(1)));
            }
            // or [State/Action]
            else if (actionClass != null) {
                TransitionAction<? extends StatefulEntity> action = instantiateAction(actionClass);
                return new TransitionContainer(new DefaultState(tokens.get(0)), action);
            }

            throw new InvalidStateMachineException("Failed to find evaluators, states or actions for tokens " + tokens);
        }
        // Otherwise its [ Evaluator / Next State / Action ]
        else if (tokens.size() == 3) {

            Class<? extends TransitionEvaluator<? extends StatefulEntity>> evaluatorClass = manifest.getTransitionEvaluators().get(tokens.get(0));
            if (evaluatorClass == null) {
                throw new InvalidStateMachineException("Unable to find reference to transition evaluator '" + tokens.get(0) + "' in manifest file");
            }
            TransitionEvaluator<? extends StatefulEntity> evaluator = instantiateEvaluator(evaluatorClass);

            Class<? extends TransitionAction<? extends StatefulEntity>> actionClass = manifest.getTransitionActions().get(tokens.get(2));
            if (actionClass == null) {
                throw new InvalidStateMachineException("Unable to find reference to transition action '" + tokens.get(2) + "' in manifest file");
            }
            TransitionAction<? extends StatefulEntity> action = instantiateAction(actionClass);

            return
                new TransitionContainer(
                    evaluator,
                    new DefaultState(tokens.get(1)),
                    action
                )
                ;
        }
        throw new InvalidStateMachineException("Unable to parse transition config in Transition Container for tokens " + tokens);
    }

    /**
     * Instantiates an instance of the specified TransitionAction class either with an empty constructor or with
     * the instance of the evaluatorInstantiationParam specified in the laoder
     *
     * @param transitionActionClass
     * @return
     */

    private TransitionAction<? extends StatefulEntity> instantiateAction(
        Class<? extends TransitionAction<? extends StatefulEntity>> transitionActionClass
    ) {
        try {
            return transitionActionClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            try {
                return transitionActionClass.getDeclaredConstructor(evaluatorInstantiationParam.getClass()).newInstance(evaluatorInstantiationParam);
            } catch (Exception ex) {
                throw new RuntimeException(
                    "Failed to construct new instance of " + transitionActionClass + " with either no constructor params, or constructor with a param of " + evaluatorInstantiationParam + " (" +
                        evaluatorInstantiationParam.getClass() + "): " + e, e);
            }
        }
    }

    /**
     * Instantiates an instance of the specified TransitionEvaluator class either with an empty constructor or with
     * the instance of the evaluatorInstantiationParam specified in the laoder
     *
     * @param transitionEvaluatorClass
     * @return
     */
    private TransitionEvaluator<? extends StatefulEntity> instantiateEvaluator(@NotNull Class<? extends TransitionEvaluator<? extends StatefulEntity>> transitionEvaluatorClass) {
        try {
            return transitionEvaluatorClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            if (evaluatorInstantiationParam != null) {
                try {
                    return transitionEvaluatorClass.getDeclaredConstructor(evaluatorInstantiationParam.getClass()).newInstance(evaluatorInstantiationParam);
                } catch (Exception ex) {
                    throw new RuntimeException(
                        "Failed to construct new instance of " + transitionEvaluatorClass + " with either no constructor params, or constructor with a param of " + evaluatorInstantiationParam + " (" +
                            evaluatorInstantiationParam.getClass() + "): " + e,
                        e);
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Uses the {@link CSVReader} to map the specified input reader for a CSV source into a 2D String arrays
     *
     * @param definitionCsvFile
     * @return
     * @throws IOException
     * @throws CsvValidationException
     */
    private List<List<String>> readCsv(Reader definitionCsvFile) throws IOException, CsvValidationException {
        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(definitionCsvFile)) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                records.add(Arrays.asList(values));
            }
        }
        return records;
    }
}
