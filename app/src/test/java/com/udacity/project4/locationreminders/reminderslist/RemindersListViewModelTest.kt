package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel: RemindersListViewModel

    //Executes each task synchronously using Architecture components
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    //Add new test coroutine dispatcher to avoid error with dispatcher.main, including for viewModelScope
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //Provide testing to the RemindersListViewModel and its live data objects
    private lateinit var fakeDataSource: FakeDataSource
    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    private val observableTasks = MutableLiveData<Result<List<ReminderDTO>>>()


    @Before
    fun setupViewModel() {
        // We initialise the reminders to 3, with one active and two completed
        fakeDataSource = FakeDataSource()
        val reminder1 = ReminderDTO("Title1", "Description1","Location1",0.0,0.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2",0.1,0.1)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3",0.3,0.3)
        addReminders(reminder1, reminder2, reminder3)

        remindersListViewModel = RemindersListViewModel(Application(), fakeDataSource)

    }

    @Test
    fun shouldReturnError(){
        //Given an entered reminder data with empty values for tittle and location
        val reminder1 = ReminderDataItem("", "description1","",0.0,0.0)

        //Make the data source return errors
        fakeDataSource.setReturnError(true)


        //When we try to load data that doesn't exist
        remindersListViewModel.loadReminders()

        //Then a message that the reminder wasn't found should be returned
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Reminder not found"))

    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
        //runBlocking { refreshTasks() }
    }



}