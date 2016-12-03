# BetterActivityTestRule 
A simple testrule which will disable all animations. It will also help
you to write UI test focused upon taking clean screenshots.

##Usage
You can check out the sample provided in the repository.

```kotlin
@get:Rule var betterActivityTestRule: BetterActivityTestRule<*> = BetterActivityTestRule(MainActivity::class.java)
```

###Demo Mode (API 23+)
```kotlin
betterActivityTestRule.setClock("0730")
betterActivityTestRule.setBatteryLevel(100, false)
betterActivityTestRule.setWifiLevel(BetterActivityTestRule.WifiLevel.LEVEL_3)
betterActivityTestRule.hideNotifications(true)
```