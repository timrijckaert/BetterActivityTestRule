package be.rijckaert.tim.disableanimations

import android.content.res.Configuration
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.linkedin.android.testbutler.TestButler
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleButlerTest {
    @Before
    fun setupClass() {
        TestButler.verifyAnimationsDisabled(InstrumentationRegistry.getContext())
        TestButler.setRotation(Configuration.ORIENTATION_LANDSCAPE)
    }

    @Test
    fun test() {

    }

    @After
    fun teardownClass() {
    }
}