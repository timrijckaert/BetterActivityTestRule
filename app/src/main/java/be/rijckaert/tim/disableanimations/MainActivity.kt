package be.rijckaert.tim.disableanimations

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun showSoftKeyboard(view: View) {
        view.postDelayed({
            val keyboard = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        }, 200)
    }
}
