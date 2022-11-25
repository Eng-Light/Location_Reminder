package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
/**Implementing FakeDataSource.kt
 *-to help unit test the behavior of the viewModels instead of real database,
 * acts as a database but with less implementation complexity and not really stored in device storage.
 *-track all the error scenarios
 */
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    //  Create a fake data source to act as a double to the real data source
    private var shouldReturnError = false
    fun setShouldReturnError(_shouldReturnError: Boolean) {
        this.shouldReturnError = _shouldReturnError
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        //"Return the reminders"
        return if (shouldReturnError) {
            Result.Error("Reminders not found", 404)
        } else {
            Result.Success(ArrayList(reminders))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        //"save the reminder"
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //"return the reminder with the id"
        return if (shouldReturnError) {
            Result.Error("Error")
        } else {
            val reminder = reminders.find { it.id == id }

            if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("Reminder not found", 404)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        //"delete all the reminders"
        reminders.clear()
    }
}