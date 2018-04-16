package io.tesseractgroup.reactor

import io.tesseractgroup.messagerouter.MessageRouter
import io.tesseractgroup.purestatemachine.PureStateMachine

/**
 *  Created by matt on 11/1/17.
 */

interface Event
interface Command

class Core<State>(
    initialState: State,
    private val commandProcessors: List<((Core<State>, Command) -> Unit)>,
    handler: (State, Event) -> Pair<State, Command>) {

    val stateChanged = MessageRouter<State>()
    var currentState: State
        get() = stateMachine.currentState
        set(value) { /* no setter */ }

    private val stateMachine = PureStateMachine(initialState, handler)

    fun fire(event: Event) {
        val command = stateMachine.handleEvent(event)
        stateChanged.send(stateMachine.currentState)
        commandProcessors.forEach { it(this, command) }
    }
}
