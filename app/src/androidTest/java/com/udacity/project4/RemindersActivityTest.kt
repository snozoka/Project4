package com.udacity.project4

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


//    Add End to End testing to the app
    @Test
    fun editReminder() = runBlocking {
        // Set initial state.
        repository.saveReminder(ReminderDTO("TITLE1", "DESCRIPTION", "Location1",0.0,0.0))

        // Start up Reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)


        // Click on the save reminder button, add reminder, and save.
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("NEW TITLE"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("NEW DESCRIPTION"))
        onView(withId(R.id.selectedLocation)).perform(replaceText("NEW LOCATION"))
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
        .check(matches(withText(R.string.err_enter_title)))
        onView(withId(com.google.android.material.R.id.snackbar_text))
        .check(matches(withText(R.string.err_select_location)))


//        onView(withText(R.string.reminder_saved)).inRoot(
//            withDecorView(
//                not(`is`(getActivity()?.window?.decorView!!))
//            )
//        ).check(matches(isDisplayed()))

        // Verify task is displayed on screen in the reminder list.
        onView(withText("NEW TITLE")).check(matches(isDisplayed()))
        // Verify previous reminder is not displayed.
        onView(withText("TITLE1")).check(doesNotExist())


        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

//    @Test
//    fun createOneTask_deleteTask() {
//
//        // start up Tasks screen
//        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
//        dataBindingIdlingResource.monitorActivity(activityScenario)
//
//        // Add active task
//        onView(withId(R.id.add_task_fab)).perform(click())
//        onView(withId(R.id.add_task_title_edit_text))
//            .perform(typeText("TITLE1"), closeSoftKeyboard())
//        onView(withId(R.id.add_task_description_edit_text)).perform(typeText("DESCRIPTION"))
//        onView(withId(R.id.save_task_fab)).perform(click())
//
//        // Open it in details view
//        onView(withText("TITLE1")).perform(click())
//        // Click delete task in menu
//        onView(withId(R.id.menu_delete)).perform(click())
//
//        // Verify it was deleted
//        onView(withId(R.id.menu_filter)).perform(click())
//        onView(withText(R.string.nav_all)).perform(click())
//        onView(withText("TITLE1")).check(doesNotExist())
//        // Make sure the activity is closed before resetting the db:
//        activityScenario.close()
//    }

    @Test
    fun testNavigationToRemindersListScreen() {
        // Create a mock NavController
        val mockNavController = mock(NavController::class.java)

        // Create a graphical FragmentScenario for the TitleScreen
        val titleScenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        // Set the NavController property on the fragment
        titleScenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        // Verify that performing a click prompts the correct Navigation action
        onView(withId(R.id.saveReminder)).perform(click())
        verify(mockNavController).navigate(R.id.action_saveReminderFragment_to_reminderListFragment)
        //verify(mockNavController).navigate(R.id.action_saveReminderFragment_to_selectLocationFragment)
    }

}
