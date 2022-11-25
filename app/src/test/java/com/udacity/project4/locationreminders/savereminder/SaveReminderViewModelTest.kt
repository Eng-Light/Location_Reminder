package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.rule.MainCoroutineRule

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //provide testing to the SaveReminderViewModel and its live data objects

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    // Use a fake repository to be injected into the viewModel
    private lateinit var fakeRepo: FakeDataSource

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    //initializing the fake repo
    @Before
    fun setupSaveReminderViewModel() {
        stopKoin()
        fakeRepo = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeRepo
        )
        //reset fakeDataBase
        runBlocking { fakeRepo.deleteAllReminders() }
    }

    private fun getInvalidReminder(): ReminderDataItem {
        return ReminderDataItem(
            title = "",
            description = "description",
            location = "Cairo",
            latitude = 30.033333,
            longitude = 31.233334
        )
    }

    @Test
    fun shouldReturnError() {
        saveReminderViewModel.validateAndSaveReminder(getInvalidReminder())
        MatcherAssert.assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            CoreMatchers.notNullValue()
        )
    }

}