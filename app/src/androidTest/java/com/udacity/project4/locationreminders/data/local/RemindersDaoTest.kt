package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    //Add testing implementation to the RemindersDao.kt
    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        val testReminder = ReminderDTO(
            title = "reminder title",
            description = "description",
            location = "Cairo",
            latitude = 30.033333,
            longitude = 31.233334
        )
        database.reminderDao().saveReminder(testReminder)

        val loaded = database.reminderDao().getReminderById(testReminder.id)

        MatcherAssert.assertThat(loaded as ReminderDTO, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(loaded.id, CoreMatchers.`is`(testReminder.id))
        MatcherAssert.assertThat(loaded.title, CoreMatchers.`is`(testReminder.title))
        MatcherAssert.assertThat(loaded.description, CoreMatchers.`is`(testReminder.description))
        MatcherAssert.assertThat(loaded.latitude, CoreMatchers.`is`(testReminder.latitude))
        MatcherAssert.assertThat(loaded.longitude, CoreMatchers.`is`(testReminder.longitude))
        MatcherAssert.assertThat(loaded.location, CoreMatchers.`is`(testReminder.location))
    }

}