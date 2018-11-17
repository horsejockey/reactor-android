package io.tesseractgroup.reactorapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import io.tesseractgroup.reactor.Core
import io.tesseractgroup.reactor.CoreUpdate
import io.tesseractgroup.reactor.EventHandler
import kotlinx.android.synthetic.main.activity_main.*

object App{

    private val handler: EventHandler<AppState, Event, Command> = fun(state, event): CoreUpdate<AppState, Command> {
        when(event){
            is AppEvent -> {
                when(event){
                    is AppEvent.UpdateName -> {
                        val updatedState = state.copy(name = event.name)
                        return CoreUpdate.State(updatedState)
                    }
                }
            }
        }

        return CoreUpdate.NoUpdate()
    }


    val core = Core(AppState("Hello World"), listOf(), handler)
}

interface Event
interface Command

sealed class AppEvent: Event {
    data class UpdateName(val name: String?): AppEvent()
}

data class AppState(val name: String?)


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                val string = s.toString()
                App.core.fire(AppEvent.UpdateName(string))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        App.core.stateChanged.add(this){ state: AppState ->
            textView.text = state.name
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.core.stateChanged.remove(this)
    }
}
