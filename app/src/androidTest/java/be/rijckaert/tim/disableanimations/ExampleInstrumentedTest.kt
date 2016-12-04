package be.rijckaert.tim.disableanimations

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test, which will execute on an Android device.

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule var betterActivityTestRule: BetterActivityTestRule<*> = BetterActivityTestRule(MainActivity::class.java)

    @Test
    @Throws(Exception::class)
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()

        val softKeyboardButton = betterActivityTestRule.activity.findViewById(R.id.soft_keyboard_button)
        softKeyboardButton.callOnClick()

        val softKeyboardEditText = betterActivityTestRule.activity.findViewById(R.id.soft_keyboard_edittext)
        softKeyboardEditText.callOnClick()

        betterActivityTestRule.setClock("0730")
        betterActivityTestRule.setBatteryLevel(100, false)
        betterActivityTestRule.setWifiLevel(BetterActivityTestRule.WifiLevel.LEVEL_3)
        betterActivityTestRule.hideNotifications(true)

        assertEquals("be.rijckaert.tim.disableanimations", appContext.packageName)
    }
}
