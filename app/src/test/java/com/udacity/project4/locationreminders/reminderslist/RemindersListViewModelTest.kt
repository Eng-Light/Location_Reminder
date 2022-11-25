package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Subject under test
    private lateinit var reminderViewModel: RemindersListViewModel

    // Use a fake repository to be injected to the viewModel
    private lateinit var fakeRepo: FakeDataSource

    // Set the main coroutines dispatcher roles for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    //initializing the fake repo
    @Before
    fun setupRemindersListViewModel() {
        stopKoin()

        fakeRepo = FakeDataSource()
        reminderViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeRepo
        )
    }

}