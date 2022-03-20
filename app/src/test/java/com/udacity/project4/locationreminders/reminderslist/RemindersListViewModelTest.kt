package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel: RemindersListViewModel
    //Add new test coroutine dispatcher to avoid error with dispatcher.main, including for viewModelScope
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    private lateinit var remindersRepository: FakeDataSource
    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    private val observableTasks = MutableLiveData<Result<List<ReminderDTO>>>()

    @Before
    fun setupViewModel() {
        // We initialise the reminders to 3, with one active and two completed
        remindersRepository = FakeDataSource()
        val reminder1 = ReminderDTO("Title1", "Description1","Location1",0.0,0.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2",0.1,0.1)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3",0.3,0.3)
        addReminders(reminder1, reminder2, reminder3)

        remindersListViewModel = RemindersListViewModel(app = Application(), remindersRepository)

    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
        //runBlocking { refreshTasks() }
    }

    //Test the snackbar and toast messages
//    @Test
//    fun completeReminder_dataAndSnackbarUpdated() {
//        // Create an active task and add it to the repository.
//        val reminder = ReminderDTO("Title", "Description","Location1",0.0,0.0)
//        remindersRepository.addTasks(reminder)
//
//        // Mark the task as complete task.
//        tasksViewModel.completeTask(task, true)
//
//        // Verify the task is completed.
//        assertThat(tasksRepository.tasksServiceData[task.id]?.isCompleted, `is`(true))
//
//        // Assert that the snackbar has been updated with the correct text.
//        val snackbarText: Event<Int> =  tasksViewModel.snackbarText.getOrAwaitValue()
//        assertThat(snackbarText.getContentIfNotHandled(), `is`(R.string.task_marked_complete))
//    }

}