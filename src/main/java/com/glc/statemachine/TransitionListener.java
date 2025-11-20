package com.glc.statemachine;

/**
 * Interface used to define concrete implementations of transition listeners for transition events
 */
public interface TransitionListener<T extends StatefulEntity> {
    void onTransition(Transition<T> transition, ActionContext<T> context);
}
