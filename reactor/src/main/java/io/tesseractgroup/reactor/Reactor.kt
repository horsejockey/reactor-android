package io.tesseractgroup.reactor

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference

/**
 *  Created by matt on 11/1/17.
 */

interface State {
    fun reactTo(event: Event)
}

interface Event

interface Command<StateType> where StateType : State {
    fun execute(state: StateType, core: Core<StateType>)
}

interface Middleware<StateType> where StateType : State {
    fun process(event: Event, state: StateType)
}

interface Subscriber<in StateType> where StateType : State {
    fun updateWith(state: StateType)
}

class Subscription<StateType>(subscriber: Subscriber<StateType>?) where StateType : State {

    private var weakSubscriber: WeakReference<Subscriber<StateType>?>? = null

    init {
        weakSubscriber = WeakReference(subscriber)
    }

    var subscriber: Subscriber<StateType>? = weakSubscriber?.get()
        private set

    fun notify(state: StateType){
        if (Looper.myLooper() == Looper.getMainLooper()){
            subscriber?.updateWith(state)
        }else{
            // Get a handler that can be used to post to the main thread
            val mainHandler = Handler(Looper.getMainLooper())
//            val runnable = Runnable
            mainHandler.post {
                subscriber?.updateWith(state)
            }
        }
    }
}

class Core<StateType>(state: StateType, private val middlewares: List< Middleware<in StateType> >) where StateType : State {

    var state: StateType
        private set

    private var subscriptions: List< Subscription<StateType> > = listOf()

    init {
        this.state = state
    }

    fun add(subscriber: Subscriber<StateType>){
        if (Looper.myLooper() == Looper.getMainLooper()){
            addOnMainThread(subscriber)
        }else{
            // Get a handler that can be used to post to the main thread
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                addOnMainThread(subscriber)
            }
        }
    }

    private fun addOnMainThread(subscriber: Subscriber<StateType>){
        if (subscriptions.find { it.subscriber === subscriber } != null){
            return
        }
        val subscription = Subscription<StateType>(subscriber)
        subscriptions = subscriptions.plus(subscription)
        subscription.notify(state)
    }

    fun remove(subscriber: Subscriber<StateType>){
        if (Looper.myLooper() == Looper.getMainLooper()){
            removeOnMainThread(subscriber)
        }else{
            // Get a handler that can be used to post to the main thread
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                removeOnMainThread(subscriber)
            }
        }
    }

    private fun removeOnMainThread(subscriber: Subscriber<StateType>){
        subscriptions = subscriptions.filter { it.subscriber !== subscriber }.toMutableList()
    }

    fun fire(event: Event){
        if (Looper.myLooper() == Looper.getMainLooper()){
            fireOnMainThread(event)
        }else{
            // Get a handler that can be used to post to the main thread
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                fireOnMainThread(event)
            }
        }
    }

    private fun fireOnMainThread(event: Event){
        middlewares.forEach { it.process(event, state) }
        state.reactTo(event)
        subscriptions = subscriptions.filter { it.subscriber != null }
        for (subscription in subscriptions){
            subscription.notify(state)
        }
    }

    fun fire(command: Command<StateType>){
        command.execute(state, this)
    }
}

class LoggingMiddleware: Middleware<State>{
    override fun process(event: Event, state: State) {
        Log.d("REACTOR", "Event: ${event} thread: ${Thread.currentThread().name}")
    }

}