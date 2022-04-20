package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.*
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {
    private lateinit var database: RemindersDatabase
    private lateinit var repositoryTest: RemindersLocalRepository


    @Before
    fun initRepository() {
        database = Room.inMemoryDatabaseBuilder(
                getApplicationContext(),
        RemindersDatabase::class.java
        ).build()
        repositoryTest = RemindersLocalRepository(database.reminderDao(),Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    //    TODO: test the displayed data on the UI.
    @Test
    fun activeReminderList_displayUI() = runBlockingTest{
        //Given a list of reminders
        val reminder1 = ReminderDTO("Title1", "Description1","Location1",0.0,0.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2",0.1,0.1)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3",0.3,0.3)

        repositoryTest.saveReminder(reminder1)
        repositoryTest.saveReminder(reminder2)
        repositoryTest.saveReminder(reminder3)

        //When the list of reminders get displayed on the screen
        //val bundle = ReminderListFragment().toBundle()
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
    }


//    TODO: test the navigation of the fragments.
@Test
fun clickAddReminderFAButton_navigateToSaveReminderFragment() {
    //Given on ReminderList screen
    val scenario = launchFragmentInContainer<ReminderListFragment> (Bundle(), R.style.AppTheme)
    val navController = mock(NavController::class.java)
    scenario.onFragment {
        Navigation.setViewNavController(it.view!!, navController)
    }

    //When - Click on '+' button
    onView(withId(R.id.addReminderFAB)).perform(click())

    //THEN - Verify that we navigate to the save reminder screen
    verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder()
    )
}

//    TODO: add testing for the error messages.
}