package io.tesseractgroup.reactor

import io.tesseractgroup.messagerouter.MessageRouter
import io.tesseractgroup.purestatemachine.PureStateMachine
import io.tesseractgroup.purestatemachine.StateUpdate

/**
 *  Created by matt on 11/1/17.
 */

typealias CommandProcessor<State, Event, Command> = (Core<State, Event, Command>, Command) -> Unit
typealias EventHandler<State, Event, Command> = (State, Event) -> StateUpdate<State, Command>

class Core<State : Any, Event : Any, Command : Any>(
    initialState: State,
    private val commandProcessors: List< CommandProcessor<State, Event, Command> >,
    handler: EventHandler<State, Event, Command>) {

    val stateChanged = MessageRouter<State>()
    var currentState: State
        get() = stateMachine.currentState
        private set(value) {}

    private val stateMachine = PureStateMachine<State, Event, Command>(initialState) { state, event ->
        handler(state, event)
    }

    fun fire(event: Event) {
        val commands = stateMachine.handleEvent(event)
        stateChanged.send(currentState)
        for (command in commands){
            commandProcessors.forEach { it(this, command) }
        }
    }

    fun perform(command: Command){
        commandProcessors.forEach { it(this, command) }
    }
}
