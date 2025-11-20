package com.glc.statemachine.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glc.statemachine.StatefulEntity;
import com.glc.statemachine.TransitionAction;
import com.glc.statemachine.TransitionEvaluator;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * Defines the contents of an externally defined statemachine definition
 * manifest file
 */
@Getter
public class StateMachineManifest<T extends StatefulEntity> {

    @JsonProperty("definition")
    String definition;

    @JsonProperty("entityClassType")
    Class<T> entityClassType;

    @JsonProperty("transitionActions")
    Map<String, Class<TransitionAction<T>>> transitionActions;

    @JsonProperty("transitionEvaluators")
    Map<String, Class<TransitionEvaluator<T>>> transitionEvaluators;

    @JsonProperty("defaultPath")
    List<String> defaultPath;
}
