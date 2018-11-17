package io.tesseractgroup.reactor

import io.tesseractgroup.messagerouter.MessageRouter
import io.tesseractgroup.purestatemachine.PureStateMachine

/**
 *  Created by matt on 11/1/17.
 */

typealias CommandProcessor<State, Event, Command> = (Core<State, Event, Command>, Command) -> Unit
typealias EventHandler<State, Event, Command> = (State, Event) -> CoreUpdate<State, Command>

class Core<State : Any, Event : Any, Command : Any>(
    initialState: State,
    private val commandProcessors: List< CommandProcessor<State, Event, Command> >,
    handler: EventHandler<State, Event, Command>) {

    val stateChanged = MessageRouter<State>()
    var currentState: State
        get() = stateMachine.currentState
        private set(value) {}

    private val stateMachine = PureStateMachine<State, Event, CoreUpdate<State, Command>>(initialState) { state, event ->
        val update = handler(state, event)

        Pair(update.state() ?: state, update)
    }

    fun fire(event: Event) {
        val update = stateMachine.handleEvent(event)
        stateChanged.send(stateMachine.currentState)
        for (command in update.commands()){
            commandProcessors.forEach { it(this, command) }
        }
    }

    fun perform(command: Command){
        commandProcessors.forEach { it(this, command) }
    }
}


sealed class CoreUpdate<State, Command> {
    class NoUpdate<State, Command>: CoreUpdate<State, Command>()
    class State<State, Command>(val state: State): CoreUpdate<State, Command>()
    class Commands<State, Command>(val commands: List<Command>): CoreUpdate<State, Command>()
    class StateAndCommands<State, Command>(val state: State, val commands: List<Command>): CoreUpdate<State, Command>()

    fun state(): State? {
        return when(this){
            is NoUpdate -> null
            is CoreUpdate.State -> this.state
            is Commands -> null
            is StateAndCommands -> this.state
        }
    }

    fun commands(): List<Command> {
        return when(this){
            is NoUpdate -> listOf()
            is CoreUpdate.State -> listOf()
            is Commands -> commands
            is StateAndCommands -> commands
        }
    }

    fun <T>mapState(lambda: (State) -> T): CoreUpdate<T, Command> {
        return when(this){
            is NoUpdate -> NoUpdate()
            is CoreUpdate.State -> CoreUpdate.State(lambda(state))
            is Commands -> Commands(commands)
            is StateAndCommands -> CoreUpdate.StateAndCommands(lambda(state), commands)
        }
    }

    fun <T>mapCommand(lambda: (Command) -> T): CoreUpdate<State, T> {
        return when(this){
            is NoUpdate -> NoUpdate()
            is CoreUpdate.State -> CoreUpdate.State(state)
            is Commands -> Commands(commands.map(lambda))
            is StateAndCommands -> CoreUpdate.StateAndCommands(state, commands.map(lambda))
        }
    }
}
