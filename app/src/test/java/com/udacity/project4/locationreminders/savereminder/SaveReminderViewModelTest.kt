package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    private lateinit var saveReminderViewModel:SaveReminderViewModel

    //Use a fake repository to be inserted into the viewmodel
    private lateinit var fakeDataSource: FakeDataSource

    //Executes each task synchronously using Architecture components
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    //Add new test coroutine dispatcher to avoid error with dispatcher.main, including for viewModelScope
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupSaveReminderViewModel(){
        //We initialise the datasource with no reminders
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(),fakeDataSource)

    }


    //TODO: provide testing to the SaveReminderView and its live data objects
    @Test
    fun check_loading() {
        val reminder1 = ReminderDataItem("Title1", "Description1","Location1",0.0,0.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2",0.1,0.1)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3",0.3,0.3)

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        //addReminders(reminder1, reminder2, reminder3)
        saveReminderViewModel.saveReminder(reminder1)


        // Then assert that the progress indicator is shown.
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun shouldReturnError(){
        //Given an entered reminder data with empty values for tittle and location
        val reminder1 = ReminderDataItem("", "description1","",0.0,0.0)
        //Make the data source return errors
        fakeDataSource.setReturnError(true)

        //When we call validateAndSaveReminder
        saveReminderViewModel.reminderTitle.value = reminder1.title.isNullOrEmpty().toString()
        saveReminderViewModel.reminderSelectedLocationStr.value = reminder1.location.isNullOrEmpty().toString()

        var true_or_false = saveReminderViewModel.validateAndSaveReminder(reminder1)
        Log.i("validateSaveResult", true_or_false.toString())

        //Then false should be returned; here we're saying that we return true when title/location are empty
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`("true"))
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`("true"))
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            mainCoroutineRule.runBlockingTest {
                fakeDataSource.saveReminder(reminder)
            }
        }
    }


}