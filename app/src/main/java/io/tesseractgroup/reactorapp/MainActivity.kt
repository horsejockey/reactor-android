package io.tesseractgroup.reactorapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import io.tesseractgroup.reactor.Command
import io.tesseractgroup.reactor.Core
import io.tesseractgroup.reactor.Event
import kotlinx.android.synthetic.main.activity_main.*

object App{

    private val handler = fun(state: AppState, event: Event ): Pair<AppState, Command> {
        val updatedState: AppState

        when(event){
            is AppEvent -> {
                when(event){
                    is AppEvent.UpdateName -> {
                        val updatedState = state.copy(name = event.name)
                        return Pair(updatedState, AppCommand.NoOp())
                    }
                }
            }
        }

        return Pair(state, AppCommand.NoOp())

    }


    val core = Core<AppState>(AppState("Hello World"), listOf(), handler)
}

sealed class AppEvent: Event {
    data class UpdateName(val name: String?): AppEvent()
}

sealed class AppCommand: Command {
    class NoOp: AppCommand()
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

        App.core.stateChanged.add(this){ state ->
            textView.text = state.name
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.core.stateChanged.remove(this)
    }
}
