package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    //Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    //Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        //Inject The Dispatcher.
        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveAndRetrieveReminder() = runBlocking {

        val newReminder = ReminderDTO(
            title = "reminder title",
            description = "description",
            location = "Cairo",
            latitude = 30.033333,
            longitude = 31.233334
        )
        repository.saveReminder(newReminder)

        val result = repository.getReminder(newReminder.id)

        MatcherAssert.assertThat(result is Result.Success, CoreMatchers.`is`(true))
        result as Result.Success

        MatcherAssert.assertThat(result.data.title, CoreMatchers.`is`(newReminder.title))
        MatcherAssert.assertThat(
            result.data.description,
            CoreMatchers.`is`(newReminder.description)
        )
        MatcherAssert.assertThat(result.data.latitude, CoreMatchers.`is`(newReminder.latitude))
        MatcherAssert.assertThat(result.data.longitude, CoreMatchers.`is`(newReminder.longitude))
        MatcherAssert.assertThat(result.data.location, CoreMatchers.`is`(newReminder.location))
    }

}