package com.retrorts

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @Test
    fun splash_then_home_shows_launcher_buttons() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withText("RetroRTS")).check(matches(isDisplayed()))
        Thread.sleep(1700)
        onView(withText("Settings")).check(matches(isDisplayed()))
        onView(withText("About")).check(matches(isDisplayed()))
        onView(withText("Add Game")).check(matches(isDisplayed()))
    }

    @Test
    fun open_settings_and_back_to_home() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1700)
        onView(withText("Settings")).perform(click())
        onView(withText("Display Scaling")).check(matches(isDisplayed()))
        onView(withText("Save & Back")).perform(click())
        onView(withText("Add Game")).check(matches(isDisplayed()))
    }

    @Test
    fun open_about_and_back_to_home() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1700)
        onView(withText("About")).perform(click())
        onView(withText("Powered by DOSBox (open source)."))
            .check(matches(isDisplayed()))
        onView(withText("Back")).perform(click())
        onView(withText("Settings")).check(matches(isDisplayed()))
    }
}
