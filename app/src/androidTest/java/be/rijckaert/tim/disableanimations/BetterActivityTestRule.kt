package be.rijckaert.tim.disableanimations

import android.annotation.TargetApi
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.rule.ActivityTestRule
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiScrollable
import android.support.test.uiautomator.UiSelector
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import org.junit.runner.Description
import org.junit.runners.model.Statement


/**
 * Created by TimR.
 */
class BetterActivityTestRule<T : Activity>(activityClass: Class<T>) : ActivityTestRule<T>(activityClass) {

    companion object {
        private val ANIMATION_SCALE_PERMISSION = "android.permission.SET_ANIMATION_SCALE"
    }

    lateinit var launchActivity: Activity
    lateinit var instrumentation : Instrumentation
    var device: UiDevice? = null

    override fun apply(userTest: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                instrumentation = getInstrumentation()
                device = UiDevice.getInstance(instrumentation)
                deviceWakeUp()

                grantScalePermission()

                launchActivity = launchActivity(activityIntent)
                installWakeLock()

                runTest(userTest)
            }
        }
    }

    private fun runTest(base: Statement) {
        setInTestMode(true)
        try {
            base.evaluate()
        } finally {
            setInTestMode(false)
            deviceSleep()
        }
    }

    //<editor-fold desc="Helper Function">
    private fun setInTestMode(isInTestMode: Boolean) {
        toggleSoftKeyboard(!isInTestMode)
        toggleAnimations(!isInTestMode)
        setDemoMode(isInTestMode)
    }

    private fun toggleSoftKeyboard(isEnabled: Boolean) {
        //Disable hardware keyboard (useful for emulators)
        //http://www.drewhannay.com/2016/04/how-to-hide-android-soft-keyboard-in.html
        device?.executeShellCommand("settings put secure show_ime_with_hard_keyboard " + booleanToInt(isEnabled))

        //hacky! But Android does not have a good way to check whether the keyboard was shown
        val viewGroup = (launchActivity.findViewById(android.R.id.content) as ViewGroup).getChildAt(0) as ViewGroup
        viewGroup.viewTreeObserver.addOnGlobalLayoutListener {
            val isShowingKeyboard = device?.executeShellCommand("dumpsys window InputMethod")?.contains("mHasSurface=true")
            if (isShowingKeyboard == true) {
                val view = launchActivity.currentFocus
                if (view != null) {
                    val imm = launchActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
    }

    private fun toggleAnimations(isEnabled: Boolean) {
        val permStatus = launchActivity.checkCallingOrSelfPermission(ANIMATION_SCALE_PERMISSION)
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            AnimationType.values().forEach {
                device?.executeShellCommand(it.shellCommand + booleanToInt(isEnabled))
            }
        } else {
            throw RuntimeException("You did not declare the permission")
        }
    }

    private fun booleanToInt(isEnabled: Boolean): Int = if (isEnabled) 1 else 0

    private fun deviceSleep() {
        device?.sleep()
    }

    private fun deviceWakeUp() {
        device?.let {
            if (it.isScreenOn) {
                it.wakeUp()
            }
        }
    }

    /**
     * Bypasses the activity to run on top of the lockscreen
     */
    private fun installWakeLock() {
        this.runOnUiThread {
            launchActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        }
    }

    /**
     *
     * You should define the following permission in your AndroidManifest.xml
     * <uses-permission android:name="android.permission.SET_ANIMATION_SCALE"/>
     * Be sure you exclude this permission in release builds
     *
     * Either via gradle script or using different AndroidManifest files
     * for different flavors
     *
     */
    private fun grantScalePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device?.executeShellCommand("pm grant ${launchActivity.packageName} $ANIMATION_SCALE_PERMISSION")
            return
        }

        grantScalePermissionCompat()
    }

    private fun grantScalePermissionCompat() {
        openDevelopersOptionsMenu()
        var settingRecyclerView: UiScrollable = UiScrollable(UiSelector()
                .className(ClassName.RECYCLER_VIEW.className)
                .resourceId("com.android.settings:id/list"))

        AnimationScaleType.values().forEach {
            settingRecyclerView?.getChildByText(
                    UiSelector()
                            .className(ClassName.TEXT_VIEW.className), it.transitionName).clickAndWaitForNewWindow()

            val list = UiScrollable(UiSelector()
                    .className(ClassName.LIST_VIEW.className)
                    .resourceId("android:id/select_dialog_listview"))

            list.getChild(UiSelector().index(getIndexForToggle(false))).click()
        }
    }

    private fun openDevelopersOptionsMenu() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        instrumentation.context.startActivity(intent)
    }

    private fun getIndexForToggle(enabled: Boolean): Int = if (enabled) 2 else 0

    enum class AnimationType(val shellCommand: String) {
        WINDOW_ANIMATION_SCALE("settings put global window_animation_scale "),
        TRANSITION_ANIMATION_SCALE("settings put global transition_animation_scale "),
        ANIMATOR_DURATION_SCALE("settings put global animator_duration_scale ")
    }
    //</editor-fold>

    //<editor-fold desc="Demo Mode">
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setDemoMode(inDemoMode: Boolean) {
        val currentApiVersion = Build.VERSION.SDK_INT
        if (currentApiVersion >= Build.VERSION_CODES.M) {
            device?.executeShellCommand("settings put global sysui_demo_allowed" + booleanToInt(inDemoMode))

            val command = "am broadcast -a com.android.systemui.demo -e command" + if (inDemoMode) "enter" else "exit"
            device?.executeShellCommand(command)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setClock(hhmm: String = "0700") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device?.executeShellCommand("am broadcast -a com.android.systemui.demo -e command clock -e hhmm $hhmm")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setWifiLevel(connectivityLevel: ConnectivityLevel = ConnectivityLevel.LEVEL_4) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device?.executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level ${connectivityLevel.level}")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun hideNotifications(isNotificationVisible: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device?.executeShellCommand("am broadcast -a com.android.systemui.demo -e command notifications -e visible $isNotificationVisible")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setBatteryLevel(batterLevelPercentage: Int, isPlugged: Boolean = true) {
        if (batterLevelPercentage > 100 || batterLevelPercentage < 0) {
            throw IllegalArgumentException("Dude, seriously?!")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device?.executeShellCommand("am broadcast -a com.android.systemui.demo -e command battery -e batterLevelPercentage $batterLevelPercentage -e plugged $isPlugged")
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setNetworkStatus(connectivityLevel: ConnectivityLevel = ConnectivityLevel.LEVEL_4, datatype: DataType = DataType.LTE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device?.executeShellCommand("adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level ${connectivityLevel.level} -e datatype ${datatype.dataType}")
        }
    }

    enum class DataType(val dataType: String) {
        NONE("none"),
        GEN_1("1x"),
        GEN_3("3g"),
        GEN_4("4g"),
        EDGE("e"),
        GPRS("g"),
        HSDPA("h"),
        LTE("lte"),
        ROAMING("roam")
    }

    enum class ConnectivityLevel(val level: String) {
        LEVEL_1("1"),
        LEVEL_2("2"),
        LEVEL_3("3"),
        LEVEL_4("4")
    }

    enum class ClassName(val className: String) {
        RECYCLER_VIEW("android.support.v7.widget.RecyclerView"),
        LIST_VIEW("android.widget.ListView"),
        TEXT_VIEW("android.widget.TextView"),
    }

    enum class AnimationScaleType(val transitionName: String) {
        WINDOW_ANIMATION_SCALE("Window animation scale"),
        TRANSITION_ANIMATION_SCALE("Transition animation scale"),
        ANIMATOR_DURATION_SCALE("Animator duration scale")
    }
    //</editor-fold>
}