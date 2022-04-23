package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.local.RemindersDao
import org.junit.Rule

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {
    private var shouldReturnError = false
    //var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    //Add new test coroutine dispatcher to avoid error with dispatcher.main, including for viewModelScope
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Error(
                "Reminder not found"
            )
        }
        reminders?.let { return Success(ArrayList(it)) }
        return Error(
            "Reminder not found"
        )
    }



    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Error(
                "Reminder not found"
            )
        }
        reminders?.find{it.id == id }?.let { return Success(it) }
        //val reminderSelected = reminders?.find{ it.id == id }
        //return Success(reminderSelected)
        return Error(
            "Reminder not found"
        )

    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }


}